package reteDiCode;

import desUtils.Rvms;

public class BetweenRunsMetric {

    private static double CONFIDENCE = 0.95;
    private static Rvms rvms = new Rvms();

    private long i;
    private double sampleMean;
    private double vi;

    public BetweenRunsMetric(){
        sampleMean = 0.0;

        vi = 0.0;
        i = 0;
    }

    public void resetValue(){
        sampleMean = 0.0;

        vi = 0.0;
        i = 0;
    }

    public void updateMetrics(double newValue){
        i++;
        double diff = newValue - sampleMean;
        sampleMean = sampleMean + diff / i;
        vi = vi + diff * diff * (i-1) / i;
    }

    public double getSampleMean(){
        return sampleMean;
    }

/*    public double getSampleVariance(){
        return vi / i;
    }
*/
    public double[] getConfidenceInterval(){
        double alpha = 1-CONFIDENCE;
        double criticalValue = rvms.idfStudent(i-1, 1 - alpha / 2);
        double intervalWidth = criticalValue * Math.sqrt(vi / i) / Math.sqrt(i-1);
        return new double[]{sampleMean, intervalWidth};
    }

}
