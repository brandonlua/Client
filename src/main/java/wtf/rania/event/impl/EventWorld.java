package wtf.rania.event.impl;

import wtf.rania.event.Event;
import net.minecraft.world.World;

public class EventWorld extends Event {
    private final World oldWorld;
    private final World newWorld;

    public EventWorld(World oldWorld, World newWorld) {
        this.oldWorld = oldWorld;
        this.newWorld = newWorld;
    }

    public World getOldWorld() {
        return oldWorld;
    }

    public World getNewWorld() {
        return newWorld;
    }
}