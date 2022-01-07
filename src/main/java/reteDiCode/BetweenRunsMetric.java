package reteDiCode;

import desUtils.AcsModified;
import desUtils.Rvms;

public class BetweenRunsMetric {

    private static double CONFIDENCE = 0.95;
    private static Rvms rvms = new Rvms();

    private long i;
    private double sampleMean;
    private double vi;

    private AcsModified acs = null;

    public BetweenRunsMetric(){
        sampleMean = 0.0;

        vi = 0.0;
        i = 0;

        acs = new AcsModified();
    }

    public void resetValue(){
        sampleMean = 0.0;

        vi = 0.0;
        i = 0;

        acs.resetValues();
    }

    public void updateMetrics(double newValue){
        i++;
        if(i <= 2){
            double diff = newValue - sampleMean;
            sampleMean = sampleMean + diff / i;
            vi = vi + diff * diff * (i-1) / i;
            acs.insertValue(newValue);
        } else {
            double diff = newValue - sampleMean;
            sampleMean = sampleMean + diff / i;
            vi = vi + diff * diff * (i - 1) / i;
            acs.updateValue(newValue);
        }
    }

    public double getSampleMean(){
        return sampleMean;
    }

/*    public double getSampleVariance(){
        return vi / i;
    }
*/
    public double[] getConfidenceIntervalAndAutocorrelationLagOne(){
        double alpha = 1-CONFIDENCE;
        double criticalValue = rvms.idfStudent(i-1, 1 - alpha / 2);
        double intervalWidth = criticalValue * Math.sqrt(vi / i) / Math.sqrt(i-1);

        return new double[]{sampleMean, intervalWidth, acs.getAutocorrelationWithLagOneComputation()};
    }

    public double[] getConfidenceInterval(){
        double alpha = 1-CONFIDENCE;
        double criticalValue = rvms.idfStudent(i-1, 1 - alpha / 2);
        double intervalWidth = criticalValue * Math.sqrt(vi / i) / Math.sqrt(i-1);
        return new double[]{sampleMean, intervalWidth};
    }

    public void computeAutocorrelationValues() {
        acs.computeAutocorrelationValues();
    }
}
