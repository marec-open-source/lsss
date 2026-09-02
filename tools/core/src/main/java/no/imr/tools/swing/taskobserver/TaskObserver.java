package no.imr.tools.swing.taskobserver;

/**
 * Observer of tasks and sub-tasks.
 */
public interface TaskObserver {
   void setPrimaryTask(String text);

   void setSecondaryTask(String text);

   void done();

   static TaskObserver ignore() {
      return new TaskObserver() {
         @Override
         public void setPrimaryTask(String text) {
         }

         @Override
         public void setSecondaryTask(String text) {
         }

         @Override
         public void done() {
         }
      };
   }
}
