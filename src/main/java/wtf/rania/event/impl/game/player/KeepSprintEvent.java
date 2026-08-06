package wtf.rania.event.impl.game.player;

import wtf.rania.event.Event;

public class KeepSprintEvent extends Event {
    public boolean greater;

    public KeepSprintEvent(boolean greater) {
        this.greater = greater;
    }
}
