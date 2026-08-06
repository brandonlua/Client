package wtf.rania.event.impl.game.player;

import wtf.rania.event.Event;

public class StrafeEvent extends Event {
    public float strafe;
    public float forward;
    public float friction;
    private float yaw;

    public StrafeEvent(float strafe, float forward, float friction) {
        this.strafe = strafe;
        this.forward = forward;
        this.friction = friction;
    }

    public float getStrafe() {
        return this.strafe;
    }

    public float getForward() {
        return this.forward;
    }

    public float getFriction() {
        return this.friction;
    }

    public void setStrafe(float float1) {
        this.strafe = float1;
    }

    public void setForward(float float1) {
        this.forward = float1;
    }

    public void setFriction(float float1) {
        this.friction = float1;
    }
}