package no.imr.korona.data.util.geometry;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.tools.range.FloatRange;

public record EchogramRectangle(PingRange pingRange, FloatRange zRange, DepthTransform depthTransform) {

   public EchogramRectangle(EchogramPoint pointA, EchogramPoint pointB, DepthTransform depthTransform) {
      this(PingRange.ofUnsorted(pointA.pingIndex(), pointB.pingIndex()),
            FloatRange.ofUnsorted(depthTransform.depthToZ(pointA), depthTransform.depthToZ(pointB)),
            depthTransform);
   }

   public FloatRange depthRange(PingIndex pingIndex) {
      return depthTransform.zToDepth(zRange, pingIndex);
   }

   public boolean intersectsBoundingBox(BoundingBox boundingBox, PingContainer pingContainer) {
      if (!boundingBox.pingRange().intersects(pingRange)) {
         return false;
      }
      if (!depthTransform.dependsOnPingIndex()) {
         return depthTransform.depthToZ(boundingBox.depthRange(), boundingBox.pingRange().begin()).intersects(zRange);
      }
      return pingContainer.getPingIndexStream(boundingBox.pingRange().intersection(pingRange)).anyMatch(pingIndex -> {
         return depthTransform.depthToZ(boundingBox.depthRange(), pingIndex).intersects(zRange);
      });
   }
}
