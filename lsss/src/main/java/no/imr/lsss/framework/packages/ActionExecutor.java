package no.imr.lsss.framework.packages;

public interface ActionExecutor {
   boolean isEnabled();

   void run(ActionArgument argument);
}
