package entity;


/**
 * Lists the Scheduling Discipline types
 * */
public enum SchedulingDisciplineType {

    FIFO("FIFO"),
    IS("IS"),
    PS("PS");

    private final String discipline;

    SchedulingDisciplineType(String discipline) {
        this.discipline = discipline;
    }
}
