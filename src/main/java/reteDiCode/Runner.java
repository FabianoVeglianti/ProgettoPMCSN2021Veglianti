package reteDiCode;

import debug.ArrivalsCounter;
import debug.RoutingMatrix;
import debug.ServiceTime;
import processorSharingSingolo.Event;
import processorSharingSingolo.Server;
import writer.CSVWriter;
import writer.DebugCSVWriter;

import java.util.ArrayList;

import static reteDiCode.SchedulingDisciplineEnum.*;


public class Runner {

    public static final double START = 0.0;
    private static double current;
    private static char[] networkConfigurationCodes = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
    private static double currentCounterLimit = 0;
    private static int counter = 0;

    private static char actualConfiguration;

    private static void initServers(Server[] servers){
        Server serverVM1;
        Server serverS3;
        Server serverVM2CPU;
        Server serverVM2Band;
        if(Params.FIFO) {
            serverVM1 = new Server(CenterEnum.VM1, FIFO);
            serverS3 = new Server(CenterEnum.S3, IS);
            serverVM2CPU = new Server(CenterEnum.VM2CPU, FIFO);
            serverVM2Band = new Server(CenterEnum.VM2BAND, FIFO);
        } else {
            serverVM1 = new Server(CenterEnum.VM1, PS);
            serverS3 = new Server(CenterEnum.S3, IS);
            serverVM2CPU = new Server(CenterEnum.VM2CPU, PS);
            serverVM2Band = new Server(CenterEnum.VM2BAND, PS);
        }
        servers[0] = serverVM1;
        servers[1] = serverS3;
        servers[2] = serverVM2CPU;
        servers[3] = serverVM2Band;
    }

    public static void main(String[] args) {

        if(Params.FIFO && Params.PS){
            System.err.println("Più di una disciplina di scheduling abilitata, errore!");
            System.exit(-1);
        }

        Generator generator = new Generator();
        NetworkConfiguration networkConfiguration = NetworkConfiguration.getInstance();


        for(char configuration: networkConfigurationCodes){
            actualConfiguration = configuration;
            CSVWriter writer = new CSVWriter(actualConfiguration);

            networkConfiguration.setConfiguration(configuration);


            Server[] servers = new Server[4];
            initServers(servers);

            if(Params.DEBUG_MODE_ON){
                runDebugMode(generator, servers, configuration);
            }

            if(Params.runFiniteHorizonSimulation){
                System.out.println("Configurazione: " + configuration);
                runFiniteHorizonSimulation(generator, writer, servers);
            }

            if(Params.runBatchMeansSimulation){
                System.out.println("Configurazione: " + configuration);
                runBatchMeansSimulation(generator, writer, servers);
            }
        }
    }

