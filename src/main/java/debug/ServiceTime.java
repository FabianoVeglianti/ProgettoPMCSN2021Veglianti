package debug;

import processorSharingSingolo.Server;
import reteDiCode.BetweenRunsMetric;
import reteDiCode.CenterEnum;

import java.util.ArrayList;
import java.util.HashMap;

public class ServiceTime {

    HashMap<CenterEnum, BetweenRunsMetric> serviceTimeMap;

    public ServiceTime(Server[] servers){
        serviceTimeMap = new HashMap<>();
        for(Server center: servers){
            serviceTimeMap.put(center.getType(), new BetweenRunsMetric());
        }
    }

    public void updateTimes(Server[] servers){
        for(Server server: servers){
            double time = server.getServiceTime();
            serviceTimeMap.get(server.getType()).updateMetrics(time);
        }
    }

    public ArrayList<Double> getServiceTimes(){
        ArrayList<Double> returnValues = new ArrayList<>();

        CenterEnum[] centers = {CenterEnum.VM1, CenterEnum.S3, CenterEnum.VM2CPU, CenterEnum.VM2BAND};

        for(CenterEnum center: centers){
            returnValues.add(serviceTimeMap.get(center).getSampleMean());
        }

        return returnValues;
    }

}
