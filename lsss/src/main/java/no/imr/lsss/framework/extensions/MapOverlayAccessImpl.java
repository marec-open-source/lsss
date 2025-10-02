package no.imr.lsss.framework.extensions;

import no.imr.lsss.modules.map.overlays.BaseMapOverlay;
import no.marec.lsss.api.modules.MapOverlayAccess;
import no.marec.lsss.api.util.GeoTransform;
import no.marec.lsss.api.util.observing.Observable;

final class MapOverlayAccessImpl extends OverlayAccessImpl<BaseMapOverlay> implements MapOverlayAccess {
   MapOverlayAccessImpl(BaseMapOverlay overlay) {
      super(overlay);
   }

   @Override
   public Observable<?> mapArea() {
      return module.getMapModule().getGeographicalAreaChangeManager();
   }

   @Override
   public GeoTransform geoTransform() {
      return module.getMapModule().getGeoTransform();
   }
}
