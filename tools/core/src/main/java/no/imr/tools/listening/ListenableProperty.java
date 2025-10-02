package no.imr.tools.listening;

import no.marec.lsss.api.util.observing.ObservableProperty;
import no.marec.lsss.api.util.observing.Subscription;

import java.util.Objects;
import java.util.function.Consumer;

public final class ListenableProperty<T> implements Consumer<T>, ObservableProperty<T> {
   private final ArgChangeManager<T> changeManager = new ArgChangeManager<>();
   private T value;

   public ListenableProperty(T value) {
      this.value = value;
   }

   @Override
   public String toString() {
      return Objects.toString(value);
   }

   @Override
   public Subscription subscribe(Consumer<? super T> observer) {
      return changeManager.subscribe(observer);
   }

   @Override
   public T getValue() {
      return value;
   }

   @Override
   public void setValue(T value) {
      if (this.value.equals(value)) {
         return;
      }
      this.value = value;
      changeManager.notifyListeners(value);
   }

   @Override
   public void accept(T value) {
      setValue(value);
   }
}
