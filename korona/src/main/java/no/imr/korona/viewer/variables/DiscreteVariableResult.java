package no.imr.korona.viewer.variables;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

/**
 * The result of a call to {@link DiscreteVariable#evaluate(Ping)}.
 */
public final class DiscreteVariableResult {
   public final byte[] byteData;
   public final FloatRange depthRange;

   private DiscreteVariableResult(byte[] byteData, FloatRange depthRange) {
      this.byteData = byteData;
      this.depthRange = depthRange;
   }

   public static @Nullable DiscreteVariableResult of(byte[] byteData, FloatRange depthRange) {
      return byteData.length > 0 ? new DiscreteVariableResult(byteData, depthRange) : null;
   }
}
