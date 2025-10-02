package no.imr.korona.viewer.variables;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.Utils;
import no.imr.tools.range.FloatRange;

/**
 * The result of a call to {@link ContinuousVariable#evaluate(int, Ping)}.
 */
public record ContinuousVariableResult(float[] floatData, FloatRange depthRange) {

   public static final ContinuousVariableResult EMPTY = new ContinuousVariableResult(Utils.EMPTY_FLOAT_ARRAY, FloatRange.EMPTY_RANGE);

   public int depthToIndex(float depth) {
      return Math.round(depthRange.valueToFraction(depth) * floatData.length);
   }

   public float indexToDepth(int index) {
      return depthRange.fractionToValue(index / (float) floatData.length);
   }
}
