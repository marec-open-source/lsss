package no.marec.lsss.api.modules;

import no.marec.lsss.api.DoNotImplement;

/**
 * Access to the echogram plot module.
 */
@DoNotImplement
public interface EchogramPlot {
   /**
    * Adds a new echogram plot function.
    *
    * @param id               the ID
    * @param label            the label
    * @param description      the description
    * @param unit             the unit
    * @param channelDependent whether the function is channel-dependent
    * @param evaluation       the ping function
    * @return the echogram function added
    */
   EchogramPlotFunction addFunction(String id, String label, String description,
                                    String unit, boolean channelDependent, EchogramPlotFunctionEvaluation evaluation);

   /**
    * Removes a previously added function.
    *
    * @param function the function to remove
    */
   void removeFunction(EchogramPlotFunction function);
}
