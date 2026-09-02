package no.imr.tools.listening;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

final class ArgChangeManagerTest {
   @Test
   void addRemove() {
      AtomicInteger n = new AtomicInteger();
      Consumer<String> argListener = _ -> n.incrementAndGet();
      Listener listener = n::incrementAndGet;
      ChangeManager changeManager = new ChangeManager();
      changeManager.addListener(n::incrementAndGet);

      ArgChangeManager<String> argChangeManager = new ArgChangeManager<>();
      assertEquals(0, argChangeManager.getListenerCount());
      argChangeManager.notifyListeners("");
      assertEquals(0, n.get());

      argChangeManager.addListener(argListener);
      assertEquals(1, argChangeManager.getListenerCount());
      argChangeManager.notifyListeners("");
      assertEquals(1, n.get());

      argChangeManager.addListener(listener);
      assertEquals(2, argChangeManager.getListenerCount());
      argChangeManager.notifyListeners("");
      assertEquals(3, n.get());

      argChangeManager.addListener(changeManager);
      assertEquals(3, argChangeManager.getListenerCount());
      argChangeManager.notifyListeners("");
      assertEquals(6, n.get());

      argChangeManager.removeListener(argListener);
      assertEquals(2, argChangeManager.getListenerCount());
      argChangeManager.notifyListeners("");
      assertEquals(8, n.get());

      argChangeManager.removeListener(listener);
      assertEquals(1, argChangeManager.getListenerCount());
      argChangeManager.notifyListeners("");
      assertEquals(9, n.get());

      argChangeManager.removeListener(changeManager);
      assertEquals(0, argChangeManager.getListenerCount());
      argChangeManager.notifyListeners("");
      assertEquals(9, n.get());
   }

   @Test
   void doNotAddThis() {
      ArgChangeManager<String> argChangeManager = new ArgChangeManager<>();
      assertThrows(IllegalArgumentException.class, () -> {
         argChangeManager.addListener(argChangeManager);
      });
   }
}
