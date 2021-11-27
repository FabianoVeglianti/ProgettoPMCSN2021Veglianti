package reteDiCode;

public class Params {

    //routing matrix
    private static final double P01 = 0.7;
    private static final double P02 = 0.2;
    private static final double P03 = 0.1;

    public static final double P10 = 0.45;
    public static final double P11 = 0.05;
    public static final double P12 = 0.3;
    public static final double P13 = 0.2;

    public static final double P20 = 0.15;
    public static final double P21 = 0.3;
    public static final double P22 = 0.05;
    public static final double P23 = 0.5;

    public static final double P30 = 0.25;
    public static final double P31 = 0.15;
    public static final double P32 = 0.05;
    public static final double P33 = 0.05;
    public static final double P34 = 0.5;

    public static final double P40 = 0.8;
    public static final double P41 = 0.1;
    public static final double P42 = 0.05;
    public static final double P43 = 0.05;


    //network parameters
    private static final double MEAN_INTERARRIVAL_RATE = 7.0;
    public static double MEAN_INTERARRIVAL_VM1 = 1/(MEAN_INTERARRIVAL_RATE * P01);
    public static double MEAN_INTERARRIVAL_S3 = 1/(MEAN_INTERARRIVAL_RATE * P02);
    public static double MEAN_INTERARRIVAL_VM2CPU = 1/(MEAN_INTERARRIVAL_RATE * P03);
    public static double MEAN_SERVICE_TIME_VM1;
    public static double MEAN_SERVICE_TIME_S3 = 1.0;
    public static double MEAN_SERVICE_TIME_VM2CPU;
    public static double MEAN_SERVICE_TIME_VM2BAND;

    public static double VM1_PRICE_PER_MINUTE;
    public static double S3_PRICE_PER_REQUEST = 0.02;
    public static double VM2CPU_PRICE_PER_MINUTE;
    public static double VM2BAND_PRICE_PER_MINUTE;

    //simulations enablers
    public static final boolean runFiniteHorizonSimulation = true;
    public static final boolean runBatchMeansSimulation = false;

    //simulations parameters
    public static final double NUM_REPLICAS = 64;

    //finite horizon parameters
    public static final int FH_MIN_COUNTER_LIMIT = 2;
    public static final double FH_MAX_COUNTER_LIMIT = 100;

    //batch means parameters
    public static final int BM_NUM_BATCHES = 64;
    public static final int BM_NUM_EVENTS = 10000000;

    //debug parameters
    public static final boolean DEBUG_MODE_ON = false;
    public static final int DEBUG_ITERATIONS = 100000;
}
