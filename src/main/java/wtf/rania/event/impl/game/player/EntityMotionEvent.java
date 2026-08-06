package wtf.rania.event.impl.game.player;

import wtf.rania.event.Event;

public class EntityMotionEvent extends Event {
    public double motion;

    public EntityMotionEvent(double motion) {
        this.motion = motion;
    }
}
