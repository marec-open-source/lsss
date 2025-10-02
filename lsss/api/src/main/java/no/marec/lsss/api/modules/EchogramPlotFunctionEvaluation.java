package no.marec.lsss.api.modules;

import no.marec.lsss.api.data.Ping;

/**
 * The echogram plot function evaluation.
 */
@FunctionalInterface
public interface EchogramPlotFunctionEvaluation {
   /**
    * Computes the resulting value.
    *
    * @param ping    a ping
    * @param channel a channel
    * @return a value, possibly {@code NaN}
    */
   double evaluate(Ping ping, int channel);
}
