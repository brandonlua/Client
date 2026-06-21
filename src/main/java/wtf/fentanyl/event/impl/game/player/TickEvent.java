package wtf.fentanyl.event.impl.game.player;

import wtf.fentanyl.event.Event;

public class TickEvent extends Event {

    private final EventType type;

    public TickEvent(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public enum EventType {
        PRE,
        POST
    }
}
