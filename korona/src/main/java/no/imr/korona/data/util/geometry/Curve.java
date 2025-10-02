package no.imr.korona.data.util.geometry;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.misc.FloatBinaryOperator;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.FloatRangeBuilder;

import java.util.Arrays;
import java.util.Collection;

/**
 * A function defined on the closure of a PingRange from PingIndex to depth.
 */
public final class Curve {
   private final long beginPingNumber;
   private final PingRange pingRange;
   private final float[] depths;

   public Curve(PingRange range) {
      beginPingNumber = range.begin().getPingNumber();
      pingRange = range;
      depths = new float[range.getPingCount()];
   }

   @Override
   public String toString() {
      FloatRangeBuilder depthRangeBuilder = new FloatRangeBuilder();
      depthRangeBuilder.expand(depths);
      return "pingRange: " + pingRange + ", depthRange: " + depthRangeBuilder.toFloatRange();
   }

   public boolean isEmpty() {
      return pingRange.isEmpty();
   }

   public Curve intersect(PingRange range) {
      return copyOf(pingRange.intersection(range));
   }

   public Curve extend(PingRange range) {
      return copyOf(pingRange.union(range));
   }

   public Curve copyOf(PingRange range) {
      Curve curve = new Curve(range);
      curve.adjust(this);
      return curve;
   }

   public void adjust(float depth) {
      Arrays.fill(depths, depth);
   }

   public void adjust(PingIndex pingIndex, float depth) {
      depths[getIndex(pingIndex)] = depth;
   }

   public void adjust(Curve curve) {
      adjust(curve, curve.getPingRange());
   }

   public void adjust(Collection<Curve> curves) {
      for (Curve curve : curves) {
         adjust(curve);
      }
   }

   public void adjust(Curve curve, PingRange range) {
      PingRange intersection = pingRange.intersection(range).intersection(curve.getPingRange());
      if (intersection.isEmpty()) {
         return;
      }
      System.arraycopy(curve.getDepths(), curve.getIndex(intersection.begin()),
            depths, getIndex(intersection.begin()), intersection.getPingCount());
   }

   public void adjust(ToFloatFunction<PingIndex> depthFunction, PingContainer pingContainer) {
      adjust(depthFunction, pingRange, pingContainer);
   }

   public void adjust(ToFloatFunction<PingIndex> depthFunction, PingRange range, PingContainer pingContainer) {
      PingRange intersection = pingRange.intersection(range);
      for (PingIndex pingIndex : pingContainer.getPingIndices(intersection)) {
         adjust(pingIndex, depthFunction.applyAsFloat(pingIndex));
      }
   }

   public void adjust(Curve curve, FloatBinaryOperator operator) {
      adjust(curve, curve.getPingRange(), operator);
   }

   public void adjust(Curve curve, PingRange range, FloatBinaryOperator operator) {
      PingRange intersection = pingRange.intersection(range).intersection(curve.getPingRange());
      if (intersection.isEmpty()) {
         return;
      }
      int thisBeginIndex = getIndex(intersection.begin());
      int thisEndIndex = thisBeginIndex + intersection.getPingCount();
      int otherIndex = curve.getIndex(intersection.begin());
      for (int thisIndex = thisBeginIndex; thisIndex < thisEndIndex; thisIndex++, otherIndex++) {
         float thisDepth = depths[thisIndex];
         float otherDepth = curve.depths[otherIndex];
         depths[thisIndex] = operator.applyAsFloat(thisDepth, otherDepth);
      }
   }

   public float[] getDepths() {
      return depths;
   }

   public PingRange getPingRange() {
      return pingRange;
   }

   public PingIndex getEndPing() {
      return pingRange.end();
   }

   public PingIndex getStartPing() {
      return pingRange.begin();
   }

   public float getStartDepth() {
      return depths[0];
   }

   public EchogramPoint getStartPoint() {
      return new EchogramPoint(getStartPing(), getStartDepth());
   }

   public EchogramPoint getEndPoint() {
      return new EchogramPoint(getEndPing(), getLastDepth());
   }

   public int getIndex(PingIndex pingIndex) {
      return (int) (pingIndex.getPingNumber() - beginPingNumber);
   }

   public float getDepth(PingIndex pingIndex) {
      return depths[getIndex(pingIndex)];
   }

   public float getLastDepth() {
      return depths[depths.length - 1];
   }

   public float getClampedDepth(PingIndex pingIndex) {
      int index = Math.clamp(getIndex(pingIndex), 0, depths.length - 1);
      return depths[index];
   }
}
