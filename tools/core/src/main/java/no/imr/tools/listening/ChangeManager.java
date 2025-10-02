package no.imr.tools.listening;

import java.util.Optional;

/**
 * For managing a set of listeners.
 */
public final class ChangeManager extends ArgChangeManager<Object> implements Listener {
   public ChangeManager() {
   }

   public void notifyListeners() {
      notifyListeners(Optional.empty());
   }

   @Override
   public void listen() {
      notifyListeners(Optional.empty());
   }
}
