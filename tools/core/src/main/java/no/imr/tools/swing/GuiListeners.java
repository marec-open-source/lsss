package no.imr.tools.swing;

import no.imr.tools.listening.Listener;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

public final class GuiListeners {
   private GuiListeners() {
   }

   public static Listener later(Runnable listener) {
      return () -> SwingUtilities.invokeLater(listener);
   }

   public static <T> Consumer<T> later(Consumer<T> listener) {
      return argument -> {
         SwingUtilities.invokeLater(() -> listener.accept(argument));
      };
   }

   public static Listener coalescingLater(Runnable listener) {
      return () -> SwingDelayer.invokeLater(listener, listener);
   }

   public static <T> Consumer<T> coalescingLater(Consumer<T> listener) {
      return argument -> {
         SwingDelayer.invokeLater(listener, () -> listener.accept(argument));
      };
   }

   public static Listener nowOrLater(Runnable listener) {
      return () -> GuiUtils.invokeNowOrLater(listener);
   }
}
