package no.imr.tools;

/**
 * For conditions and checked exceptions that should not happen.
 */
public final class ShouldNotHappenException extends RuntimeException {
   public ShouldNotHappenException() {
      super("Should not happen");
   }

   public ShouldNotHappenException(String message) {
      super(message);
   }

   public ShouldNotHappenException(Enum<?> aEnum) {
      super(aEnum.name());
   }

   public ShouldNotHappenException(String message, Throwable cause) {
      super(message, cause);
   }

   public ShouldNotHappenException(Throwable cause) {
      super("Should not happen", cause);
   }
}
