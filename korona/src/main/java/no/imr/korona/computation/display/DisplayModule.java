package no.imr.korona.computation.display;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.viewer.KoronaPlaybox;
import no.imr.tools.swing.GuiUtils;

import javax.swing.JComponent;

public final class DisplayModule extends VisualizerModule {
   private final Display display;

   public DisplayModule() {
      display = GuiUtils.getNowOrWait(Display::new);
   }

   @Override
   public JComponent getComponent() {
      return display.getComponent();
   }

   @Override
   protected void init(KoronaPlaybox koronaPlaybox) {
      display.setKoronaPlaybox(koronaPlaybox);
   }

   @Override
   protected SimplePingModuleComputation createPlayboxComputation(ComputationContext computationContext, PingSource pingSource) {
      return new DisplayModuleComputation(this, computationContext, pingSource);
   }

   private static final class DisplayModuleComputation extends SimplePingModuleComputation {
      private final DisplayModule module;

      private DisplayModuleComputation(DisplayModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         this.module = module;
         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         GuiUtils.invokeNowOrWait(() -> module.display.setPingConfiguration(pingConfiguration));
      }

      @Override
      protected void processPing(Ping ping) {
         module.display.addPing(ping);
      }
   }
}
