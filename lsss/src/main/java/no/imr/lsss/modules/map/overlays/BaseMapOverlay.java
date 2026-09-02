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

   protected OverlayDisplayData.Wrapper transformed(OverlayDisplayData overlayDisplayData) {
      return new TransformedDisplayData(overlayDisplayData);
   }

   private final class TransformedDisplayData implements OverlayDisplayData.Wrapper {
      private final GeoZoom geoZoom = mapModule.getGeoZoom();
      private final OverlayDisplayData overlayDisplayData;

      private TransformedDisplayData(OverlayDisplayData overlayDisplayData) {
         this.overlayDisplayData = overlayDisplayData;
      }

      @Override
      public void draw(Graphics2D g2d) {
         transform(g2d);
         overlayDisplayData.draw(g2d);
      }

      private void transform(Graphics2D g2d) {
         if (mapModule.getGeoZoom() == geoZoom) {
            return;
         }
         Rectangle2D pixRect = mapModule.getGeoTransform().geoToPix(geoZoom.getGeoRect());
         g2d.translate(pixRect.getX(), pixRect.getY());
         g2d.scale(pixRect.getWidth() / geoZoom.width, pixRect.getHeight() / geoZoom.height);
      }

      @Override
      public void drawText(Graphics2D g2d) {
         overlayDisplayData.drawText(g2d);
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         return overlayDisplayData.intersects(rectangle);
      }

      @Override
      public OverlayDisplayData getOverlayDisplayData() {
         return overlayDisplayData;
      }
   }
}
