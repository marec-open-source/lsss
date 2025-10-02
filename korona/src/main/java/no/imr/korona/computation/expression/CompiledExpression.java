package no.imr.korona.computation.expression;

import no.imr.korona.data.util.ResampledFloatArray;

/**
 * Base class for implementations compiled at runtime from generated sources.
 */
public interface CompiledExpression {
   void run(float[] result, ResampledFloatArray[] resampledArrays);
}
