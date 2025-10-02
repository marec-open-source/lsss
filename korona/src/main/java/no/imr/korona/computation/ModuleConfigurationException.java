package no.imr.korona.computation;

/**
 * Base class for exceptions thrown by modules during configuration,
 * when a module cannot meaningfully do its job.
 */
public class ModuleConfigurationException extends ModuleException {
   public ModuleConfigurationException(BaseModule module, String message) {
      super(module, message);
   }

   public ModuleConfigurationException(BaseModule module, Throwable cause) {
      super(module, cause);
   }
}
