package no.imr.korona.viewer.variables;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

/**
 * The result of a call to {@link ContinuousVariable#evaluate(int, Ping)}.
 */
public final class ContinuousVariableResult {
   public final float[] floatData;
   public final FloatRange depthRange;

   private ContinuousVariableResult(float[] floatData, FloatRange depthRange) {
      this.floatData = floatData;
      this.depthRange = depthRange;
   }

   public static @Nullable ContinuousVariableResult of(float[] floatData, FloatRange depthRange) {
      return floatData.length > 0 ? new ContinuousVariableResult(floatData, depthRange) : null;
   }

   public int depthToIndex(float depth) {
      return Math.round(depthRange.valueToFraction(depth) * floatData.length);
   }

   public float indexToDepth(int index) {
      return depthRange.fractionToValue(index / (float) floatData.length);
   }
}
