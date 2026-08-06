package wtf.rania.event;

public interface Cancellable {

    /**
     * Cancels this event. Equivalent to {@code setCancelled(true)}.
     */
    default void cancel() {
        this.setCancelled(true);
    }

    /**
     * Sets the cancelled state of this event
     *
     * @param cancel The new state
     */
    void setCancelled(boolean cancel);

    /**
     * Returns whether the event has been cancelled
     */
    boolean isCancelled();
}
