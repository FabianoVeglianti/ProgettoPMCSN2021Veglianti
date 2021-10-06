package reteDiCode;

import processorSharingSingolo.Server;

public class BetweenRunsSystemMetrics {

    private BetweenRunsMetrics throughput;
    private BetweenRunsMetrics wait;

    public BetweenRunsSystemMetrics(){
        throughput = new BetweenRunsMetrics();
        wait = new BetweenRunsMetrics();
    }

    public void updateBetweenRunsSystemMetrics(Server[] servers, double current){
        double systemExitCounter = 0.0;
        double totalDepartures = 0.0;
        for(Server server:servers){
            totalDepartures += server.getDeparture();
        }

        double systemAverageWait = 0.0;
        for(int i = 0; i < servers.length; i++){
            systemExitCounter += servers[i].getExitCounter();
            double serverDeparture = servers[i].getDeparture();
            systemAverageWait += serverDeparture / totalDepartures * servers[i].getAverageWait();
        }
        throughput.updateMetrics(systemExitCounter / current);
        wait.updateMetrics(systemAverageWait);
    }

    public void resetMetrics(){
        wait.resetValue();
        throughput.resetValue();
    }

    public double[] getThroughputConfidenceInterval(){
        return throughput.getConfidenceInterval();
    }

    public double[] getWaitConfidenceInterval(){
        return wait.getConfidenceInterval();
    }

}
