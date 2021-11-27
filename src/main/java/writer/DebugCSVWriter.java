package writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DebugCSVWriter {


    private static final String path = "debug.csv";
    private PrintWriter pw;
    private static DebugCSVWriter writer;

    private void openFile() {
        FileWriter fw = null;
        try {
            fw = new FileWriter(path, false);
            BufferedWriter fh_bw = new BufferedWriter(fw);
            pw = new PrintWriter(fh_bw);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHeader(){
        pw.println("configuration,lambda,lambda_VM1,lambda_S3,lambda_VM2CPU,mean_service_time_VM1,mean_service_time_S3,mean_service_time_VM2CPU,mean_service_time_VM2BAND,P00,P01,P02,P03,P04,P10,P11,P12,P13,P14,P20,P21,P22,P23,P24,P30,P31,P32,P33,P34," +
                "P40,P41,P42,P43,P44");
    }

    public void writeLine(char configuration, double[] values) {
        String line = "" + configuration + ",";
        for(int i = 0; i < values.length; i++){
            if(i != values.length -1){
                line = line + values[i]+",";
            } else {
                line = line + values[i];
            }
        }
        pw.println(line);
    }

    public void flushAndClose(){
        pw.flush();
        pw.close();
    }

    public static DebugCSVWriter getWriter() {
        if(writer == null){
            writer = new DebugCSVWriter();
        }
        return writer;
    }

    private DebugCSVWriter(){
        openFile();
        writeHeader();
    }

}
