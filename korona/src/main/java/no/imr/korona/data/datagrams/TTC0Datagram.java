package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.TableOfContentsPingItem;

import java.nio.ByteBuffer;

public final class TTC0Datagram extends DatagramPingItem implements TableOfContentsPingItem {
   public static final DatagramType TYPE = DatagramType.simple("TTC0", TTC0Datagram::new);

   private final int[] validIds;
   private final long[] ntDates;

   public TTC0Datagram(long ntDate, int[] validIds, long[] trackNTDates) {
      super(ntDate);

      this.validIds = validIds;
      ntDates = trackNTDates;
   }

   public TTC0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      validIds = ByteBufferUtils.readCountAndIntArray(byteBuffer);
      if (byteBuffer.hasRemaining()) { // For retaining backward compatibility
         ntDates = ByteBufferUtils.readCountAndLongArray(byteBuffer);
      } else {
         ntDates = new long[0];
      }
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCountAndIntArray(byteBuffer, validIds);
      ByteBufferUtils.writeCountAndLongArray(byteBuffer, ntDates);
   }

   public int[] getValidIds() {
      return validIds;
   }

   @Override
   public long[] getNTDates() {
      return ntDates;
   }
}
