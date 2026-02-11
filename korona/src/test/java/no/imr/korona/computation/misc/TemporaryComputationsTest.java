package no.imr.korona.computation.misc;

import no.imr.korona.Korona;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.computation.datareduction.ChannelRemovalModule;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TemporaryComputationsTest {
   @Test
   void bottom() throws IOException {
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      moduleContainer.addModule(new TemporaryComputationsBeginModule());
      ChannelRemovalModule channelRemovalModule = moduleContainer.addModule(new ChannelRemovalModule());
      channelRemovalModule.channels.setValue(List.of(2, 3));
      moduleContainer.addModule(new ConcurrentPingModule() {
         @Override
         public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
            return new ConcurrentPingModuleComputation(this, computationContext, pingSource) {
               @Override
               protected void processPing(Ping ping) {
                  Arrays.fill(ping.getBot0Datagram().getChannelDepths(), 66);
               }
            };
         }
      });
      moduleContainer.addModule(new TemporaryComputationsEndModule());
      ConstantSyntheticData syntheticData = new ConstantSyntheticData() {
         @Override
         protected float getBottomDepth(PingIndex pingIndex, int channel) {
            return 71;
         }
      };
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(1, 1000);

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         Ping ping = computation.nextPing();
         assertNotNull(ping);
         assertEquals(6, ping.getPingItems().size());
         assertArrayEquals(new double[]{66, 71, 71, 66, 66, 66}, ping.getBot0Datagram().getChannelDepths());
      }
   }
}