    public static void runDebugMode(Generator generator, Server[] servers, char configuration){
        generator.plantSeeds(123456789);
        DebugCSVWriter writer = DebugCSVWriter.getWriter();
        RoutingMatrix routingMatrix = new RoutingMatrix();
        ArrivalsCounter arrivalsCounter = new ArrivalsCounter();
        ServiceTime serviceTime = new ServiceTime(servers);
        currentCounterLimit = Double.MAX_VALUE;
        for(int i = 0; i < Params.NUM_REPLICAS; i++) {
            System.out.println("Configurazione: " + configuration + ". Replica: " + i + " ...");
            current = START;
            ArrayList<Double> currentLedger = new ArrayList<>();
            currentLedger.add(0, current);


            EventList eventList = new EventList(generator, current);

            int iterations = 0;
            while (iterations < Params.DEBUG_ITERATIONS) {


            /*
                ArrayList<Event> events = new ArrayList<>();
                System.out.println("Current at iteration " + iterations + " = " + current);
                for(EventType type: eventList.eventList.keySet()){
                    if(eventList.eventList.get(type) != null){
                        events.add(eventList.eventList.get(type));
                    } else {
                        System.out.println(type + " nessun evento programmato.");
                    }
                }
                events.sort((o1, o2) -> {
                    if(o1.getEndTime() - ( o2).getEndTime() > 0) {
                        return 1;
                    } else if (( o1).getEndTime() - ( o2).getEndTime() < 0) {
                        return -1;
                    }
                    else {
                        return 0;
                    }
                });
                for(Event evento: events){
                    System.out.println(evento.toString());
                }
            */




                Event nextEvent = eventList.removeNextEvent();

                EventType nextEventType = nextEvent.getType();
                if (nextEventType == EventType.ARRIVALVM1 || nextEventType == EventType.ARRIVALS3 ||
                        nextEventType == EventType.ARRIVALVM2CPU) {
                    arrivalsCounter.increaseCounter(nextEventType);
                } else {
                    CenterEnum nextCenter = nextEvent.getNextCenter();
                    switch (nextEventType) {
                        case COMPLETATIONVM1:
                            routingMatrix.increaseCounter(CenterEnum.VM1, nextCenter);
                            break;
                        case COMPLETATIONS3:
                            routingMatrix.increaseCounter(CenterEnum.S3, nextCenter);
                            break;
                        case COMPLETATIONVM2CPU:
                            routingMatrix.increaseCounter(CenterEnum.VM2CPU, nextCenter);
                            break;
                        case COMPLETATIONVM2BAND:
                            routingMatrix.increaseCounter(CenterEnum.VM2BAND, nextCenter);
                            break;
                        default:
                            System.err.println("Errore Fatale");
                            System.exit(-1);
                    }
                }


                handleEvent(nextEvent, servers, generator, eventList);
                iterations++;

                currentLedger.add(0, current);
            }

            for (int j = 1; j < currentLedger.size(); j++) {
                if (currentLedger.get(j - 1) < currentLedger.get(j)) {
                    System.err.println("Errore nell'avanzamento del clock.");
                    System.err.println(j + " - " + currentLedger.get(j - 1) + " < " + currentLedger.get(j));
                    System.exit(-1);
                }
            }

            routingMatrix.commitCounters();
            routingMatrix.resetCounters();

            arrivalsCounter.commitCounters(current);
            arrivalsCounter.resetCounters();

            serviceTime.updateTimes(servers);

            for (Server server : servers) {
                //server.printMetrics(current);
                server.updateBetweenRunsMetrics(current);
                server.removeAllEventsInCenter();
                server.resetInRunMetrics();
                //server.printBetweenRunsMetrics();
            }
        }

        ArrayList<Double> frequencies = routingMatrix.getRoutingFrequencies();
        ArrayList<Double> counters = arrivalsCounter.getCounters();
        ArrayList<Double> serviceTimers = serviceTime.getServiceTimes();
        double[] values = new double[33];
        values[1] = counters.get(0);    //lambda_VM1
        values[2] = counters.get(1);    //lambda_S3
        values[3] = counters.get(2);    //lambda_VM2CPU
        values[0] = values[1]+values[2]+values[3];    //lambda

        values[4] = serviceTimers.get(0);   //mean_service_time_VM1
        values[5] = serviceTimers.get(1);   //mean_service_time_S3
        values[6] = serviceTimers.get(2);   //mean_service_time_VM2CPU
        values[7] = serviceTimers.get(3);   //mean_service_time_VM2BAND

        values[8] = 0;  //P00
        values[9] = values[1]/values[0];    //P01
        values[10] = values[2]/values[0];    //P02
        values[11] = values[3]/values[0];    //P03
        values[12] = 0;  //P04


        for(int k = 0; k < frequencies.size(); k++){
            values[13+k] = frequencies.get(k);
        }

        writer.writeLine(configuration, values);
        if(configuration == networkConfigurationCodes[networkConfigurationCodes.length-1]){
            writer.flushAndClose();
        }
    }

    private static boolean systemEmpty(EventList eventList) {
        if (eventList.eventList.size() != 0){
            return false;
        }
        return true;
    }

