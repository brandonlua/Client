package wtf.fentanyl.event.impl.game.player;

import wtf.fentanyl.event.Event;

public class SafeWalkEvent extends Event {
    private boolean safeWalk;

    public SafeWalkEvent(boolean safeWalk) {
        this.safeWalk = safeWalk;
    }

    public boolean isSafeWalk() {
        return this.safeWalk;
    }

    public void setSafeWalk(boolean safeWalk) {
        this.safeWalk = safeWalk;
    }
}