package no.marec.lsss.api.util;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.util.observing.ObservableValue;

import java.util.Optional;

/**
 * Various observables connected to mouse position.
 */
@DoNotImplement
public interface Mouseover {
   /**
    * {@return an observable for whether the mouse position is frozen or not}
    */
   ObservableValue<Boolean> frozen();

   /**
    * {@return an observable for mouse geo location}
    */
   ObservableValue<? extends Optional<GeoPoint>> geoLocation();

   /**
    * {@return an observable for mouse ping index}
    */
   ObservableValue<? extends Optional<? extends PingIndex>> pingIndex();

   /**
    * {@return an observable for mouse depth}
    */
   ObservableValue<Optional<Float>> depth();

   /**
    * {@return an observable for mouse frequency in kHz}
    */
   ObservableValue<Optional<Float>> kHz();
}
