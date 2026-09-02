package no.imr.lsss.modules.map.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.interpretation.InterpretationSummary;
import no.imr.lsss.modules.map.MapModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.DoubleRange;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.imr.tools.swing.linestrip.ShiftedLineStripBuilder;
import no.marec.lsss.api.util.LineStripBuilder;
import org.jspecify.annotations.Nullable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public final class SurveyLineOverlay extends BaseMapOverlay {
   private static final Color PING_RANGE_COLOR = Color.BLUE;
   private static final Color PING_RANGE_EXCLUDED_COLOR = Color.BLUE;
   private static final Color NORMAL_COLOR = Color.GRAY;
   private static final Color NORMAL_EXCLUDED_COLOR = Color.LIGHT_GRAY;
   private static final Color STORED_COLOR = Color.RED;
   private static final Stroke DOTTED_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{1, 1}, 0);

   private @Nullable Double grabFraction;

   public SurveyLineOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getMapModule().getGeographicalAreaChangeManager(),
            getInterpretationSettings().getPingRangeChangeManager(),
            getInterpretationSettings().getMapSettings().getExtendedSurveyLine().getChangeManager(),
            getRegionManager().getExclusionManager().getChangeManager(),
            getLSSS().getInterpretationSummary().getChangeManager()
      ));
   }

   @Override
   public void onActivate() {
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
   }

   @Override
   public void onDeactivate() {
      grabFraction = null;
   }

   private @Nullable PingIndex getClosestPingIndex(MouseEvent mouseEvent) {
      if (!(getUnwrappedDisplayData() instanceof DisplayData displayData)) {
         return null;
      }
      return displayData.surveyLineBuilder.getClosestPingIndex(mouseEvent.getPoint());
   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {
      PingIndex closestPingIndex = getClosestPingIndex(mouseEvent);
      if (closestPingIndex == null) {
         return;
      }
      getInterpretationSettings().setCenter(closestPingIndex);
   }

   @Override
   public void mousePressed(MouseEvent mouseEvent) {
      PingIndex closestPingIndex = getClosestPingIndex(mouseEvent);
      if (closestPingIndex == null) {
         return;
      }
      double value = getInterpretationSettings().getPingMapping().valueOf(closestPingIndex);
      grabFraction = Math.clamp(getInterpretationSettings().getValueRange().valueToFraction(value), 0, 1);
   }

   @Override
   public void mouseReleased(MouseEvent mouseEvent) {
      grabFraction = null;
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      if (grabFraction == null) {
         return;
      }
      PingIndex closestPingIndex = getClosestPingIndex(mouseEvent);
      if (closestPingIndex == null) {
         return;
      }
      PingMapping pingMapping = getInterpretationSettings().getPingMapping();
      double value = pingMapping.valueOf(closestPingIndex);
      double size = getInterpretationSettings().getValueRange().getSize();
      double minValue = value - size * grabFraction;
      DoubleRange totalValueRange = pingMapping.toValueRange(getInterpretationSettings().getDataFileSet().getTotalRange());
      DoubleRange valueRange = DoubleRange.of(minValue, minValue + size)
            .shiftToBeContainedIn(totalValueRange);
      getInterpretationSettings().setValueRange(valueRange);
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      SurveyLineBuilder surveyLineBuilder = new SurveyLineBuilder(getMapModule(), getLSSS().getInterpretationSettings().getDataFileSet());
      SurveyLineBuilder extendedSurveyLineBuilder = new SurveyLineBuilder(getMapModule(), getInterpretationSettings().getMapSettings().getExtendedSurveyLine().getExtendedPingIndices().stream());
      if (surveyLineBuilder.isEmpty() && extendedSurveyLineBuilder.isEmpty()) {
         return null;
      }
      return transformed(new DisplayData(getLSSS(), surveyLineBuilder, extendedSurveyLineBuilder));
   }

   private static final class DisplayData implements OverlayDisplayData {
      private final SurveyLineBuilder surveyLineBuilder;
      private final Path2D pingRangePath = new Path2D.Float();
      private final Path2D pingRangeExcludedPath = new Path2D.Float();
      private final Path2D normalPath = new Path2D.Float();
      private final Path2D normalExcludedPath = new Path2D.Float();
      private final Path2D storedPath = new Path2D.Float();
      private final Path2D extendedPath = new Path2D.Float();

      private final boolean thinStrokes;

      private DisplayData(LSSS lsss, SurveyLineBuilder surveyLineBuilder, SurveyLineBuilder extendedSurveyLineBuilder) {
         this.surveyLineBuilder = surveyLineBuilder;

         PingRange pingRange = lsss.getInterpretationSettings().getPingRange();
         LineStripBuilder pingRangeBuilder = LineStripBuilders.coalescing(pingRangePath);
         LineStripBuilder pingRangeExcludedBuilder = LineStripBuilders.coalescing(pingRangeExcludedPath);
         LineStripBuilder normalBuilder = LineStripBuilders.coalescing(normalPath);
         LineStripBuilder normalExcludedBuilder = LineStripBuilders.coalescing(normalExcludedPath);

         surveyLineBuilder.buildCompositePath(pingIndex -> {
            boolean excluded = lsss.getRegionManager().getExclusionManager().isExcluded(pingIndex);
            if (pingRange.contains(pingIndex)) {
               return excluded ? pingRangeExcludedBuilder : pingRangeBuilder;
            } else {
               return excluded ? normalExcludedBuilder : normalBuilder;
            }
         });

         LineStripBuilder storedLineStrip = new ShiftedLineStripBuilder(LineStripBuilders.coalescing(storedPath), 2);
         InterpretationSummary interpretationSummary = lsss.getInterpretationSummary();
         surveyLineBuilder.buildCompositePath(pingIndex -> interpretationSummary.getStoredPings().contains(pingIndex) ? storedLineStrip : null);

         LineStripBuilder extendedLineStrip = LineStripBuilders.coalescing(extendedPath);
         extendedSurveyLineBuilder.buildPath(extendedLineStrip);

         thinStrokes = surveyLineBuilder.getPointCount() > 10000;
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         return GuiUtils.intersects(pingRangePath, rectangle)
               || GuiUtils.intersects(pingRangeExcludedPath, rectangle)
               || GuiUtils.intersects(normalPath, rectangle)
               || GuiUtils.intersects(normalExcludedPath, rectangle);
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(STORED_COLOR);
         g2d.setStroke(thinStrokes ? GuiUtils.STROKE_1 : GuiUtils.STROKE_2);
         g2d.draw(storedPath);

         g2d.setColor(PING_RANGE_COLOR);
         g2d.setStroke(thinStrokes ? GuiUtils.STROKE_1 : GuiUtils.STROKE_2);
         g2d.draw(pingRangePath);

         g2d.setColor(PING_RANGE_EXCLUDED_COLOR);
         g2d.setStroke(DOTTED_STROKE);
         g2d.draw(pingRangeExcludedPath);

         g2d.setColor(NORMAL_COLOR);
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.draw(normalPath);

         g2d.setColor(NORMAL_EXCLUDED_COLOR);
         g2d.setStroke(DOTTED_STROKE);
         g2d.draw(normalExcludedPath);

         g2d.setColor(ColorUtils.ORANGE);
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.draw(extendedPath);
      }
   }
}
