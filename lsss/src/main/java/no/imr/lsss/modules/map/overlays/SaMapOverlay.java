package no.imr.lsss.modules.map.overlays;

import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.util.ExportRounding;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.acousticcategories.AcousticCategoryConf;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.overlays.SaCurve;
import no.imr.lsss.modules.integration.IntegrationArea;
import no.imr.lsss.modules.integration.IntegrationCurvePoint;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.modules.pojodata.PojoDataUtils;
import no.imr.tools.geo.GeoTransform;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ParameterExport;
import no.imr.tools.swing.GuiText;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class SaMapOverlay extends BaseMapOverlay implements PojoDataContainer {
   private final ObjectParameter<Optional<AcousticCategory>> acousticCategory = new ObjectParameter<>(
         new Name("AcousticCategory", "Acoustic category"),
         Optional.empty()) {
      @Override
      public String toString(Optional<AcousticCategory> optional) {
         return optional
               .map(acCat -> Integer.toString(acCat.getCompId().getAcousticCategory()))
               .orElse("");
      }

      @Override
      public String toDisplayString(Optional<AcousticCategory> optional) {
         return optional
               .map(acCat -> AcousticCategoryConf.acousticCategoryToListText(acCat, getConfigurationManager().getLanguageUtils()))
               .orElse(" ");
      }
   };

   private final Supplier<RegionIntegrationModule> regionIntegrationModule = moduleSupplier(RegionIntegrationModule.class);

   public SaMapOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);

      AcousticCategoryConf acousticCategoryConf = getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf();
      acousticCategoryConf.getAcousticCategoryChangeManager().addListener(() -> {
         List<Optional<AcousticCategory>> allowedValues = Stream.concat(
                     Stream.<Optional<AcousticCategory>>of(Optional.empty()),
                     acousticCategoryConf.getSelectedCategories().stream().map(Optional::of))
               .toList();
         acousticCategory.setAllowedValuesAndPossiblyValue(allowedValues, Optional.empty());
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            acousticCategory
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      Listener recomputeListener = createRecomputeListener();
      registry.add(recomputeListener, List.of(
            getMapModule().getGeographicalAreaChangeManager(),
            getRegionManager().selectedRegions(),
            getRegionManager().getInterpretationChangeManager(),
            getConfigurationManager().getAppMiscConf().useEnglish,
            regionIntegrationModule.get().getRegionIntegrationChangeManager()
      ));
      registry.add(getParameters(), recomputeListener);
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      SaCurve saCurve = computeSaCurve();
      if (saCurve.getPoints().isEmpty()) {
         return null;
      }

      GeoTransform geoTransform = getMapModule().getGeoTransform();
      Point2D.Float pixPos = new Point2D.Float();

      float maxDiameter = 50;
      int scaleCount = 4;

      List<Ellipse2D.Float> circles = new ArrayList<>(saCurve.getPoints().size() + scaleCount);
      List<GuiText> texts = new ArrayList<>(scaleCount + 1);

      float maxSa = saCurve.getMaxSa();
      if (maxSa > 0) {
         for (SaCurve.Point point : saCurve.getPoints()) {
            GeoPoint geoPos = point.pingIndex().getGeographicalPosition();
            if (geoPos == null) {
               continue;
            }
            float sa = point.sa();
            if (sa > 0) {
               geoTransform.geoToPix(geoPos, pixPos);
               float diameter = maxDiameter * sa / maxSa;
               circles.add(new Ellipse2D.Float(pixPos.x - diameter / 2, pixPos.y - diameter / 2, diameter, diameter));
            }
         }
      }

      float x = getWidth() - maxDiameter / 2;
      float y = getHeight() - 5;
      for (int i = scaleCount; i > 0; i--) {
         float sa = i * maxSa / scaleCount;
         float diameter = maxDiameter * i / scaleCount; // Same as `maxDiameter * sa / maxSa`, but works when `maxSa = 0`.
         y -= diameter + 1;

         circles.add(new Ellipse2D.Float(x - diameter / 2, y, diameter, diameter));
         texts.add(new GuiText(Integer.toString((int) sa), Color.BLACK,
               x, y + diameter / 2,
               GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.CENTER, null));
      }

      AcousticCategory acCat = acousticCategory.getValue().orElse(null);
      if (acCat != null) {
         texts.add(new GuiText(getConfigurationManager().getLanguageUtils().getAcCatInitials(acCat), Color.MAGENTA,
               x, y - 3,
               GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, null));
      }

      return new DisplayData(List.copyOf(circles), List.copyOf(texts));
   }

   private SaCurve computeSaCurve() {
      List<IntegrationCurvePoint> curve = acousticCategory.getValue()
            .map(acCat -> {
               int id = acCat.getCompId().getAcousticCategory();
               int channel = getInterpretationSettings().getChannel();
               return regionIntegrationModule.get().calculateSa(getRegionManager().getSelectedRegions(), region -> {
                  ChannelInterpretation channelInterpretation = region.getInterpretation().getChannelInterpretation(channel);
                  return channelInterpretation.getAssignment(id);
               });
            })
            .orElse(regionIntegrationModule.get().getSaCurve());
      return SaCurve.compute(curve, IntegrationArea.TOTAL);
   }

   @Override
   public PojoData getPojoData() {
      List<SaCurve.Point> points = computeSaCurve().getPoints();
      ParameterExport longitudeExport = new ParameterExport("longitude", Unit.DEGREES, ExportRounding.geoPos());
      ParameterExport latitudeExport = new ParameterExport("latitude", Unit.DEGREES, ExportRounding.geoPos());
      ParameterExport saExport = new ParameterExport("sa", Unit.SA, ExportRounding.sa());
      PojoData.Builder builder = PojoData.newBuilder(getPersistentName());
      return builder
            .with(PojoDataUtils.DATASETS, List.of(
                  builder.newBuilder()
                        .with(PojoDataUtils.DATASET_NAME, "sA")
                        .withCoordinateVariable(longitudeExport, latitudeExport)
                        .withDataVariable(saExport)
                        .with(longitudeExport, points.stream().map(p -> p.pingIndex().getGeographicalPosition()).mapToDouble(PojoDataUtils::getLongitudeOrNaN))
                        .with(latitudeExport, points.stream().map(p -> p.pingIndex().getGeographicalPosition()).mapToDouble(PojoDataUtils::getLatitudeOrNaN))
                        .with(saExport, points.stream().mapToDouble(SaCurve.Point::sa))
                        .build()))
            .build();
   }

   private static final class DisplayData extends OverlayDisplayData {
      private final List<Ellipse2D.Float> circles;
      private final List<GuiText> texts;

      private DisplayData(List<Ellipse2D.Float> circles, List<GuiText> texts) {
         this.circles = circles;
         this.texts = texts;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(Color.MAGENTA);
         for (Ellipse2D.Float circle : circles) {
            g2d.fill(circle);
         }
      }

      @Override
      public void drawText(Graphics2D g2d) {
         for (GuiText text : texts) {
            text.draw(g2d);
         }
      }
   }
}
