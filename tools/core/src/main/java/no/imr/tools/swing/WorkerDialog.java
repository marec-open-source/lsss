package no.imr.tools.swing;

import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.ThrowingConsumer;
import no.imr.tools.misc.ThrowingFunction;
import no.imr.tools.misc.ThrowingRunnable;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Shows a dialog while a task is done in a background thread.
 */
public final class WorkerDialog {
   private final Supplier<@Nullable Component> referenceComponentSupplier;
   private final Supplier<?> messageSupplier;

   private final AsyncHandle asyncHandle = new AsyncHandle();

   private boolean modalDialog = true;
   private boolean waitUntilFinishedIfCancelled = true;
   private boolean hidden;
   private int delay = 500;
   private String cancelText = "Cancel";
   private Dimension minimumSize = new Dimension();
   private @Nullable JDialog dialog;
   private final CountDownLatch finishCountDownLatch = new CountDownLatch(1);

   private Consumer<Throwable> onError = e -> Log.global.log(Level.WARNING, e.getMessage(), e);

   public WorkerDialog(@Nullable Component referenceComponent, String message) {
      referenceComponentSupplier = () -> referenceComponent;
      messageSupplier = () -> message;
   }

   public WorkerDialog(@Nullable Component referenceComponent, Component message) {
      referenceComponentSupplier = () -> referenceComponent;
      messageSupplier = () -> message;
   }

   public WorkerDialog(Supplier<@Nullable Component> referenceComponentSupplier, String message) {
      this.referenceComponentSupplier = referenceComponentSupplier;
      messageSupplier = () -> message;
   }

   public WorkerDialog(Supplier<@Nullable Component> referenceComponentSupplier, Supplier<Component> messageSupplier) {
      this.referenceComponentSupplier = referenceComponentSupplier;
      this.messageSupplier = messageSupplier;
   }

   public WorkerDialog setModalDialog(boolean modalDialog) {
      this.modalDialog = modalDialog;
      return this;
   }

   public WorkerDialog setWaitUntilFinishedIfCancelled(boolean waitUntilFinishedIfCancelled) {
      this.waitUntilFinishedIfCancelled = waitUntilFinishedIfCancelled;
      return this;
   }

   public WorkerDialog setHidden(boolean hidden) {
      this.hidden = hidden;
      return this;
   }

   public WorkerDialog setDelay(int delay) {
      this.delay = delay;
      return this;
   }

   public WorkerDialog setCancelText(String cancelText) {
      this.cancelText = cancelText;
      return this;
   }

   public WorkerDialog setMinimumSize(Dimension minimumSize) {
      this.minimumSize = minimumSize;
      return this;
   }

   public WorkerDialog setOnError(Consumer<Throwable> onError) {
      this.onError = onError;
      return this;
   }

   public Result start(ThrowingConsumer<AsyncHandle, Exception> task) {
      return doStart(task, true);
   }

   public Result startWithoutCancel(ThrowingRunnable<Exception> task) {
      return doStart(asyncHandle -> task.run(), false);
   }

   public <T> @Nullable T startMakeValue(ThrowingFunction<AsyncHandle, T, Exception> task) {
      AtomicReference<@Nullable T> valueRef = new AtomicReference<>();
      doStart(asyncHandle -> {
         T value = task.apply(asyncHandle);
         if (!asyncHandle.isCancelled()) {
            valueRef.set(value);
         }
      }, true);
      return valueRef.get();
   }

   private Result doStart(ThrowingConsumer<AsyncHandle, Exception> task, boolean cancellable) {
      FutureTask<@Nullable Void> future = new FutureTask<>(() -> {
         task.accept(asyncHandle);
         return null;
      }) {
         @Override
         protected void done() {
            finishCountDownLatch.countDown();
            SwingUtilities.invokeLater(() -> {
               if (dialog != null) {
                  dialog.dispose();
               }
            });
         }
      };
      Exec.CACHED_THREAD_POOL.execute(future);

      try {
         future.get(hidden ? Integer.MAX_VALUE : delay, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
         if (Utils.isMainRun()) {
            GuiUtils.invokeNowOrWait(() -> {
               if (!future.isDone()) {
                  showDialog(cancellable);
               }
            });
         }
      } catch (CancellationException | InterruptedException | ExecutionException e) {
         // Handled in waitForFuture.
      }

      return waitForFuture(future);
   }

   private void showDialog(boolean cancellable) {
      JButton cancelButton = new JButton(cancelText);
      cancelButton.addActionListener(e -> {
         asyncHandle.cancel();
         finishCountDownLatch.countDown(); // After asyncHandle is cancelled.
         cancelButton.setFocusable(false);
         cancelButton.setEnabled(false);
         if (!waitUntilFinishedIfCancelled && dialog != null) {
            dialog.dispose();
         }
      });
      JOptionPane optionPane = new JOptionPane(messageSupplier.get(), JOptionPane.INFORMATION_MESSAGE);
      optionPane.setOptions(cancellable ? new Object[]{cancelButton} : Utils.EMPTY_OBJECT_ARRAY);
      Component referenceComponent = referenceComponentSupplier.get();
      dialog = optionPane.createDialog(referenceComponent, "Working");
      dialog.setModalityType(modalDialog ? Dialog.ModalityType.DOCUMENT_MODAL : Dialog.ModalityType.MODELESS);
      GuiUtils.expandSizeTo(dialog, minimumSize);
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            if (cancellable) {
               cancelButton.doClick();
            }
         }
      });
      dialog.setResizable(true);
      dialog.setVisible(true);
   }

   private Result waitForFuture(Future<Void> future) {
      boolean success = false;
      try {
         finishCountDownLatch.await(); // Waits until finished with success, error, or cancellation.
         if (!asyncHandle.isCancelled() || waitUntilFinishedIfCancelled) {
            future.get();
         }
         if (!asyncHandle.isCancelled()) {
            success = true;
         }
      } catch (CancellationException e) {
         // Cancelled.
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
         onError.accept(e.getCause());
      }
      return new Result(success);
   }

   public record Result(boolean success) {
   }
}
