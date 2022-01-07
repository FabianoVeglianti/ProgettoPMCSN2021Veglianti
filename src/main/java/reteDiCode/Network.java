package reteDiCode;

import processorSharingSingolo.Server;

import java.text.DecimalFormat;

public class Network {


    //between runs attributes
    private BetweenRunsMetric throughput;
    private BetweenRunsMetric wait;
    private BetweenRunsMetric utilization;
    private BetweenRunsMetric population;
    private double currentBatchStartTime;

    public  void updateBetweenRunsMetrics(double current){
        //in teoria si dovrebbe avere departure/lastArrivalTime, ma ciò crea problemi nel BM
        if(lastArrivalTime != 0) {
            throughput.updateMetrics(departure / lastArrivalTime);
            population.updateMetrics(node / lastArrivalTime);
        }
        if(departure != 0) {
            wait.updateMetrics(node / departure);
        }

    }

    public  void updateBetweenRunsMetricsUsingServers(double current, Server[] servers){
        double systemExitCounter = 0.0;
        double totalDepartures = 0.0;
        for(Server server:servers){
            systemExitCounter += server.getExitCounter();
            totalDepartures += server.getDeparture();
        }
        double systemAveragePopulation = 0.0;
        double systemAverageWait = 0.0;
        for(int i = 0; i < servers.length; i++){
            double serverDeparture = servers[i].getDeparture();
            systemAverageWait += serverDeparture / systemExitCounter * servers[i].getAverageWait();
            systemAveragePopulation += servers[i].getAveragePopulation();
        }

        population.updateMetrics(systemAveragePopulation);
        throughput.updateMetrics(systemExitCounter / current);
        wait.updateMetrics(systemAverageWait);
    }

    public double[] getThroughputConfidenceInterval(){
        return throughput.getConfidenceInterval();
    }

    public double[] getWaitConfidenceInterval(){
        return wait.getConfidenceInterval();
    }


    public double[] getPopulationInterval() {return population.getConfidenceInterval(); }

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
        utilization.resetValue();
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

    public Network(){


        departure = 0.0;
        numJobsInNetwork = 0;

        throughput = new BetweenRunsMetric();
        wait = new BetweenRunsMetric();
        utilization = new BetweenRunsMetric();
        population = new BetweenRunsMetric();

        node = 0.0;
    }



    public void insertJobInNetwork(double current){
        numJobsInNetwork = numJobsInNetwork +1;
        lastArrivalTime = current;
    }


 //   public void removeNextEvent(){
//        numJobsInNetwork = numJobsInNetwork -1;
  //  }



    public double getAverageWait(){
        return node/departure;
    }


    public void printBetweenRunsMetrics(){
        DecimalFormat f = new DecimalFormat("###0.00000000000");
        System.out.println("   average throughput =   " + f.format(throughput.getSampleMean()));
        System.out.println("   average wait =   " + f.format(wait.getSampleMean()));
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
}
