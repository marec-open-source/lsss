package no.imr.lsss.modules.map.overlays;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.tools.geo.GeoTransform;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class FileMarkerMapOverlay extends BaseMapOverlay {
   private static final float MARKER_RADIUS = 1.5f;

   public FileMarkerMapOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getMapModule().getGeographicalAreaChangeManager(),
            getInterpretationSettings().getDataFileChangeManager()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      GeoTransform geoTransform = getMapModule().getGeoTransform();
      Rectangle2D geoRect = getMapModule().getGeoRect();
      AtomicInteger counter = new AtomicInteger();
      Path2D.Float path = new Path2D.Float();
      Point pixPos = new Point();
      Point prevPixPos = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
      for (DataFile dataFile : getInterpretationSettings().getDataFileSet().getDataFiles()) {
         dataFile.getPingIndices().stream()
               .map(PingIndex::getGeographicalPosition)
               .filter(Objects::nonNull)
               .findFirst()
               .ifPresent(geoPos -> {
                  if (!geoRect.contains(geoPos)) {
                     return;
                  }
                  geoTransform.geoToPix(geoPos, pixPos);
                  int x = pixPos.x;
                  int y = pixPos.y;
                  if (x == prevPixPos.x && y == prevPixPos.y) {
                     return;
                  }
                  GuiUtils.appendRectangleFromCenterAndRadius(path, x, y, MARKER_RADIUS);
                  prevPixPos.setLocation(pixPos);
                  counter.incrementAndGet();
               });
      }
      if (counter.get() == 0) {
         return null;
      }
      return new DisplayData(path, counter.get() < 2000);
   }

   private final class DisplayData extends TransformedDisplayData {
      private final Path2D path;
      private final boolean useFill;

      private DisplayData(Path2D path, boolean useFill) {
         this.path = path;
         this.useFill = useFill;
      }

      @Override
      public void transformedDraw(Graphics2D g2d) {
         if (useFill) {
            g2d.setColor(Color.WHITE);
            g2d.fill(path);
         }
         g2d.setColor(Color.GRAY);
         g2d.draw(path);
      }
   }
}
