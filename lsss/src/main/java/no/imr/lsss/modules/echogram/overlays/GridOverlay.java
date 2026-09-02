package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.interpretation.Grid;
import no.imr.lsss.modules.interpretation.GridIntegrator;
import no.imr.lsss.modules.interpretation.InterpretationSummary;
import no.imr.lsss.modules.interpretation.StoreUtils;
import no.imr.lsss.util.LsssUtils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GridLineBuilder;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Draws the interpretation grid.
 */
public final class GridOverlay extends BaseEchogramOverlay {
   private final BooleanParameter showNumbers = new BooleanParameter(
         new Name("ShowNumbers", "Show numbers"),
         false,
         "Displays total assigned sA in each grid cell");

   private List<GridIntegrator> gridIntegrators = List.of();
   private InterpretationSummary.@Nullable ScatterSet scatterSet;
   private @Nullable String message;

   public GridOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            showNumbers
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      Listener recomputeListener = createRecomputeListener();
      registry.add(getParameters(), recomputeListener);
      registry.add(recomputeListener, List.of(
            getInterpretationSettings().getChannelChangeManager(),
            getLSSS().getInterpretationSummary().getChangeManager(),
            getEchogramModule().echogramArea()
      ));
   }

   public void setGridData(List<GridIntegrator> gridIntegrators, InterpretationSummary.@Nullable ScatterSet scatterSet, @Nullable String message) {
      executeIfEnabled(() -> {
         this.gridIntegrators = gridIntegrators;
         this.scatterSet = scatterSet;
         this.message = message;
         recompute();
      });
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      return new DisplayDataBuilder(gridIntegrators, scatterSet, message).build();
   }

   private record DisplayData(
         Path2D gridPath,
         List<GuiText> texts,
         @Nullable GuiText messageText
   ) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(GuiUtils.STROKE_1);
         g2d.setColor(ColorUtils.MEDIUMORCHID);
         g2d.draw(gridPath);
      }

      @Override
      public void drawText(Graphics2D g2d) {
         GuiUtils.draw(g2d, texts);

         if (messageText != null) {
            Font previousFont = g2d.getFont();
            g2d.setFont(previousFont.deriveFont(previousFont.getSize() * 2.5f));
            messageText.draw(g2d);
            g2d.setFont(previousFont);
         }
      }
   }

   private final class DisplayDataBuilder {
      private final List<GuiText> texts = new ArrayList<>();
      private final GridLineBuilder gridLineBuilder = new GridLineBuilder();
      private @Nullable GuiText messageText;

      private DisplayDataBuilder(List<GridIntegrator> gridIntegrators, InterpretationSummary.@Nullable ScatterSet scatterSet, @Nullable String message) {
         gridIntegrators.forEach(this::drawIntegration);
         if (scatterSet != null) {
            drawScatters(scatterSet);
         }
         if (message != null) {
            messageText = new GuiText(message, Color.BLACK, getEchogramModule().getWidth() / 2f, getEchogramModule().getHeight() / 2f,
                  GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.CENTER, null);
         }
      }

      private @Nullable OverlayDisplayData build() {
         Path2D path = gridLineBuilder.build();
         if (path.getCurrentPoint() == null && texts.isEmpty() && messageText == null) {
            return null;
         }
         return new DisplayData(path, texts, messageText);
      }

      private void drawIntegration(GridIntegrator gridIntegrator) {
         addGrids(gridIntegrator.getGrids(getEchogramModule().getScatterTypeEnum()));
         addGrids(gridIntegrator.getGrids(getEchogramModule().getSchoolScatterTypeEnum()));
      }

      private void addGrids(Collection<Grid> grids) {
         grids.forEach(this::addGrid);
      }

      private void addGrid(Grid grid) {
         int channel = getInterpretationSettings().getChannel();
         grid.getGridColumns().forEach(gridColumn -> {
            PingIndex beginPingIndex = gridColumn.getPingRange().begin();
            PingIndex endPingIndex = gridColumn.getPingRange().end();

            Integer x0 = getPingSettings().pingIndexToXIndex(beginPingIndex);
            Integer x1 = getPingSettings().pingIndexToXIndex(endPingIndex);

            gridColumn.getGridCells().forEach(gridCell -> {
               int y0 = getZSettings().zToYIndex(gridCell.getZRange().min());
               int y1 = getZSettings().zToYIndex(gridCell.getZRange().max());

               gridLineBuilder.rectangle(x0, y0, x1, y1);

               if (showNumbers.getBooleanValue()) {
                  String text = AccumulatedSaOverlay.toMinimalString(gridCell.getTotalAssignedSa(channel));
                  texts.add(new GuiText(text, Color.BLACK, x1 - 2, y0 + 2,
                        GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.TOP, null));
               }
            });
         });
      }

      private void drawScatters(InterpretationSummary.ScatterSet scatterSet) {
         float frequency = getInterpretationSettings().getFrequency();

         addScatters(scatterSet.getScatters(getEchogramModule().getScatterTypeEnum(), frequency));
         addScatters(scatterSet.getScatters(getEchogramModule().getSchoolScatterTypeEnum(), frequency));
      }

      private void addScatters(Collection<Scatter> scatters) {
         scatters.forEach(this::addScatter);
      }

      private void addScatter(Scatter scatter) {
         PingRange pingRange = LsssUtils.getPingRange(getInterpretationSettings().getDataFileSet(), scatter);

         Integer x0 = getPingSettings().pingIndexToXIndex(pingRange.begin());
         Integer x1 = getPingSettings().pingIndexToXIndex(pingRange.end());

         FloatRange zRange = StoreUtils.getStoredZRange(scatter);
         float minZ = zRange.min();
         float maxZ = zRange.max();
         float deltaZ = scatter.getChannelThickness();

         int cellCount = Math.round((maxZ - minZ) / deltaZ);

         int y0 = getZSettings().zToYIndex(minZ);
         int y1 = getZSettings().zToYIndex(maxZ);

         gridLineBuilder.rectangle(x0, y0, x1, y1);

         for (int i = 1; i < cellCount; i++) {
            int y = getZSettings().zToYIndex(minZ + i * deltaZ);
            gridLineBuilder.horizontalLine(x0, x1, y);
         }
      }
   }
}
