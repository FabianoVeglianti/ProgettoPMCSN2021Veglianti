package entity;

import utils.BetweenRunsMetric;

import java.text.DecimalFormat;


/**
 * It is used to handle the network's metrics.
 * */
public class Network {

    //between runs attributes
    private BetweenRunsMetric throughput;
    private BetweenRunsMetric wait;
    private BetweenRunsMetric population;
    private double currentBatchStartTime;


    public  void updateBetweenRunsMetrics(double current){
        //this operation has not effect if the simulation is not BatchMeans
        double lastArrivalTimeInBatch = lastArrivalTime - currentBatchStartTime;
        double currentBatchDuration = current - currentBatchStartTime;

        if(lastArrivalTimeInBatch != 0) {
            throughput.updateMetrics(departure / lastArrivalTimeInBatch);
            population.updateMetrics(node / currentBatchDuration);
        }
        if(departure != 0) {
            wait.updateMetrics(node / departure);
        }
    }


    public double[] getThroughputConfidenceInterval(){
        return throughput.getConfidenceInterval();
    }

    public double[] getWaitConfidenceInterval(){
        return wait.getConfidenceInterval();
    }


    public double[] getPopulationConfidenceInterval() {return population.getConfidenceInterval(); }

    public double[] getThroughputConfidenceIntervalAndAutocorrelationLagOne(){
        return throughput.getConfidenceIntervalAndAutocorrelationLagOne();
    }

    public double[] getWaitConfidenceIntervalAndAutocorrelationLagOne(){
        return wait.getConfidenceIntervalAndAutocorrelationLagOne();
    }

    public double[] getPopulationConfidenceIntervalAndAutocorrelationLagOne(){
        return population.getConfidenceIntervalAndAutocorrelationLagOne();
    }


    public void resetBetweenRunsMetrics(){
        wait.resetValue();
        throughput.resetValue();
        population.resetValue();
    }


    //in run attributes
    private double node;
    private double departure;
    private double lastArrivalTime;
    private int numJobsInNetwork;


    public double getDeparture(){
        return departure;
    }

    public void increaseDeparture(){
        numJobsInNetwork = numJobsInNetwork -1;
        departure +=1;
    }

    public int getNumJobsInNetwork(){
        return numJobsInNetwork;
    }

    public void resetInRunMetrics(){
        departure = 0.0;
        node = 0.0;
    }

    public void removeAllEventsInNetwork(){
        numJobsInNetwork = 0;
    }


    public void updateInRunMetrics(double currentTime, double nextTime) {
        if (numJobsInNetwork > 0) {
            node = node + numJobsInNetwork * (nextTime - currentTime);
        }
    }

    public void setCurrentBatchStartTime(double currentBatchStartTime) {
        this.currentBatchStartTime = currentBatchStartTime;
    }

    public Network(){
        departure = 0.0;
        numJobsInNetwork = 0;

        throughput = new BetweenRunsMetric();
        wait = new BetweenRunsMetric();
        population = new BetweenRunsMetric();

        node = 0.0;
    }



    public void insertJobInNetwork(double current){
        numJobsInNetwork = numJobsInNetwork +1;
        lastArrivalTime = current;
    }


    public void printMetrics(double current){
        DecimalFormat f = new DecimalFormat("###0.00000000000");
        // dovrebbe essere lastInterarrival al posto di current
        System.out.println("   average interarrival time =   " + f.format(lastArrivalTime / departure));
        System.out.println("   average wait ............ =   " + f.format(node/ departure));
        System.out.println("   average # in the node ... =   " + f.format(node / lastArrivalTime));
        // dovrebbe essere lastInterarrival al posto di current
        System.out.println("   lambda .................. =   " + f.format(departure/lastArrivalTime));
        System.out.println("");
    }

    public void computeAutocorrelationValues() {
        wait.computeAutocorrelationValues();
        throughput.computeAutocorrelationValues();
        population.computeAutocorrelationValues();
    }
}
