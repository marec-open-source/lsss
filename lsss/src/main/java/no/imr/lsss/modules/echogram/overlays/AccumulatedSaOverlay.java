package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.integration.IntegrationArea;
import no.imr.lsss.modules.integration.IntegrationCurvePoint;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.modules.pojodata.PojoDataUtils;
import no.imr.tools.Utils;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.swing.GuiText;
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
 * Draws a curve representing the accumulated s<sub>A</sub> from lower left corner to upper right corner.
 */
public final class AccumulatedSaOverlay extends BaseEchogramOverlay implements PojoDataContainer {
   private static final int TEXT_MARGIN = 5;

   private final Supplier<RegionIntegrationModule> regionIntegrationModule = moduleSupplier(RegionIntegrationModule.class);

   public AccumulatedSaOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            regionIntegrationModule.get().getRegionIntegrationChangeManager(),
            getEchogramModule().echogramArea()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      List<IntegrationCurvePoint> curve = regionIntegrationModule.get().getSaCurve();
      if (curve.isEmpty()) {
         return null;
      }

      IntegrationArea integrationArea = getEchogramModule().getIntegrationArea();

      IntegrationCurvePoint lastCurvePoint = curve.getLast();
      float totalHorizontallyIntegratedSv = lastCurvePoint.getHorizontallyIntegratedSv(integrationArea);
      if (totalHorizontallyIntegratedSv == 0) {
         return null;
      }

      Path2D path = new Path2D.Float(Path2D.WIND_NON_ZERO, curve.size());
      LineStripBuilder pathBuilder = LineStripBuilders.coalescing(path);

      int height = getHeight();
      double fy = height / totalHorizontallyIntegratedSv;
      float x = 0;
      for (IntegrationCurvePoint curvePoint : curve) {
         x = getPingSettings().pingIndexToX(curvePoint.pingIndex());
         int y = height - (int) (fy * curvePoint.getHorizontallyIntegratedSv(integrationArea));
         pathBuilder.addPoint(x, y);
      }
      pathBuilder.endLineStrip();

      float sa = regionIntegrationModule.get().getSa(integrationArea);

      GuiText text = new GuiText(toMinimalString(sa), Color.BLACK, x - TEXT_MARGIN, TEXT_MARGIN,
            GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.TOP, null);
      return new DisplayData(path, text);
   }

   @Override
   public PojoData getPojoData() {
      List<IntegrationCurvePoint> curve = regionIntegrationModule.get().getSaCurve();
      IntegrationArea integrationArea = getEchogramModule().getIntegrationArea();
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
                        .with(pingMappingExport, curve.stream().mapToDouble(p -> pingMapping.valueOf(p.pingIndex())))
                        .with(saExport, curve.stream().mapToDouble(p -> p.getHorizontallyIntegratedSv(integrationArea)))
                        .build()))
            .build();
   }

   private static final class DisplayData extends OverlayDisplayData {
      private final Path2D path;
      private final GuiText text;

      private DisplayData(Path2D path, GuiText text) {
         this.path = path;
         this.text = text;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.setColor(Color.BLACK);
         g2d.draw(path);
      }

      @Override
      public void drawText(Graphics2D g2d) {
         text.draw(g2d);
      }
   }

   /**
    * Converts a float value to a string using few digits.
    * The result is suitable for display in the echogram.
    *
    * @param value a float value
    * @return the float value as text
    */
   public static String toMinimalString(float value) {
      return Utils.format("%.0f", value);
   }
}
