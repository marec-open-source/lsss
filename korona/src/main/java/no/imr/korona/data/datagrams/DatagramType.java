package no.imr.korona.data.datagrams;

import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * A datagram type, with factory method {@link #createDatagram(long, ByteBuffer, DatagramTypeManager)}.
 */
public abstract class DatagramType {
   private final String asciiQuad;
   private final int intCode;

   protected DatagramType(String asciiQuad) {
      this.asciiQuad = asciiQuad;
      intCode = toIntCode(asciiQuad);
   }

   protected DatagramType(int intCode) {
      asciiQuad = toAsciiQuad(intCode);
      this.intCode = intCode;
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

   public abstract BaseDatagram createDatagram(long ntDate, ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) throws DatagramFormatException;

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

   public static final class Simple extends DatagramType {
      private final Reader reader;

      public Simple(String asciiQuad, Reader reader) {
         super(asciiQuad);
         this.reader = reader;
      }

      @Override
      public BaseDatagram createDatagram(long ntDate, ByteBuffer byteBuffer, DatagramTypeManager datagramTypeManager) throws DatagramFormatException {
         return reader.read(ntDate, byteBuffer);
      }

      @FunctionalInterface
      public interface Reader {
         BaseDatagram read(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException;
      }
   }
}
