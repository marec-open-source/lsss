package no.imr.korona.ekremote.responses;

public final class ResponseException extends Exception {
   public ResponseException(String message) {
      super(message);
   }

   public ResponseException(Throwable cause) {
      super(cause);
   }
}
