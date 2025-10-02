package no.imr.tools.listening;

import no.marec.lsss.api.util.observing.Observable;
import no.marec.lsss.api.util.observing.Subscription;

import java.util.function.Consumer;

public final class ObservableChangeManager<T> implements Observable<T> {
   private final Listener subscriptionListener;
   private final ArgChangeManager<T> changeManager = new ArgChangeManager<>();

   public ObservableChangeManager(Listener subscriptionListener) {
      this.subscriptionListener = subscriptionListener;
   }

   @Override
   public Subscription subscribe(Consumer<? super T> observer) {
      changeManager.addListener(observer);
      subscriptionListener.listen();
      return () -> {
         changeManager.removeListener(observer);
         subscriptionListener.listen();
      };
   }

   public void notifyListeners(T argument) {
      changeManager.notifyListeners(argument);
   }

   public boolean isEmpty() {
      return changeManager.isEmpty();
   }
}
