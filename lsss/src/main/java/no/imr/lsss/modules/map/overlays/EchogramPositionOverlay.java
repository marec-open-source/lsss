package no.imr.lsss.modules.map.overlays;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.tools.listening.ListenerRegistry;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Displays the mouse position in an echogram window on the map.
 */
public final class EchogramPositionOverlay extends BaseMapOverlay {
   private static final float SIZE = 8;

   public EchogramPositionOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getMapModule().getGeographicalAreaChangeManager(),
            getInterpretationSettings().mouseover().geoPos(),
            getInterpretationSettings().mouseover().frozen()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      if (getMapModule().getMousePosition() != null && !getInterpretationSettings().mouseover().isFrozen()) {
         return null;
      }
      GeoPoint geoPos = getInterpretationSettings().mouseover().getGeoPos();
      if (geoPos == null) {
         return null;
      }
      Point2D.Float pixPos = new Point2D.Float();
      getMapModule().getGeoTransform().geoToPix(geoPos, pixPos);
      return new DisplayData(new Ellipse2D.Float(pixPos.x - SIZE / 2, pixPos.y - SIZE / 2, SIZE, SIZE));
   }

   private static final class DisplayData extends OverlayDisplayData {
      private final Ellipse2D.Float circe;

      private DisplayData(Ellipse2D.Float circe) {
         this.circe = circe;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(Color.RED);
         g2d.fill(circe);
      }
   }
}
