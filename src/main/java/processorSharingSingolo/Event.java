package processorSharingSingolo;

import desUtils.Rngs;
import reteDiCode.CenterEnum;
import reteDiCode.EventType;
import reteDiCode.Generator;

import static reteDiCode.Params.*;

public class Event {

    private CenterEnum nextCenter;

    private final EventType type;

    private double time;

    public EventType getType() {
        return type;
    }
    public double getTime(){
        return time;
    }
    public CenterEnum getNextCenter(){
        return nextCenter;
    }


    /**
    * Istanzia un nuovo evento:
     * se l'evento è un completamento di un job ne viene effettuato il routing.
     * se l'evento è un completamento di un job in un centro PS, l'istante di completamento dipende dai jobs nel server.
    * */
    public Event(EventType type, Generator t, double currentTime, double numJobsInServer){
        this.type = type;
        double routingProbability;
        switch (type){
            case ARRIVALVM1:
                t.selectStream(1);
                time = currentTime + t.exponential(MEAN_INTERARRIVAL_VM1);
                break;

            case ARRIVALS3:
                t.selectStream(3);
                time = currentTime + t.exponential(MEAN_INTERARRIVAL_S3);
                break;

            case ARRIVALVM2CPU:
                t.selectStream(5);
                time = currentTime + t.exponential(MEAN_INTERARRIVAL_VM2CPU);
                break;

            case COMPLETATIONVM1:
                t.selectStream(7);
                time = currentTime + t.exponential(MEAN_SERVICE_TIME_VM1) * (numJobsInServer+1);


                t.selectStream(37);
                routingProbability = t.uniform( 0, 1);

                if(routingProbability<P10){
                    this.nextCenter = null;
                } else if (P10 <= routingProbability && routingProbability < P10+P12){
                    this.nextCenter = CenterEnum.S3;
                } else {
                    this.nextCenter = CenterEnum.VM2CPU;
                }


                break;

            case COMPLETATIONS3:
                t.selectStream(9);
                time = currentTime + t.exponential(MEAN_SERVICE_TIME_S3);


                t.selectStream(39);
                routingProbability = t.uniform( 0, 1);

                if(routingProbability<P20){
                    this.nextCenter = null;
                } else if (P20 <= routingProbability && routingProbability < P20+P21){
                    this.nextCenter = CenterEnum.VM1;
                } else {
                    this.nextCenter = CenterEnum.VM2CPU;
                }


                break;

            case COMPLETATIONVM2CPU:
                t.selectStream(11);
                time = currentTime + t.exponential(MEAN_SERVICE_TIME_VM2CPU) * (numJobsInServer+1);


                t.selectStream(41);
                routingProbability = t.uniform( 0, 1);

                if(routingProbability<P30){
                    this.nextCenter = null;
                } else if (P30 <= routingProbability && routingProbability < P30+P31){
                    this.nextCenter = CenterEnum.VM1;
                } else if(P30+P31 <= routingProbability && routingProbability < P30+P31+P32){
                    this.nextCenter = CenterEnum.S3;
                } else {
                    this.nextCenter = CenterEnum.VM2BAND;
                }

                break;

            case COMPLETATIONVM2BAND:
                t.selectStream(13);
                time = currentTime + t.exponential(MEAN_SERVICE_TIME_VM2BAND) * (numJobsInServer+1);

                t.selectStream(43);
                routingProbability = t.uniform( 0, 1);

                if(routingProbability<P40){
                    this.nextCenter = null;
                } else if (P40 <= routingProbability && routingProbability < P40+P41){
                    this.nextCenter = CenterEnum.VM1;
                } else if (P40+P41 <= routingProbability && routingProbability < P40+P41+P42){
                    this.nextCenter = CenterEnum.S3;
                } else {
                    this.nextCenter = CenterEnum.VM2CPU;
                }

                break;
            default:
                System.err.println("ERRORE FATALE");
                System.exit(-1);
                break;
        }
    }


    /**
     * Aggiorna l'istante in cui l'evento occorre in base al tempo rimanente e al numero di jobs nel server.
     * Il metodo updateTime è pensato per cambiare l'istante di completamento di un job in un centro PS all'occorrenza
     * di un evento che cambia il numero di jobs nel centro (e dunque cambia il tasso di servizio di ciascun job).
     * */
    public void updateTime(double changeTime, double numJobsInServer, boolean isNextEventArrival){
        if (isNextEventArrival) {
            time = changeTime + (time - changeTime) * (numJobsInServer + 1) / numJobsInServer;
        } else {
            time = changeTime + (time - changeTime) * (numJobsInServer - 1) / numJobsInServer;
        }
    }


    @Override
    public String toString(){
        String string = "";
        string = string + type + " - " + time;
        return string;
    }
}