    private static void runFiniteHorizonSimulation(Generator generator, CSVWriter writer, Server[] servers){

        currentCounterLimit = 0;
        while(currentCounterLimit < Params.FH_MAX_COUNTER_LIMIT) {

            BetweenRunsSystemMetrics systemMetrics = BetweenRunsSystemMetrics.getIstance();

            generator.plantSeeds(123456789+(long)currentCounterLimit);
            currentCounterLimit += Params.FH_MIN_COUNTER_LIMIT;


            int iterations = 0;
            for(int i = 0; i < Params.NUM_REPLICAS; i++) {
                current = START;
                counter = 0;

                EventList eventList = new EventList(generator, current);
                iterations = 0;
                //while (current < currentCounterLimit || !systemEmpty(eventList)) {
                while (!systemEmpty(eventList)) {

                    /*
                    ArrayList<Event> events = new ArrayList<>();
                    System.out.println("Current at iteration " + iterations + " = " + current);
                    for(EventType type: eventList.eventList.keySet()){
                        if(eventList.eventList.get(type) != null){
                            events.add(eventList.eventList.get(type));
                        } else {
                            System.out.println(type + " nessun evento programmato.");
                        }
                    }
                    events.sort((o1, o2) -> {
                        if(o1.getEndTime()- ( o2).getEndTime() > 0) {
                            return 1;
                        } else if (( o1).getEndTime() - ( o2).getEndTime() < 0) {
                            return -1;
                        }
                        else {
                            return 0;
                        }
                    });
                    for(Event evento: events){
                        System.out.println(evento.toString());
                    }
                    System.out.println("\n");
                    */

                    /*
                    for(Server server_ : servers){
                        if(server_.getDiscipline() == FIFO) {
                            System.out.println("\n" + server_.getType());
                            int positionInList = 0;
                            for (Event event_ : server_.getJobsInCenterList()) {
                                System.out.println(positionInList + " " + event_.getServiceTime() + " " + event_.getEndTime());
                                positionInList ++;
                            }
                        }
                    }
                    */

                    Event nextEvent = eventList.removeNextEvent();
                    handleEvent(nextEvent, servers, generator, eventList);
                    iterations++;

                }

                /*
                for (Server server : servers) {
                    server.printMetrics(current);
                }
                System.exit(0);
                */

                systemMetrics.updateSystemMetrics(servers, current);
                for (Server server : servers) {
                    //server.printMetrics(current);
                    server.updateBetweenRunsMetrics(current);
                    server.removeAllEventsInCenter();
                    server.resetInRunMetrics();
                    //server.printBetweenRunsMetrics();
                }


            }
            double price = 0.0;
            double[] values = new double[31];
            values[0] = iterations;
            int j = 0;
            int numServers = servers.length;
            for (Server server : servers) {
                switch (server.getType()){
                    case VM1:
                        price = price + server.getUtilitationInterval()[0]*Params.VM1_PRICE_PER_MINUTE*current;
                        break;
                    case S3:
                        price = price + server.getDeparture()*Params.S3_PRICE_PER_REQUEST;
                        break;
                    case VM2CPU:
                        price = price + server.getUtilitationInterval()[0]*Params.VM2CPU_PRICE_PER_MINUTE*current;
                        break;
                    case VM2BAND:
                        price = price + server.getUtilitationInterval()[0]*Params.VM2BAND_PRICE_PER_MINUTE*current;
                        break;
                    default:
                        System.err.println("ERRORE FATALE!");
                        System.exit(-1);
                        break;
                }
                values[1+6*j] = server.getWaitConfidenceInterval()[0];
                values[2+6*j] = server.getWaitConfidenceInterval()[1];
                values[3+6*j] = server.getThroughputConfidenceInterval()[0];
                values[4+6*j] = server.getThroughputConfidenceInterval()[1];
                values[5+6*j] = server.getPopulationInterval()[0];
                values[6+6*j] = server.getPopulationInterval()[1];
                j++;
            }
            values[1+6*j] = systemMetrics.getWaitConfidenceInterval()[0];
            values[2+6*j] = systemMetrics.getWaitConfidenceInterval()[1];
            values[3+6*j] = systemMetrics.getThroughputConfidenceInterval()[0];
            values[4+6*j] = systemMetrics.getThroughputConfidenceInterval()[1];
            values[29] = price;
            values[30] = currentCounterLimit;
            writer.writeFHLine(values);
            writer.flushFH();
            for(Server server : servers){
        //        server.resetInRunMetrics();
        //        server.removeAllEventsInCenter();
                server.resetBetweenRunsMetrics();
            }
            systemMetrics.resetMetrics();
        }
        writer.flushAndCloseFHFile();
    }

