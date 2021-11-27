package writer;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CSVWriter {


    private static String fhpath = null;
    private static String bmpath = null;

    PrintWriter fh_pw;
    PrintWriter bm_pw;

    public CSVWriter(char configuration){
        String prefix = "configuration_" + configuration + "_";
        fhpath = prefix + "fh_metrics.csv";
        bmpath = prefix + "bm_metrics.csv";
        openFiles();
        writeHeaders();
    }



    private void openFiles() {
        FileWriter fh_fw = null;
        FileWriter bm_fw = null;
        try {
            fh_fw = new FileWriter(fhpath, false);
            BufferedWriter fh_bw = new BufferedWriter(fh_fw);
            fh_pw = new PrintWriter(fh_bw);

            bm_fw = new FileWriter(bmpath, false);
            BufferedWriter bm_bw = new BufferedWriter(bm_fw);
            bm_pw = new PrintWriter(bm_bw);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flushFH(){
        fh_pw.flush();
    }

    public void flushAndCloseFHFile(){
        fh_pw.flush();
        fh_pw.close();
    }

    public void flushAndCloseBMFile(){
        bm_pw.flush();
        bm_pw.close();
    }

    private void writeHeaders(){
        fh_pw.println("ITERATIONS,VM1 Mean Wait,VM1 Wait Interval Width,"+
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
                "System Mean Throughput,System Throughput Interval Width,Price,Simulation Time");

        bm_pw.println("Arrival rate,VM1 Mean Wait,VM1 Wait Interval Width,"+
                "VM1 Mean Throughput,VM1 Throughput Interval Width,"+
                "S3 Mean Wait,S3 Wait Interval Width,"+
                "S3 Mean Throughput,S3 Throughput Interval Width,"+
                "VM2CPU Mean Wait,VM2CPU Wait Interval Width,"+
                "VM2CPU Mean Throughput,VM2CPU Throughput Interval Width,"+
                "VM2Band Mean Wait,VM2Band Wait Interval Width,"+
                "VM2Band Mean Throughput,VM2Band Throughput Interval Width,"+
                "System Mean Wait,System Wait Interval Width,"+
                "System Mean Throughput,System Throughput Interval Width,Price,Simulation Time");
    }


    public void writeBMLine(double[] values){
        writeValueInLine(values, bm_pw);
    }

    private void writeValueInLine(double[] values, PrintWriter pw) {
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

    public void writeFHLine(double[] values){
        writeValueInLine(values, fh_pw);
    }

}
