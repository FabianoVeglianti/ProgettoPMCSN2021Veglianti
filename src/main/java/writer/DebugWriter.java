package writer;

/**
 * Writer for Debug simulations.
 * */
public class DebugWriter extends Writer{


    protected void writeHeader(){
        pw.println("discipline,configuration,lambda,mean_service_time_VM1,mean_service_time_S3,mean_service_time_VM2CPU,mean_service_time_VM2BAND,P00,P01,P02,P03,P04,P10,P11,P12,P13,P14,P20,P21,P22,P23,P24,P30,P31,P32,P33,P34," +
                "P40,P41,P42,P43,P44");
    }



    public DebugWriter(){
        super();
        path = "debug.csv";
        super.openFile();
        writeHeader();
    }

    public void writeLine(String discipline, char configuration ,double[] values) {
        String line = "" + discipline + ","+configuration+",";
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