    private static void runBatchMeansSimulation(Generator generator, CSVWriter writer, Server[] servers){

        int numBatch = 0;


        current = START;

        EventList eventList = new EventList(generator, current);

        BetweenRunsSystemMetrics systemMetrics = BetweenRunsSystemMetrics.getIstance();

        while(numBatch < Params.BM_NUM_BATCHES){
            double currentBatchStartTime = current;


            int batchSize = Params.BM_NUM_EVENTS / Params.BM_NUM_BATCHES;
            for(int executionInBatch = 0; executionInBatch < batchSize; executionInBatch ++){

                Event nextEvent = eventList.removeNextEvent();
                handleEvent(nextEvent, servers, generator, eventList);


            }

            systemMetrics.updateSystemMetrics(servers, current-currentBatchStartTime);
            for (Server server : servers) {
                server.printMetrics(current-currentBatchStartTime);
                server.updateBetweenRunsMetrics(current-currentBatchStartTime);
                server.resetInRunMetrics();
            }

            numBatch ++;
        }

        double[] values = new double[21];
        values[0] = 1/Params.MEAN_INTERARRIVAL_S3+ 1/Params.MEAN_INTERARRIVAL_VM1+ 1/Params.MEAN_INTERARRIVAL_VM2CPU;
        int j = 0;
        int numServers = servers.length;
        for (Server server : servers) {
            values[1+numServers*j] = server.getWaitConfidenceInterval()[0];
            values[2+numServers*j] = server.getWaitConfidenceInterval()[1];
            values[3+numServers*j] = server.getThroughputConfidenceInterval()[0];
            values[4+numServers*j] = server.getThroughputConfidenceInterval()[1];
            j++;
        }
        values[1+numServers*j] = systemMetrics.getWaitConfidenceInterval()[0];
        values[2+numServers*j] = systemMetrics.getWaitConfidenceInterval()[1];
        values[3+numServers*j] = systemMetrics.getThroughputConfidenceInterval()[0];
        values[4+numServers*j] = systemMetrics.getThroughputConfidenceInterval()[1];
        writer.writeBMLine(values);
        writer.flushAndCloseBMFile();


    }


    private static void handleArrival(Server server, Event event, EventList eventList, Generator generator, EventType newEventType){
        Event newEvent = new Event(newEventType, generator, current, server.getNumJobsInCenter(), server.getDiscipline());

        int position = server.insertJobInCenter(newEvent, current);

        if(position == 0){
            eventList.putEvent(newEvent.getType(), newEvent);
        }

        Event newArrival = new Event(event.getType(), generator, current, server.getNumJobsInCenter(), server.getDiscipline());

        //Blocco gli arrivi dopo un certo istante di tempo, dunque da quel punto in poi il sistema si svuota e basta
        if(newArrival.getEndTime() <= currentCounterLimit) {
            eventList.putEvent(newArrival.getType(), newArrival);
        }

    }

    private static void routeTo(Event event, Server destination, EventList eventList){
        int position = destination.insertJobInCenter(event, current);
        if(position == 0){
            eventList.putEvent(event.getType(), event);
        }
    }

