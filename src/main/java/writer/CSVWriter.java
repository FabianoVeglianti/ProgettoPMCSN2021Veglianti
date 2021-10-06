package writer;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CSVWriter {

    private static CSVWriter writer = null;
    private static String fhpath = null;

    PrintWriter pw;

    private CSVWriter(){
        fhpath = "fh_metrics.csv";
        openFile();
        writeHeader();
    }


    public static CSVWriter getInstance(){
        if(writer == null) {
            writer = new CSVWriter();
        }
        return writer;
    }


    private void openFile() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(fhpath, false);
            BufferedWriter bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flush(){
        pw.flush();
    }

    public void flushAndCloseFiles(){
        pw.flush();
        pw.close();
    }

    private void writeHeader(){
        pw.println("ITERATIONS,VM1 Mean Wait,VM1 Wait Interval Width,"+
                "VM1 Mean Throughput,VM1 Throughput Interval Width,"+
                "S3 Mean Wait,S3 Wait Interval Width,"+
                "S3 Mean Throughput,S3 Throughput Interval Width,"+
                "VM2CPU Mean Wait,VM2CPU Wait Interval Width,"+
                "VM2CPU Mean Throughput,VM2CPU Throughput Interval Width,"+
                "VM2Band Mean Wait,VM2Band Wait Interval Width,"+
                "VM2Band Mean Throughput,VM2Band Throughput Interval Width,"+
                "System Mean Wait,System Wait Interval Width,"+
                "System Mean Throughput,System Throughput Interval Width");
    }


    public void writeFHLine(double[] values){
        String line = "";
        for(int i = 0; i < values.length; i++){
            if(i != values.length -1){
                line = line + values[i]+",";
            } else {
                line = line + values[i];
            }
        }
        pw.println(line);
    }

}
