package no.imr.korona.data.datagrams;

/**
 * Thrown when parsing a datagram fails.
 */
public final class DatagramFormatException extends Exception {
   public DatagramFormatException(String message) {
      super(message);
   }

   public DatagramFormatException(String message, Throwable cause) {
      super(message, cause);
   }

   public DatagramFormatException(Throwable cause) {
      super(cause);
   }
}
