package reteDiCode;

import processorSharingSingolo.Event;

import java.util.HashMap;

public class EventList {

    HashMap<EventType, Event> eventList;

    /*
    * Nota: questo costrutture dovrebbe essere chiamato una sola volta e il valore di current dovrebbe essere 0.
    * */
    public EventList(Generator generator, double current){
        eventList = new HashMap<>();
        for(EventType type: EventType.values()) {
            if (type == EventType.ARRIVALS3 || type == EventType.ARRIVALVM1 || type == EventType.ARRIVALVM2CPU) {
                Event event = new Event(type, generator, current, 0);
                eventList.put(type, event);
            } else {
                eventList.put(type, null);
            }
        }
    }

    /*
    * Usato anche per aggiornare gli eventi di completamento quando cambia il numero di jobs in un nodo
    * */
    public void putEvent(EventType type, Event event){
        eventList.put(type, event);
    }

    public Event removeEventByType(EventType type){
        return eventList.remove(type);
    }

    public Event removeNextEvent(){
        return removeEventByType(this.getNextEventType());
    }

    public EventType getNextEventType(){
        double time = Double.MAX_VALUE;
        EventType type = null;
        for(EventType eventType: EventType.values()){
            Event event = eventList.get(eventType);
            if(event != null){
                if(event.getEndTime() < time){
                    type = eventType;
                    time = event.getEndTime();
                }
            }
        }
        return type;
    }


}
