package no.marec.lsss.api.echogram;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.data.Ping;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.data.PingRange;
import no.marec.lsss.api.util.observing.Observable;
import no.marec.lsss.api.util.observing.ObservableValue;

import java.util.List;

/**
 * The ping data displayed in the echogram.
 */
@DoNotImplement
public interface EchogramData {
   /**
    * {@return the channel displayed in the echogram}
    */
   ObservableValue<Integer> channel();

   /**
    * {@return the ping range displayed in the echogram}
    */
   ObservableValue<? extends PingRange> pingRange();

   /**
    * {@return the ping indices to be displayed in the echogram}
    * This is a subsampling of the ping indices in {@link #pingRange()}.
    * <p>
    * Not all pings are immediately available.
    * As pings are loaded, they will be included in {@link #currentlyLoadedPings()},
    * and observers of {@link #newlyLoadedPings()} will be notified.
    */
   List<? extends PingIndex> subsampledPingIndices();

   /**
    * {@return the pings that have so far been loaded}
    * <p>
    * This list will grow as pings are loaded
    * and will eventually correspond to {@link #subsampledPingIndices()}.
    */
   List<? extends Ping> currentlyLoadedPings();

   /**
    * {@return an observable for newly loaded pings}
    * <p>
    * When the observers are notified these newly loaded pings have been included in
    * {@link #currentlyLoadedPings()}.
    */
   Observable<? extends List<? extends Ping>> newlyLoadedPings();
}
