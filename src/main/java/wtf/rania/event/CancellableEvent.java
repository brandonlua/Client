package wtf.rania.event;

import me.zero.alpine.event.Cancellable;

public class CancellableEvent implements Cancellable {

    private boolean cancelled;

    public CancellableEvent() {
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }
}