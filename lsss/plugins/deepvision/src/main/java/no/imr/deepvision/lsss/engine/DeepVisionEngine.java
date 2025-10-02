package no.imr.deepvision.lsss.engine;

import no.imr.deepvision.lsss.DeepVisionPlugin;
import no.imr.deepvision.lsss.config.DeepVisionDataConf;
import no.imr.deepvision.lsss.engine.data.DeepVisionDataAdministrator;
import no.imr.deepvision.lsss.engine.data.DeepVisionSelectedFrame;
import no.imr.deepvision.lsss.engine.mapping.DeepVisionMappingManager;
import no.imr.lsss.LSSS;

public final class DeepVisionEngine {
   private final LSSS lsss;
   private final DeepVisionDataConf deepVisionDataConf;
   private final DeepVisionDataAdministrator dataAdministrator;
   private final DeepVisionMappingManager deepVisionMappingManager;
   private final DeepVisionSelectedFrame deepVisionSelectedFrame;
   private final ImageDiffCache imageDiffCache;

   public DeepVisionEngine(DeepVisionPlugin plugin) {
      lsss = plugin.getLSSS();
      deepVisionDataConf = new DeepVisionDataConf(plugin);
      dataAdministrator = new DeepVisionDataAdministrator(lsss, deepVisionDataConf);
      deepVisionMappingManager = new DeepVisionMappingManager(lsss, deepVisionDataConf, dataAdministrator);
      deepVisionSelectedFrame = new DeepVisionSelectedFrame(lsss, dataAdministrator);
      imageDiffCache = new ImageDiffCache(dataAdministrator);
   }

   public void setup() {
      deepVisionMappingManager.setup();
   }

   public LSSS getLSSS() {
      return lsss;
   }

   public DeepVisionDataConf getDeepVisionDataConf() {
      return deepVisionDataConf;
   }

   public DeepVisionDataAdministrator getDataAdministrator() {
      return dataAdministrator;
   }

   public DeepVisionMappingManager getDeepVisionMappingManager() {
      return deepVisionMappingManager;
   }

   public DeepVisionSelectedFrame getDeepVisionSelectedFrame() {
      return deepVisionSelectedFrame;
   }

   public ImageDiffCache getImageDiffCache() {
      return imageDiffCache;
   }
}
