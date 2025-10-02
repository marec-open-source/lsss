package no.imr.korona.viewer.variables;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.Utils;
import no.imr.tools.range.FloatRange;

/**
 * The result of a call to {@link DiscreteVariable#evaluate(Ping)}.
 */
public record DiscreteVariableResult(byte[] byteData, FloatRange depthRange) {

   public static final DiscreteVariableResult EMPTY = new DiscreteVariableResult(Utils.EMPTY_BYTE_ARRAY, FloatRange.EMPTY_RANGE);
}
