package no.imr.tools.smoke;

import javax.swing.SwingUtilities;

/**
 * A smoke test runnable that runs in the swing thread.
 */
public abstract class SwingSmokeTestRunnable extends SmokeTestRunnable {
   protected SwingSmokeTestRunnable() {
   }

   @Override
   public final void run() throws Exception {
      if (SwingUtilities.isEventDispatchThread()) {
         swingRun();
      } else {
         SwingUtilities.invokeAndWait(() -> {
            try {
               swingRun();
            } catch (Exception e) {
               throw new SmokeTestException("Error executing smoke test in swing thread", e);
            }
         });
      }
   }

   public abstract void swingRun() throws Exception;
}
