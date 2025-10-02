package no.imr.tools.swing;

import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.Listener;
import no.marec.lsss.api.util.observing.Observable;
import no.marec.lsss.api.util.observing.Subscription;

import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Collection;
import java.util.List;

/**
 * Adds a listener to a collection of {@link ArgChangeManager} only when a component is showing.
 * <p>
 * Note that {@link Listener#listen()} is called also each time this listener is added.
 */
public final class WhenShowingListening implements HierarchyListener {
   private final Component component;
   private final Collection<? extends Observable<?>> observables;
   private final Listener listener;
   private List<Subscription> subscriptions = List.of();

   private WhenShowingListening(Component component, Collection<? extends Observable<?>> observables, Listener listener) {
      this.component = component;
      this.observables = List.copyOf(observables);
      this.listener = listener;
      component.addHierarchyListener(this);
      update();
   }

   public static void connect(Component component, Observable<?> observable, Listener listener) {
      new WhenShowingListening(component, List.of(observable), listener);
   }

   public static void connect(Component component, Collection<? extends Observable<?>> observables, Listener listener) {
      new WhenShowingListening(component, observables, listener);
   }

   @Override
   public void hierarchyChanged(HierarchyEvent e) {
      if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
         update();
      }
   }

   private void update() {
      subscriptions.forEach(Subscription::unsubscribe);
      subscriptions = List.of();

      if (component.isShowing()) {
         subscriptions = observables.stream()
               .map(observable -> observable.subscribe(listener))
               .toList();
         listener.listen();
      }
   }
}
