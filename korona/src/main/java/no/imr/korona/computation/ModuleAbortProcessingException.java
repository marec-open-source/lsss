package no.imr.korona.computation;

public final class ModuleAbortProcessingException extends ModuleException {
   public ModuleAbortProcessingException(BaseModule module, String message) {
      super(module, message);
   }
}
