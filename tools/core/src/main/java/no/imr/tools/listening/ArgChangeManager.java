package no.imr.tools.listening;

import no.marec.lsss.api.util.observing.Observable;
import no.marec.lsss.api.util.observing.Subscription;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * For managing a set of listeners.
 */
public sealed class ArgChangeManager<T> implements Consumer<T>, Observable<T> permits ChangeManager {

   private final CopyOnWriteArrayList<Consumer<? super T>> listeners = new CopyOnWriteArrayList<>();

   public ArgChangeManager() {
   }

   @Override
   public String toString() {
      return listeners.size() + " listeners";
   }

   @Override
   public Subscription subscribe(Consumer<? super T> observer) {
      addListener(observer);
      return () -> removeListener(observer);
   }

   public void addListener(Consumer<? super T> listener) {
      if (listener == this) {
         throw new IllegalArgumentException();
      }
      listeners.addIfAbsent(listener);
   }

   public void addListener(Listener listener) {
      addListener((Consumer<? super T>) listener);
   }

   public void removeListener(Consumer<? super T> listener) {
      listeners.remove(listener);
   }

   public boolean isEmpty() {
      return listeners.isEmpty();
   }

   public int getListenerCount() {
      return listeners.size();
   }

   public void notifyListeners(T argument) {
      listeners.forEach(listener -> listener.accept(argument));
   }

   @Override
   public void accept(T argument) {
      notifyListeners(argument);
   }
}
