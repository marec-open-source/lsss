package no.imr.lsss.util.phantom.echogram;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.lsss.LSSS;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public final class PhantomEchogramPingSettings extends EchogramPingSettings {
   private final PhantomEchogramSettings phantomEchogramSettings;
   private final EchogramPingSettings lsssPingSettings;
   private final LSSS lsss;

   PhantomEchogramPingSettings(PhantomEchogramSettings phantomEchogramSettings, LSSS lsss) {
      this.lsss = lsss;
      this.phantomEchogramSettings = phantomEchogramSettings;
      lsssPingSettings = lsss.getInterpretationSettings().getPingSettings();

      Listener.of(this::update).addToAndNotify(
            phantomEchogramSettings.getDataSelectionChangeManager(),
            lsssPingSettings.getChangeManager()
      );
   }

   void update() {
      PingIndex begin = xToClosestPingIndex(0);
      PingIndex end = xToClosestPingIndex(getWidth());
      setPingRange(PingRange.of(begin, end));
      getChangeManager().notifyListeners();
   }

   @Override
   public PingContainer getPingContainer() {
      return phantomEchogramSettings.getPhantomDataFileSet();
   }

   @Override
   public int getWidth() {
      return phantomEchogramSettings.getEchogramWidth();
   }

   @Override
   public float pingIndexToX(PingIndex pingIndex) {
      Instant lsssTime = phantomEchogramSettings.getPhantomPingIndexConverter().otherTimeToLsssTime(pingIndex.getInstant());
      //Clamp lsssTime to total LSSS range.
      PingRange totalRange = lsss.getDataManager().getDataFileSet().getTotalRange();
      if (!totalRange.isEmpty()) {
         PingIndex lastPingIndex = lsss.getDataManager().getDataFileSet().previousOrSame(totalRange.end());
         lsssTime = Utils.clamp(lsssTime, totalRange.begin().getInstant(), lastPingIndex.getInstant());
      }
      float xLSSS = lsssPingSettings.instantToX(lsssTime);
      return getWidth() * xLSSS / lsssPingSettings.getWidth();
   }

   @Override
   public Instant xToInstant(double x) {
      double xLSSS = lsssPingSettings.getWidth() * x / getWidth();
      Instant lsssTime = lsssPingSettings.xToInstant(xLSSS);
      return phantomEchogramSettings.getPhantomPingIndexConverter().lsssTimeToOtherTime(lsssTime);
   }

   @Override
   public float instantToX(Instant instant) {
      Instant lsssTime = phantomEchogramSettings.getPhantomPingIndexConverter().otherTimeToLsssTime(instant);
      float xLSSS = lsssPingSettings.instantToX(lsssTime);
      return getWidth() * xLSSS / lsssPingSettings.getWidth();
   }

   @Override
   public PingIndex xToClosestPingIndex(double x) {
      Instant instant = xToInstant(x);
      return getPingContainer().getClosestPingIndex(PingMapping.instantToTimeValue(instant), PingMapping.TIME);
   }

   @Override
   public @Nullable PingIndex xToContainingPingIndex(double x) {
      Instant instant = xToInstant(x);
      return getPingContainer().getContainingPingIndex(PingMapping.instantToTimeValue(instant), PingMapping.TIME);
   }

   @Override
   public void zoom(PingRange pingRange) {
      lsssPingSettings.zoom(phantomEchogramSettings.getPhantomPingIndexConverter().otherToClosestLsss(pingRange));
   }

   @Override
   public void zoom(double x, double zoomFactor) {
      lsssPingSettings.zoom(x, zoomFactor);
   }

   @Override
   public void zoomOut() {
      lsssPingSettings.zoomOut();
   }
}
