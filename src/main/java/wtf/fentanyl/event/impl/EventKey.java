package wtf.fentanyl.event.impl;

import wtf.fentanyl.event.Event;
import lombok.Getter;

@Getter
public class EventKey extends Event {
    private final int key;

    public EventKey(int key) {
        this.key = key;
    }
}