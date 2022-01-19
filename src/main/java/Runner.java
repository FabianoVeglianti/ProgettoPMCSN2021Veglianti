import config.NetworkConfiguration;
import config.Params;
import debug.ArrivalsCounter;
import debug.RoutingMatrix;

import entity.*;
import utils.Generator;
import utils.Price;
import writer.BatchMeansWriter;
import writer.FiniteHorizonWriter;
import writer.DebugWriter;

import java.util.ArrayList;

import static entity.SchedulingDisciplineType.*;


public class Runner {

    public static final double START = 0.0;
    private static final SchedulingDisciplineType[] disciplines = {FIFO, PS};
    private static final char[] networkConfigurationCodes = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};
    private static final double[] arrivalRates = {5.0, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9,
            6.0, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9,
            7.0, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9,
            8.0, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9,
            9.0, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9,
            10.0, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9,11.0};

    private static double currentTime;
    private static double currentTimeLimit = 0;
    private static Network network;
    private static Price price;
    public static SchedulingDisciplineType actualDiscipline;
    private static char actualConfiguration;


    /**
     * Initialize the servers according to the scheduling discipline for the current simulation
     * (actualDiscipline value)
     * */
    private static void initServers(Server[] servers){
        Server serverVM1 = null;
        Server serverS3 = null;
        Server serverVM2CPU = null;
        Server serverVM2Band = null;
        if(actualDiscipline.equals(FIFO)) {
            serverVM1 = new Server(ServerEnum.VM1, FIFO);
            serverS3 = new Server(ServerEnum.S3, IS);
            serverVM2CPU = new Server(ServerEnum.VM2CPU, FIFO);
            serverVM2Band = new Server(ServerEnum.VM2BAND, FIFO);
        } else if(actualDiscipline.equals(PS)){
            serverVM1 = new Server(ServerEnum.VM1, PS);
            serverS3 = new Server(ServerEnum.S3, IS);
            serverVM2CPU = new Server(ServerEnum.VM2CPU, PS);
            serverVM2Band = new Server(ServerEnum.VM2BAND, PS);
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

        DebugWriter debugWriter = null;

        for(SchedulingDisciplineType discipline : disciplines) {
            actualDiscipline = discipline;
            System.out.println("Disciplina: " + discipline);

            Generator generator = new Generator();
            NetworkConfiguration networkConfiguration = NetworkConfiguration.getInstance();


            BatchMeansWriter batchMeansWriter;


            for (char configuration : networkConfigurationCodes) {
                actualConfiguration = configuration;

                if (Params.DEBUG_MODE_ON) {
                    //it opens the output file when the first simulation runs
                    if(configuration == networkConfigurationCodes[0] && discipline == disciplines[0])
                        debugWriter = new DebugWriter();

                    network = new Network();
                    price = new Price();
                    networkConfiguration.setConfiguration(configuration);
                    Server[] servers = new Server[4];
                    initServers(servers);

                    runDebugMode(generator, debugWriter, servers);
                    //it close the output file when the last simulation runs
                    if(configuration == networkConfigurationCodes[networkConfigurationCodes.length-1] && discipline.equals(disciplines[disciplines.length -1])) {
                        assert debugWriter != null;
                        debugWriter.flushAndClose();
                    }
                }

                if (Params.runFiniteHorizonSimulation) {
                    //for each (discipline, configuration) pair it opens a new file
                    FiniteHorizonWriter writer = new FiniteHorizonWriter(actualConfiguration, actualDiscipline.name());
                    System.out.println("FH - Configurazione: " + actualConfiguration);

                    network = new Network();
                    price = new Price();
                    networkConfiguration.setConfiguration(configuration);
                    Server[] servers = new Server[4];
                    initServers(servers);

                    runFiniteHorizonSimulation(generator, writer, servers);
                }

                if (Params.runBatchMeansSimulation) {
                    //for each (discipline, configuration) pair it opens a new file
                    batchMeansWriter = new BatchMeansWriter(actualConfiguration, actualDiscipline.name());
                    System.out.println("BM - Configurazione: " + actualConfiguration);

                    network = new Network();
                    price = new Price();
                    networkConfiguration.setConfiguration(configuration);
                    Server[] servers = new Server[4];
                    initServers(servers);

                    runBatchMeansSimulation(generator, batchMeansWriter, servers);
                }
            }
        }
    }


    /**
     * It runs a debug mode simulation.
     * Debug mode simulation is a long finite-horizon simulation that collects different data.
     * */
    public static void runDebugMode(Generator generator, DebugWriter writer, Server[] servers){
        generator.plantSeeds(123456789);
        RoutingMatrix routingMatrix = new RoutingMatrix();
        ArrivalsCounter arrivalsCounter = new ArrivalsCounter();
        currentTimeLimit = Double.MAX_VALUE;

        for(int i = 0; i < Params.NUM_REPLICAS; i++) {
            System.out.println("Debug - Configurazione: " + actualConfiguration + ". Replica: " + i + " ...");
            currentTime = START;

            //currentLedger is a ledger that memorize all the values of currentTime
            ArrayList<Double> currentLedger = new ArrayList<>();
            currentLedger.add(0, currentTime);


            EventList eventList = new EventList(generator, currentTime);

            int iterations = 0;
            while (iterations < Params.DEBUG_ITERATIONS) {
                Event nextEvent = eventList.removeNextEvent();
                EventType nextEventType = nextEvent.getType();

                //update arrivalsCounter or routingMatrix
                if (nextEventType == EventType.ARRIVALS3) {
                    arrivalsCounter.increaseCounter(nextEventType);
                } else {
                    ServerEnum nextCenter = nextEvent.getNextCenter();
                    switch (nextEventType) {
                        case COMPLETATIONVM1:
                            routingMatrix.increaseCounter(ServerEnum.VM1, nextCenter);
                            break;
                        case COMPLETATIONS3:
                            routingMatrix.increaseCounter(ServerEnum.S3, nextCenter);
                            break;
                        case COMPLETATIONVM2CPU:
                            routingMatrix.increaseCounter(ServerEnum.VM2CPU, nextCenter);
                            break;
                        case COMPLETATIONVM2BAND:
                            routingMatrix.increaseCounter(ServerEnum.VM2BAND, nextCenter);
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

            //it checks that, for the current replica, the currentTime values are monotonically strictly increasing
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

            for (Server server : servers) {
                server.updateBetweenRunsMetrics(currentTime);
                server.removeAllEventsInCenter();
                server.resetInRunMetrics();
            }
            network.updateBetweenRunsMetrics(currentTime);
            network.removeAllEventsInNetwork();
            network.resetInRunMetrics();
        }

        ArrayList<Double> frequencies = routingMatrix.getRoutingFrequencies();
        ArrayList<Double> counters = arrivalsCounter.getCounters();
        double[] values = new double[30];
        values[0] = counters.get(0);    //lambda_S3

        values[1] = servers[0].getServiceTimeInterval()[0];   //mean_service_time_VM1
        values[2] = servers[1].getServiceTimeInterval()[0];   //mean_service_time_S3
        values[3] = servers[2].getServiceTimeInterval()[0];   //mean_service_time_VM2CPU
        values[4] = servers[3].getServiceTimeInterval()[0];   //mean_service_time_VM2BAND

        //These values are statically set because we can only have arrivals to S3
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




    /**
     * It runs a finite horizon mode simulation.
     * */
    private static void runFiniteHorizonSimulation(Generator generator, FiniteHorizonWriter writer, Server[] servers){
        currentTimeLimit = 0;
        //the runs are done until the currentTimeLimit value reach Params.FH_MAX_TIME_LIMIT (=100)
        //each run the currentTimeLimit value is increased by Params.FH_TIME_INCREASE_STEP (=2)
        while(currentTimeLimit < Params.FH_MAX_TIME_LIMIT) {
            generator.plantSeeds(123456789+(long) currentTimeLimit);
            currentTimeLimit += Params.FH_TIME_INCREASE_STEP;


            int iterations = 0;
            for(int i = 0; i < Params.NUM_REPLICAS; i++) {

                currentTime = START;


                EventList eventList = new EventList(generator, currentTime);
                iterations = 0;

                while (currentTime < currentTimeLimit) {
                    Event nextEvent = eventList.removeNextEvent();
                    handleEvent(nextEvent, servers, generator, eventList);
                    iterations++;

                }


                network.updateBetweenRunsMetrics(currentTime);
                network.removeAllEventsInNetwork();
                network.resetInRunMetrics();

                for (Server server : servers) {

                    server.updateBetweenRunsMetrics(currentTime);
                    server.removeAllEventsInCenter();
                    server.resetInRunMetrics();
                }

                price.updateBetweenRunsValues(currentTime);
                price.resetInRunValues();

            }

            double[] values = new double[34];
            values[0] = iterations;
            int j = 0;
            for (Server server : servers) {
                values[1+6*j] = server.getWaitConfidenceInterval()[0];
                values[2+6*j] = server.getWaitConfidenceInterval()[1];
                values[3+6*j] = server.getThroughputConfidenceInterval()[0];
                values[4+6*j] = server.getThroughputConfidenceInterval()[1];
                values[5+6*j] = server.getPopulationConfidenceInterval()[0];
                values[6+6*j] = server.getPopulationConfidenceInterval()[1];
                j++;
            }
            values[1+6*j] = network.getWaitConfidenceInterval()[0];
            values[2+6*j] = network.getWaitConfidenceInterval()[1];
            values[3+6*j] = network.getThroughputConfidenceInterval()[0];
            values[4+6*j] = network.getThroughputConfidenceInterval()[1];
            values[5+6*j] = network.getPopulationConfidenceInterval()[0];
            values[6+6*j] = network.getPopulationConfidenceInterval()[1];
            values[31] = price.getTotalNetworkPriceInterval()[0];
            values[32] = price.getTotalNetworkPriceInterval()[1];
            values[33] = currentTimeLimit;
            writer.writeLine(values);
            writer.flush();
            for(Server server : servers){
                server.resetBetweenRunsMetrics();
            }
            network.resetBetweenRunsMetrics();
            price.resetBetweenRunsValues();

        }
        writer.flushAndClose();
    }


    /**
     * It runs a batch means mode simulation.
     * */
    private static void runBatchMeansSimulation(Generator generator, BatchMeansWriter writer, Server[] servers) {
        int batchSize = Params.BM_NUM_EVENTS / Params.BM_NUM_BATCHES;
        double initialArrivalRate = Params.MEAN_INTERARRIVAL_RATE;
        /*
         * To reduce the duration of the simulation, the used values for the arrivalRate used are differentiated
         * according to the configuration of the network.
         * This is done because when the network is congested and the scheduling discipline is PS, it is extremely
         * expensive to run the simulation.
         * */
        for(double arrivalRate: arrivalRates) {
            if(arrivalRate > 7.0 && (actualConfiguration == 'A' || actualConfiguration == 'D' || actualConfiguration == 'G')){
                continue;
            } else if(arrivalRate <7.0 && (actualConfiguration != 'A' && actualConfiguration != 'D' && actualConfiguration != 'G')){
                continue;
            }
            generator.plantSeeds(123456789);
            setArrivalRate(arrivalRate);
            System.out.println("BM - Arrival Rate = " + arrivalRate);


            currentTime = START;
            EventList eventList = new EventList(generator, currentTime);

            int numBatch = 0;
            while (numBatch < Params.BM_NUM_BATCHES) {

                //to correctly collect measures we need to know when the current batch is started
                double currentBatchStartTime = currentTime;
                for (Server server : servers) {
                    server.setCurrentBatchStartTime(currentBatchStartTime);
                }
                network.setCurrentBatchStartTime(currentBatchStartTime);

                for (int executionInBatch = 0; executionInBatch < batchSize; executionInBatch++) {
                    Event nextEvent = eventList.removeNextEvent();
                    handleEvent(nextEvent, servers, generator, eventList);
                }


                for (Server server : servers) {
                    server.updateBetweenRunsMetrics(currentTime);
                    server.resetInRunMetrics();
                }
                network.updateBetweenRunsMetrics(currentTime);
                price.updateBetweenRunsValues(currentTime - currentBatchStartTime);
                network.resetInRunMetrics();
                price.resetInRunValues();
                numBatch++;

            }

            for(Server server:servers){
                server.computeAutocorrelationValues();
            }
            network.computeAutocorrelationValues();

            double[] values = new double[48];
            values[0] = arrivalRate;
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
            values[46] = price.getTotalNetworkPriceInterval()[0];
            values[47] = price.getTotalNetworkPriceInterval()[1];
            writer.writeLine(values);
            writer.flush();


            for (Server server : servers) {
                server.removeAllEventsInCenter();
                server.resetBetweenRunsMetrics();
            }
            network.resetBetweenRunsMetrics();
            network.removeAllEventsInNetwork();
            price.resetBetweenRunsValues();

        }
        writer.flushAndClose();
        setArrivalRate(initialArrivalRate);
    }

    /**
     * For batch means simulation only.
     * */
    private static void setArrivalRate(double arrivalRate) {
        Params.MEAN_INTERARRIVAL_RATE = arrivalRate;
        Params.MEAN_INTERARRIVAL_S3 = 1/arrivalRate;
    }

    /**
     * Handle an Event with EventType Arrival*
     * It puts the Event event in the Server server; if the position of the event in the server's queue is 0, we need to
     * insert the event in the EventList.
     * A new Event of the same Arrival* type is created and inserted in the EventList.
     * NOTE: This model has only 1 EventType Arrival* (ArrivalS3), but this method is more general than that.
     * */
    private static void handleArrival(Server server, Event event, EventList eventList, Generator generator, EventType newEventType){
        Event newEvent = new Event(newEventType, generator, currentTime, server.getNumJobsInCenter(), server.getDiscipline());

        int position = server.insertJobInCenter(newEvent, currentTime);
        network.insertJobInNetwork(currentTime);

        if(position == 0){
            eventList.putEvent(newEvent.getType(), newEvent);
        }

        Event newArrival = new Event(event.getType(), generator, currentTime, server.getNumJobsInCenter(), server.getDiscipline());
        eventList.putEvent(newArrival.getType(), newArrival);
    }

    /**
     * Insert the Event event in the Server destination, if the position of the event in the destination queue is 0
     * we need to insert the event in the EventList.
     * */
    private static void routeTo(Event event, Server destination, EventList eventList){
        int position = destination.insertJobInCenter(event, currentTime);
        if(position == 0){
            eventList.putEvent(event.getType(), event);
        }
    }


    /**
     * Handle an Event with EventType Completation*
     * */
    private static void handleCompletation(Server server, Event event, Generator generator, EventList eventList, Server[] servers){
        //It removes the event from the server queue and, if the queue is not empty, it update the EventList.
        server.removeNextEvent();
        Event newEvent = server.getNextCompletation(currentTime);
        if(newEvent != null) {
            eventList.putEvent(newEvent.getType(), newEvent);
        }
        server.increaseDeparture();

        //In handles the routing.
        ServerEnum nextCenter = event.getNextCenter();
        if(nextCenter == null){
            network.increaseDeparture();
        } else {
            switch (nextCenter) {
                case VM1:
                    Server vm1 = servers[ServerEnum.VM1.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM1, generator, currentTime, vm1.getNumJobsInCenter(), vm1.getDiscipline());
                    if(vm1.getDiscipline() == PS)
                        vm1.updateJobsTimeAfterArrival(event);
                    routeTo(newEvent, vm1, eventList);
                    break;
                case S3:
                    Server s3 = servers[ServerEnum.S3.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONS3, generator, currentTime, s3.getNumJobsInCenter(), s3.getDiscipline());
                    routeTo(newEvent, s3, eventList);
                    break;
                case VM2CPU:
                    Server vm2cpu = servers[ServerEnum.VM2CPU.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM2CPU, generator, currentTime, vm2cpu.getNumJobsInCenter(), vm2cpu.getDiscipline());
                    if(vm2cpu.getDiscipline() == PS)
                        vm2cpu.updateJobsTimeAfterArrival(event);
                    routeTo(newEvent, vm2cpu, eventList);
                    break;
                case VM2BAND:
                    Server vm2band = servers[ServerEnum.VM2BAND.getCenterIndex()-1];
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

    /**
     * It handles an event: it update the system state and the clock and it calls handleArrival or handleCompletation.
     * */
    private static void handleEvent(Event event, Server[] servers, Generator generator, EventList eventList){
        double nextEventTime = event.getEndTime();

        for(Server server: servers) {
            server.updateInRunMetrics(currentTime, nextEventTime);
        }
        network.updateInRunMetrics(currentTime, nextEventTime);
        price.updateInRunValues(currentTime,nextEventTime, servers, event.getType()==EventType.ARRIVALS3);

        currentTime = nextEventTime;

        Server vm1 = servers[ServerEnum.VM1.getCenterIndex()-1];
        Server s3 = servers[ServerEnum.S3.getCenterIndex()-1];
        Server vm2cpu = servers[ServerEnum.VM2CPU.getCenterIndex()-1];
        Server vm2band = servers[ServerEnum.VM2BAND.getCenterIndex()-1];

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
                System.err.println("ERRORE FATALE");
                System.exit(-1);
                break;
        }

    }

}
