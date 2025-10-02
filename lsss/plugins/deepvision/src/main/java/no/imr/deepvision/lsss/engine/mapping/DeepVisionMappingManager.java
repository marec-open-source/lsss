package no.imr.deepvision.lsss.engine.mapping;

import no.imr.deepvision.lsss.config.DeepVisionDataConf;
import no.imr.deepvision.lsss.engine.data.DeepVisionDataAdministrator;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.lsss.LSSS;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;

import java.util.List;

public final class DeepVisionMappingManager {
   private final LSSS lsss;
   private final DeepVisionDataConf deepVisionDataConf;
   private final DeepVisionDataAdministrator dataAdministrator;
   private DeepVisionMapping deepVisionMapping = new IdentityDeepVisionMapping(List.of(), DataFileSet.empty(), 0);
   private final ChangeManager changeManager = new ChangeManager();

   public DeepVisionMappingManager(LSSS lsss, DeepVisionDataConf deepVisionDataConf, DeepVisionDataAdministrator dataAdministrator) {
      this.lsss = lsss;
      this.deepVisionDataConf = deepVisionDataConf;
      this.dataAdministrator = dataAdministrator;
   }

   public void setup() {
      Listener listener = this::update;
      listener.addTo(
            deepVisionDataConf.distanceMapping,
            deepVisionDataConf.distanceBehindShip,
            dataAdministrator.getChangeManager(),
            lsss.getDataManager().getDataFileSetChangeManager());
   }

   public DeepVisionMapping getDeepVisionMapping() {
      return deepVisionMapping;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   private void update() {
      deepVisionMapping = switch (deepVisionDataConf.distanceMapping.getValue()) {
         case IDENTITY -> new IdentityDeepVisionMapping(dataAdministrator.getAllFiles(), lsss.getDataManager().getDataFileSet(), deepVisionDataConf.distanceBehindShip.getValue());
         case GEO -> new GeoPosDeepVisionMapping(dataAdministrator.getAllFiles(), lsss.getDataManager().getDataFileSet());
      };
      changeManager.notifyListeners();
   }
}
