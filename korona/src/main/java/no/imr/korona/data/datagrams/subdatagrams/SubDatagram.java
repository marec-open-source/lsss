package no.imr.korona.data.datagrams.subdatagrams;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.time.NTDate;

import java.nio.ByteBuffer;

public abstract class SubDatagram {
   private long ntDate;

   protected SubDatagram(long ntDate) {
      this.ntDate = ntDate;
   }

   public long getNTDate() {
      return ntDate;
   }

   public void setNTDate(long ntDate) {
      this.ntDate = ntDate;
   }

   public abstract void write(ByteBuffer byteBuffer);

   public abstract DatagramSubType getDatagramSubType();

   public abstract void addPingItems(PingConversion pingConversion);

   public boolean isSampleDatagram() {
      return false;
   }

   public ByteBuffer toByteBuffer() {
      return ByteBufferUtils.toByteBuffer(this::write);
   }

   public SubDatagram makeCopy() {
      try {
         return getDatagramSubType().createSubDatagram(getNTDate(), toByteBuffer());
      } catch (DatagramFormatException e) {
         throw new ShouldNotHappenException(e);
      }
   }

   @Override
   public String toString() {
      return getDatagramSubType().getLabel() + " " + NTDate.ntDateToInstant(getNTDate()) + " " + toStringExtra();
   }

   public String toStringExtra() {
      return "";
   }
}
