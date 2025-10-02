package no.imr.korona.viewer.overlays;

import no.imr.tools.listening.ChangeManager;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.IntRange;

public final class RectangleRegion {
   public static final int MIN_PINGS = 1;
   public static final float MIN_DEPTH = 0.001f;

   private FloatRange pingOffset;
   private FloatRange depthRange;
   private final ChangeManager changeManager = new ChangeManager();

   public RectangleRegion(FloatRange pingOffset, FloatRange depthRange) {
      this.pingOffset = pingOffset;
      this.depthRange = depthRange;
   }

   @Override
   public String toString() {
      return pingOffset + " " + depthRange;
   }

   public void setPingOffset(FloatRange offset) {
      if (pingOffset.equals(offset)) {
         return;
      }
      pingOffset = offset;
      changeManager.notifyListeners();
   }

   public void setDepth(FloatRange range) {
      if (depthRange.equals(range)) {
         return;
      }
      depthRange = range;
      changeManager.notifyListeners();
   }

   public FloatRange getPingOffset() {
      return pingOffset;
   }

   public IntRange getIntPingOffset() {
      return new IntRange(Math.round(pingOffset.min()), Math.round(pingOffset.max()));
   }

   public FloatRange getDepthRange() {
      return depthRange;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }
}
