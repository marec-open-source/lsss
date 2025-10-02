package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.time.NTDate;

import java.nio.ByteBuffer;

/**
 * Base class for datagrams.
 */
public abstract class BaseDatagram {
   /**
    * The size on file in bytes of data common for all datagrams.
    * <ul>
    * <li>Number of bytes
    * <li>Type
    * <li>Time
    * <li>(Datagram data...)
    * <li>Number of bytes
    * </ul>
    */
   public static final int ENVELOPE_AND_HEADER_SIZE = 4 + 4 + 8 + 4;
   public static final int MAX_DATAGRAM_SIZE = 10 * 1024 * 1024;

   private long ntDate;

   protected BaseDatagram(long ntDate) {
      this.ntDate = ntDate;
   }

   public void writeIncludingHeader(ByteBuffer byteBuffer) {
      byteBuffer.putInt(getDatagramType().getIntCode());
      byteBuffer.putLong(ntDate);
      write(byteBuffer);
   }

   public abstract void write(ByteBuffer byteBuffer);

   public abstract void addPingItems(PingConversion pingConversion);

   public boolean isSampleDatagram() {
      return false;
   }

   public abstract DatagramType getDatagramType();

   public long getNTDate() {
      return ntDate;
   }

   public void setNTDate(long ntDate) {
      this.ntDate = ntDate;
   }

   @Override
   public String toString() {
      return getDatagramType() + " " + NTDate.ntDateToInstant(ntDate) + " " + toStringExtra();
   }

   public String toStringExtra() {
      return "";
   }

   public ByteBuffer toByteBufferIncludingHeader() {
      return ByteBufferUtils.toByteBuffer(this::writeIncludingHeader);
   }

   public ByteBuffer toByteBufferExcludingHeader() {
      return ByteBufferUtils.toByteBuffer(this::write);
   }

   public BaseDatagram makeCopy() {
      try {
         return getDatagramType().createDatagram(ntDate, toByteBufferExcludingHeader(), null); // Datagram types using DatagramTypeManager should override makeCopy
      } catch (DatagramFormatException e) {
         throw new ShouldNotHappenException(e);
      }
   }
}
