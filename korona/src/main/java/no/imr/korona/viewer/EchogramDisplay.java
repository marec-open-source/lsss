package no.imr.korona.viewer;

import no.imr.korona.data.ChannelSelector;
import no.imr.korona.data.buffer.PingAnimator;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.overlays.EchogramOverlay;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.overlay.OverlaidComponent;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Displays a scrollable echogram.
 */
public final class EchogramDisplay {
   private final PingAnimator pingAnimator;
   private final Fonts fonts;
   private final ColorConverterContainer converterContainer;
   private final OverlaidComponent<EchogramOverlay> overlaidComponent = new OverlaidComponent<>();
   private FloatRange depthRange;
   private ChannelSelector channelSelector = ChannelSelector.channelOne();

   public EchogramDisplay(PingAnimator pingAnimator, Fonts fonts, ColorConverterContainer converterContainer, FloatRange depthRange) {
      this.pingAnimator = pingAnimator;
      this.fonts = fonts;
      this.converterContainer = converterContainer;
      this.depthRange = depthRange;
   }

   public void addOverlay(EchogramOverlay overlay) {
      overlaidComponent.addOverlay(overlay);
      overlay.setEchogramDisplay(this);
      overlay.init();
   }

   public Fonts getFonts() {
      return fonts;
   }

   public PingAnimator getPingAnimator() {
      return pingAnimator;
   }

   public FloatRange getDepthRange() {
      return depthRange;
   }

   public void setDepthRange(FloatRange depthRange) {
      this.depthRange = depthRange;
      overlaidComponent.getOverlays().forEach(EchogramOverlay::depthRangeChanged);
   }

   public float pingIndexToX(@Nullable PingIndex pingIndex) {
      if (pingIndex == null) {
         return overlaidComponent.getWidth();
      }
      PingIndex lastPingIndex = pingAnimator.getLastPingIndex();
      if (lastPingIndex == null) {
         return overlaidComponent.getWidth();
      }
      long offset = lastPingIndex.getPingNumber() - pingIndex.getPingNumber();
      return overlaidComponent.getWidth() - offset;
   }

   public @Nullable PingIndex xToPingIndex(float x) {
      PingIndex lastPingIndex = pingAnimator.getLastPingIndex();
      if (lastPingIndex == null) {
         return null;
      }
      long pingNumber = (long) x - overlaidComponent.getWidth() + lastPingIndex.getPingNumber();
      if (pingNumber < pingAnimator.getPingBuffer().getFirstPingIndex().getPingNumber()) {
         return pingAnimator.getPingBuffer().getFirstPingIndex();
      }
      Ping lastPing = pingAnimator.getPingBuffer().getLastPing();
      if (lastPing == null) {
         return null;
      }
      if (pingNumber > lastPing.getPingNumber()) {
         return lastPing.getPingIndex();
      }
      return pingAnimator.getPingBuffer().getContainingPingIndex(pingNumber, PingMapping.NUMBER);
   }

   public PingRange getPingRange() {
      PingIndex begin = xToPingIndex(0);
      if (begin == null) {
         return PingRange.EMPTY_RANGE;
      }
      PingIndex last = xToPingIndex(overlaidComponent.getWidth());
      if (last == null) {
         return PingRange.EMPTY_RANGE;
      }
      PingIndex end = pingAnimator.getPingBuffer().nextOrSame(last);
      return PingRange.of(begin, end);
   }

   public float yToDepth(float y) {
      return depthRange.fractionToValue(y / overlaidComponent.getHeight());
   }

   public float depthToY(float depth) {
      return overlaidComponent.getHeight() * depthRange.valueToFraction(depth);
   }

   public ChannelSelector getChannelSelector() {
      return channelSelector;
   }

   public void setChannelSelector(ChannelSelector channelSelector) {
      this.channelSelector = channelSelector;
      overlaidComponent.getOverlays().forEach(EchogramOverlay::channelChanged);
   }

   public void svRangeChanged() {
      overlaidComponent.getOverlays().forEach(EchogramOverlay::svRangeChanged);
   }

   public ColorConverterContainer getConverterContainer() {
      return converterContainer;
   }

   public OverlaidComponent<EchogramOverlay> getOverlaidComponent() {
      return overlaidComponent;
   }

   public JComponent getComponent() {
      return overlaidComponent;
   }

   public void refresh() {
      overlaidComponent.getOverlays().forEach(EchogramOverlay::refresh);
   }

   public void close() {
      overlaidComponent.getOverlays().forEach(EchogramOverlay::close);
   }
}
