package reteDiCode;

public enum SchedulingDisciplineEnum {

    FIFO("FIFO"),
    IS("IS"),
    PS("PS");

    private final String discipline;

    SchedulingDisciplineEnum(String discipline) {
        this.discipline = discipline;
    }
}
