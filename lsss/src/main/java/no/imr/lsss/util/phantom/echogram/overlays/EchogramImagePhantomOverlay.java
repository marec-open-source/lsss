package no.imr.lsss.util.phantom.echogram.overlays;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.util.echogram.EchogramImage;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.util.phantom.echogram.BasePhantomEchogramModule;
import no.imr.lsss.util.phantom.echogram.PhantomEchogramImageSettings;
import no.imr.tools.listening.ListenerRegistry;

import java.awt.Graphics2D;
import java.util.List;

public final class EchogramImagePhantomOverlay extends BasePhantomOverlay {
   private final PhantomEchogramImageSettings echogramImageSettings;
   private EchogramImage echogramImage;

   public EchogramImagePhantomOverlay(ModuleInfo<?> moduleInfo, BasePhantomEchogramModule phantomEchogramModule) {
      super(moduleInfo, phantomEchogramModule);

      echogramImageSettings = new PhantomEchogramImageSettings(getLSSS(), getPhantomEchogramSettings());
      echogramImage = new EchogramImage(echogramImageSettings);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getPhantomEchogramModule().getSizeChangeManager(), newCoalescingExecListener(this::resized));

      registry.add(createRecomputeListener(), List.of(
            getPhantomEchogramModule().getEchogramAreaChangeManager(),
            getPhantomEchogramSettings().getChannelChangeManager(),
            getInterpretationSettings().getColorConverterContainer().getChangeManager()
      ));

      registry.add(getPhantomEchogramSettings().getPingSampler().getNewPingsChangeManager(), newExecListener(this::processPings));

      //---

      resized();
   }

   @Override
   protected void onDisable() {
      echogramImage = new EchogramImage(echogramImageSettings);
   }

   @Override
   public boolean isUserVisibleForegroundOverlay() {
      return false;
   }

   private void resized() {
      int width = Math.max(1, getWidth());
      int height = Math.max(1, getHeight());

      EchogramImage newEchogramImage = new EchogramImage(echogramImageSettings, getPhantomEchogramModule().getGraphicsConfiguration(), width, height);
      newEchogramImage.processPings(getPhantomEchogramSettings().getPingSampler().getAvailablePings()); // Avoids flickering by new empty image
      echogramImage = newEchogramImage;
      recompute();
   }

   private void processPings(List<Ping> pings) {
      boolean needRepaint = echogramImage.processPings(pings);
      if (needRepaint) {
         repaint();
      }
   }

   @Override
   protected OverlayDisplayData recomputeDisplayData() {
      echogramImage.clearData();
      if (getPingSettings().getPingRange().isEmpty()) {
         echogramImage.clearImage();
      } else {
         processPings(getPhantomEchogramSettings().getPingSampler().getAvailablePings());
      }
      return new DisplayData(echogramImage);
   }

   private record DisplayData(EchogramImage echogramImage) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         echogramImage.draw(g2d);
      }
   }
}
