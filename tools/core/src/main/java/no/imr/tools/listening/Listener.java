package no.imr.tools.listening;

import no.marec.lsss.api.util.observing.Observable;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * A {@link Consumer} that does not use the argument passed to {@link Consumer#accept(Object)}.
 */
@FunctionalInterface
public interface Listener extends Consumer<Object> {
   @Override
   default void accept(Object argument) {
      listen();
   }

   void listen();

   default void addTo(Observable<?>... observables) {
      for (Observable<?> observable : observables) {
         observable.subscribe(this);
      }
   }

   default void addTo(Collection<? extends Observable<?>> observables) {
      for (Observable<?> observable : observables) {
         observable.subscribe(this);
      }
   }

   default void addToAndNotify(Observable<?>... observables) {
      addTo(observables);
      listen();
   }

   static Listener of(Listener listener) {
      return listener;
   }

   static Listener doNothing() {
      return () -> {
      };
   }
}
