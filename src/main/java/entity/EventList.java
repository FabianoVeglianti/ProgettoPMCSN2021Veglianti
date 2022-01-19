package entity;

import entity.EventType;
import utils.Generator;
import entity.Event;

import java.util.HashMap;

/**
 * It maintains the next Event for each eventType.
 * */
public class EventList {

    HashMap<EventType, Event> eventList;

    public EventList(Generator generator, double current){
        eventList = new HashMap<>();
        for(EventType type: EventType.values()) {
            if (type == EventType.ARRIVALS3) {
                Event event = new Event(type, generator, current, 0, null);
                eventList.put(type, event);
            } else {
                eventList.put(type, null);
            }
        }
    }

    /*
    * Used also to update the Completation* Events if the associated center scheduling discipline is PS and the number
    * of jobs in the center changes.
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
