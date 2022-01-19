package entity;

import utils.Generator;

import static config.Params.*;
import static entity.SchedulingDisciplineType.FIFO;


/**
 * It models an occuring event.
 * */
public class Event {

    private ServerEnum nextCenter;

    private final EventType type;


    /**
     * endTime keeps the Clock value at which the event will be triggered.
     * If the event has associated a job (type is not Arrival*):
     *      arrivalTime keeps the Clock value at which the job entered in the server;
     *      serviceTime keeps the amount of time the server must spend on the job before the event can be triggered.
     * */
    private double endTime;
    private double arrivalTime;
    private double serviceTime;

    public EventType getType() {
        return type;
    }
    public double getEndTime(){
        return endTime;
    }
    public double getServiceTime(){
        return serviceTime;
    }
    public double getArrivalTime() { return arrivalTime; }
    public ServerEnum getNextCenter(){
        return nextCenter;
    }


    /**
     * If the EventType is Completation* the nextCenter variable is set to a value according to a RNG and the
     * routing table.
     *
     * If the EventType is Completation* and the associated center's scheduling discipline is FIFO the endTime variable
     * is not set immediately.
     *
     * If the EventType is Completation* and the associated center's scheduling discipline is PS the endTime variable is
     * set immediately, it depends of the number of jobs in the center and it can change if the number of jobs
     * in the center changes
     *
     * If the EventType is Completation* and the associated center's scheduling discipline is IS the endTime variable is
     * set immediately, is equals to arrivalTime+serviceTime and it cannot change.
    * */
    public Event(EventType type, Generator t, double currentTime, double numJobsInServer, SchedulingDisciplineType serverSchedulingDiscipline){
        this.type = type;
        double routingProbability;
        switch (type){

            case ARRIVALS3:
                t.selectStream(3);
                endTime = currentTime + t.exponential(MEAN_INTERARRIVAL_S3);
                break;

            case COMPLETATIONVM1:
                t.selectStream(57);
                double distributionProb = t.uniform(0,1);


                t.selectStream(7);
                arrivalTime = currentTime;
                if(serverSchedulingDiscipline == FIFO) {
                    serviceTime = t.exponential(MEAN_SERVICE_TIME_VM1);
                } else {
                    serviceTime = t.exponential(MEAN_SERVICE_TIME_VM1);
                    endTime = currentTime + serviceTime * (numJobsInServer+1);
                }


                t.selectStream(37);
                routingProbability = t.uniform( 0, 1);

                if(routingProbability<P10) {
                    this.nextCenter = null;
                }else if (P10 <= routingProbability && routingProbability < P10+P11){
                    this.nextCenter = ServerEnum.VM1;
                } else if (P10+P11 <= routingProbability && routingProbability < P10+P11+P12){
                    this.nextCenter = ServerEnum.S3;
                } else {
                    this.nextCenter = ServerEnum.VM2CPU;
                }


                break;

            case COMPLETATIONS3:
                t.selectStream(9);
                arrivalTime = currentTime;
                serviceTime = t.exponential(MEAN_SERVICE_TIME_S3);
                endTime = currentTime + serviceTime;


                t.selectStream(39);
                routingProbability = t.uniform( 0, 1);

                if(routingProbability<P20){
                    this.nextCenter = null;
                } else if (P20 <= routingProbability && routingProbability < P20+P21) {
                    this.nextCenter = ServerEnum.VM1;
                } else if (P20+P21 <= routingProbability && routingProbability < P20+P21+P22){
                    this.nextCenter = ServerEnum.S3;
                } else {
                    this.nextCenter = ServerEnum.VM2CPU;
                }


                break;

            case COMPLETATIONVM2CPU:
                t.selectStream(11);
                arrivalTime = currentTime;
                if(serverSchedulingDiscipline == FIFO) {
                    serviceTime = t.exponential(MEAN_SERVICE_TIME_VM2CPU);
                } else {
                    serviceTime = t.exponential(MEAN_SERVICE_TIME_VM2CPU);
                    endTime = currentTime + serviceTime * (numJobsInServer+1);
                }



                t.selectStream(41);
                routingProbability = t.uniform( 0, 1);

                if(routingProbability<P30){
                    this.nextCenter = null;
                } else if (P30 <= routingProbability && routingProbability < P30+P31){
                    this.nextCenter = ServerEnum.VM1;
                } else if(P30+P31 <= routingProbability && routingProbability < P30+P31+P32) {
                    this.nextCenter = ServerEnum.S3;
                } else if (P30+P31+P32 <= routingProbability && routingProbability < P30+P31+P32+P33) {
                    this.nextCenter = ServerEnum.VM2CPU;
                } else {
                    this.nextCenter = ServerEnum.VM2BAND;
                }

                break;

            case COMPLETATIONVM2BAND:
                t.selectStream(13);
                arrivalTime = currentTime;
                if(serverSchedulingDiscipline == FIFO) {
                    serviceTime = t.exponential(MEAN_SERVICE_TIME_VM2BAND);
                } else {
                    serviceTime = t.exponential(MEAN_SERVICE_TIME_VM2BAND);
                    endTime = currentTime + serviceTime * (numJobsInServer+1);
                }


                t.selectStream(43);
                routingProbability = t.uniform( 0, 1);

                if(routingProbability<P40){
                    this.nextCenter = null;
                } else if (P40 <= routingProbability && routingProbability < P40+P41){
                    this.nextCenter = ServerEnum.VM1;
                } else if (P40+P41 <= routingProbability && routingProbability < P40+P41+P42){
                    this.nextCenter = ServerEnum.S3;
                } else {
                    this.nextCenter = ServerEnum.VM2CPU;
                }

                break;
            default:
                System.err.println("ERRORE FATALE");
                System.exit(-1);
                break;
        }
    }

    /**
     * If the EventType is Completation* and the associated center's scheduling discipline is FIFO the endTime depends
     * on the time the job is started and the serviceTime associated with it.
     * */
    public void setEndTime(double startServiceTime){
        endTime = startServiceTime + serviceTime;
    }

    /**
     * If the EventType is Completation* and the associated center's scheduling discipline is PS the endTime variable
     * value changes as the number of jobs in the center changes.
     * Read the documentation for more details about the formulas.
     * */
    public void updateTime(double changeTime, double numJobsInServer, boolean isNextEventArrival){
        if (isNextEventArrival) {
            endTime = changeTime + (endTime - changeTime) * (numJobsInServer + 1) / numJobsInServer;
        } else {
            endTime = changeTime + (endTime - changeTime) * (numJobsInServer - 1) / numJobsInServer;
        }
    }


    @Override
    public String toString(){
        String string = "";
        string = string + type + " - " + endTime;
        return string;
    }
}
