package no.imr.korona.viewer.variables;

import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.range.FloatRange;

/**
 * Settings for a {@link ContinuousVariable}.
 */
public final class ContinuousVariableSettings {
   private FloatRange maxRange;
   private FloatRange range;
   private boolean clipAbove;
   private final double delta;
   private final boolean proportional;
   private PerPingSettings perPingSettings = new MyPerPingSettings();

   private final ChangeManager changeManager = new ChangeManager();

   public ContinuousVariableSettings(FloatRange maxRange, FloatRange range, double delta, boolean proportional) {
      if (!maxRange.contains(range)) {
         throw new IllegalArgumentException(maxRange + " does not contain " + range);
      }
      this.maxRange = maxRange;
      this.range = range;
      this.delta = delta;
      this.proportional = proportional;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public FloatRange getMaxRange() {
      return maxRange;
   }

   public void setMaxRange(FloatRange newMaxRange) {
      newMaxRange = newMaxRange.roundToMultipleOf(delta);
      if (maxRange.equals(newMaxRange)) {
         return;
      }
      maxRange = newMaxRange;
      setRange(maxRange.intersection(range));
      changeManager.notifyListeners();
   }

   public float getEffectiveMax() {
      return clipAbove ? range.max() : Float.POSITIVE_INFINITY;
   }

   public FloatRange getEffectiveRange() {
      return FloatRange.of(range.min(), getEffectiveMax());
   }

   public FloatRange getRange() {
      return range;
   }

   public void setRange(FloatRange newRange) {
      newRange = newRange.roundToMultipleOf(delta).intersection(maxRange);
      if (newRange.isEmpty()) {
         return;
      }
      if (range.equals(newRange)) {
         return;
      }
      range = newRange;
      changeManager.notifyListeners();
   }

   public float getDelta() {
      return (float) delta;
   }

   public boolean isClipAbove() {
      return clipAbove;
   }

   public void setClipAbove(boolean clip) {
      if (clipAbove == clip) {
         return;
      }
      clipAbove = clip;
      changeManager.notifyListeners();
   }

   public boolean isProportional() {
      return proportional;
   }

   public PerPingSettings getPerPingSettings() {
      return perPingSettings;
   }

   public void setPerPingSettings(PerPingSettings perPingSettings) {
      this.perPingSettings = perPingSettings;
   }

   public void setMin(float newMin) {
      double min = Math.clamp(round(newMin), maxRange.min(), maxRange.max() - delta);
      double max = Math.clamp(range.max(), min + delta, maxRange.max());
      setRange(FloatRange.of(min, max));
   }

   public void setMax(float newMax) {
      double max = Math.clamp(round(newMax), maxRange.min() + delta, maxRange.max());
      double min = Math.clamp(range.min(), maxRange.min(), max - delta);
      setRange(FloatRange.of(min, max));
   }

   private double round(double value) {
      return Math.round(value / delta) * delta;
   }

   private final class MyPerPingSettings implements PerPingSettings {
      private MyPerPingSettings() {
      }

      @Override
      public FloatRange getClipRange(PingIndex pingIndex) {
         return getEffectiveRange();
      }
   }
}
