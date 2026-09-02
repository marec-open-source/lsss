package no.imr.lsss.modules.echogram.overlays;

import com.google.common.collect.ImmutableSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Region;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.BottomEchogramModule;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.PelagicEchogramModule;
import no.imr.lsss.modules.integration.IntegrationArea;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.time.TimeUtils;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Draws vertical lines on the echogram.
 */
public final class VerticalLineOverlay extends BaseEchogramOverlay {
   private final IntParameter pixelsBetweenLines = new IntParameter(
         new Name("PixelsBetweenLines", "Pixels between lines"),
         200, Unit.COUNT, ValueConstraints.gte(1),
         "Approximate distance in pixels between vertical lines");

   private final ObjectParameter<PingMapping> textPingMapping = new ObjectParameter<>(
         new Name("Text"),
         PingMapping.DISTANCE, PingMapping.values(),
         "Text at vertical markers");

   private final FloatParameter distanceFontSize = new FloatParameter(
         new Name("FontSize", "Distance font size"),
         14, Unit.PT, ValueConstraints.gte(1f),
         "Font size for distance texts");

   private final FloatParameter saFontSize = new FloatParameter(
         new Name("SaFontSize", "Sa font size"),
         12, Unit.PT, ValueConstraints.gte(1f),
         "Font size for sA numbers");

   private final BooleanParameter showSa = new BooleanParameter(
         new Name("ShowSa", "Show sA"),
         true,
         "Displays sA numbers");

   private final BooleanParameter showAcousticCategories = new BooleanParameter(
         new Name("ShowAcousticCategories", "Show acoustic categories"),
         false,
         "Displays acoustic categories assigned on current channel");

   private final BooleanParameter showLabels = new BooleanParameter(
         new Name("ShowLabels", "Show labels"),
         false,
         "Displays region labels");

