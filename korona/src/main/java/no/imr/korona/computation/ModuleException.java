package no.imr.korona.computation;

import java.io.IOException;

public abstract class ModuleException extends IOException {
   private final BaseModule module;

   ModuleException(BaseModule module, String message) {
      super(message);

      this.module = module;
   }

   ModuleException(BaseModule module, String message, Throwable cause) {
      super(message, cause);

      this.module = module;
   }

   ModuleException(BaseModule module, Throwable cause) {
      super(cause);

      this.module = module;
   }

   public BaseModule getModule() {
      return module;
   }
}
