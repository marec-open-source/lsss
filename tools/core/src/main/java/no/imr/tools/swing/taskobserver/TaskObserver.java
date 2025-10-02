package no.imr.tools.swing.taskobserver;

/**
 * Observer of tasks and sub-tasks.
 */
public interface TaskObserver {
   void setPrimaryTask(String text);

   void setSecondaryTask(String text);

   void done();
}
