package no.imr.lsss.modules.broadband.ts;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.modules.pojodata.PojoDataUtils;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class BroadbandTsEchogramOverlay extends BaseEchogramOverlay implements PojoDataContainer {
   private static final Color FILL_COLOR = new Color(64, 64, 64, 128);

   private final Supplier<BroadbandTsModule> broadbandTsModule = moduleSupplier(BroadbandTsModule.class);

   public BroadbandTsEchogramOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            broadbandTsModule.get().getTSDetectionChangeManager(),
            getEchogramModule().echogramArea(),
            getInterpretationSettings().getChannelChangeManager()
      ));
   }

   @Override
   public PojoData getPojoData() {
      Multimap<PingIndex, BroadbandTsData> multimap = ArrayListMultimap.create();
      int channel = getInterpretationSettings().getChannel();
      getRegionManager().getSelectedRegions().forEach(region -> {
         broadbandTsModule.get().getTSData(region).forEach((pingIndex, pingCache) -> {
            pingCache.getTsData(channel).forEach(tsData -> {
               multimap.put(pingIndex, tsData);
            });
         });
      });

      PingMapping pingMapping = getInterpretationSettings().getPingMapping();
      ParameterExport pingMappingExport = PojoDataUtils.getParameterExport(pingMapping);
      ParameterExport depthExport = new ParameterExport("depth", Unit.METER, ExportRounding.depth());
      PojoData.Builder builder = PojoData.newBuilder(getPersistentName());
      return builder
            .with(PojoDataUtils.DATASETS, List.of(
                  builder.newBuilder()
                        .with(PojoDataUtils.DATASET_NAME, "targets")
                        .withCoordinateVariable(pingMappingExport)
                        .withDataVariable(depthExport)
                        .with(pingMappingExport, multimap.entries().stream().mapToDouble(entry -> pingMapping.valueOf(entry.getKey())))
                        .with(depthExport, multimap.entries().stream().mapToDouble(entry -> entry.getValue().depth()))
                        .build()))
            .build();
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      int channel = getInterpretationSettings().getChannel();
      Path2D.Float peak = new Path2D.Float();
      Path2D.Float extent = new Path2D.Float();
      AtomicInteger counter = new AtomicInteger();
      getRegionManager().getSelectedRegions().forEach(region -> {
         broadbandTsModule.get().getTSData(region).forEach((pingIndex, pingCache) -> {
            float x0 = getPingSettings().pingIndexToX(pingIndex);
            PingIndex nextPingIndex = dataFileSet.nextOrNull(pingIndex);
            float x1 = nextPingIndex != null ? Math.max(x0 + 1, getPingSettings().pingIndexToX(nextPingIndex)) : getWidth();

            pingCache.getTsData(channel).forEach(tsData -> {
               FloatRange range = tsData.depthRange();
               float y0 = getZSettings().depthToY(range.min(), pingIndex);
               float y1 = getZSettings().depthToY(range.max(), pingIndex);
               if (y1 >= 0 && y0 <= getHeight()) {
                  float y = getZSettings().depthToY(tsData.depth(), pingIndex);
                  peak.moveTo(x0, y);
                  peak.lineTo(x1, y);

                  GuiUtils.appendRectangle(extent, x0, y0, x1, y1);
                  counter.incrementAndGet();
               }
            });
         });
      });
      if (counter.get() == 0) {
         return null;
      }
      return new DisplayData(peak, extent, counter.get() < 2000);
   }

   private final class DisplayData extends TransformedDisplayData {
      private final Path2D.Float peak;
      private final Path2D.Float extent;
      private final boolean useFill;

      private DisplayData(Path2D.Float peak, Path2D.Float extent, boolean useFill) {
         this.peak = peak;
         this.extent = extent;
         this.useFill = useFill;
      }

      @Override
      public void transformedDraw(Graphics2D g2d) {
         if (useFill) {
            g2d.setColor(FILL_COLOR);
            g2d.fill(extent);
         }

         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.setColor(Color.DARK_GRAY);
         g2d.draw(extent);

         g2d.setColor(Color.BLACK);
         g2d.draw(peak);
      }
   }
}
