package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.korona.viewer.overlays.DepthMarkerData;
import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.tools.concurrent.ConcurrentObject;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.listening.ObservableChangeManager;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.observing.Observable;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Optional;

/**
 * Draw lines at constant z values.
 */
public final class DepthMarkerEngine extends ConcurrentObject implements ParameterContainer {
   private final BaseModuleOverlay overlay;
   private final EchogramZSettings zSettings;
   private final Listener recomputeListener = newCoalescingExecListener(this::recompute);

   private final IntParameter pixelsPerMarker = new IntParameter(
         new Name("PixelsPerMarker", "Pixels per marker"),
         DepthMarkerData.PIXELS_PER_MARKER, Unit.COUNT, ValueConstraints.gte(1),
         "Distance in pixels between depth lines");

   private final FloatParameter fontSize = new FloatParameter(
         new Name("FontSize", "Font size"),
         14, Unit.PT, ValueConstraints.gte(1f),
         "Font size");

   private final ObservableChangeManager<Optional<OverlayDisplayData>> changeManager = new ObservableChangeManager<>(this::updateEnabled);
   private @Nullable OverlayDisplayData displayData;
   private volatile @Nullable Font font;
   private @Nullable FontMetrics fontMetrics;

   public DepthMarkerEngine(BaseModuleOverlay overlay, EchogramZSettings zSettings) {
      super(overlay.getLSSS().getInterpretationSettings().createObservingSerialExecutor());

      this.overlay = overlay;
      this.zSettings = zSettings;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            pixelsPerMarker,
            fontSize
      );
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(recomputeListener, List.of(
            overlay.getOverlaidModule().getSizeChangeManager(),
            zSettings.getZoomedChangeManager()
      ));
      registry.add(getParameters(), newCoalescingExecListener(() -> {
         font = null;
         recomputeListener.listen();
      }));

      //---

      recompute();
   }

   @Override
   protected void onDisable() {
      displayData = null;
      font = null;
      fontMetrics = null;
   }

   public Observable<Optional<OverlayDisplayData>> getChangeManager() {
      return changeManager;
   }

   public @Nullable OverlayDisplayData getDisplayData() {
      return displayData;
   }

   private void updateEnabled() {
      setEnabled(!changeManager.isEmpty());
   }

   private void recompute() {
      displayData = fontMetrics == null
            ? new DisplayData(new DepthMarkerData())
            : new DisplayData(new DepthMarkerData(zSettings.getZoomedZRange(), overlay.getWidth(), overlay.getHeight(), pixelsPerMarker.getIntValue(), fontMetrics, false));
      changeManager.notifyListeners(Optional.of(displayData));
   }

   private final class DisplayData extends OverlayDisplayData {
      private final DepthMarkerData depthMarkerData;

      private DisplayData(DepthMarkerData depthMarkerData) {
         this.depthMarkerData = depthMarkerData;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setStroke(DepthMarkerData.STROKE);
         g2d.setColor(Color.BLACK);
         depthMarkerData.draw(g2d);
      }

      @Override
      public void drawText(Graphics2D g2d) {
         Font font = DepthMarkerEngine.this.font;
         if (font == null) {
            font = g2d.getFont().deriveFont(Font.BOLD, fontSize.getFloatValue());
            DepthMarkerEngine.this.font = font;
            fontMetrics = g2d.getFontMetrics(font);
            recomputeListener.listen();
         }
         Font previousFont = g2d.getFont();
         g2d.setFont(font);
         depthMarkerData.drawText(g2d);
         g2d.setFont(previousFont);
      }
   }
}
