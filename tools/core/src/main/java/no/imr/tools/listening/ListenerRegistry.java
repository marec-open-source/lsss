package no.imr.tools.listening;

import com.google.common.collect.ImmutableList;
import no.marec.lsss.api.util.observing.Observable;
import no.marec.lsss.api.util.observing.Subscription;
import no.marec.lsss.api.util.observing.SubscriptionRegistry;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ListenerRegistry implements SubscriptionRegistry {
   private final ImmutableList.Builder<Subscription> builder = ImmutableList.builder();

   public ListenerRegistry() {
   }

   @Override
   public <T> void add(Observable<T> observable, Consumer<? super T> observer) {
      builder.add(observable.subscribe(observer));
   }

   public <T> void add(Observable<T> observable, Listener listener) {
      add(observable, (Consumer<? super T>) listener);
   }

   public <T> void add(Collection<? extends Observable<? extends T>> observables, Consumer<T> observer) {
      observables.forEach(observable -> add(observable, observer));
   }

   public void add(Collection<? extends Observable<?>> observables, Listener listener) {
      add(observables, (Consumer<Object>) listener);
   }

   public void add(Stream<? extends Observable<?>> observables, Listener listener) {
      observables.forEach(observable -> add(observable, listener));
   }

   @Override
   public <T> void add(Consumer<T> observer, Collection<? extends Observable<? extends T>> observables) {
      add(observables, observer);
   }

   public void add(Listener listener, Collection<? extends Observable<?>> observables) {
      add(observables, listener);
   }

   public ImmutableList<Subscription> build() {
      return builder.build();
   }
}
