package no.imr.tools.swing;

import no.imr.tools.listening.Listener;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Encapsulation of swing components to ensure access is done in EDT.
 */
public final class ViewHolder<T extends ViewHolder.View> {
   private final Supplier<T> viewSupplier;
   private @Nullable T view;

   public ViewHolder(Supplier<T> viewSupplier) {
      this.viewSupplier = viewSupplier;
   }

   public T getView() {
      assert SwingUtilities.isEventDispatchThread() : Thread.currentThread();
      T view = this.view;
      if (view == null) {
         view = Objects.requireNonNull(viewSupplier.get());
         this.view = view;
      }
      return view;
   }

   public boolean hasView() {
      return view != null;
   }

   public void removeView() {
      SwingUtilities.invokeLater(() -> view = null);
   }

   public JComponent getComponent() {
      return getView().getComponent();
   }

   public void ifView(Consumer<T> consumer) {
      SwingUtilities.invokeLater(() -> consumeIfView(consumer));
   }

   public void ifViewDelayed(Object key, Consumer<T> consumer) {
      SwingDelayer.invokeLater(key, () -> consumeIfView(consumer));
   }

   public <A> Consumer<A> listener(BiConsumer<T, A> consumer) {
      return GuiListeners.later(argument -> {
         consumeIfView(view -> consumer.accept(view, argument));
      });
   }

   public Listener coalescingListener(Consumer<T> consumer) {
      return GuiListeners.coalescingLater(() -> consumeIfView(consumer));
   }

   private void consumeIfView(Consumer<T> consumer) {
      T view = this.view;
      if (view != null) {
         consumer.accept(view);
      }
   }

   public void withView(Consumer<T> consumer) {
      SwingUtilities.invokeLater(() -> consumer.accept(getView()));
   }

   public interface View {
      JComponent getComponent();
   }
}
