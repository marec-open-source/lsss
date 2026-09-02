package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.TableOfContentsPingItem;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

public final class TTC0Datagram extends DatagramPingItem implements TableOfContentsPingItem {
   public static final DatagramType TYPE = DatagramType.simple("TTC0", TTC0Datagram::new);

   private final int[] validIds;
   private final List<Instant> instants;

   public TTC0Datagram(Instant instant, int[] validIds, List<Instant> trackInstants) {
      super(instant);

      this.validIds = validIds;
      instants = trackInstants;
   }

   public TTC0Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      validIds = ByteBufferUtils.readCountAndIntArray(byteBuffer);
      if (byteBuffer.hasRemaining()) { // For retaining backward compatibility
         instants = ByteBufferUtils.readInstantsAsNTDates(byteBuffer);
      } else {
         instants = List.of();
      }
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCountAndIntArray(byteBuffer, validIds);
      ByteBufferUtils.writeInstantsAsNTDates(byteBuffer, instants);
   }

   public int[] getValidIds() {
      return validIds;
   }

   @Override
   public List<Instant> getInstants() {
      return instants;
   }
}
