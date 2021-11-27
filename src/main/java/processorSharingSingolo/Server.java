package processorSharingSingolo;

import reteDiCode.BetweenRunsMetric;
import reteDiCode.CenterEnum;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Server {

    //fixed attributes
    private final CenterEnum type;
    public CenterEnum getType() {
        return type;
    }


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
        utilization.updateMetrics(server/current);

    }

    public double[] getThroughputConfidenceInterval(){
        return throughput.getConfidenceInterval();
    }

    public double[] getWaitConfidenceInterval(){
        return wait.getConfidenceInterval();
    }

    public double[] getUtilitationInterval() {return utilization.getConfidenceInterval(); }

    public double[] getPopulationInterval() {return population.getConfidenceInterval(); }

    public void resetBetweenRunsMetrics(){
        wait.resetValue();
        throughput.resetValue();
        utilization.resetValue();
        population.resetValue();
    }


    //in run attributes
    private double node;
    private double server;
    private double departure;
    private int exitCounter;
    private double lastArrivalTime;
    ArrayList<Event> jobsInCenterList;
    private int arrivi = 0;

    public void aumentaArrivi(){
        arrivi+=1;
    }

    public int getArrivi(){
        return arrivi;
    }
    public double getDeparture(){
        return departure;
    }

    public void increaseDeparture(){
        departure +=1;
    }

    public double getExitCounter(){
        return (double)exitCounter;
    }

    public void increaseExitCounter(){
        exitCounter++;
    }

    public int getNumJobsInCenter(){
        return jobsInCenterList.size();
    }

    public void resetInRunMetrics(){
        departure = 0.0;
        exitCounter = 0;
        node = 0.0;
        server = 0.0;
    }

    public void removeAllEventsInCenter(){
        jobsInCenterList = new ArrayList<>();
    }


    public void updateInRunMetrics(double currentTime, double nextTime) {
        if (jobsInCenterList.size() > 0) {
            node = node + jobsInCenterList.size() * (nextTime - currentTime);
            server = server + (nextTime - currentTime);
        }
    }

    public Server(CenterEnum type){
        this.type = type;
        jobsInCenterList = new ArrayList<>();

        departure = 0.0;
        exitCounter = 0;

        throughput = new BetweenRunsMetric();
        wait = new BetweenRunsMetric();
        utilization = new BetweenRunsMetric();
        population = new BetweenRunsMetric();

        node = 0.0;
        server = 0.0;
    }

    private int findPosition(Event event){
        int low = 0;
        int high = jobsInCenterList.size() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            Double midTime = jobsInCenterList.get(mid).getEndTime();
            int cmp = midTime.compareTo(event.getEndTime());

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        return low;
    }

    public int insertJobInCenter(Event event, double current){
        int position = 0;
        if(jobsInCenterList.size() == 0){
            jobsInCenterList.add(event);
        } else{
            position = findPosition(event);
            jobsInCenterList.add(position, event);
        }
        lastArrivalTime = current;
        this.aumentaArrivi();  //DA ELIMINARE
        return position;
    }

    private double getLastArrivalTime(){return lastArrivalTime;}

    public void removeNextEvent(){
        if(jobsInCenterList.size()>0){
            jobsInCenterList.remove(0);
        }
    }

    public Event getNextCompletation(){
        if (jobsInCenterList.size()>0)
            return jobsInCenterList.get(0);
        else
            return null;
    }

    public void updateJobsTimeAfterCompletation(Event nextEvent){
        for(Event job : jobsInCenterList){
            job.updateTime(nextEvent.getEndTime(), jobsInCenterList.size(), false);
        }
    }

    public void updateJobsTimeAfterArrival(Event nextEvent){
        for(Event job : jobsInCenterList){
            job.updateTime(nextEvent.getEndTime(), jobsInCenterList.size(), true);
        }
    }

    public double getAverageWait(){
        return node/departure;
    }

    public double getServiceTime() {
        if(type != CenterEnum.S3) {
            return server / departure;
        } else {
            return node / departure;
        }
    }

    public void printBetweenRunsMetrics(){
        DecimalFormat f = new DecimalFormat("###0.00000000000");
        System.out.println("\nServer " +this.type + " beetwen runs metrics:");
        System.out.println("   average throughput =   " + f.format(throughput.getSampleMean()));
        System.out.println("   average wait =   " + f.format(wait.getSampleMean()));
    }

    public void printMetrics(double current){
        DecimalFormat f = new DecimalFormat("###0.00000000000");
        System.out.println("\nServer " +this.type + " for " + departure + " jobs");
        // dovrebbe essere lastInterarrival al posto di current
        System.out.println("   average interarrival time =   " + f.format(lastArrivalTime / departure));
        System.out.println("   average wait ............ =   " + f.format(node/ departure));
        System.out.println("   average service time .... =   " + f.format(server/ departure));
        System.out.println("   average # in the node ... =   " + f.format(node / lastArrivalTime));
        System.out.println("   utilization ............. =   " + f.format(server / lastArrivalTime));
        // dovrebbe essere lastInterarrival al posto di current
        System.out.println("   lambda .................. =   " + f.format(departure/lastArrivalTime));
        System.out.println("   arrivi .................. =   " + f.format(arrivi));
        System.out.println("   jobs nel centro ......... =   " + f.format(jobsInCenterList.size()));
        System.out.println("");
    }
}
