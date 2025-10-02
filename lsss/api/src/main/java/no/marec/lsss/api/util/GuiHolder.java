package no.marec.lsss.api.util;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A class for helping to enforce that a gui is only accessed in the
 * {@link SwingUtilities#isEventDispatchThread() event dispatch thread}.
 *
 * @param <T> the GUI type
 */
public final class GuiHolder<T extends GuiHolder.Gui> {
   private final Supplier<T> guiSupplier;
   private @Nullable T gui;

   /**
    * Creates a new GUI holder.
    *
    * @param guiSupplier a factory for creating a new GUI instance
    */
   public GuiHolder(Supplier<T> guiSupplier) {
      this.guiSupplier = guiSupplier;
   }

   /**
    * {@return the GUI}
    * <p>
    * The GUI is created if needed.
    * <p>
    * NOTE: Must only be called from the
    * {@link SwingUtilities#isEventDispatchThread() event dispatch thread}.
    */
   public T getGui() {
      if (!SwingUtilities.isEventDispatchThread()) {
         throw new IllegalStateException("Not GUI thread: " + Thread.currentThread());
      }
      T gui = this.gui;
      if (gui == null) {
         gui = Objects.requireNonNull(guiSupplier.get());
         this.gui = gui;
      }
      return gui;
   }

   /**
    * Removes the current gui.
    * A new gui instance will be created later when needed.
    * <p>
    * Make sure that there are no strong references to the gui
    * that could cause a memory leak.
    */
   public void removeGui() {
      SwingUtilities.invokeLater(() -> gui = null);
   }

   /**
    * Accesses the GUI later in the
    * {@link SwingUtilities#isEventDispatchThread() event dispatch thread}.
    *
    * @param consumer the function that will access the GUI
    */
   public void withGui(Consumer<T> consumer) {
      SwingUtilities.invokeLater(() -> consumer.accept(getGui()));
   }

   /**
    * The GUI that should normally only be accessed in the
    * {@link SwingUtilities#isEventDispatchThread() event dispatch thread}.
    */
   public interface Gui {
      /**
       * {@return the top component of this gui}
       */
      JComponent getComponent();
   }
}
