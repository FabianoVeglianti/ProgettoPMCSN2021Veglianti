package entity;

import utils.BetweenRunsMetric;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static entity.SchedulingDisciplineType.*;

public class Server {

    //fixed attributes
    private final ServerEnum type;
    private final SchedulingDisciplineType discipline;
    public SchedulingDisciplineType getDiscipline() { return discipline; }
    public ServerEnum getType() {
        return type;
    }


    //between runs attributes
    private BetweenRunsMetric throughput;
    private BetweenRunsMetric wait;
    private BetweenRunsMetric population;
    private BetweenRunsMetric serviceTime;

    //for batch means only
    private double currentBatchStartTime;

    public void setCurrentBatchStartTime(double currentBatchStartTime) {
        this.currentBatchStartTime = currentBatchStartTime;
    }

    public  void updateBetweenRunsMetrics(double current){
        //these operations have no effect if the simulation is not batch means
        double lastArrivalTimeInBatch = lastArrivalTime - currentBatchStartTime;
        double currentBatchDuration = current - currentBatchStartTime;

        if(lastArrivalTimeInBatch != 0) {
            throughput.updateMetrics(departure / lastArrivalTimeInBatch);
            population.updateMetrics(node / currentBatchDuration);
        }
        if(departure != 0) {
            wait.updateMetrics(node / departure);
            if (type == ServerEnum.S3) {
                serviceTime.updateMetrics(node / departure);
            } else {
                serviceTime.updateMetrics(server / departure);
            }
        }


    }

    public double[] getServiceTimeInterval(){
        return serviceTime.getConfidenceInterval();
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
        serviceTime.resetValue();
    }


    //in run attributes
    private double node;
    private double server;
    private double departure;
    private double lastArrivalTime;
    ArrayList<Event> jobsInCenterList;


    public ArrayList<Event> getJobsInCenterList() { return jobsInCenterList;}

    public double getDeparture(){
        return departure;
    }

    public void increaseDeparture(){
        departure +=1;
    }


    public int getNumJobsInCenter(){
        return jobsInCenterList.size();
    }

    public void resetInRunMetrics(){
        departure = 0.0;
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

    public Server(ServerEnum type, SchedulingDisciplineType discipline){
        this.type = type;
        this.discipline = discipline;
        jobsInCenterList = new ArrayList<>();

        departure = 0.0;


        throughput = new BetweenRunsMetric();
        wait = new BetweenRunsMetric();
        population = new BetweenRunsMetric();
        serviceTime = new BetweenRunsMetric();

        node = 0.0;
        server = 0.0;

        currentBatchStartTime = 0.0;
    }

    /**
     * Find the position of the Event event in the queue of the center.
     * The queue is kept sorted based on the endTime value of the jobs.
     *
     * If the center scheduling discipline is FIFO the current event must be insert at the end of the queue becuase
     * the endTime of the current event is after the endTime of the (old) last job in the queue.
     *
     * If the center scheduling discipline is PS or IS the position of the current event depends on the endTime value of
     * the job: the ordering of the queue is used to apply the binary search to efficiently find the position in which
     * to insert the current event.
     * */
    private int findPosition(Event event){
        if(discipline == FIFO){
            if(jobsInCenterList.size() > 0){
                return jobsInCenterList.size();
            } else {
                return 0;
            }
        } else if (discipline == PS || discipline == IS) {
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
        else {
            System.err.println("ERRORE NELLA DISCIPLINA DI SCHEDULING");
            System.exit(-1);
            return -1;
        }
    }

    /**
     * Insert the job associated to the Event event in the queue.
     * If the center scheduling discipline is FIFO, the endTime value of event is set.
     * It uses the method findPosition() to find the position in which to insert the job.
     * */
    public int insertJobInCenter(Event event, double current){
        int position = 0;
        if(jobsInCenterList.size() == 0){
            if(discipline == FIFO){
                event.setEndTime(current);
            }
        } else{
            position = findPosition(event);
            if(discipline == FIFO){
                event.setEndTime(jobsInCenterList.get(position-1).getEndTime());
            }
        }
        jobsInCenterList.add(position, event);


        lastArrivalTime = current;

        return position;
    }

    private double getLastArrivalTime(){return lastArrivalTime;}

    /**
     * Remove the head of the queue.
     * The queue is kept ordered, so the next event is the first of the queue.
     * */
    public void removeNextEvent(){
        if(jobsInCenterList.size()>0){
            jobsInCenterList.remove(0);
        }
    }

    public Event getNextCompletation(double current){
        if (jobsInCenterList.size()>0) {
            return jobsInCenterList.get(0);
        } else{
            return null;
        }
    }

    /**
     * Used only if the scheduling discipline is PS.
     * */
    public void updateJobsTimeAfterCompletation(Event nextEvent){
        for(Event job : jobsInCenterList){
            job.updateTime(nextEvent.getEndTime(), jobsInCenterList.size(), false);
        }
    }

    /**
     * Used only if the scheduling discipline is PS.
     * */
    public void updateJobsTimeAfterArrival(Event nextEvent){
        for(Event job : jobsInCenterList){
            job.updateTime(nextEvent.getEndTime(), jobsInCenterList.size(), true);
        }
    }

    public double getAverageWait(){
        return node/departure;
    }

    public double getAveragePopulation(){
        return node/lastArrivalTime;
    }

    public double getServiceTime() {
        if(type != ServerEnum.S3) {
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
        System.out.println("   average # in the node ... =   " + f.format(node / current));
        System.out.println("   utilization ............. =   " + f.format(server / current));
        // dovrebbe essere lastInterarrival al posto di current
        System.out.println("   lambda .................. =   " + f.format(departure/lastArrivalTime));
        System.out.println("   jobs nel centro ......... =   " + f.format(jobsInCenterList.size()));
        System.out.println("");
    }


    public void computeAutocorrelationValues() {
        wait.computeAutocorrelationValues();
        throughput.computeAutocorrelationValues();
        population.computeAutocorrelationValues();
    }
}
