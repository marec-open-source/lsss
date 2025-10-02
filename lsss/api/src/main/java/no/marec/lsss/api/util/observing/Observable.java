package no.marec.lsss.api.util.observing;

import no.marec.lsss.api.DoNotImplement;

import java.util.function.Consumer;

/**
 * Interface for classes that can be observed.
 *
 * @param <T> the notification type
 */
@DoNotImplement
public interface Observable<T> {
   /**
    * Registers an observer.
    *
    * @param observer the recipient of the notifications
    * @return a subscription that the observer can later unsubscribe from
    */
   Subscription subscribe(Consumer<? super T> observer);
}
