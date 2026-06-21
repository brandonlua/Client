package wtf.fentanyl.event.impl;

import wtf.fentanyl.event.Event;
import net.minecraft.client.gui.ScaledResolution;

public class Event3D extends Event {
    private final float partialTicks;
    private final ScaledResolution scaledResolution;

    public Event3D(float partialTicks, ScaledResolution scaledResolution) {
        this.partialTicks = partialTicks;
        this.scaledResolution = scaledResolution;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public ScaledResolution getScaledResolution() {
        return scaledResolution;
    }

    public int getScaleFactor() {
        return scaledResolution.getScaleFactor();
    }
}