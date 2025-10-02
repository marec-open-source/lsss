package no.imr.lsss.framework.packages;

import java.util.function.Consumer;

public final class TaskLsssAction extends LsssAction {
   private final Consumer<ActionArgument> task;

   public TaskLsssAction(String id, String label, Consumer<ActionArgument> task) {
      super(id, label);

      this.task = task;
   }

   @Override
   protected void doRun(ActionArgument argument) {
      task.accept(argument);
   }
}
