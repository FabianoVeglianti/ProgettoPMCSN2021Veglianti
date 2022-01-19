package writer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Abstract Writer.
 * */
public abstract class Writer {


    protected String path;
    protected PrintWriter pw;

    protected void openFile() {
        FileWriter fw;
        try {
            fw = new FileWriter(path, false);
            BufferedWriter bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract void writeHeader();

    public void writeLine(double[] values) {
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

    public void flush(){
        pw.flush();
    }

    public void flushAndClose(){
        pw.flush();
        pw.close();
    }


}
