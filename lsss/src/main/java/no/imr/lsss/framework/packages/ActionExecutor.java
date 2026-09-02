package no.imr.lsss.framework.packages;

@FunctionalInterface
public interface ActionExecutor {
   void runIfEnabled(ActionArgument argument);
}
