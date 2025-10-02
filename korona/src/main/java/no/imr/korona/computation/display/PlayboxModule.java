package no.imr.korona.computation.display;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.SimplePingModule;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.viewer.KoronaPlaybox;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

public abstract class PlayboxModule extends SimplePingModule {
   private @Nullable KoronaPlaybox koronaPlaybox;

   protected PlayboxModule() {
   }

   @Override
   public final @Nullable SimplePingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      if (computationContext.isUsingKoronaPlaybox()) {
         return createPlayboxComputation(computationContext, pingSource);
      }
      return null;
   }

   protected abstract SimplePingModuleComputation createPlayboxComputation(ComputationContext computationContext, PingSource pingSource) throws IOException;

   public @Nullable KoronaPlaybox getKoronaPlaybox() {
      return koronaPlaybox;
   }

   public void setKoronaPlaybox(@Nullable KoronaPlaybox koronaPlaybox) {
      if (this.koronaPlaybox == koronaPlaybox) {
         return;
      }
      this.koronaPlaybox = koronaPlaybox;
      if (koronaPlaybox != null) {
         init(koronaPlaybox);
      } else {
         exit();
      }
   }

   protected void init(KoronaPlaybox koronaPlaybox) {
   }

   protected void exit() {
   }
}
