package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.modules.pojodata.PojoDataUtils;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.marec.lsss.api.util.LineStripBuilder;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.function.Supplier;

/**
 * Draws a s<sub>A</sub> curve, not accumulated.
 */
public final class SaOverlay extends BaseEchogramOverlay implements PojoDataContainer {
   private final Supplier<RegionIntegrationModule> regionIntegrationModule = moduleSupplier(RegionIntegrationModule.class);

   public SaOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getEchogramModule().echogramArea(),
            regionIntegrationModule.get().getRegionIntegrationChangeManager()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      SaCurve saCurve = computeSaCurve();
      if (saCurve.points().isEmpty() || saCurve.maxSa() == 0) {
         return null;
      }

      Path2D path = new Path2D.Float(Path2D.WIND_NON_ZERO, saCurve.points().size());
      LineStripBuilder pathBuilder = LineStripBuilders.coalescing(path);

      double height = getHeight();
      double fy = height / saCurve.maxSa();
      for (SaCurve.Point point : saCurve.points()) {
         double x = getPingSettings().pingIndexToX(point.pingIndex());
         double y = height - fy * point.sa();
         pathBuilder.addPoint(x, y);
      }
      pathBuilder.endLineStrip();

      return new DisplayData(path);
   }

   private SaCurve computeSaCurve() {
      return SaCurve.compute(regionIntegrationModule.get().getSaCurve(), getEchogramModule().getIntegrationArea());
   }

   @Override
   public PojoData getPojoData() {
      List<SaCurve.Point> points = computeSaCurve().points();
      PingMapping pingMapping = getInterpretationSettings().getPingMapping();
      ParameterExport pingMappingExport = PojoDataUtils.getParameterExport(pingMapping);
      ParameterExport saExport = new ParameterExport("sa", Unit.SA, ExportRounding.sa());
      PojoData.Builder builder = PojoData.newBuilder(getPersistentName());
      return builder
            .with(PojoDataUtils.DATASETS, List.of(
                  builder.newBuilder()
                        .with(PojoDataUtils.DATASET_NAME, "sA")
                        .withCoordinateVariable(pingMappingExport)
                        .withDataVariable(saExport)
                        .with(pingMappingExport, points.stream().mapToDouble(p -> pingMapping.valueOf(p.pingIndex())))
                        .with(saExport, points.stream().mapToDouble(SaCurve.Point::sa))
                        .build()))
            .build();
   }

   private record DisplayData(Path2D path) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.setColor(Color.BLACK);
         g2d.draw(path);
      }
   }
}
