package no.imr.korona.util.echogram;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import org.jspecify.annotations.Nullable;

public final class DefaultEchogramPingSettings extends EchogramPingSettings {
   private PingContainer pingContainer;
   private PingMapping pingMapping;
   private int width;

   public DefaultEchogramPingSettings(PingContainer pingContainer, PingMapping pingMapping) {
      this.pingContainer = pingContainer;
      this.pingMapping = pingMapping;
   }

   @Override
   public PingContainer getPingContainer() {
      return pingContainer;
   }

   public void setPingContainer(PingContainer pingContainer) {
      this.pingContainer = pingContainer;
   }

   @Override
   public int getWidth() {
      return width;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   @Override
   public float pingIndexToX(PingIndex pingIndex) {
      PingRange pingRange = getPingRange();
      double d = pingMapping.distance(pingRange);
      if (d == 0) {
         return 0;
      }
      PingIndex firstIdx = pingRange.begin();
      double distance = pingMapping.distance(firstIdx, pingIndex);
      double fraction = distance / d;
      return (float) fraction * width;
   }

   @Override
   public PingIndex xToClosestPingIndex(double x) {
      PingRange pingRange = getPingRange();
      double distance = pingMapping.distance(pingRange) * x / width;
      return pingContainer.getClosestPingIndex(pingRange.begin(), distance, pingMapping);
   }

   @Override
   public @Nullable PingIndex xToContainingPingIndex(double x) {
      PingRange pingRange = getPingRange();
      double distance = pingMapping.distance(pingRange) * x / width;
      return pingContainer.getContainingPingIndex(pingRange.begin(), distance, pingMapping);
   }

   @Override
   public void zoom(PingRange pingRange) {
      setPingRange(pingRange);
   }

   @Override
   public void zoom(double x, double zoomFactor) {
      throw new UnsupportedOperationException("todo"); // todo
   }

   @Override
   public void zoomOut() {
      setPingRange(pingContainer.getTotalRange());
   }
}
