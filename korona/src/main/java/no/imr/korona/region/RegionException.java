package no.imr.korona.region;

public abstract class RegionException extends Exception {
   RegionException() {
   }

   RegionException(String message) {
      super(message);
   }

   RegionException(String message, Throwable cause) {
      super(message, cause);
   }
}
