package no.imr.korona.util.echogram;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.math.MathUtils;
import no.imr.tools.time.TimeUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * The horizontal configuration of an echogram.
 */
public abstract class EchogramPingSettings {
   private final ChangeManager changeManager = new ChangeManager();
   private PingRange pingRange = PingRange.EMPTY_RANGE;

   protected EchogramPingSettings() {
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public abstract PingContainer getPingContainer();

   public PingRange getPingRange() {
      return pingRange;
   }

   public void setPingRange(PingRange pingRange) {
      this.pingRange = pingRange;
   }

   public abstract int getWidth();

   public abstract float pingIndexToX(PingIndex pingIndex);

   public abstract PingIndex xToClosestPingIndex(double x);

   public abstract @Nullable PingIndex xToContainingPingIndex(double x);

   public PingIndex xToContainingOrClosestPingIndex(double x) {
      PingIndex pingIndex = xToContainingPingIndex(x);
      if (pingIndex != null) {
         return pingIndex;
      }
      return xToClosestPingIndex(x);
   }

   public PingIndex xToClampedContainingPingIndex(double x) {
      PingIndex pingIndex = xToContainingPingIndex(x);
      if (pingIndex != null && pingRange.contains(pingIndex)) {
         return pingIndex;
      }
      if (x < 0) {
         return pingRange.begin();
      }
      return getPingContainer().previousOrSame(pingRange.end());
   }

   public abstract void zoom(PingRange pingRange);

   public abstract void zoom(double x, double zoomFactor);

   public abstract void zoomOut();

   public int pingIndexToXIndex(PingIndex pingIndex) {
      return Math.round(pingIndexToX(pingIndex));
   }

   public Instant xToInstant(double x) {
      PingIndex a = xToContainingPingIndex(x);
      if (a == null) {
         a = xToClosestPingIndex(x);
         return a.getInstant();
      }
      PingIndex b = getPingContainer().nextOrNull(a);
      if (b == null) {
         return a.getInstant();
      }
      double xa = pingIndexToX(a);
      double xb = pingIndexToX(b);
      double dx = xb - xa;
      if (dx == 0) {
         return a.getInstant();
      }
      double f = (x - xa) / dx;
      return TimeUtils.interpolateInstant(a.getInstant(), b.getInstant(), f);
   }

   public float instantToX(Instant instant) {
      return valueToX(PingMapping.instantToTimeValue(instant), PingMapping.TIME);
   }

   public float valueToX(double value, PingMapping pingMapping) {
      PingIndex a = getPingContainer().getContainingPingIndex(value, pingMapping);
      if (a == null) {
         // Clamp to [0, width]
         PingRange totalRange = getPingContainer().getTotalRange();
         return value < pingMapping.valueOf(totalRange.begin()) ? 0 : getWidth();
      }
      PingIndex b = getPingContainer().nextOrNull(a);
      if (b == null) {
         // Ping range is empty
         return -1;
      }
      double valA = pingMapping.valueOf(a);
      double valB = pingMapping.valueOf(b);
      double diff = valB - valA;
      double f = diff == 0 ? 0 : (value - valA) / diff;
      return (float) MathUtils.interpolate(pingIndexToX(a), pingIndexToX(b), f);
   }

   public double xToValue(double x, PingMapping pingMapping) {
      PingIndex a = xToContainingPingIndex(x);
      if (a == null) {
         // Clamp to total range.
         PingRange totalRange = getPingContainer().getTotalRange();
         return pingMapping.valueOf(x < pingIndexToX(totalRange.begin()) ? totalRange.begin() : totalRange.end());
      }
      PingIndex b = getPingContainer().nextOrNull(a);
      if (b == null) {
         // Ping range is empty.
         return -1;
      }
      double xA = pingIndexToX(a);
      double xB = pingIndexToX(b);
      double diff = xB - xA;
      double f = diff == 0 ? 0 : (x - xA) / diff;
      return MathUtils.interpolate(pingMapping.valueOf(a), pingMapping.valueOf(b), f);
   }
}