    private static void handleCompletation(Server server, Event event, Generator generator, EventList eventList, Server[] servers){
        server.removeNextEvent();
        Event newEvent = server.getNextCompletation(current);
        if(newEvent != null) {
            eventList.putEvent(newEvent.getType(), newEvent);
        }
        server.increaseDeparture();
        CenterEnum nextCenter = event.getNextCenter();

        if(nextCenter == null){
            server.increaseExitCounter();
        } else {
            switch (nextCenter) {
                case VM1:
                    Server vm1 = servers[CenterEnum.VM1.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM1, generator, current, vm1.getNumJobsInCenter(), vm1.getDiscipline());
                    if(vm1.getDiscipline() == PS)
                        vm1.updateJobsTimeAfterArrival(event);
                    routeTo(newEvent, vm1, eventList);
                    break;
                case S3:
                    Server s3 = servers[CenterEnum.S3.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONS3, generator, current, s3.getNumJobsInCenter(), s3.getDiscipline());
                    routeTo(newEvent, s3, eventList);
                    break;
                case VM2CPU:
                    Server vm2cpu = servers[CenterEnum.VM2CPU.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM2CPU, generator, current, vm2cpu.getNumJobsInCenter(), vm2cpu.getDiscipline());
                    if(vm2cpu.getDiscipline() == PS)
                        vm2cpu.updateJobsTimeAfterArrival(event);
                    routeTo(newEvent, vm2cpu, eventList);
                    break;
                case VM2BAND:
                    Server vm2band = servers[CenterEnum.VM2BAND.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM2BAND, generator, current, vm2band.getNumJobsInCenter(), vm2band.getDiscipline());
                    if(vm2band.getDiscipline() == PS)
                        vm2band.updateJobsTimeAfterArrival(event);
                    routeTo(newEvent, vm2band, eventList);
                    break;
                default:
                    System.err.println("ERRORE FATALE");
                    System.exit(-1);
                    break;
            }
        }
    }

    private static void handleEvent(Event event, Server[] servers, Generator generator, EventList eventList){
        double nextEventTime = event.getEndTime();
        for(Server server: servers) {
            server.updateInRunMetrics(current, nextEventTime);
        }
        current = nextEventTime;

        Server vm1 = servers[CenterEnum.VM1.getCenterIndex()-1];
        Server s3 = servers[CenterEnum.S3.getCenterIndex()-1];
        Server vm2cpu = servers[CenterEnum.VM2CPU.getCenterIndex()-1];
        Server vm2band = servers[CenterEnum.VM2BAND.getCenterIndex()-1];

        switch(event.getType()){
            case ARRIVALVM1:
                if(vm1.getDiscipline() == PS)
                    vm1.updateJobsTimeAfterArrival(event);
                handleArrival(vm1, event, eventList, generator, EventType.COMPLETATIONVM1);
                break;
            case ARRIVALS3:
                handleArrival(s3, event, eventList, generator, EventType.COMPLETATIONS3);
                break;
            case ARRIVALVM2CPU:
                if(vm2cpu.getDiscipline() == PS)
                    vm2cpu.updateJobsTimeAfterArrival(event);
                handleArrival(vm2cpu, event, eventList, generator, EventType.COMPLETATIONVM2CPU);
                break;
            case COMPLETATIONVM1:
                if(vm1.getDiscipline() == PS)
                    vm1.updateJobsTimeAfterCompletation(event);
                handleCompletation(vm1, event, generator, eventList, servers);
                break;
            case COMPLETATIONS3:
                handleCompletation(s3, event, generator, eventList, servers);
                break;
            case COMPLETATIONVM2CPU:
                if(vm2cpu.getDiscipline() == PS)
                   vm2cpu.updateJobsTimeAfterCompletation(event);
                handleCompletation(vm2cpu, event, generator, eventList, servers);
                break;
            case COMPLETATIONVM2BAND:
                if(vm2band.getDiscipline() == PS)
                    vm2band.updateJobsTimeAfterCompletation(event);
                handleCompletation(vm2band, event, generator, eventList, servers);
                break;
            default:
                System.err.println("ERRORE FATALE 291");
                System.exit(-1);
                break;
        }

    }

}
