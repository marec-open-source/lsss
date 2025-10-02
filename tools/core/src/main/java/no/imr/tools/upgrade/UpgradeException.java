package no.imr.tools.upgrade;

public final class UpgradeException extends Exception {
   public UpgradeException(String message) {
      super(message);
   }

   public UpgradeException(String message, Throwable cause) {
      super(message, cause);
   }

   public UpgradeException(Throwable cause) {
      super(cause);
   }
}
