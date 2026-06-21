package wtf.fentanyl.event.impl.game.player;

import wtf.fentanyl.event.Event;

public class KeepSprintEvent extends Event {
    public boolean greater;

    public KeepSprintEvent(boolean greater) {
        this.greater = greater;
    }
}
