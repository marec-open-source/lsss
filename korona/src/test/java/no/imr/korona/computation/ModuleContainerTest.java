package no.imr.korona.computation;

import no.imr.korona.Korona;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.plugins.ModulePlugin;
import no.imr.korona.plugins.ModuleService;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class ModuleContainerTest {
   private static Korona createKorona() {
      ModuleManager moduleManager = new ModuleManager(List.of(new TrivialModuleService()));
      return new Korona(moduleManager);
   }

   @Test
   void testNextPing() throws IOException {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData(10, 1).withFirstAndLastPingNumber(1, 5);

      ModuleContainer moduleContainer = new ModuleContainer(createKorona());
      moduleContainer.addModule(new TrivialConcurrentPingModule());
      moduleContainer.addModule(new TrivialGeneralPingModule());

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {

         int pingItemCount = 0;
         long maxNTDate = Long.MIN_VALUE;
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            assertTrue(ping.getNTDate() >= maxNTDate);
            maxNTDate = ping.getNTDate();
            pingItemCount += ping.getPingItems().size();
         }
         assertEquals(syntheticDataFile.getPingCount() * syntheticDataFile.getPingConfiguration().getRawFileConfiguration().getTransducerCount(), pingItemCount);
      }
   }

   private static final class TrivialModuleService extends ModuleService {
      private TrivialModuleService() {
         super(new Name("Test"));
      }

      @Override
      public ModulePlugin createPlugin() {
         return new TrivialModulePlugin(this);
      }
   }

   private static final class TrivialModulePlugin extends ModulePlugin {
      private TrivialModulePlugin(TrivialModuleService service) {
         super(service.getName());
      }

      @Override
      public void addModuleInfos(ModuleInfoCollector moduleInfoCollector) {
         moduleInfoCollector.top()
               .add(TrivialConcurrentPingModule.class, new Name("TrivialConcurrentPingModule"),
                     Set.of(ModuleCategory.NO_MODIFICATION), "")
               .add(TrivialGeneralPingModule.class, new Name("TrivialGeneralPingModule"),
                     Set.of(ModuleCategory.NO_MODIFICATION), "");
      }
   }

   private static final class TrivialConcurrentPingModule extends ConcurrentPingModule {
      @Override
      public @Nullable ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
         return null;
      }
   }

   private static final class TrivialGeneralPingModule extends GeneralPingModule {
      @Override
      public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
         return new GeneralPingModuleComputation(this, computationContext, pingSource) {
            @Override
            protected @Nullable Ping generateOutput() throws IOException {
               return inputPing();
            }
         };
      }
   }
}
