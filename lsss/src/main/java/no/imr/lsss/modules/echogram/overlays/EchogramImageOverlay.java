package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.util.echogram.EchogramImage;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.EchogramModuleImageSettings;
import no.imr.tools.listening.ListenerRegistry;

import java.awt.Graphics2D;
import java.util.List;

public final class EchogramImageOverlay extends BaseEchogramOverlay {
   private final EchogramModuleImageSettings echogramImageSettings;
   private EchogramImage echogramImage;

   public EchogramImageOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      echogramImageSettings = new EchogramModuleImageSettings(getInterpretationSettings(), echogramModule.getZSettings());
      echogramImage = new EchogramImage(echogramImageSettings);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getEchogramModule().getSizeChangeManager(), newCoalescingExecListener(this::resized));

      registry.add(createRecomputeListener(), List.of(
            getEchogramModule().echogramArea(),
            getInterpretationSettings().getChannelChangeManager(),
            getInterpretationSettings().getColorConverterContainer().getChangeManager(),
            getRegionManager().getThresholdManager().getChangeManager()
      ));

      registry.add(getInterpretationSettings().getPingSampler().getNewPingsChangeManager(), newExecListener(this::processPings));

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

      EchogramImage newEchogramImage = new EchogramImage(echogramImageSettings, getEchogramModule().getGraphicsConfiguration(), width, height);
      newEchogramImage.processPings(getInterpretationSettings().getPingSampler().getAvailablePings()); // Avoids flickering by new empty image
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
      if (getInterpretationSettings().getPingRange().isEmpty()) {
         echogramImage.clearImage();
      } else {
         processPings(getInterpretationSettings().getPingSampler().getAvailablePings());
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
