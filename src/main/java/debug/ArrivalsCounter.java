package debug;

import reteDiCode.BetweenRunsMetric;
import reteDiCode.EventType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

public class ArrivalsCounter {

    HashMap<EventType, Integer> arrivalsCounterPerReplica;
    HashMap<EventType, BetweenRunsMetric> arrivalsCounter;

    public ArrivalsCounter(){
        arrivalsCounterPerReplica = new HashMap<>();
        arrivalsCounter = new HashMap<>();

        arrivalsCounterPerReplica.put(EventType.ARRIVALS3, 0);

        arrivalsCounter.put(EventType.ARRIVALS3, new BetweenRunsMetric());
    }

    public void increaseCounter(EventType type){
        arrivalsCounterPerReplica.put(type, arrivalsCounterPerReplica.get(type) +1);
    }

    public void resetCounters() {
        for(EventType key: arrivalsCounterPerReplica.keySet()){
            arrivalsCounterPerReplica.put(key, 0);
        }
    }
    public void commitCounters(double current){
        for(EventType key: arrivalsCounterPerReplica.keySet()){
            arrivalsCounter.get(key).updateMetrics(arrivalsCounterPerReplica.get(key)/current);
        }
    }

    public ArrayList<Double> getCounters(){
        ArrayList<Double> counters = new ArrayList<>();

        EventType[] keys = {EventType.ARRIVALS3};

        for(EventType key: keys){
            counters.add(arrivalsCounter.get(key).getSampleMean());
        }

        return counters;
    }
}
