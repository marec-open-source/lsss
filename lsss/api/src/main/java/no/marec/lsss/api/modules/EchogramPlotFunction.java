package no.marec.lsss.api.modules;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.util.observing.ObservableProperty;

/**
 * The function created by {@link EchogramPlot#addFunction(String, String, String, String, boolean, EchogramPlotFunctionEvaluation)}.
 */
@DoNotImplement
public interface EchogramPlotFunction {
   /**
    * {@return the ID of this function}
    */
   String getId();

   /**
    * {@return an observable property for whether this function is selected}
    */
   ObservableProperty<Boolean> selected();
}
