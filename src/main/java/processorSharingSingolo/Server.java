package processorSharingSingolo;

import reteDiCode.BetweenRunsMetric;
import reteDiCode.CenterEnum;
import reteDiCode.SchedulingDisciplineEnum;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static reteDiCode.SchedulingDisciplineEnum.*;

public class Server {

    //fixed attributes
    private final CenterEnum type;
    private final SchedulingDisciplineEnum discipline;
    public SchedulingDisciplineEnum getDiscipline() { return discipline; }
    public CenterEnum getType() {
        return type;
    }


    //between runs attributes
    private BetweenRunsMetric throughput;
    private BetweenRunsMetric wait;
    private BetweenRunsMetric utilization;
    private BetweenRunsMetric population;

    //for batch means only
    private double currentBatchStartTime;

    public void setCurrentBatchStartTime(double currentBatchStartTime) {
        this.currentBatchStartTime = currentBatchStartTime;
    }

    public  void updateBetweenRunsMetrics(double current){
        //questi operazioni non ha effetto se non stiamo effettuando simulazioni batch means
        double lastArrivalTimeInBatch = lastArrivalTime - currentBatchStartTime;
        double currentBatchDuration = current - currentBatchStartTime;

        //in teoria si dovrebbe avere departure/lastArrivalTime, ma ciò crea problemi nel BM
        if(lastArrivalTimeInBatch != 0) {
            throughput.updateMetrics(departure / lastArrivalTimeInBatch);
            population.updateMetrics(node / lastArrivalTimeInBatch);
        }
        if(departure != 0) {
            wait.updateMetrics(node / departure);
        }
        utilization.updateMetrics(server/currentBatchDuration);



    }

    public double[] getThroughputConfidenceInterval(){
        return throughput.getConfidenceInterval();
    }

    public double[] getWaitConfidenceInterval(){
        return wait.getConfidenceInterval();
    }

    public double[] getUtilitationInterval() {return utilization.getConfidenceInterval(); }

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
    private double server;
    private double departure;
    private int exitCounter;
    private double lastArrivalTime;
    ArrayList<Event> jobsInCenterList;
    private int arrivi = 0;

    public void aumentaArrivi(){
        arrivi+=1;
    }

    public ArrayList<Event> getJobsInCenterList() { return jobsInCenterList;}
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

    public Server(CenterEnum type, SchedulingDisciplineEnum discipline){
        this.type = type;
        this.discipline = discipline;
        jobsInCenterList = new ArrayList<>();

        departure = 0.0;
        exitCounter = 0;

        throughput = new BetweenRunsMetric();
        wait = new BetweenRunsMetric();
        utilization = new BetweenRunsMetric();
        population = new BetweenRunsMetric();

        node = 0.0;
        server = 0.0;

        currentBatchStartTime = 0.0;
    }

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

    public int insertJobInCenter(Event event, double current){
        int position = 0;
        if(jobsInCenterList.size() == 0){
            if(discipline == FIFO){
                event.setEndTime(current);
            }
        } else{
            position = findPosition(event); //la posizione è determinata in base alla disciplina di scheduling
            if(discipline == FIFO){
                event.setEndTime(jobsInCenterList.get(position-1).getEndTime());
            }
        }
            jobsInCenterList.add(position, event);


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

    public Event getNextCompletation(double current){
        if (jobsInCenterList.size()>0) {
            Event nextCompletation = jobsInCenterList.get(0);
            return nextCompletation;
        } else{
                return null;
            }
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

    public double getAveragePopulation(){
        return node/lastArrivalTime;
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


    public void computeAutocorrelationValues() {
        wait.computeAutocorrelationValues();
        throughput.computeAutocorrelationValues();
        population.computeAutocorrelationValues();
    }
}
