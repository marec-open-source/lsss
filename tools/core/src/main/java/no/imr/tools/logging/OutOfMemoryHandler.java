package no.imr.tools.logging;

import no.imr.tools.ExitCodes;
import no.imr.tools.swing.GuiUtils;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.logging.Level;
import java.util.logging.LogRecord;

final class OutOfMemoryHandler extends HandlerAdapter {
   private final LoggingManager loggingManager;
   private boolean exitOnOutOfMemory;
   private boolean visible;

   OutOfMemoryHandler(LoggingManager loggingManager) {
      this.loggingManager = loggingManager;
      setLevel(Level.WARNING);
   }

   void doExitOnOutOfMemory() {
      exitOnOutOfMemory = true;
   }

   @Override
   public void publish(LogRecord record) {
      if (record.getThrown() instanceof OutOfMemoryError) {
         GuiUtils.invokeNowOrLater(this::run);
      }
   }

   private void run() {
      if (exitOnOutOfMemory) {
         loggingManager.stopLogging();
         System.exit(ExitCodes.OUT_OF_MEMORY);
      }

      if (visible) {
         return;
      }

      try {
         visible = true;
         String message = "An out of memory error has occurred.\n\n"
               + "It is recommended to restart " + loggingManager.getApplicationInfo().appName() + ".\n\n"
               + "The amount of available memory can be increased by editing\n"
               + loggingManager.getStartupScript();
         JOptionPane.showMessageDialog(null, message, "Out of memory", JOptionPane.ERROR_MESSAGE);
      } catch (Throwable e) {
         SwingUtilities.invokeLater(this::run); // Try again later
         Log.global.warning("Error displaying dialog for OutOfMemoryError");
      } finally {
         visible = false;
      }
   }
}
