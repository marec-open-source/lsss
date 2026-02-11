package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.util.echogram.EchogramPingSettings;
import org.jspecify.annotations.Nullable;

final class InterpretationEchogramPingSettings extends EchogramPingSettings {
   private final InterpretationSettings interpretationSettings;
   private PingMapping pingMapping;
   private PingIndex beginPingIndex;
   private double distanceToXFactor;

   InterpretationEchogramPingSettings(InterpretationSettings interpretationSettings) {
      this.interpretationSettings = interpretationSettings;
      pingMapping = interpretationSettings.getPingMapping();
      beginPingIndex = getPingRange().begin();
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
      pingMapping = interpretationSettings.getPingMapping();
      beginPingIndex = getPingRange().begin();
      double d = pingMapping.distance(getPingRange());
      distanceToXFactor = d > 0 ? getWidth() / d : 0;
      getChangeManager().notifyListeners();
   }

   @Override
   public float pingIndexToX(PingIndex pingIndex) {
      double distance = pingMapping.distance(beginPingIndex, pingIndex);
      return (float) (distance * distanceToXFactor);
   }

   @Override
   public PingIndex xToClosestPingIndex(double x) {
      double distance = x / distanceToXFactor;
      return getPingContainer().getClosestPingIndex(beginPingIndex, distance, pingMapping);
   }

   @Override
   public @Nullable PingIndex xToContainingPingIndex(double x) {
      double distance = x / distanceToXFactor;
      return getPingContainer().getContainingPingIndex(beginPingIndex, distance, pingMapping);
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
