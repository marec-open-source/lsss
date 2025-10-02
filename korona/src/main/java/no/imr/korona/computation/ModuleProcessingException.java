package no.imr.korona.computation;

public final class ModuleProcessingException extends ModuleException {
   public ModuleProcessingException(BaseModule module, String message) {
      super(module, message);
   }

   public ModuleProcessingException(BaseModule module, String message, Throwable cause) {
      super(module, message, cause);
   }
}
