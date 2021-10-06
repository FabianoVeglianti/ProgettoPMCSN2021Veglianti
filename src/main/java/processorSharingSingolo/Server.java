package processorSharingSingolo;

import reteDiCode.BetweenRunsMetrics;
import reteDiCode.CenterEnum;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class Server {

    private int numJobsInCenter;
    private double departure;

    //in run metrics
    private double node;
    private double queue;
    private double server;

    public CenterEnum getType() {
        return type;
    }

    CenterEnum type;

    private double lastArrivalTime;

    private BetweenRunsMetrics throughput;
    private BetweenRunsMetrics wait;

    private double exitCounter;

    public double getDeparture(){
        return departure;
    }

    public double getExitCounter(){
        return exitCounter;
    }

    public void increaseExitCounter(){
        exitCounter++;
    }

    public int getNumJobsInCenter(){
        return numJobsInCenter;
    }

    public  void updateBetweenRunsMetrics(double current){
        throughput.updateMetrics(departure / lastArrivalTime);
        wait.updateMetrics(node / departure);
    }

    ArrayList<Event> jobsInCenterList;

    public Server(CenterEnum type){
        this.type = type;
        jobsInCenterList = new ArrayList<>();

        numJobsInCenter = 0;
        departure = 0.0;
        exitCounter = 0.0;

        throughput = new BetweenRunsMetrics();
        wait = new BetweenRunsMetrics();

        node = 0.0;
        queue = 0.0;
        server = 0.0;
    }

    public void resetInRunMetrics(){
        node = 0.0;
        queue = 0.0;
        server = 0.0;

        jobsInCenterList = new ArrayList<>();
        numJobsInCenter = 0;
        departure = 0.0;
        exitCounter = 0.0;
    }

    public void updateInRunMetrics(double currentTime, double nextTime) {
        if (numJobsInCenter > 0) {
            node = node + numJobsInCenter * (nextTime - currentTime);
            queue = queue + (numJobsInCenter - 1) * (nextTime - currentTime);
            server = server + (nextTime - currentTime);
        }
    }

    public void increaseDeparture(){
        departure +=1;
    }

    private int findPosition(Event event){
        int low = 0;
        int high = numJobsInCenter - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            Double midTime = jobsInCenterList.get(mid).getTime();
            int cmp = midTime.compareTo(event.getTime());

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        return low;
    }

    public void removeNextEvent(){
        if(jobsInCenterList.size()>0){
            jobsInCenterList.remove(0);
            numJobsInCenter -=1;
        }
    }

    public Event getNextCompletation(){
        if (jobsInCenterList.size()>0)
            return jobsInCenterList.get(0);
        else
            return null;
    }

    public int insertJobInCenter(Event event, double current){
        int position = 0;
        if(numJobsInCenter == 0){
            jobsInCenterList.add(event);
        } else{
            position = findPosition(event);
            jobsInCenterList.add(position, event);
        }
        numJobsInCenter +=1;
        lastArrivalTime = current;
        return position;
    }


    public void updateJobsTimeAfterCompletation(Event nextEvent){
        for(Event job : jobsInCenterList){
            job.updateTime(nextEvent.getTime(), numJobsInCenter, false);
        }
    }


    public void updateJobsTimeAfterArrival(Event nextEvent){
        for(Event job : jobsInCenterList){
            job.updateTime(nextEvent.getTime(), numJobsInCenter, true);
        }
    }


    public void printBetweenRunsMetrics(){
        DecimalFormat f = new DecimalFormat("###0.00000000000");
        System.out.println("\nServer " +this.type + " beetwen runs metrics:");
        System.out.println("   average throughput =   " + f.format(throughput.getSampleMean()));
        System.out.println("   average wait =   " + f.format(wait.getSampleMean()));
    }

    public double[] getThroughputConfidenceInterval(){
        return throughput.getConfidenceInterval();
    }

    public double[] getWaitConfidenceInterval(){
        return wait.getConfidenceInterval();
    }

    public void resetBetweenRunsMetrics(){
        wait.resetValue();
        throughput.resetValue();
    }

    public double getAverageWait(){
        return node/departure;
    }

    public void printMetrics(double current){
        DecimalFormat f = new DecimalFormat("###0.00000000000");
        System.out.println("\nServer " +this.type + " for " + departure + " jobs");
        System.out.println("   average interarrival time =   " + f.format(lastArrivalTime / departure));
        System.out.println("   average wait ............ =   " + f.format(node/ departure));
        System.out.println("   average delay ........... =   " + f.format(queue/ departure));
        System.out.println("   average service time .... =   " + f.format(server/ departure));
        System.out.println("   average # in the node ... =   " + f.format(node / current));
        System.out.println("   average # in the queue .. =   " + f.format(queue / current));
        System.out.println("   utilization ............. =   " + f.format(server / current));
        System.out.println("   lambda .................. =   " + f.format(departure/lastArrivalTime));
        System.out.println("");
    }
}
