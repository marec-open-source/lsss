package no.imr.lsss.modules.map.overlays;

import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.tools.geo.GeoZoom;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * Base class for map overlays.
 */
public abstract class BaseMapOverlay extends BaseModuleOverlay {
   private final MapModule mapModule;

   protected BaseMapOverlay(ModuleInfo<?> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);

      this.mapModule = mapModule;
   }

   @Override
   public BaseOverlaidModule<?> getOverlaidModule() {
      return mapModule;
   }

   public MapModule getMapModule() {
      return mapModule;
   }

   protected abstract class TransformedDisplayData extends OverlayDisplayData {
      private final GeoZoom geoZoom = mapModule.getGeoZoom();

      protected TransformedDisplayData() {
      }

      @Override
      public final void draw(Graphics2D g2d) {
         transform(g2d);
         transformedDraw(g2d);
      }

      protected abstract void transformedDraw(Graphics2D g2d);

      private void transform(Graphics2D g2d) {
         if (mapModule.getGeoZoom() == geoZoom) {
            return;
         }
         Rectangle2D pixRect = mapModule.getGeoTransform().geoToPix(geoZoom.getGeoRect());
         g2d.translate(pixRect.getX(), pixRect.getY());
         g2d.scale(pixRect.getWidth() / geoZoom.width, pixRect.getHeight() / geoZoom.height);
      }
   }
}
