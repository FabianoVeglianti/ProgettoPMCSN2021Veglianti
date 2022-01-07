package reteDiCode;

import debug.ArrivalsCounter;
import debug.RoutingMatrix;
import debug.ServiceTime;
import processorSharingSingolo.Event;
import processorSharingSingolo.Server;
import writer.BatchMeansWriter;
import writer.CSVWriter;
import writer.DebugCSVWriter;
import writer.Writer;

import java.util.ArrayList;

import static reteDiCode.SchedulingDisciplineEnum.*;


public class Runner {

    public static final double START = 0.0;

    private static double currentTime;
    private static SchedulingDisciplineEnum[] disciplines = {FIFO, PS};
    private static char[] networkConfigurationCodes = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
    private static char[] temp = {'B'};
    private static double currentCounterLimit = 0;
    private static int counter = 0;
    private static Network network;

    public static SchedulingDisciplineEnum actualDiscipline;
    private static char actualConfiguration;

    private static void initServers(Server[] servers){
        Server serverVM1 = null;
        Server serverS3 = null;
        Server serverVM2CPU = null;
        Server serverVM2Band = null;
        if(actualDiscipline.equals(FIFO)) {
            serverVM1 = new Server(CenterEnum.VM1, FIFO);
            serverS3 = new Server(CenterEnum.S3, IS);
            serverVM2CPU = new Server(CenterEnum.VM2CPU, FIFO);
            serverVM2Band = new Server(CenterEnum.VM2BAND, FIFO);
        } else if(actualDiscipline.equals(PS)){
            serverVM1 = new Server(CenterEnum.VM1, PS);
            serverS3 = new Server(CenterEnum.S3, IS);
            serverVM2CPU = new Server(CenterEnum.VM2CPU, PS);
            serverVM2Band = new Server(CenterEnum.VM2BAND, PS);
        } else {
            System.err.println("Errore nella gestione delle discipline di scheduling.");
            System.exit(-1);
        }
        servers[0] = serverVM1;
        servers[1] = serverS3;
        servers[2] = serverVM2CPU;
        servers[3] = serverVM2Band;
    }

    public static void main(String[] args) {

        network = new Network();

        for(SchedulingDisciplineEnum discipline : disciplines) {
            actualDiscipline = discipline;
            System.out.println("Disciplina: " + discipline);

            Generator generator = new Generator();
            NetworkConfiguration networkConfiguration = NetworkConfiguration.getInstance();

            DebugCSVWriter debugCSVWriter = null;

            BatchMeansWriter batchMeansWriter = null;


            for (char configuration : networkConfigurationCodes) {
                actualConfiguration = configuration;


                networkConfiguration.setConfiguration(configuration);


                Server[] servers = new Server[4];
                initServers(servers);

                if (Params.DEBUG_MODE_ON) {
                    if(configuration == networkConfigurationCodes[0])
                        debugCSVWriter = new DebugCSVWriter();
                    runDebugMode(generator,debugCSVWriter, servers);
                    if(configuration == networkConfigurationCodes[networkConfigurationCodes.length-1])
                        debugCSVWriter.flushAndClose();
                }

                if (Params.runFiniteHorizonSimulation) {
                    CSVWriter writer = new CSVWriter(actualConfiguration, actualDiscipline.name());
                    System.out.println("Configurazione: " + configuration);
                    runFiniteHorizonSimulation(generator, writer, servers);
                }

                if (Params.runBatchMeansSimulation) {
                 //   if(configuration != 'A' && configuration != 'D' && configuration != 'G') {
                        batchMeansWriter = new BatchMeansWriter(actualConfiguration, actualDiscipline.name());
                        System.out.println("Configurazione: " + configuration);
                        runBatchMeansSimulation(generator, batchMeansWriter, servers);
                  //  }
                }
            }
        }
    }

