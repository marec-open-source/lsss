package no.imr.lsss.modules.ctd;

import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.modules.map.overlays.BaseMapOverlay;
import no.imr.tools.geo.GeoTransform;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Supplier;

public final class CTDMapOverlay extends BaseMapOverlay {
   private static final float MARKER_WIDTH = 10;

   private final Supplier<CTDDataModule> ctdDataModule = moduleSupplier(CTDDataModule.class);

   public CTDMapOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getMapModule().getGeographicalAreaChangeManager(),
            getInterpretationSettings().getMapSettings().getExtendedSurveyLine().getChangeManager(),
            ctdDataModule.get().getChangeManager()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      PingRange pingRange = getInterpretationSettings().getMapSettings().getExtendedSurveyLine().getTotalPingRange();
      if (pingRange.isEmpty()) {
         return null;
      }
      long beginTimeInMillis = pingRange.begin().getTimeInMillis();
      long endTimeInMillis = pingRange.end().getTimeInMillis();

      GeoTransform geoTransform = getMapModule().getGeoTransform();
      Point2D.Float pixPos = new Point2D.Float();

      float w2 = MARKER_WIDTH / 2;

      Path2D.Float path = new Path2D.Float();

      // Insert data into marker vector
      for (CTDData ctdData : ctdDataModule.get().getCTDDatas()) {
         long timeInMillis = ctdData.timeInMillis();
         if (timeInMillis >= beginTimeInMillis && timeInMillis < endTimeInMillis) {
            geoTransform.geoToPix(ctdData.geographicalPosition(), pixPos);
            float x = pixPos.x - w2;
            float y = pixPos.y - w2;

            // Make ctd sign, this is a z in the map.
            path.moveTo(x - w2, y - w2);
            path.lineTo(x + w2, y - w2);
            path.lineTo(x - w2, y + w2);
            path.lineTo(x + w2, y + w2);
         }
      }
      if (path.getCurrentPoint() == null) {
         return null;
      }
      return new DisplayData(path);
   }

   private final class DisplayData extends TransformedDisplayData {
      private final Path2D path;

      private DisplayData(Path2D path) {
         this.path = path;
      }

      @Override
      public void transformedDraw(Graphics2D g2d) {
         g2d.setColor(Color.GRAY);
         g2d.setStroke(GuiUtils.STROKE_2);
         g2d.draw(path);
      }
   }
}
