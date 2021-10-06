package reteDiCode;

import processorSharingSingolo.Event;
import processorSharingSingolo.Server;
import writer.CSVWriter;


public class Runner {

    public static int MIN_ITERATIONS_PER_REPLICA = 100;
    public static double MAX_ITERATIONS_PER_REPLICA = 10000;
    public static double NUM_REPLICAS = 100;
    public static double START = 0.0;
    static double current;



    public static void main(String[] args) {
        Generator generator = new Generator();
        CSVWriter writer = CSVWriter.getInstance();

        Server serverVM1 = new Server(CenterEnum.VM1);
        Server serverS3 = new Server(CenterEnum.S3);
        Server serverVM2CPU = new Server(CenterEnum.VM2CPU);
        Server serverVM2Band = new Server(CenterEnum.VM2BAND);

        Server[] servers = new Server[4];
        servers[0] = serverVM1;
        servers[1] = serverS3;
        servers[2] = serverVM2CPU;
        servers[3] = serverVM2Band;


        int actualIterationsPerReplica = 0;

        while(actualIterationsPerReplica < MAX_ITERATIONS_PER_REPLICA) {
            BetweenRunsSystemMetrics systemMetrics = new BetweenRunsSystemMetrics();

            generator.plantSeeds(123456789);
            actualIterationsPerReplica += MIN_ITERATIONS_PER_REPLICA;
            for(int i = 0; i < NUM_REPLICAS; i++) {
                current = START;

                EventList eventList = new EventList(generator, current);
                int iterations = 0;
                while (iterations < actualIterationsPerReplica) {
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
                    if(o1.getTime() - ( o2).getTime() > 0) {
                        return 1;
                    } else if (( o1).getTime() - ( o2).getTime() < 0) {
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

                    Event nextEvent = eventList.removeNextEvent();
                    handleEvent(nextEvent, servers, generator, eventList);
                    iterations++;

                }


                systemMetrics.updateBetweenRunsSystemMetrics(servers, current);
                for (Server server : servers) {
                    server.printMetrics(current);
                    server.updateBetweenRunsMetrics(current);
                    server.resetInRunMetrics();
                    server.printBetweenRunsMetrics();
                }


            }

            double[] values = new double[21];
            values[0] = actualIterationsPerReplica;
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
            writer.writeFHLine(values);
            writer.flush();
            for(Server server : servers){
                server.resetBetweenRunsMetrics();
            }
            systemMetrics.resetMetrics();
        }
        writer.flushAndCloseFiles();
    }


    private static void handleArrival(Server server, Event event, EventList eventList, Generator generator, EventType newEventType){
        Event newEvent = new Event(newEventType, generator, current, server.getNumJobsInCenter());
        int position = server.insertJobInCenter(newEvent, current);
        if(position == 0){
            eventList.putEvent(newEvent.getType(), newEvent);
        }

        Event newArrival = new Event(event.getType(), generator, current, server.getNumJobsInCenter());
        eventList.putEvent(newArrival.getType(), newArrival);
    }

    private static void routeTo(Event event, Server destination, EventList eventList){
        int position = destination.insertJobInCenter(event, current);
        if(position == 0){
            eventList.putEvent(event.getType(), event);
        }
    }

    private static void handleCompletation(Server server, Event event, Generator generator, EventList eventList, Server[] servers){
        server.removeNextEvent();
        Event newEvent = server.getNextCompletation();
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
                    newEvent = new Event(EventType.COMPLETATIONVM1, generator, current, vm1.getNumJobsInCenter());
                    vm1.updateJobsTimeAfterArrival(event);
                    routeTo(newEvent, vm1, eventList);
                    break;
                case S3:
                    Server s3 = servers[CenterEnum.S3.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONS3, generator, current, s3.getNumJobsInCenter());
                    routeTo(newEvent, s3, eventList);
                    break;
                case VM2CPU:
                    Server vm2cpu = servers[CenterEnum.VM2CPU.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM2CPU, generator, current, vm2cpu.getNumJobsInCenter());
                    vm2cpu.updateJobsTimeAfterArrival(event);
                    routeTo(newEvent, vm2cpu, eventList);
                    break;
                case VM2BAND:
                    Server vm2band = servers[CenterEnum.VM2BAND.getCenterIndex()-1];
                    newEvent = new Event(EventType.COMPLETATIONVM2BAND, generator, current, vm2band.getNumJobsInCenter());
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
        double nextEventTime = event.getTime();
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
                vm1.updateJobsTimeAfterArrival(event);
                handleArrival(vm1, event, eventList, generator, EventType.COMPLETATIONVM1);
                break;
            case ARRIVALS3:
                handleArrival(s3, event, eventList, generator, EventType.COMPLETATIONS3);
                break;
            case ARRIVALVM2CPU:
                vm2cpu.updateJobsTimeAfterArrival(event);
                handleArrival(vm2cpu, event, eventList, generator, EventType.COMPLETATIONVM2CPU);
                break;
            case COMPLETATIONVM1:
                vm1.updateJobsTimeAfterCompletation(event);
                handleCompletation(vm1, event, generator, eventList, servers);
                break;
            case COMPLETATIONS3:
                handleCompletation(s3, event, generator, eventList, servers);
                break;
            case COMPLETATIONVM2CPU:
                vm2cpu.updateJobsTimeAfterCompletation(event);
                handleCompletation(vm2cpu, event, generator, eventList, servers);
                break;
            case COMPLETATIONVM2BAND:
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
