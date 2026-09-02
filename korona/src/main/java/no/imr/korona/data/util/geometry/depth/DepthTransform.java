package no.imr.korona.data.util.geometry.depth;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.tools.math.MathUtils;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.FloatRange;

/**
 * Performs a transformation between depth, i.e., distance downwards from the surface,
 * to z, another vertical distance relative to something else, e.g., the bottom.
 * The transformation is translation, but may be different for each ping.
 */
public interface DepthTransform {
   default boolean dependsOnPingIndex() {
      return true;
   }

   PerPingDepthTransform forPing(PingIndex pingIndex);

   default float depthToZ(float depth, PingIndex pingIndex) {
      return forPing(pingIndex).depthToZ(depth);
   }

   default float depthToZ(EchogramPoint echogramPoint) {
      return forPing(echogramPoint.pingIndex()).depthToZ(echogramPoint.depth());
   }

   default FloatRange depthToZ(FloatRange depthRange, PingIndex pingIndex) {
      return forPing(pingIndex).depthToZ(depthRange);
   }

   default float zToDepth(float z, PingIndex pingIndex) {
      return forPing(pingIndex).zToDepth(z);
   }

   default FloatRange zToDepth(FloatRange zRange, PingIndex pingIndex) {
      return forPing(pingIndex).zToDepth(zRange);
   }

   default ToFloatFunction<PingIndex> constantZToDepthFunction(EchogramPoint p) {
      return constantZToDepthFunction(depthToZ(p));
   }

   default ToFloatFunction<PingIndex> constantZToDepthFunction(float z) {
      return pingIndex -> zToDepth(z, pingIndex);
   }

   default ToFloatFunction<PingIndex> linearZToDepthFunction(EchogramPoint a, EchogramPoint b) {
      return linearZToDepthFunction(a, b, PingMapping.NUMBER);
   }

   default ToFloatFunction<PingIndex> linearZToDepthFunction(EchogramPoint a, EchogramPoint b, PingMapping pingMapping) {
      if (b.pingIndex().getPingNumber() < a.pingIndex().getPingNumber()) {
         return linearZToDepthFunction(b, a, pingMapping);
      }
      double totalDistance = pingMapping.distance(a.pingIndex(), b.pingIndex());
      if (totalDistance == 0) {
         long middlePingNumber = (a.pingIndex().getPingNumber() + b.pingIndex().getPingNumber()) / 2;
         return pingIndex -> (pingIndex.getPingNumber() <= middlePingNumber) ? a.depth() : b.depth();
      }
      float az = depthToZ(a);
      float bz = depthToZ(b);
      return pingIndex -> {
         double distance = pingMapping.distance(a.pingIndex(), pingIndex);
         double f = distance / totalDistance;
         double z = MathUtils.interpolate(az, bz, f);
         return zToDepth((float) z, pingIndex);
      };
   }
}
