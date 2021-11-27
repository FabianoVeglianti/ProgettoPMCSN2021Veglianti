package reteDiCode;

import processorSharingSingolo.Server;

public class BetweenRunsSystemMetrics {

    private BetweenRunsMetric throughput;
    private BetweenRunsMetric wait;

    static private BetweenRunsSystemMetrics istance = null;

    public static BetweenRunsSystemMetrics getIstance(){
            if(istance == null){
                istance = new BetweenRunsSystemMetrics();
            }
            return istance;
    }

    private BetweenRunsSystemMetrics(){
        throughput = new BetweenRunsMetric();
        wait = new BetweenRunsMetric();
    }

    public void updateSystemMetrics(Server[] servers, double current){
        double systemExitCounter = 0.0;
        double totalDepartures = 0.0;
        for(Server server:servers){
            systemExitCounter += server.getExitCounter();
            totalDepartures += server.getDeparture();
        }

        double systemAverageWait = 0.0;
        for(int i = 0; i < servers.length; i++){
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
