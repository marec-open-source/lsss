package no.imr.korona.data.datagrams.subdatagrams;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.tools.ShouldNotHappenException;

import java.nio.ByteBuffer;
import java.time.Instant;

public abstract class SubDatagram {
   private Instant instant;

   protected SubDatagram(Instant instant) {
      this.instant = instant;
   }

   public Instant getInstant() {
      return instant;
   }

   public void setInstant(Instant instant) {
      this.instant = instant;
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
         return getDatagramSubType().createSubDatagram(instant, toByteBuffer());
      } catch (DatagramFormatException e) {
         throw new ShouldNotHappenException(e);
      }
   }

   @Override
   public String toString() {
      return getDatagramSubType().label() + " " + instant + " " + toStringExtra();
   }

   public String toStringExtra() {
      return "";
   }
}
