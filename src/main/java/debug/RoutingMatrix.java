package debug;

import utils.BetweenRunsMetric;
import entity.ServerEnum;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * It calculate routing frequencies and keeps them in a list of RouteData.
 *
 * RouteData calculate the routing frequency between source and destination.
 * Source and Destination are ServerEnum data types, but destination can be null to indicate the outbound routing.
 *
 *
 * */

public class RoutingMatrix {

    private static class RouteData {
        private final ServerEnum source;
        private final ServerEnum destination;
        private int counter;
        private BetweenRunsMetric estimation;

        private RouteData(ServerEnum source, ServerEnum destination){
            this.source = source;
            this.destination = destination;

            counter = 0;
            estimation = new BetweenRunsMetric();
        }

        private void resetCounter(){
            counter = 0;
        }


        private void increaseCounter(){
            counter += 1;
        }

        private void commitCounter(double totalSourceDepartures){
            double value = counter/totalSourceDepartures;
            estimation.updateMetrics(value);
        }


    }

    ArrayList<RouteData> routeDataList;

    public RoutingMatrix(){
        routeDataList = new ArrayList<>();
        for(ServerEnum source: ServerEnum.values()){
            RouteData data = new RouteData(source, null);
            routeDataList.add(data);
            for (ServerEnum destination: ServerEnum.values()){
                data = new RouteData(source, destination);
                routeDataList.add(data);
            }
        }
    }

    private RouteData findRouteData(ServerEnum source, ServerEnum destination){
        for(RouteData data: routeDataList){
            if(destination == null){
                if (data.source.equals(source) && data.destination == null)
                    return data;
            } else {
                if(data.destination != null){
                    if(data.source.equals(source) && data.destination.equals(destination)){
                        return data;
                    }
                }
            }

        }
        return null;
    }

    public void increaseCounter(ServerEnum source, ServerEnum destination){
        RouteData data = findRouteData(source, destination);
        assert data != null;
        data.increaseCounter();
    }

    public void commitCounters(){
        HashMap<ServerEnum, Integer> mapSourceDepartures = new HashMap<>();
        for(ServerEnum serverEnum : ServerEnum.values()){
            mapSourceDepartures.put(serverEnum, 0);
        }

        for(RouteData data: routeDataList){
            mapSourceDepartures.put(data.source, mapSourceDepartures.get(data.source)+data.counter);
        }

        for(RouteData data: routeDataList){
            data.commitCounter(mapSourceDepartures.get(data.source));
        }
    }

    public void resetCounters(){
        for(RouteData data: routeDataList){
            data.resetCounter();
        }
    }

    public ArrayList<Double> getRoutingFrequencies(){
        ArrayList<Double> frequencies = new ArrayList<>();

        ServerEnum[] serverEnums = {ServerEnum.VM1, ServerEnum.S3, ServerEnum.VM2CPU, ServerEnum.VM2BAND};

        for(ServerEnum source: serverEnums){
            RouteData data = findRouteData(source, null);
            assert data != null;
            frequencies.add(data.estimation.getSampleMean());
            for(ServerEnum destination: serverEnums){
                data = findRouteData(source, destination);
                assert data != null;
                frequencies.add(data.estimation.getSampleMean());

            }
        }
        return frequencies;
    }





}
