package no.imr.tools.misc;

@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {
   void run() throws E;
}
