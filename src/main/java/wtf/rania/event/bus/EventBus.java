package wtf.rania.event.bus;

import wtf.rania.event.Cancellable;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscriber;
import org.jetbrains.annotations.NotNull;

public interface EventBus {

    /**
     * Returns the name of this event bus
     *
     * @since 2.0.0
     */
    @NotNull String name();

    /**
     * Discovers all the valid Listener instances defined by the specified {@link Subscriber} and adds them to the bus.
     *
     * @param subscriber The subscriber to be added
     * @since 1.2
     */
    void subscribe(@NotNull Subscriber subscriber);

    <T> void subscribe(@NotNull Listener<T> listener);

    default void subscribeAll(@NotNull Subscriber... subscribers) {
        for (Subscriber subscriber : subscribers) {
            this.subscribe(subscriber);
        }
    }

    default void subscribeAll(@NotNull Iterable<Subscriber> subscribers) {
        subscribers.forEach(this::subscribe);
    }


    default void subscribeAll(@NotNull Listener<?>... listeners) {
        for (Listener<?> listener : listeners) {
            this.subscribe(listener);
        }
    }

    void unsubscribe(@NotNull Subscriber subscriber);

    <T> void unsubscribe(@NotNull Listener<T> listener);

    default void unsubscribeAll(@NotNull Subscriber... subscribers) {
        for (Subscriber subscriber : subscribers) {
            this.unsubscribe(subscriber);
        }
    }

    default void unsubscribeAll(@NotNull Iterable<Subscriber> subscribers) {
        subscribers.forEach(this::unsubscribe);
    }

    default void unsubscribeAll(@NotNull Listener<?>... listeners) {
        for (Listener<?> listener : listeners) {
            this.unsubscribe(listener);
        }
    }

    <T> void post(@NotNull T event);

    default boolean post(@NotNull Cancellable event) {
        this.post((Object) event);
        return event.isCancelled();
    }
}
