package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.marec.lsss.api.util.LineStripBuilder;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Draws bottom.
 */
public final class BottomOverlay extends BaseEchogramOverlay {
   public BottomOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getInterpretationSettings().getChannelChangeManager(),
            getEchogramModule().echogramArea()
      ));
   }

   @Override
   protected OverlayDisplayData recomputeDisplayData() {
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();

      int channelIndex = getInterpretationSettings().getChannel() - 1;

      Path2D.Float channelPath = new Path2D.Float();
      Path2D.Float coordinatedPath = new Path2D.Float();
      LineStripBuilder channelPathBuilder = LineStripBuilders.piecewiseHorizontal(channelPath);
      LineStripBuilder coordinatedPathBuilder = LineStripBuilders.piecewiseHorizontal(coordinatedPath);

      float yChannel = 0;
      float yCoordinated = 0;

      for (PingIndex pingIndex : getInterpretationSettings().getPingSampler().getRequestedPingIndices()) {
         float x = getPingSettings().pingIndexToX(pingIndex);

         float channelDepth = (float) dataFileSet.getBot0Datagram(pingIndex).getChannelDepths()[channelIndex];
         yChannel = getZSettings().depthToY(channelDepth, pingIndex);
         channelPathBuilder.addPoint(x, yChannel);

         float coordinateDepth = dataFileSet.getCoordinatedDepth(pingIndex);
         yCoordinated = getZSettings().depthToY(coordinateDepth, pingIndex);
         coordinatedPathBuilder.addPoint(x, yCoordinated);
      }

      channelPathBuilder.addPoint(getWidth(), yChannel);
      coordinatedPathBuilder.addPoint(getWidth(), yCoordinated);

      channelPathBuilder.endLineStrip();
      coordinatedPathBuilder.endLineStrip();

      return transformed(new DisplayData(channelPath, coordinatedPath));
   }

   private record DisplayData(
         Path2D.Float channelPath,
         Path2D.Float coordinatedPath
   ) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.setColor(Color.BLACK);
         g2d.draw(channelPath);
         g2d.draw(coordinatedPath);
      }
   }
}
