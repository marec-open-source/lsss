package no.imr.korona.computation;

/**
 * Thrown when a {@link BaseModule} cannot be created.
 */
public final class ModuleCreationException extends Exception {
   public ModuleCreationException(String message) {
      super(message);
   }

   public ModuleCreationException(String message, Throwable cause) {
      super(message, cause);
   }
}