    public static void runDebugMode(Generator generator,DebugCSVWriter writer, Server[] servers){
        generator.plantSeeds(123456789);

        RoutingMatrix routingMatrix = new RoutingMatrix();
        ArrivalsCounter arrivalsCounter = new ArrivalsCounter();
        ServiceTime serviceTime = new ServiceTime(servers);
        currentCounterLimit = Double.MAX_VALUE;
        for(int i = 0; i < Params.NUM_REPLICAS; i++) {
            System.out.println("Configurazione: " + actualConfiguration + ". Replica: " + i + " ...");
            currentTime = START;
            ArrayList<Double> currentLedger = new ArrayList<>();
            currentLedger.add(0, currentTime);


            EventList eventList = new EventList(generator, currentTime);

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
                if (nextEventType == EventType.ARRIVALS3) {
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

                currentLedger.add(0, currentTime);
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

            arrivalsCounter.commitCounters(currentTime);
            arrivalsCounter.resetCounters();

            serviceTime.updateTimes(servers);

            for (Server server : servers) {
                //server.printMetrics(current);
                server.updateBetweenRunsMetrics(currentTime);
                server.removeAllEventsInCenter();
                server.resetInRunMetrics();
                //server.printBetweenRunsMetrics();
            }
            network.updateBetweenRunsMetrics(currentTime);
            network.removeAllEventsInNetwork();
            network.resetInRunMetrics();
        }

        ArrayList<Double> frequencies = routingMatrix.getRoutingFrequencies();
        ArrayList<Double> counters = arrivalsCounter.getCounters();
        ArrayList<Double> serviceTimers = serviceTime.getServiceTimes();
        double[] values = new double[30];
        values[0] = counters.get(0);    //lambda_S3

        values[1] = serviceTimers.get(0);   //mean_service_time_VM1
        values[2] = serviceTimers.get(1);   //mean_service_time_S3
        values[3] = serviceTimers.get(2);   //mean_service_time_VM2CPU
        values[4] = serviceTimers.get(3);   //mean_service_time_VM2BAND

        /* Posso impostare questi valori staticamente dato che non ci sono eventi che di arrivo a centri diversi da S3?*/
        values[5] = 0;  //P00
        values[6] = 0;  //P01
        values[7] = 1;  //P02
        values[8] = 0;  //P03
        values[9] = 0;  //P04


        for(int k = 0; k < frequencies.size(); k++){
            values[10+k] = frequencies.get(k);
        }

        writer.writeLine(actualDiscipline.name(), actualConfiguration, values);
        if(actualConfiguration == networkConfigurationCodes[networkConfigurationCodes.length-1]){
            writer.flush();
        }
    }

    private static boolean systemEmpty(EventList eventList) {
        if (eventList.eventList.size() != 0){
            return false;
        }
        return true;
    }

    /* La probabilità che il numero di jobs in uno dei centri sia, ad un certo punto, maggiore di 1000 è
    * talmente bassa da poter assumere con un grado di errore trascurabile, che il server è congestionato*/
    private static boolean isSystemCongested(Server[] servers){
        boolean cond1 = servers[0].getNumJobsInCenter()>1000;
        boolean cond2 = servers[1].getNumJobsInCenter()>1000;
        boolean cond3 = servers[2].getNumJobsInCenter()>1000;
        boolean cond4 = servers[3].getNumJobsInCenter()>1000;
        return cond1 || cond2 || cond3 || cond4;
    }



    private static void runFiniteHorizonSimulation(Generator generator, CSVWriter writer, Server[] servers){

        currentCounterLimit = 0;
        while(currentCounterLimit < Params.FH_MAX_COUNTER_LIMIT) {



            generator.plantSeeds(123456789+(long)currentCounterLimit);
            currentCounterLimit += Params.FH_MIN_COUNTER_LIMIT;



            int iterations = 0;
            for(int i = 0; i < Params.NUM_REPLICAS; i++) {

                currentTime = START;
                counter = 0;

                EventList eventList = new EventList(generator, currentTime);
                iterations = 0;
                //while (currentTime < currentCounterLimit || !systemEmpty(eventList)) {
                //     while (!systemEmpty(eventList)) {
                while (currentTime < currentCounterLimit) {
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
                    server.printMetrics(currentTime);
                }
                network.printMetrics(currentTime);
                System.exit(0);
*/
                network.updateBetweenRunsMetricsUsingServers(currentTime, servers);
                network.removeAllEventsInNetwork();
                network.resetInRunMetrics();

                for (Server server : servers) {
                    //server.printMetrics(current);
                    server.updateBetweenRunsMetrics(currentTime);
                    server.removeAllEventsInCenter();     //commentare
                    server.resetInRunMetrics();           //commentare
                    //server.printBetweenRunsMetrics();
                }
/*      IN ALTERNATIVA TOGLIERE QUESTO COMMENTO
                eventList.removeEventByType(EventType.ARRIVALS3);
                while (!systemEmpty(eventList)) {
                    Event nextEvent = eventList.removeNextEvent();
                    handleEvent(nextEvent, servers, generator, eventList);
                    iterations++;
                }

                for (Server server : servers) {
                    server.removeAllEventsInCenter();
                    server.resetInRunMetrics();
                }
*/
                /*
                network.updateBetweenRunsMetrics(currentTime);
                network.removeAllEventsInNetwork();
                network.resetInRunMetrics();
                */

            }
            double price = 0.0;
            double[] values = new double[33];
            values[0] = iterations;
            int j = 0;
            int numServers = servers.length;
            for (Server server : servers) {
                switch (server.getType()){
                    case VM1:
                        price = price + server.getUtilitationInterval()[0]*Params.VM1_PRICE_PER_MINUTE* currentTime;
                        break;
                    case S3:
                        price = price + server.getDeparture()*Params.S3_PRICE_PER_REQUEST;
                        break;
                    case VM2CPU:
                        price = price + server.getUtilitationInterval()[0]*Params.VM2CPU_PRICE_PER_MINUTE* currentTime;
                        break;
                    case VM2BAND:
                        price = price + server.getUtilitationInterval()[0]*Params.VM2BAND_PRICE_PER_MINUTE* currentTime;
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
            values[1+6*j] = network.getWaitConfidenceInterval()[0];
            values[2+6*j] = network.getWaitConfidenceInterval()[1];
            values[3+6*j] = network.getThroughputConfidenceInterval()[0];
            values[4+6*j] = network.getThroughputConfidenceInterval()[1];
            values[5+6*j] = network.getPopulationInterval()[0];
            values[6+6*j] = network.getPopulationInterval()[1];
            values[31] = price;
            values[32] = currentCounterLimit;
            writer.writeLine(values);
            writer.flush();
            for(Server server : servers){
        //        server.resetInRunMetrics();
        //        server.removeAllEventsInCenter();
                server.resetBetweenRunsMetrics();
            }
            network.resetBetweenRunsMetrics();

        }
        writer.flushAndClose();
    }

    private static void runBatchMeansSimulation(Generator generator, BatchMeansWriter writer, Server[] servers) {

        for(double arrivalRate = 5.0; arrivalRate <= 11.0; arrivalRate += 0.1) {
            if(arrivalRate > 7.0 && (actualConfiguration == 'A' || actualConfiguration == 'D' || actualConfiguration == 'G')){
                continue;
            } else if(arrivalRate <7.0 && (actualConfiguration != 'A' && actualConfiguration != 'D' && actualConfiguration != 'G')){
                continue;
            }
            arrivalRate = Math.round(arrivalRate*10.0)/10.0;
            setArrivalRate(Math.round(arrivalRate*10.0)/10.0);
            System.out.println(arrivalRate);
            currentTime = START;
            EventList eventList = new EventList(generator, currentTime);

            int numBatch = 0;


            while (numBatch < Params.BM_NUM_BATCHES) {
                double currentBatchStartTime = currentTime;

                for (Server server : servers) {
                    server.setCurrentBatchStartTime(currentBatchStartTime);
                }

                int batchSize = Params.BM_NUM_EVENTS / Params.BM_NUM_BATCHES;
                for (int executionInBatch = 0; executionInBatch < batchSize; executionInBatch++) {

            /*
                    ArrayList<Event> events = new ArrayList<>();
                    System.out.println("Current at iteration " + executionInBatch + " = " + currentTime);
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


                }


                for (Server server : servers) {
                    //    server.printMetrics(currentTime -currentBatchStartTime);
                    server.updateBetweenRunsMetrics(currentTime - currentBatchStartTime);
                    server.resetInRunMetrics();
                }
                network.updateBetweenRunsMetrics(currentTime - currentBatchStartTime);
                network.removeAllEventsInNetwork();
                network.resetInRunMetrics();
                numBatch++;
                /*
                //Se il sistema è congestionato interrompo la simulazione attuale
                if(isSystemCongested(servers)){
                    numBatch = Params.BM_NUM_BATCHES;
                }
                 */
            }

            for(Server server:servers){
                server.computeAutocorrelationValues();
            }


            double[] values = new double[46];
            values[0] = 1 / Params.MEAN_INTERARRIVAL_S3;
            int j = 0;
            for (Server server : servers) {
                values[1 + 9 * j] = server.getWaitConfidenceIntervalAndAutocorrelationLagOne()[0];
                values[2 + 9 * j] = server.getWaitConfidenceIntervalAndAutocorrelationLagOne()[1];
                values[3 + 9 * j] = server.getWaitConfidenceIntervalAndAutocorrelationLagOne()[2];
                values[4 + 9 * j] = server.getThroughputConfidenceIntervalAndAutocorrelationLagOne()[0];
                values[5 + 9 * j] = server.getThroughputConfidenceIntervalAndAutocorrelationLagOne()[1];
                values[6 + 9 * j] = server.getThroughputConfidenceIntervalAndAutocorrelationLagOne()[2];
                values[7 + 9 * j] = server.getPopulationConfidenceIntervalAndAutocorrelationLagOne()[0];
                values[8 + 9 * j] = server.getPopulationConfidenceIntervalAndAutocorrelationLagOne()[1];
                values[9 + 9 * j] = server.getPopulationConfidenceIntervalAndAutocorrelationLagOne()[2];
                j++;
            }
            values[1 + 9 * j] = network.getWaitConfidenceIntervalAndAutocorrelationLagOne()[0];
            values[2 + 9 * j] = network.getWaitConfidenceIntervalAndAutocorrelationLagOne()[1];
            values[3 + 9 * j] = network.getWaitConfidenceIntervalAndAutocorrelationLagOne()[2];
            values[4 + 9 * j] = network.getThroughputConfidenceIntervalAndAutocorrelationLagOne()[0];
            values[5 + 9 * j] = network.getThroughputConfidenceIntervalAndAutocorrelationLagOne()[1];
            values[6 + 9 * j] = network.getThroughputConfidenceIntervalAndAutocorrelationLagOne()[2];
            values[7 + 9 * j] = network.getPopulationConfidenceIntervalAndAutocorrelationLagOne()[0];
            values[8 + 9 * j] = network.getPopulationConfidenceIntervalAndAutocorrelationLagOne()[1];
            values[9 + 9 * j] = network.getPopulationConfidenceIntervalAndAutocorrelationLagOne()[2];
            writer.writeLine(values);
            writer.flush();

            for (Server server : servers) {
                server.removeAllEventsInCenter();
                server.resetBetweenRunsMetrics();
            //    server.printMetrics(currentTime);
            }
            network.resetBetweenRunsMetrics();

        }
        writer.flushAndClose();
    }

    private static void setArrivalRate(double arrivalRate) {
        Params.MEAN_INTERARRIVAL_RATE = arrivalRate;

        /**/
        Params.MEAN_INTERARRIVAL_S3 = 1/arrivalRate;
    }


    private static void handleArrival(Server server, Event event, EventList eventList, Generator generator, EventType newEventType){
        Event newEvent = new Event(newEventType, generator, currentTime, server.getNumJobsInCenter(), server.getDiscipline());

        int position = server.insertJobInCenter(newEvent, currentTime);
        network.insertJobInNetwork(currentTime);

        if(position == 0){
            eventList.putEvent(newEvent.getType(), newEvent);
        }

        Event newArrival = new Event(event.getType(), generator, currentTime, server.getNumJobsInCenter(), server.getDiscipline());

        //Blocco gli arrivi dopo un certo istante di tempo, dunque da quel punto in poi il sistema si svuota e basta
        //if(Params.runBatchMeansSimulation) {
        if(false){
            if (newArrival.getEndTime() <= currentCounterLimit) {
                eventList.putEvent(newArrival.getType(), newArrival);
            }
        } else {
            eventList.putEvent(newArrival.getType(), newArrival);
        }
        /*
        if (newArrival.getEndTime() <= currentCounterLimit) {
            eventList.putEvent(newArrival.getType(), newArrival);
        }
        */
    }

    private static void routeTo(Event event, Server destination, EventList eventList){
        int position = destination.insertJobInCenter(event, currentTime);
        if(position == 0){
            eventList.putEvent(event.getType(), event);
        }
    }

    private static void handleCompletation(Server server, Event event, Generator generator, EventList eventList, Server[] servers){
        server.removeNextEvent();
        Event newEvent = server.getNextCompletation(currentTime);
        if(newEvent != null) {
            eventList.putEvent(newEvent.getType(), newEvent);
        }
        server.increaseDeparture();
        CenterEnum nextCenter = event.getNextCenter();

        if(nextCenter == null){
            server.increaseExitCounter();
            network.increaseDeparture();
        } else {
            switch (nextCenter) {
                case VM1:
                    Server vm1 = servers[CenterEnum.VM1.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM1, generator, currentTime, vm1.getNumJobsInCenter(), vm1.getDiscipline());
                    if(vm1.getDiscipline() == PS)
                        vm1.updateJobsTimeAfterArrival(event);
                    routeTo(newEvent, vm1, eventList);
                    break;
                case S3:
                    Server s3 = servers[CenterEnum.S3.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONS3, generator, currentTime, s3.getNumJobsInCenter(), s3.getDiscipline());
                    routeTo(newEvent, s3, eventList);
                    break;
                case VM2CPU:
                    Server vm2cpu = servers[CenterEnum.VM2CPU.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM2CPU, generator, currentTime, vm2cpu.getNumJobsInCenter(), vm2cpu.getDiscipline());
                    if(vm2cpu.getDiscipline() == PS)
                        vm2cpu.updateJobsTimeAfterArrival(event);
                    routeTo(newEvent, vm2cpu, eventList);
                    break;
                case VM2BAND:
                    Server vm2band = servers[CenterEnum.VM2BAND.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM2BAND, generator, currentTime, vm2band.getNumJobsInCenter(), vm2band.getDiscipline());
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
            server.updateInRunMetrics(currentTime, nextEventTime);
        }
        network.updateInRunMetrics(currentTime, nextEventTime);

        currentTime = nextEventTime;

        Server vm1 = servers[CenterEnum.VM1.getCenterIndex()-1];
        Server s3 = servers[CenterEnum.S3.getCenterIndex()-1];
        Server vm2cpu = servers[CenterEnum.VM2CPU.getCenterIndex()-1];
        Server vm2band = servers[CenterEnum.VM2BAND.getCenterIndex()-1];

        switch(event.getType()){
            case ARRIVALS3:
                handleArrival(s3, event, eventList, generator, EventType.COMPLETATIONS3);
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
