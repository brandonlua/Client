package wtf.fentanyl.event.impl.system;

import wtf.fentanyl.event.Event;

public class KeyboardEvent extends Event {
    public final int key;

    public KeyboardEvent(int key) {
        this.key = key;
    }
}
