package no.marec.lsss.api;

import no.marec.lsss.api.data.PingDataset;
import no.marec.lsss.api.echogram.EchogramData;
import no.marec.lsss.api.modules.EchogramPlot;
import no.marec.lsss.api.regions.Regions;
import no.marec.lsss.api.util.AsyncHandle;
import no.marec.lsss.api.util.Mouseover;
import no.marec.lsss.api.util.observing.Observable;

import java.util.function.Consumer;

/**
 * Access to LSSS functionality.
 */
@DoNotImplement
public interface LsssAccess {
   /**
    * {@return the echosounder data}
    */
   PingDataset pingDataset();

   /**
    * {@return the data displayed in the echogram}
    */
   EchogramData echogram();

   /**
    * {@return access to regions and interpretation}
    */
   Regions regions();

   /**
    * {@return an observable for when data should be reloaded}
    */
   Observable<?> reloadData();

   /**
    * {@return a collection of observables for mouse movement}
    */
   Mouseover mouseover();

   /**
    * {@return access to the echogram plot module}
    */
   EchogramPlot echogramPlot();

   /**
    * Runs a task and shows a dialog with a cancel button after a while.
    * <p>
    * If the user presses the cancel button, then {@link AsyncHandle#isCancelled()}
    * will subsequently return {@code true}, and the task should stop.
    * <p>
    * This should typically be used when loading data.
    *
    * @param message a message shown in the dialog
    * @param task    a task
    */
   void runCancellableTask(String message, Consumer<AsyncHandle> task);
}
