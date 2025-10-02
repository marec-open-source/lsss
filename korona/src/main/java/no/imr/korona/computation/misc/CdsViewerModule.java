package no.imr.korona.computation.misc;

import no.imr.korona.Korona;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleEditor;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.datagrams.Cds0Datagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;

/**
 * For viewing the module configuration stored in a processed file.
 */
public final class CdsViewerModule extends SimplePingModule {
   public CdsViewerModule() {
   }

   @Override
   public SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new CdsViewerModuleComputation(this, computationContext, pingSource);
   }

   private static final class CdsViewerModuleComputation extends SimplePingModuleComputation {
      private CdsViewerModuleComputation(CdsViewerModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         for (PingItem pingItem : pingConfiguration.getConfigurationItems()) {
            if (pingItem instanceof Cds0Datagram cds0Datagram) {
               showConfiguration(cds0Datagram, computationContext.getModuleContainer().getKorona());
            }
         }
      }

      private static void showConfiguration(Cds0Datagram cds0Datagram, Korona korona) {
         ModuleContainer moduleContainer = new ModuleContainer(korona);
         moduleContainer.fromXml(cds0Datagram.getDocument().getRootElement());
         new ModuleEditor(moduleContainer, false, null)
               .show();
      }
   }
}
