package writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class BatchMeansWriter extends Writer {


    public BatchMeansWriter(char configuration, String discipline){
        super();
        String prefix = "configuration_" + configuration + "_" + discipline + "_";
        path = prefix + "bm_metrics.csv";
        super.openFile();
        writeHeader();
    }


    protected void writeHeader(){
        pw.println("Arrival rate,VM1 Mean Wait,VM1 Wait Interval Width,VM1 Wait Autocorrelation Lag One,"+
                "VM1 Mean Throughput,VM1 Throughput Interval Width,VM1 Throughput Autocorrelation Lag One,"+
                "VM1 Mean Population,VM1 Population Interval Width,VM1 Population Autocorrelation Lag One,"+
                "S3 Mean Wait,S3 Wait Interval Width,S3 Wait Autocorrelation Lag One,"+
                "S3 Mean Throughput,S3 Throughput Interval Width,S3 Throughput Autocorrelation Lag One,"+
                "S3 Mean Population,S3 Population Interval Width,S3 Population Autocorrelation Lag One,"+
                "VM2CPU Mean Wait,VM2CPU Wait Interval Width,VM2CPU Wait Autocorrelation Lag One,"+
                "VM2CPU Mean Throughput,VM2CPU Throughput Interval Width,VM2CPU Throughput Autocorrelation Lag One,"+
                "VM2CPU Mean Population,VM2CPU Population Interval Width,VM2CPU Population Autocorrelation Lag One,"+
                "VM2Band Mean Wait,VM2Band Wait Interval Width,VM2Band Wait Autocorrelation Lag One,"+
                "VM2Band Mean Throughput,VM2Band Throughput Interval Width,VM2Band Throughput Autocorrelation Lag One,"+
                "VM2Band Mean Population,VM2Band Population Interval Width,VM2Band Population Autocorrelation Lag One,"+
                "System Mean Wait,System Wait Interval Width,System Wait Autocorrelation Lag One,"+
                "System Mean Throughput,System Throughput Interval Width,System Throughput Autocorrelation Lag One,"+
                "System Mean Population,System Population Interval Width,System Population Autocorrelation Lag One");
    }

}
