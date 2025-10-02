package no.imr.korona.viewer.overlays;

import no.imr.korona.data.buffer.PingAnimator;
import no.imr.korona.data.buffer.PingBuffer;
import no.imr.korona.viewer.EchogramDisplay;
import no.imr.tools.LateInit;
import no.imr.tools.swing.overlay.Overlay;

/**
 * Base class for overlays on the {@link EchogramDisplay}.
 */
public abstract class EchogramOverlay extends Overlay {
   private final LateInit<EchogramDisplay> echogramDisplay = new LateInit<>();
   private boolean enabled = true;

   protected EchogramOverlay() {
   }

   public void init() {
   }

   public void close() {
   }

   public EchogramDisplay getEchogramDisplay() {
      return echogramDisplay.get();
   }

   public void setEchogramDisplay(EchogramDisplay display) {
      echogramDisplay.init(display);
   }

   public PingBuffer getPingBuffer() {
      return getEchogramDisplay().getPingAnimator().getPingBuffer();
   }

   public PingAnimator getPingAnimator() {
      return getEchogramDisplay().getPingAnimator();
   }

   public void channelChanged() {
   }

   public void depthRangeChanged() {
   }

   public void svRangeChanged() {
   }

   public void refresh() {
   }

   @Override
   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
}
