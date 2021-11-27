package debug;

import reteDiCode.BetweenRunsMetric;
import reteDiCode.CenterEnum;

import java.util.ArrayList;
import java.util.HashMap;

public class RoutingMatrix {

    private static class RouteData {
        private final CenterEnum source;
        private final CenterEnum destination;
        private int counter;
        private BetweenRunsMetric estimation;

        private RouteData(CenterEnum source, CenterEnum destination){
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
        for(CenterEnum source: CenterEnum.values()){
            RouteData data = new RouteData(source, null);
            routeDataList.add(data);
            for (CenterEnum destination: CenterEnum.values()){
                data = new RouteData(source, destination);
                routeDataList.add(data);
            }
        }
        int i = 0;
        for(RouteData routeData: routeDataList){
            System.out.println("Elemento " + i + ": " + routeData.source + " " + routeData.destination);
            i++;
        }
    }

    private RouteData findRouteData(CenterEnum source, CenterEnum destination){
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
        //non dovrebbe mai essere eseguito
        return null;
    }

    public void increaseCounter(CenterEnum source, CenterEnum destination){
        RouteData data = findRouteData(source, destination);
        assert data != null;
        data.increaseCounter();
    }

    public void commitCounters(){
        HashMap<CenterEnum, Integer> mapSourceDepartures = new HashMap<>();
        for(CenterEnum centerEnum: CenterEnum.values()){
            mapSourceDepartures.put(centerEnum, 0);
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

        CenterEnum[] centerEnums = {CenterEnum.VM1, CenterEnum.S3, CenterEnum.VM2CPU, CenterEnum.VM2BAND};

        for(CenterEnum source: centerEnums){
            RouteData data = findRouteData(source, null);
            assert data != null;
            frequencies.add(data.estimation.getSampleMean());
            for(CenterEnum destination: centerEnums){
                data = findRouteData(source, destination);
                assert data != null;
                frequencies.add(data.estimation.getSampleMean());

            }
        }
        return frequencies;
    }





}
