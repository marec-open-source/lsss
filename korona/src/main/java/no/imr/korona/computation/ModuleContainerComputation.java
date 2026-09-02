package no.imr.korona.computation;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModuleContainerComputation implements PingSource {
   private final ComputationContext computationContext;
   private final PingSource pingSource;
   private final List<BaseModuleComputation> moduleComputations = new ArrayList<>();

   public ModuleContainerComputation(ComputationContext computationContext) throws IOException {
      this.computationContext = computationContext;
      PingReader pingReader = computationContext.getPingReader();
      try {
         PingSource pingSource = new PingSource() {
            @Override
            public PingConfiguration getPingConfiguration() {
               return pingReader.getPingConfiguration();
            }

            @Override
            public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
               Ping ping = pingReader.nextPing(asyncHandle);
               computationContext.getPingBuffering().in(ping);
               return ping;
            }

            @Override
            public void close() throws IOException {
               pingReader.close();
            }
         };
         for (BaseModule module : computationContext.getModuleContainer().getModules()) {
            if (computationContext.getAsyncHandle().isCancelled()) {
               close();
               this.pingSource = pingSource;
               return;
            }
            BaseModuleComputation moduleComputation = module.doConfigure(computationContext, pingSource);
            if (moduleComputation != null) {
               moduleComputations.add(moduleComputation);
               pingSource = moduleComputation;
            }
         }
         this.pingSource = pingSource;
      } catch (Exception e) {
         Utils.closeOrSuppress(e, this);
         throw e;
      }
   }

   public ComputationContext getComputationContext() {
      return computationContext;
   }

   public List<BaseModuleComputation> getModuleComputations() {
      return moduleComputations;
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingSource.getPingConfiguration();
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      computationContext.setAsyncHandle(asyncHandle);
      return nextPing();
   }

   public @Nullable Ping nextPing() throws IOException {
      Ping ping = pingSource.nextPing(computationContext.getAsyncHandle());
      computationContext.getPingBuffering().out(ping);
      return ping;
   }

   @Override
   public void close() throws IOException {
      computationContext.getPingReader().close();
      for (BaseModuleComputation moduleComputation : moduleComputations) {
         moduleComputation.close();
      }
      Path koronaDir = computationContext.getAssociatedKoronaDirectory();
      if (koronaDir != null) {
         FileUtils.deleteDirectoryIfEmpty(koronaDir);
      }
   }
}
