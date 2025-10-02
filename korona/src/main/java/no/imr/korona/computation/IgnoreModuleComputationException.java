package no.imr.korona.computation;

/**
 * Thrown if a {@link BaseModuleComputation} constructor decides it cannot do anything,
 * and should be ignored.
 * This approach might have several benefits, such as making fields final and non-nullable,
 * and avoiding an inactive module computation.
 */
public final class IgnoreModuleComputationException extends Exception {
   public IgnoreModuleComputationException() {
   }
}
