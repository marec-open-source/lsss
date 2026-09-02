package no.imr.korona.data.datagrams;

import no.imr.tools.range.FloatRange;

import java.nio.ByteBuffer;
import java.time.Instant;

public final class TBR0Datagram extends DatagramPingItem implements TrackPingItem {
   public static final DatagramType TYPE = DatagramType.simple("TBR0", TBR0Datagram::new);

   private final int id;
   private int channel;
   private final FloatRange depthRange;
   private final float peakDepth;

   public TBR0Datagram(Instant instant, int id, int channel, FloatRange depthRange, float peakDepth) {
      super(instant);

      this.id = id;
      this.channel = channel;
      this.depthRange = depthRange;
      this.peakDepth = peakDepth;
   }

   public TBR0Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      id = byteBuffer.getInt();
      channel = 0xffff & byteBuffer.getShort();
      float minRange = byteBuffer.getFloat();
      float maxRange = byteBuffer.getFloat();
      if (minRange > maxRange) {
         throw new DatagramFormatException(minRange + " > " + maxRange);
      }
      depthRange = FloatRange.of(minRange, maxRange);
      peakDepth = byteBuffer.remaining() >= 4 ? byteBuffer.getFloat() : depthRange.getCenter();
      if (!depthRange.contains(peakDepth)) {
         throw new DatagramFormatException(peakDepth + " not in " + depthRange);
      }
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt(id);
      byteBuffer.putShort((short) channel);
      byteBuffer.putFloat(depthRange.min());
      byteBuffer.putFloat(depthRange.max());
      byteBuffer.putFloat(peakDepth);
   }

   @Override
   public String toStringExtra() {
      return "id: " + id +
            ", channel: " + channel +
            ", depthRange: " + depthRange +
            ", peakDepth: " + peakDepth;
   }

   @Override
   public int getId() {
      return id;
   }

   @Override
   public int getChannel() {
      return channel;
   }

   @Override
   public void setChannel(int channel) {
      this.channel = channel;
   }

   public FloatRange getDepthRange() {
      return depthRange;
   }

   public float getPeakDepth() {
      return peakDepth;
   }
}
