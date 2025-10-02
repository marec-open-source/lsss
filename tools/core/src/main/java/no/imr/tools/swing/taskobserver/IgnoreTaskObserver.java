package no.imr.tools.swing.taskobserver;

/**
 * A task observer that does nothing.
 */
public final class IgnoreTaskObserver implements TaskObserver {
   public IgnoreTaskObserver() {
   }

   @Override
   public void setPrimaryTask(String text) {
   }

   @Override
   public void setSecondaryTask(String text) {
   }

   @Override
   public void done() {
   }
}
