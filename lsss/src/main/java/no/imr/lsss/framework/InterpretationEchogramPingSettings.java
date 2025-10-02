package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.util.echogram.EchogramPingSettings;
import org.jspecify.annotations.Nullable;

final class InterpretationEchogramPingSettings extends EchogramPingSettings {
   private final InterpretationSettings interpretationSettings;

   InterpretationEchogramPingSettings(InterpretationSettings interpretationSettings) {
      this.interpretationSettings = interpretationSettings;
   }

   @Override
   public PingContainer getPingContainer() {
      return interpretationSettings.getDataFileSet();
   }

   @Override
   public void setPingRange(PingRange pingRange) {
      super.setPingRange(pingRange);
      update();
   }

   @Override
   public int getWidth() {
      return interpretationSettings.getSampledPingCount();
   }

   void update() {
      getChangeManager().notifyListeners();
   }

   @Override
   public float pingIndexToX(PingIndex pingIndex) {
      PingRange pingRange = getPingRange();
      PingMapping pingMapping = interpretationSettings.getPingMapping();
      double d = pingMapping.distance(pingRange);
      if (d == 0) {
         return 0;
      }
      PingIndex firstIdx = pingRange.begin();
      double distance = pingMapping.distance(firstIdx, pingIndex);
      double fraction = distance / d;
      return (float) fraction * getWidth();
   }

   @Override
   public PingIndex xToClosestPingIndex(double x) {
      PingRange pingRange = getPingRange();
      PingMapping pingMapping = interpretationSettings.getPingMapping();
      double distance = pingMapping.distance(pingRange) * x / getWidth();
      return getPingContainer().getClosestPingIndex(pingRange.begin(), distance, pingMapping);
   }

   @Override
   public @Nullable PingIndex xToContainingPingIndex(double x) {
      PingRange pingRange = getPingRange();
      PingMapping pingMapping = interpretationSettings.getPingMapping();
      double distance = pingMapping.distance(pingRange) * x / getWidth();
      return getPingContainer().getContainingPingIndex(pingRange.begin(), distance, pingMapping);
   }

   @Override
   public void zoom(PingRange pingRange) {
      interpretationSettings.setPingRange(pingRange);
   }

   @Override
   public void zoom(double x, double zoomFactor) {
      double fraction = x / getWidth();
      double value = interpretationSettings.getValueRange().fractionToValue(fraction);
      interpretationSettings.zoomHorizontally(value, zoomFactor);
   }

   @Override
   public void zoomOut() {
      interpretationSettings.setPingRange(getPingContainer().getTotalRange());
   }
}
