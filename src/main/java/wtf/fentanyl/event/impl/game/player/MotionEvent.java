package wtf.fentanyl.event.impl.game.player;

import wtf.fentanyl.event.Event;

public class MotionEvent extends Event {

    public float yaw;
    public float pitch;
    public double posY;
    public double posZ;
    public double posX;
    public boolean onGround;
    public State state;

    public MotionEvent(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, State state) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.state = state;
    }

    public MotionEvent(State state) {
        this.state = state;
    }

    public enum State {
        PRE,
        POST
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public double getY() {
        return posY;
    }

    public double getZ() {
        return posZ;
    }

    public double getX() {
        return posX;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public State getState() {
        return state;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setY(double posY) {
        this.posY = posY;
    }

    public void setZ(double posZ) {
        this.posZ = posZ;
    }

    public void setX(double posX) {
        this.posX = posX;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public void setState(State state) {
        this.state = state;
    }
}
