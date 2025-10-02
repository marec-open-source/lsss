package no.marec.lsss.api.util.observing;

import no.marec.lsss.api.DoNotImplement;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * A collection of {@link Subscription}s that can be unsubscribed later.
 */
@DoNotImplement
public interface SubscriptionRegistry {

   /**
    * Subscribes an observer to an observable and adds the subscription to this registry.
    *
    * @param observable an observable
    * @param observer   an observer
    */
   <T> void add(Observable<T> observable, Consumer<? super T> observer);

   /**
    * Subscribes an observer to several observables and adds the subscription to this registry.
    *
    * @param observer    an observer
    * @param observables several observables
    */
   <T> void add(Consumer<T> observer, Collection<? extends Observable<? extends T>> observables);
}