   private static final Stroke NORMAL_STROKE = GuiUtils.STROKE_1;
   private static final Stroke LONG_STROKE = GuiUtils.STROKE_2;
   private static final BasicStroke NEAR_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 6}, 0);

   private @Nullable Font distanceTextFont;
   private float distanceTextFontHeight;
   private @Nullable Font saTextFont;
   private float saTextFontHeight;

   private double normalInterval = 1;
   private double longInterval = 10;
   private PingMapping gridPingMapping = PingMapping.DISTANCE;

   private boolean initialized;
   private @Nullable VerticalLineOverlay bottomEchogramVerticalLineOverlay;
   private final Supplier<RegionIntegrationModule> regionIntegrationModule = moduleSupplier(RegionIntegrationModule.class);
   private final IntegrationArea integrationArea;
   private final Listener recomputeListener = createRecomputeListener();

   public VerticalLineOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      integrationArea = echogramModule.getIntegrationArea();
      if (echogramModule instanceof BottomEchogramModule) {
         textPingMapping.setValue(PingMapping.TIME);
      }
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            pixelsBetweenLines,
            textPingMapping,
            distanceFontSize,
            saFontSize,
            showSa,
            showAcousticCategories,
            showLabels
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   private void init() {
      if (getEchogramModule() instanceof PelagicEchogramModule) {
         bottomEchogramVerticalLineOverlay = getModuleManager().getModules(VerticalLineOverlay.class)
               .filter(verticalLineOverlay -> verticalLineOverlay.getEchogramModule() instanceof BottomEchogramModule)
               .findFirst()
               .orElse(null);
      }
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      if (!initialized) {
         initialized = true;
         init();
      }

      registry.add(GuiListeners.later(this::clearFonts), List.of(
            distanceFontSize,
            saFontSize
      ));
      registry.add(getParameters(), recomputeListener);
      registry.add(recomputeListener, List.of(
            getEchogramModule().echogramArea(),
            getLSSS().getInterpretationSummary().getChangeManager()
      ));
      registry.add(getRegionManager().getLabelsChangeManager(), _ -> {
         if (showLabels.getValue()) {
            recomputeListener.listen();
         }
      });
      Listener acousticCategoryListener = () -> {
         if (showAcousticCategories.getValue()) {
            recomputeListener.listen();
         }
      };
      registry.add(getRegionManager().getInterpretationChangeManager(), acousticCategoryListener);
      registry.add(getConfigurationManager().getAppMiscConf().useEnglish, acousticCategoryListener);

      registry.add(getConfigurationManager().getGridConf().horizontalGridUnit, newCoalescingExecListener(() -> {
         updateGridPingMapping();
         recomputeListener.listen();
      }));
      registry.add(regionIntegrationModule.get().getRegionIntegrationChangeManager(), recomputeListener);

      if (bottomEchogramVerticalLineOverlay != null) {
         registry.add(bottomEchogramVerticalLineOverlay.textPingMapping, recomputeListener);
      }

      //---

      updateGridPingMapping();
      clearFonts();
   }

   private void updateGridPingMapping() {
      gridPingMapping = getConfigurationManager().getGridConf().horizontalGridUnit.getValue();

      switch (gridPingMapping) {
         case DISTANCE -> {
            normalInterval = 1;
            longInterval = 10;
         }
         case NUMBER -> {
            normalInterval = 100;
            longInterval = 1000;
         }
         case TIME -> {
            normalInterval = 60;
            longInterval = 600;
         }
      }
   }

   private void clearFonts() {
      distanceTextFont = null;
      saTextFont = null;
      recompute();
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return null;
      }

      PingIndex firstIdx = pingRange.begin();
      double d0 = gridPingMapping.valueOf(firstIdx);
      double d1 = gridPingMapping.valueOf(pingRange.end());

      int lineCount = Math.max(3, getWidth() / pixelsBetweenLines.getIntValue());
      double interval = (d1 - d0) / lineCount;
      interval = gridPingMapping == PingMapping.TIME ? NiceNumber.niceSecond(interval, true) : NiceNumber.niceNumber(interval, true);
      d0 = interval * Math.ceil(d0 / interval);
      d1 = interval * Math.floor(d1 / interval);
      lineCount = (int) Math.round((d1 - d0) / interval);
      if (d1 < gridPingMapping.valueOf(pingRange.end())) {
         // Draw also the line at d1.
         lineCount++;
      }

      DisplayData displayData = new DisplayData();

      List<PingIndex> lineIndices = new ArrayList<>(lineCount);

      for (int i = 0; i < lineCount; i++) {
         double d = d0 + i * interval;
         PingIndex pingIndex = getInterpretationSettings().getDataFileSet().getContainingPingIndex(d, gridPingMapping);
         if (pingIndex == null) { // May happen that d is past the extrapolated ping index because of rounding errors.
            continue;
         }
         lineIndices.add(pingIndex);
         float x = getPingSettings().pingIndexToX(pingIndex);

         Path2D.Float path;
         if (Math.abs(longInterval * Math.round(d / longInterval) - d) < interval / 2) {
            path = displayData.longPath;
         } else if (Math.abs(normalInterval * Math.round(d / normalInterval) - d) < interval / 2) {
            path = displayData.normalPath;
         } else {
            path = displayData.nearPath;
         }

         path.moveTo(x, 0);
         path.lineTo(x, getHeight());
      }

      Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());

      if (showSa.getBooleanValue() || showAcousticCategories.getBooleanValue() || showLabels.getBooleanValue()) {
         addRegionTexts(lineIndices, displayData.saTexts);
      }

      PingMapping lowerTextPingMapping = textPingMapping.getValue();
      PingMapping upperTextPingMapping = lowerTextPingMapping;
      if (getConfigurationManager().getSurveyMiscConf().pelagicMode.getBooleanValue() && bottomEchogramVerticalLineOverlay != null) {
         PingMapping bottomEchogramTextPingMapping = bottomEchogramVerticalLineOverlay.textPingMapping.getValue();
         if (bottomEchogramTextPingMapping == PingMapping.TIME) {
            upperTextPingMapping = bottomEchogramTextPingMapping;
         } else {
            lowerTextPingMapping = bottomEchogramTextPingMapping;
         }
      }

      float y = getHeight() - 2;
      addVerticalLineTexts(lowerTextPingMapping, lineIndices, interval, displayData.distanceTexts, y, bounds);
      if (upperTextPingMapping != lowerTextPingMapping) {
         addVerticalLineTexts(upperTextPingMapping, lineIndices, interval, displayData.distanceTexts, y - distanceTextFontHeight, bounds);
      }

      return displayData;
   }

   private void addRegionTexts(List<PingIndex> lineIndices, List<GuiText> texts) {
      Map<Integer, AcousticCategory> acousticCategoryMap = getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryMap();
      PingRange pingRange = getInterpretationSettings().getPingRange();

      TextOverlapReducer textOverlapReducer = new TextOverlapReducer();

      getRegionManager().visibleRegions().forEach(region -> {
         PingRange visibleRange = pingRange.intersection(region.getPingRange());
         if (visibleRange.isEmpty()) {
            return;
         }

         Color colorSa;
         Color colorSl;
         if (region.isSelected()) {
            colorSa = ColorUtils.CRIMSON;
            colorSl = ColorUtils.BLUE;
         } else if (region.isSelectedAtLeastOnce()) {
            colorSa = Color.BLACK;
            colorSl = Color.BLACK;
         } else {
            colorSa = ColorUtils.MOCCASIN;
            colorSl = ColorUtils.MOCCASIN;
         }

         PingIndex fromPingIndex = visibleRange.begin();
         boolean drawTotalSa = false;
         boolean nonZeroSa = regionIntegrationModule.get().getSa(region, integrationArea) != 0;
         if (nonZeroSa) {
            for (PingIndex pingIndex : lineIndices) {
               if (pingIndex.getPingNumber() <= visibleRange.begin().getPingNumber()) {
                  continue;
               }
               if (pingIndex.getPingNumber() >= visibleRange.end().getPingNumber()) {
                  break;
               }
               addRegionText(textOverlapReducer, region, PingRange.ofUnsorted(fromPingIndex, pingIndex), true, false, false, colorSa, colorSl, acousticCategoryMap, texts);
               drawTotalSa = true;
               fromPingIndex = pingIndex;
            }
         }
         addRegionText(textOverlapReducer, region, PingRange.ofUnsorted(fromPingIndex, visibleRange.end()), nonZeroSa, drawTotalSa, true, colorSa, colorSl, acousticCategoryMap, texts);
      });
   }

   private void addRegionText(TextOverlapReducer textOverlapReducer, Region region, PingRange pingRange, boolean nonZeroSa, boolean drawTotalSa, boolean atEndOfRegion, Color colorSa, Color colorSl, Map<Integer, AcousticCategory> acousticCategoryMap, List<GuiText> texts) {
      PingIndex lastPingIndex = getInterpretationSettings().getDataFileSet().previousOrSame(pingRange.end());
      if (getLSSS().getInterpretationSummary().getStoredPings().contains(lastPingIndex)) {
         colorSa = Color.GREEN;
         colorSl = Color.CYAN;
      }

      float x = getPingSettings().pingIndexToX(pingRange.end()) - 1;
      float y = Math.max(0, getZSettings().depthToY(getRegionManager().getRepresentativeUpperDepth(region, lastPingIndex), lastPingIndex)) + 1;

      if (nonZeroSa && showSa.getBooleanValue()) {
         if (textOverlapReducer.canAddText(x, y)) {
            float sa = regionIntegrationModule.get().getSa(region, pingRange, integrationArea);
            String saText = AccumulatedSaOverlay.toMinimalString(sa);
            texts.add(new GuiText(saText, colorSa, x, y,
                  GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.TOP, null));
         }
         y += saTextFontHeight;

         if (drawTotalSa) {
            if (textOverlapReducer.canAddText(x, y)) {
               float totalSa = regionIntegrationModule.get().getSa(region, integrationArea);
               String totalSaText = AccumulatedSaOverlay.toMinimalString(totalSa);
               texts.add(new GuiText(totalSaText, colorSa, x, y,
                     GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.TOP, null));
            }
            y += saTextFontHeight;
         }

         if (atEndOfRegion) {
            if (textOverlapReducer.canAddText(x, y)) {
               float sl = regionIntegrationModule.get().getSL(region, integrationArea);
               String slText = AccumulatedSaOverlay.toMinimalString(sl);
               texts.add(new GuiText(slText, colorSl, x, y,
                     GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.TOP, null));
            }
            y += saTextFontHeight;
         }
      }

      if (atEndOfRegion) {
         if (showAcousticCategories.getValue()) {
            Map<Integer, Float> assignments = region.getInterpretation().getChannelInterpretation(getInterpretationSettings().getChannel()).getAssignments();
            if (!assignments.isEmpty()) {
               if (textOverlapReducer.canAddText(x, y)) {
                  String categoryText = assignments.entrySet().stream()
                        .map(e -> getConfigurationManager().getLanguageUtils().getAcCatInitials(acousticCategoryMap.get(e.getKey())) + " (" + Utils.format("%.1f", e.getValue() * 100) + "%)")
                        .collect(Collectors.joining(", "));
                  texts.add(new GuiText(categoryText, colorSa, x, y,
                        GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.TOP, null));
               }
               y += saTextFontHeight;
            }
         }
         if (showLabels.getValue()) {
            ImmutableSet<String> labels = region.getLabels();
            if (!labels.isEmpty()) {
               if (textOverlapReducer.canAddText(x, y)) {
                  texts.add(new GuiText(String.join(", ", labels), colorSa, x, y,
                        GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.TOP, null));
               }
               // No need to increase y since this is the last block.
            }
         }
      }
   }

   private void addVerticalLineTexts(PingMapping pingMapping, List<PingIndex> lineIndices, double interval, List<GuiText> texts, float y, Rectangle bounds) {
      switch (pingMapping) {
         case DISTANCE -> addDistanceTexts(lineIndices, interval, texts, y, bounds);
         case TIME -> addTimeTexts(lineIndices, texts, y, bounds);
         case NUMBER -> addPingNumberTexts(lineIndices, texts, y, bounds);
      }
   }

   private void addDistanceTexts(List<PingIndex> lineIndices, double interval, List<GuiText> texts, float y, Rectangle bounds) {
      String format = Utils.getPrecisionString(interval);

      for (PingIndex pingIndex : lineIndices) {
         float x = getPingSettings().pingIndexToX(pingIndex);
         double vesselDistance = getInterpretationSettings().getDataFileSet().getVesselDistanceUncorrectedForWrapAround(pingIndex);
         String text = Utils.format(format, vesselDistance);
         text = Utils.removeTrailingZeros(text);

         texts.add(new GuiText(text + " nmi", Color.BLACK, x, y,
               GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, bounds));
      }
   }

   private void addTimeTexts(List<PingIndex> lineIndices, List<GuiText> texts, float y, Rectangle bounds) {
      boolean useSecond = getInterpretationSettings().getPingRange().getSeconds() / (lineIndices.size() + 1) < 60;
      ChronoUnit roundingUnit = useSecond ? ChronoUnit.SECONDS : ChronoUnit.MINUTES;

      DateTimeFormatter dateFormat = TimeUtils.createUTCDateTimeFormatter("yyyy.MM.dd");
      DateTimeFormatter timeFormat = TimeUtils.createUTCDateTimeFormatter(useSecond ? "HH:mm:ss" : "HH:mm");

      float dateY = y - distanceTextFontHeight;

      LocalDate previousLocalDate = null;

      for (PingIndex pingIndex : lineIndices) {
         float x = getPingSettings().pingIndexToX(pingIndex);

         Instant instant = TimeUtils.roundedTo(pingIndex.getInstant(), roundingUnit);
         String timeString = timeFormat.format(instant);

         LocalDate localDate = LocalDate.ofInstant(instant, dateFormat.getZone());
         if (previousLocalDate == null || !previousLocalDate.equals(localDate)) {
            texts.add(new GuiText(dateFormat.format(instant), Color.BLACK, x, dateY,
                  GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, bounds));

            timeString += " UTC";
         }
         previousLocalDate = localDate;

         texts.add(new GuiText(timeString, Color.BLACK, x, y,
               GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, bounds));
      }
   }

   private void addPingNumberTexts(List<PingIndex> lineIndices, List<GuiText> texts, float y, Rectangle bounds) {
      for (PingIndex pingIndex : lineIndices) {
         float x = getPingSettings().pingIndexToX(pingIndex);
         String text = String.valueOf(pingIndex.getPingNumber());

         texts.add(new GuiText(text, Color.BLACK, x, y,
               GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.BOTTOM, bounds));
      }
   }

   private final class DisplayData implements OverlayDisplayData {
      private final Path2D.Float longPath = new Path2D.Float();
      private final Path2D.Float normalPath = new Path2D.Float();
      private final Path2D.Float nearPath = new Path2D.Float();
      private final List<GuiText> distanceTexts = new ArrayList<>();
      private final List<GuiText> saTexts = new ArrayList<>();

      private DisplayData() {
      }

      @Override
      public void draw(Graphics2D g2d) {
         if (distanceTextFont == null) {
            distanceTextFont = g2d.getFont().deriveFont(Font.BOLD, distanceFontSize.getFloatValue());
            distanceTextFontHeight = g2d.getFontMetrics(distanceTextFont).getHeight();

            saTextFont = g2d.getFont().deriveFont(Font.PLAIN, saFontSize.getFloatValue());
            saTextFontHeight = g2d.getFontMetrics(saTextFont).getHeight();

            recomputeListener.listen();
         }

         g2d.setColor(Color.BLACK);

         g2d.setStroke(LONG_STROKE);
         g2d.draw(longPath);

         g2d.setStroke(NORMAL_STROKE);
         g2d.draw(normalPath);

         g2d.setStroke(NEAR_STROKE);
         g2d.draw(nearPath);
      }

      @Override
      public void drawText(Graphics2D g2d) {
         Font previousFont = g2d.getFont();

         g2d.setFont(saTextFont);
         GuiUtils.draw(g2d, saTexts);

         g2d.setFont(distanceTextFont);
         GuiUtils.draw(g2d, distanceTexts);

         g2d.setFont(previousFont);
      }
   }


   private static final class TextOverlapReducer {
      private final Set<Integer> taken = new HashSet<>();

      private TextOverlapReducer() {
      }

      private boolean canAddText(float x, float y) {
         int a = (int) Math.ceil(x / 16);
         int b = (int) Math.ceil(y / 8);
         int c = (a << 16) | b;
         return taken.add(c);
      }
   }
}
