package no.imr.korona.data.util.geometry;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.range.FloatRange;

/**
 * A bounding box which is half open in the ping and depth directions.
 */
public record BoundingBox(PingRange pingRange, FloatRange depthRange) {
   public static final BoundingBox EMPTY_BOX = new BoundingBox(PingRange.EMPTY_RANGE, FloatRange.EMPTY_RANGE);

   public boolean isEmpty() {
      return pingRange.isEmpty() || depthRange.isEmpty();
   }

   public boolean contains(EchogramPoint echogramPoint) {
      return contains(echogramPoint.pingIndex(), echogramPoint.depth());
   }

   public boolean contains(PingIndex pingIndex, float depth) {
      return pingRange.contains(pingIndex) && depthRange.contains(depth);
   }

   public boolean intersects(BoundingBox boundingBox) {
      return pingRange.intersects(boundingBox.pingRange) && depthRange.intersects(boundingBox.depthRange);
   }

   public static BoundingBox union(BoundingBox a, BoundingBox b) {
      if (a.isEmpty()) {
         return b;
      }
      if (b.isEmpty()) {
         return a;
      }
      return new BoundingBox(
            a.pingRange.union(b.pingRange),
            a.depthRange.union(b.depthRange));
   }

   @Override
   public String toString() {
      return "pingRange: " + pingRange + ", depthRange: " + depthRange;
   }
}
