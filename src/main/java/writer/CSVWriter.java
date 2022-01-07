package writer;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CSVWriter extends Writer{



    public CSVWriter(char configuration, String discipline){
        String prefix = "configuration_" + configuration + "_" + discipline + "_";
        path = prefix + "fh_metrics.csv";
        openFile();
        writeHeader();
    }


    protected void writeHeader(){
        pw.println("ITERATIONS,VM1 Mean Wait,VM1 Wait Interval Width,"+
                "VM1 Mean Throughput,VM1 Throughput Interval Width,"+
                "VM1 Mean Population,VM1 Population Interval Width,"+
                "S3 Mean Wait,S3 Wait Interval Width,"+
                "S3 Mean Throughput,S3 Throughput Interval Width,"+
                "S3 Mean Population,S3 Population Interval Width,"+
                "VM2CPU Mean Wait,VM2CPU Wait Interval Width,"+
                "VM2CPU Mean Throughput,VM2CPU Throughput Interval Width,"+
                "VM2CPU Mean Population,VM2CPU Population Interval Width,"+
                "VM2Band Mean Wait,VM2Band Wait Interval Width,"+
                "VM2Band Mean Throughput,VM2Band Throughput Interval Width,"+
                "VM2Band Mean Population,VM2Band Population Interval Width,"+
                "System Mean Wait,System Wait Interval Width,"+
                "System Mean Throughput,System Throughput Interval Width,"+
                "System Mean Population,System Population Interval Width,Price,Simulation Time");

    }


}
