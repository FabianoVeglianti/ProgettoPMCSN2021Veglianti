package utils;

import entity.Server;
import utils.BetweenRunsMetric;

import static config.Params.*;

/**
 * Class used to compute the mean price (€/min) to keep the system running.
 * */
public class Price {


    private BetweenRunsMetric betweenRunsVM1Utilization;
    private BetweenRunsMetric betweenRunsS3ArrivalRate;
    private BetweenRunsMetric betweenRunsVM2Utilization;
    private double inRunVM1ActiveTime;
    private int inRunS3Arrival;
    private double inRunVM2ActiveTime;

    public Price(){
        betweenRunsVM1Utilization = new BetweenRunsMetric();
        betweenRunsS3ArrivalRate = new BetweenRunsMetric();
        betweenRunsVM2Utilization = new BetweenRunsMetric();
        inRunVM1ActiveTime = 0.0;
        inRunS3Arrival = 0;
        inRunVM2ActiveTime = 0.0;
    }

    public void updateBetweenRunsValues(double currentBatchDuration){
        betweenRunsVM1Utilization.updateMetrics(inRunVM1ActiveTime /currentBatchDuration);
        betweenRunsS3ArrivalRate.updateMetrics(inRunS3Arrival / currentBatchDuration);
        betweenRunsVM2Utilization.updateMetrics(inRunVM2ActiveTime /currentBatchDuration);
    }

    public void updateInRunValues(double currentTime, double nextTime, Server[] servers, boolean isArrivalS3){
        if(isArrivalS3){
            inRunS3Arrival += 1;
        }
        double timeInterval = nextTime - currentTime;

        if(servers[0].getNumJobsInCenter() > 0){
            inRunVM1ActiveTime += timeInterval;
        }

        //VM2 is active if VM2CPU is active or VM2Band is active
        if(servers[2].getNumJobsInCenter() > 0 || servers[3].getNumJobsInCenter() > 0){
            inRunVM2ActiveTime += timeInterval;
        }

    }

    public void resetInRunValues(){
        inRunVM1ActiveTime = 0.0;
        inRunS3Arrival = 0;
        inRunVM2ActiveTime = 0.0;
    }

    public void resetBetweenRunsValues() {
        betweenRunsVM1Utilization.resetValue();
        betweenRunsS3ArrivalRate.resetValue();
        betweenRunsVM2Utilization.resetValue();
    }


    public double[] getTotalNetworkPriceInterval(){
        double[] vm1 = betweenRunsVM1Utilization.getConfidenceInterval();
        double[] s3 = betweenRunsS3ArrivalRate.getConfidenceInterval();
        double[] vm2 = betweenRunsVM2Utilization.getConfidenceInterval();

        double[] result = {0, 0};

        for(int i = 0; i < 2; i++){
            result[i] = vm1[i] * VM1_PRICE_PER_MINUTE + s3[i] * S3_PRICE_PER_REQUEST + vm2[i] * VM2CPU_PRICE_PER_MINUTE;
        }

        return result;
    }


}
