package wtf.rania.event.impl.game.player;

import wtf.rania.event.Event;

public class SwapItemEvent extends Event {
    private final int slot;
    private final int offset;

    public SwapItemEvent(int slot, int offset) {
        this.slot = slot;
        this.offset = Math.min(Math.max(offset, -1), 1);
    }

    public int setSlot(int integer) {
        return this.slot >= 0 && this.slot <= 8 ? this.slot : Math.floorMod(integer - this.offset, 9);
    }
}