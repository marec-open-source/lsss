package no.marec.lsss.api.modules;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.echogram.EchogramDepthTransform;
import no.marec.lsss.api.echogram.EchogramPingTransform;
import no.marec.lsss.api.util.observing.Observable;

/**
 * Access to the actual echogram overlay.
 */
@DoNotImplement
public interface EchogramOverlayAccess extends LsssOverlayAccess {
   /**
    * {@return an observable for changes to the displayed area in the echogram}
    */
   Observable<?> echogramArea();

   /**
    * {@return the transformation between pings and the echogram x-axis}
    */
   EchogramPingTransform echogramPingTransform();

   /**
    * {@return the transformation between depth and the echogram y-axis}
    */
   EchogramDepthTransform echogramDepthTransform();
}
