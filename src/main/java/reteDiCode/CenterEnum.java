package reteDiCode;

public enum CenterEnum {

    VM1("VM1", 1),
    S3("S3", 2),
    VM2CPU("VM2CPU", 3),
    VM2BAND("VM2Band", 4);

    private final String centerName;
    private final int centerIndex;

    CenterEnum(String centerName, int centerIndex) {
        this.centerName = centerName;
        this.centerIndex = centerIndex;
    }

    public int getCenterIndex(){
        return this.centerIndex;
    }

    public String getCenterName(){
        return this.centerName;
    }

}
