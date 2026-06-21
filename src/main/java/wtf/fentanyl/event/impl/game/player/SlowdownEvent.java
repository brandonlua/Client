package wtf.fentanyl.event.impl.game.player;

import wtf.fentanyl.event.Event;

public class SlowdownEvent extends Event {
    public float strafe, forward;
    public boolean allowedSprinting;

    public SlowdownEvent(float strafe, float forward) {
        this.strafe = strafe;
        this.forward = forward;
        this.allowedSprinting = false;
    }

    public float getStrafe() {
        return strafe;
    }

    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }

    public float getForward() {
        return forward;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public boolean isAllowedSprinting() {
        return allowedSprinting;
    }

    public void setAllowedSprinting(boolean allowedSprinting) {
        this.allowedSprinting = allowedSprinting;
    }
}