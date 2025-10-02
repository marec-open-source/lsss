package no.imr.korona.computation.misc;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class CombinationModuleTest {
   @Test
   void testResampling() throws IOException {
      float svValue = PowerData.logSvToSv(-50);
      int count = 100;
      float normalTransducerDepth = 7.5f;
      float firstChannelDepth = 1;

      SyntheticData syntheticData = new ConstantSyntheticData(count, svValue) {
         @Override
         protected float getTransducerDepth(PingIndex pingIndex, int channel) {
            return channel == 1 ? firstChannelDepth : normalTransducerDepth; //offset for the first channel
         }
      };
      syntheticData.setFirstAndLastPingNumber(1, 5);

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      CombinationModule combinationModule = moduleContainer.addModule(new CombinationModule());
      combinationModule.firstOperandChannel.setIntValue(1);
      combinationModule.secondOperandChannel.setIntValue(2);
      combinationModule.operation.setValue(CombinationModule.Operation.MEAN);

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.toPingReader())) {

         int lastChannel = computation.getPingConfiguration().getRawFileConfiguration().getTransducerCount();
         int pingCount = 0;

         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            pingCount++;
            PowerData powerData = ping.getPowerData(lastChannel);
            assertNotNull(powerData);
            assertEquals(normalTransducerDepth, powerData.getSampleDepth(0), powerData.getSampleDistance());
            assertEquals(count - Math.round((normalTransducerDepth - firstChannelDepth) / powerData.getSampleDistance()), powerData.getCount());
            assertEquals(svValue, powerData.getSv()[0], 1e-4);
         }

         assertEquals(5, pingCount);
      }
   }
}
