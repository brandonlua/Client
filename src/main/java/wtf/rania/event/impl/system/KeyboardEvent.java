package wtf.rania.event.impl.system;

import wtf.rania.event.Event;

public class KeyboardEvent extends Event {
    public final int key;

    public KeyboardEvent(int key) {
        this.key = key;
    }
}
