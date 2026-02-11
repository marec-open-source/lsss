package no.imr.korona.data.datagrams.subdatagrams;

import no.imr.korona.data.datagrams.DatagramFormatException;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

public record DatagramSubType(
      int intCode,
      String label,
      Reader reader
) {
   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof DatagramSubType that
            && intCode == that.intCode;
   }

   @Override
   public int hashCode() {
      return intCode;
   }

   @Override
   public String toString() {
      return Integer.toString(intCode);
   }

   public SubDatagram createSubDatagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      return reader.read(ntDate, byteBuffer);
   }

   @FunctionalInterface
   public interface Reader {
      SubDatagram read(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException;
   }
}
