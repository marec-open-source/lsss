package no.marec.lsss.api.modules;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.util.GeoTransform;
import no.marec.lsss.api.util.observing.Observable;

/**
 * Access to the actual map overlay.
 */
@DoNotImplement
public interface MapOverlayAccess extends LsssOverlayAccess {
   /**
    * {@return an observable for changes to the displayed area in the map}
    */
   Observable<?> mapArea();

   /**
    * {@return the transformation between geographical coordinates and image coordinates}
    */
   GeoTransform geoTransform();
}
