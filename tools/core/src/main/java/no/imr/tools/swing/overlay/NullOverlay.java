package no.imr.tools.swing.overlay;

/**
 * An overlay that does nothing.
 */
final class NullOverlay extends Overlay {
   static final NullOverlay INSTANCE = new NullOverlay();

   private NullOverlay() {
   }
}
