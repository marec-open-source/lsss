package no.imr.korona.data.datagrams;

import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * A datagram type, consisting of a code and a {@link DatagramType.Factory datagram factory}.
 */
public final class DatagramType {
   private final String asciiQuad;
   private final int intCode;
   private final Factory factory;

   public DatagramType(String asciiQuad, Factory factory) {
      this.asciiQuad = asciiQuad;
      intCode = toIntCode(asciiQuad);
      this.factory = factory;
   }

   public DatagramType(int intCode, Factory factory) {
      asciiQuad = toAsciiQuad(intCode);
      this.intCode = intCode;
      this.factory = factory;
   }

   public static DatagramType simple(String asciiQuad, SimpleFactory factory) {
      return new DatagramType(asciiQuad, factory);
   }

   public static DatagramType simple(int intCode, SimpleFactory factory) {
      return new DatagramType(intCode, factory);
   }

   @Override
   public String toString() {
      return asciiQuad;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof DatagramType that
            && intCode == that.intCode;
   }

   @Override
   public int hashCode() {
      return intCode;
   }

   public String getAsciiQuad() {
      return asciiQuad;
   }

   public int getIntCode() {
      return intCode;
   }

   public Factory getFactory() {
      return factory;
   }

   static int toIntCode(String asciiQuad) {
      if (asciiQuad.length() != 4) {
         throw new IllegalArgumentException(asciiQuad);
      }
      return asciiQuad.charAt(0) |
            (asciiQuad.charAt(1) << 8) |
            (asciiQuad.charAt(2) << 16) |
            (asciiQuad.charAt(3) << 24);
   }

   static String toAsciiQuad(int intCode) {
      return new String(new char[]{
            (char) (intCode & 0xff),
            (char) ((intCode >>> 8) & 0xff),
            (char) ((intCode >>> 16) & 0xff),
            (char) ((intCode >>> 24) & 0xff)
      });
   }

   @FunctionalInterface
   public interface Factory {
      BaseDatagram read(Instant instant, ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) throws DatagramFormatException;
   }

   @FunctionalInterface
   public interface SimpleFactory extends Factory {
      @Override
      default BaseDatagram read(Instant instant, ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) throws DatagramFormatException {
         return read(instant, byteBuffer);
      }

      BaseDatagram read(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException;
   }
}
