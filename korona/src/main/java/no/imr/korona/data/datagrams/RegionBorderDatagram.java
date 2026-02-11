package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Region borders.
 */
public final class RegionBorderDatagram extends DatagramPingItem implements Comparable<RegionBorderDatagram> {
   public static final DatagramType TYPE = DatagramType.simple("RBR0", RegionBorderDatagram::new);

   private final int channel;
   private final float threshold;
   private final List<BorderInfo> borderInfos;

   public RegionBorderDatagram(long ntDate, int channel, float threshold, List<BorderInfo> borderInfos) {
      super(ntDate);

      this.channel = channel;
      this.threshold = threshold;
      this.borderInfos = borderInfos;
   }

   public RegionBorderDatagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      channel = byteBuffer.getInt();
      threshold = byteBuffer.getFloat();
      borderInfos = ByteBufferUtils.readCountAndList(byteBuffer, 20, BorderInfo::new);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt(channel);
      byteBuffer.putFloat(threshold);
      ByteBufferUtils.writeCountAndList(byteBuffer, borderInfos, BorderInfo::write);
   }

   public int getChannel() {
      return channel;
   }

   public float getThreshold() {
      return threshold;
   }

   public List<BorderInfo> getBorderInfos() {
      return borderInfos;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public int compareTo(RegionBorderDatagram o) {
      return Float.compare(threshold, o.threshold);
   }

   public record BorderInfo(
         int id,
         float startDepth,
         float endDepth,
         float meanLogSv,
         boolean currentlyAccepted
   ) {
      public BorderInfo(ByteBuffer byteBuffer) {
         this(byteBuffer.getInt(),
               byteBuffer.getFloat(),
               byteBuffer.getFloat(),
               byteBuffer.getFloat(),
               byteBuffer.getInt() != 0);
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(id);
         byteBuffer.putFloat(startDepth);
         byteBuffer.putFloat(endDepth);
         byteBuffer.putFloat(meanLogSv);
         byteBuffer.putInt(currentlyAccepted ? 1 : 0);
      }
   }
}
