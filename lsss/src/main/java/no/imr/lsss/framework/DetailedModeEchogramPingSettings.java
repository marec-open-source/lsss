package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.util.echogram.EchogramPingSettings;
import org.jspecify.annotations.Nullable;

final class DetailedModeEchogramPingSettings extends EchogramPingSettings {
   private final InterpretationSettings interpretationSettings;

   DetailedModeEchogramPingSettings(InterpretationSettings interpretationSettings, PingRange pingRange) {
      this.interpretationSettings = interpretationSettings;

      setPingRange(pingRange);
   }

   @Override
   public PingContainer getPingContainer() {
      return interpretationSettings.getDataFileSet();
   }

   @Override
   public int getWidth() {
      return getPingRange().getPingCount();
   }

   @Override
   public float pingIndexToX(PingIndex pingIndex) {
      PingRange pingRange = getPingRange();
      if (pingRange.isEmpty()) {
         return 0;
      }
      return pingIndex.getPingNumber() - pingRange.begin().getPingNumber();
   }

   @Override
   public PingIndex xToClosestPingIndex(double x) {
      return getPingContainer().getClosestPingIndex(getPingRange().begin(), x, PingMapping.NUMBER);
   }

   @Override
   public @Nullable PingIndex xToContainingPingIndex(double x) {
      return getPingContainer().getContainingPingIndex(getPingRange().begin(), x, PingMapping.NUMBER);
   }

   @Override
   public void zoom(PingRange pingRange) {
      interpretationSettings.getPingSettings().zoom(pingRange);
   }

   @Override
   public void zoom(double x, double zoomFactor) {
      interpretationSettings.getPingSettings().zoom(x, zoomFactor);
   }

   @Override
   public void zoomOut() {
      interpretationSettings.getPingSettings().zoomOut();
   }
}
