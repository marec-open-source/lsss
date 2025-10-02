package no.imr.lsss.util.phantom.echogram;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.PingSampler;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.util.DataUtils;
import no.imr.lsss.LSSS;
import no.imr.lsss.util.PingIndexConverter;
import no.imr.lsss.util.phantom.PhantomDataAdministrator;
import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import org.jspecify.annotations.Nullable;

import java.util.function.LongSupplier;

/**
 * Settings for a phantom echogram.
 */
public final class PhantomEchogramSettings {
   private final LSSS lsss;
   private final PhantomDataAdministrator phantomDataAdministrator;
   private final PingIndexConverter phantomPingIndexConverter;

   private int echogramWidth;
   private final PhantomEchogramPingSettings echogramPingSettings;
   private final PhantomEchogramZSettings echogramZSettings;
   private final PingSampler pingSampler;

   private int channel = 1;
   private final ChangeManager channelChangeManager = new ChangeManager();

   public PhantomEchogramSettings(LSSS lsss, PhantomDataAdministrator phantomDataAdministrator, LongSupplier ntDateOffset) {
      this.lsss = lsss;
      this.phantomDataAdministrator = phantomDataAdministrator;
      phantomPingIndexConverter = new PingIndexConverter(lsss, phantomDataAdministrator.getPhantomDataManager(), ntDateOffset);

      echogramPingSettings = new PhantomEchogramPingSettings(this, lsss);
      echogramZSettings = new PhantomEchogramZSettings(this);

      pingSampler = new PingSampler(getPhantomDataManager(), echogramPingSettings);

      echogramPingSettings.getChangeManager().addListener(this::update);
      echogramZSettings.getZoomedChangeManager().addListener(lsss.getInterpretationSettings().getNavigationHistory()::addCheckPoint);
      getDataSelectionChangeManager().addListener(() -> {
         echogramZSettings.reset();
         lsss.getInterpretationSettings().getNavigationHistory().reset();
      });
   }

   public PhantomEchogramPingSettings getEchogramPingSettings() {
      return echogramPingSettings;
   }

   public PhantomEchogramZSettings getEchogramZSettings() {
      return echogramZSettings;
   }

   public PingIndexConverter getPhantomPingIndexConverter() {
      return phantomPingIndexConverter;
   }

   public int getEchogramWidth() {
      return echogramWidth;
   }

   public void setEchogramWidth(int echogramWidth) {
      this.echogramWidth = echogramWidth;
      echogramPingSettings.update();
   }

   public PingSampler getPingSampler() {
      return pingSampler;
   }

   private void update() {
      pingSampler.cancelPingRequest();
      pingSampler.waitForPingRequest();

      pingSampler.requestPings(lsss.getConfigurationManager().getAppMiscConf().pingLoading.getValue());
   }

   public ChangeManager getDataSelectionChangeManager() {
      return phantomDataAdministrator.getChangedManager();
   }

   public PhantomDataAdministrator getPhantomDataAdministrator() {
      return phantomDataAdministrator;
   }

   public DataFileSet getPhantomDataFileSet() {
      return phantomDataAdministrator.getPhantomDataManager().getDataFileSet();
   }

   public DataManager getPhantomDataManager() {
      return phantomDataAdministrator.getPhantomDataManager();
   }

   public int getChannel() {
      return channel;
   }

   public void setChannel(int channel) {
      if (this.channel == channel) {
         return;
      }
      this.channel = channel;
      channelChangeManager.notifyListeners();
   }

   public ChangeManager getChannelChangeManager() {
      return channelChangeManager;
   }

   public void shiftChannel(int shift) {
      int transducerCount = getPhantomDataFileSet().getTransducerCount();
      if (transducerCount > 0) {
         setChannel(Utils.mod(channel - 1 + shift, transducerCount) + 1);
      }
   }

   public @Nullable Ping xToAvailablePing(double x) {
      PingIndex pingIndex = echogramPingSettings.xToContainingPingIndex(x);
      if (pingIndex == null) {
         return null;
      }
      return DataUtils.getContainingPingIndex(pingSampler.getAvailablePings(), null, pingIndex.getPingNumber(), PingMapping.NUMBER);
   }
}
