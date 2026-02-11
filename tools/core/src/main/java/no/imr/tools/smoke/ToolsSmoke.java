package no.imr.tools.smoke;

import no.imr.tools.adm.AdmService;
import no.imr.tools.logging.Log;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.util.ArrayList;
import java.util.List;

/**
 * Tools smoke test.
 */
public final class ToolsSmoke extends SwingSmokeTestRunnable {
   public ToolsSmoke() {
   }

   @Override
   public void swingRun() {
      AdmService.INSTANCE.smokeTest();
      testSwingUncaughtException();
   }

   /**
    * Tests uncaught exceptions in the EDT while showing modal dialogs.
    * <p>
    * No need to use "sun.awt.exception.handler".
    * See <a href="https://bugs.openjdk.org/browse/JDK-6727884">JDK-6727884</a>
    */
   private static void testSwingUncaughtException() {
      Thread.UncaughtExceptionHandler originalUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
      List<Throwable> uncaughtThrowables = new ArrayList<>();
      Thread.setDefaultUncaughtExceptionHandler((_, e) -> uncaughtThrowables.add(e));

      SwingUtilities.invokeLater(() -> {
         throw new SmokeTestException("expected");
      });

      JDialog dialog = new JDialog(null, "Uncaught exception test", Dialog.ModalityType.DOCUMENT_MODAL);
      SwingUtilities.invokeLater(dialog::dispose);
      dialog.setVisible(true);

      Thread.setDefaultUncaughtExceptionHandler(originalUncaughtExceptionHandler);
      if (uncaughtThrowables.size() != 1 || !uncaughtThrowables.getFirst().getMessage().equals("expected")) {
         throw new SmokeTestException("Uncaught exception error: " + uncaughtThrowables);
      }
      Log.global.info(OK + "Swing uncaught exception");
   }

   static void main() {
      SmokeTestExecutor.execute(null, new ToolsSmoke());
   }
}
