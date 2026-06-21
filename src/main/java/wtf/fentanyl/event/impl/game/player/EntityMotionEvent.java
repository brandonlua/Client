package wtf.fentanyl.event.impl.game.player;

import wtf.fentanyl.event.Event;

public class EntityMotionEvent extends Event {
    public double motion;

    public EntityMotionEvent(double motion) {
        this.motion = motion;
    }
}
