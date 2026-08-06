package wtf.rania.event.impl;

import wtf.rania.event.Event;
import lombok.Getter;

@Getter
public class EventKey extends Event {
    private final int key;

    public EventKey(int key) {
        this.key = key;
    }
}