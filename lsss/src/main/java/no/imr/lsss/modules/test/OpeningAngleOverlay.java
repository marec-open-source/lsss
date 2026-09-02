package no.imr.lsss.modules.test;

import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.tools.geo.Earth;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.marec.lsss.api.util.GeoPoint;
import no.marec.lsss.api.util.LineStripBuilder;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class OpeningAngleOverlay extends BaseEchogramOverlay {
   private static final Color FILL_COLOR = new Color(128, 0, 0, 128);

   public OpeningAngleOverlay(ModuleInfo<TestPlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getEchogramModule().echogramArea(),
            getInterpretationSettings().getChannelChangeManager(),
            getInterpretationSettings().mouseover().pingIndex(),
            getInterpretationSettings().mouseover().depth()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      PingIndex mousePingIndex = getInterpretationSettings().mouseover().getPingIndex();
      if (mousePingIndex == null) {
         return null;
      }
      GeoPoint geographicalPosition = mousePingIndex.getGeographicalPosition();
      if (geographicalPosition == null) {
         return null;
      }
      Float mouseDepth = getInterpretationSettings().mouseover().getDepth();
      int channelIndex = getInterpretationSettings().getChannel() - 1;
      double openingAngle = Math.toRadians(getInterpretationSettings().getDataFileSet().getRawFileConfiguration().getTransducers().get(channelIndex).getBeamWidthAlongship());
      OpeningAngleCoverage coverage = new OpeningAngleCoverage(geographicalPosition, mouseDepth, openingAngle);

      // Iterate through all visible ping indexes, and update ping index to depth map
      NavigableMap<PingIndex, Float> pingIndexToStartDepth = new TreeMap<>();
      Path2D.Float contributingLinePath = new Path2D.Float();
      LineStripBuilder pathBuilder = LineStripBuilders.piecewiseHorizontal(contributingLinePath);
      for (PingIndex requestedPingIndex : getInterpretationSettings().getPingSampler().getRequestedPingIndices()) {
         GeoPoint requestedGeoPos = requestedPingIndex.getGeographicalPosition();
         if (requestedGeoPos == null) {
            pathBuilder.endLineStrip();
            continue;
         }
         pingIndexToStartDepth.put(requestedPingIndex, coverage.getStartDepth(requestedGeoPos));
         float contributingDepth = coverage.getContributingDepth(requestedGeoPos);
         if (contributingDepth < Float.MAX_VALUE) {
            float y = getZSettings().depthToY(contributingDepth, requestedPingIndex);
            float x = getPingSettings().pingIndexToX(requestedPingIndex);
            pathBuilder.addPoint(x, y);
         } else {
            pathBuilder.endLineStrip();
         }
      }
      pathBuilder.endLineStrip();

      if (pingIndexToStartDepth.isEmpty()) {
         return null;
      }

      Path2D.Float fillPath = new Path2D.Float();
      PingIndex index1 = pingIndexToStartDepth.firstKey();
      for (PingIndex pingIndex : pingIndexToStartDepth.keySet()) {
         if (pingIndex.equals(index1)) {
            continue;
         }
         float d1 = pingIndexToStartDepth.get(index1);
         float y = getZSettings().depthToY(d1, index1);
         if (y < getHeight()) {
            float x1 = getPingSettings().pingIndexToX(index1);
            float x2 = getPingSettings().pingIndexToX(pingIndex);
            GuiUtils.appendRectangle(fillPath, x1, y, x2, getHeight());
         }
         index1 = pingIndex;
      }
      //last ping index
      float d1 = pingIndexToStartDepth.get(index1);
      float y = getZSettings().depthToY(d1, index1);
      if (y < getHeight()) {
         float x1 = getPingSettings().pingIndexToX(index1);
         float x2 = getWidth();
         GuiUtils.appendRectangle(fillPath, x1, y, x2, getHeight());
      }

      return transformed(new DisplayData(fillPath, contributingLinePath));
   }

   private static final class OpeningAngleCoverage {
      private final GeoPoint geographicalPosition;
      private final @Nullable Float depth;
      private final float tanHalfOpeningAngle;

      private OpeningAngleCoverage(GeoPoint geographicalPosition, @Nullable Float depth, double openingAngle) {
         this.geographicalPosition = geographicalPosition;
         this.depth = depth;
         tanHalfOpeningAngle = (float) Math.tan(openingAngle * 0.5);
      }

      private float getStartDepth(GeoPoint geoPos) {
         float dist = (float) Earth.getApproximateDistance(geographicalPosition, geoPos);
         return dist / tanHalfOpeningAngle;
      }

      private float getContributingDepth(GeoPoint geoPos) {
         if (depth == null) {
            return Float.POSITIVE_INFINITY;
         }
         double dist = Earth.getApproximateDistance(geographicalPosition, geoPos);
         if (dist > depth) {
            return Float.POSITIVE_INFINITY;
         }
         return (float) Math.sqrt(depth * depth - dist * dist);
      }
   }

   private record DisplayData(
         Path2D.Float fillPath,
         Path2D.Float contributingLinePath
   ) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(FILL_COLOR);
         g2d.fill(fillPath);
         g2d.setColor(Color.BLACK);
         g2d.draw(contributingLinePath);
      }
   }
}
