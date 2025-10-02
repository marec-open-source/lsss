package no.imr.tools.concurrent;

@FunctionalInterface
public interface ManagedRunnableFactory {
   Runnable createManagedRunnable(Runnable runnable);
}
