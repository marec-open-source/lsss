package no.imr.tools.parameter;

public final class ParameterException extends RuntimeException {
   private final BaseParameter<?> parameter;

   public ParameterException(BaseParameter<?> parameter, String message) {
      super(parameter.getPersistentName() + ": " + message);

      this.parameter = parameter;
   }

   public ParameterException(BaseParameter<?> parameter, String message, Throwable cause) {
      super(parameter.getPersistentName() + ": " + message, cause);

      this.parameter = parameter;
   }

   public ParameterException(BaseParameter<?> parameter, Throwable cause) {
      super(parameter.getPersistentName() + ": " + cause, cause);

      this.parameter = parameter;
   }

   public BaseParameter<?> getParameter() {
      return parameter;
   }
}
