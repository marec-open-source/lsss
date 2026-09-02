package no.imr.korona.data.datagrams;

import no.imr.korona.computation.misc.DepthModule;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Base class for depth datagrams.
 * <p>
 * * <h2>Contents</h2>
 * <ul>
 * <li><b>{@code Depth:}</b> The detected bottom depth.
 * <li><b>{@code MinimumDepth:}</b> The minimum bottom depth, see {@link DepthModule}.
 * </ul>
 */
public abstract class BaseDepDatagram extends DatagramPingItem {
   private float depth;
   private float minimumDepth;

   protected BaseDepDatagram(Instant instant) {
      super(instant);
   }

   protected BaseDepDatagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant);

      depth = byteBuffer.getFloat();
      minimumDepth = byteBuffer.getFloat();
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putFloat(depth);
      byteBuffer.putFloat(minimumDepth);
   }

   public float getDepth() {
      return depth;
   }

   public void setDepth(float depth) {
      this.depth = depth;
   }

   public float getMinimumDepth() {
      return minimumDepth;
   }

   public void setMinimumDepth(float minimumDepth) {
      this.minimumDepth = minimumDepth;
   }
}
