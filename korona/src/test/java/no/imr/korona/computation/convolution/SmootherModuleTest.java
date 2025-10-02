package no.imr.korona.computation.convolution;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class SmootherModuleTest {
   @Test
   void constantData() throws IOException {
      float svValue = PowerData.logSvToSv(-50);

      testConstantData(simpleSyntheticData(svValue), svValue, 30);
      testConstantData(increasingTransducerDepthDefinition(svValue), svValue, 60);
      testConstantData(decreasingTransducerDepthDefinition(svValue), svValue, 60);
      testConstantData(varyingSampleIntervalDefinition(svValue), svValue, 60);
      testConstantData(missingPowerDataDefinition(svValue), svValue, 30);
   }

   private static void testConstantData(SyntheticData syntheticData, float svValue, int expectedPowerDataCount) throws IOException {
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      moduleContainer.getConfigFileSettings().restoreInstallationLocations();
      moduleContainer.addModule(new SmootherModule());

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.toPingReader())) {
         AtomicInteger powerDataCounter = new AtomicInteger();
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            ping.getNonNullPowerDatas().forEach(powerData -> {
               powerDataCounter.getAndIncrement();
               for (float sv : powerData.getSv()) {
                  assertEquals(svValue, sv, svValue * 1e-6);
               }
            });
         }
         assertEquals(expectedPowerDataCount, powerDataCounter.get());
      }
   }

   private static SyntheticData simpleSyntheticData(float svValue) {
      SyntheticData syntheticData = new ConstantSyntheticData(10, svValue);
      syntheticData.setFirstAndLastPingNumber(1, 5);
      return syntheticData;
   }

   private static SyntheticData varyingSampleIntervalDefinition(float svValue) {
      SyntheticData syntheticData = new ConstantSyntheticData(100, svValue) {
         @Override
         protected float getSampleInterval(PingIndex pingIndex, int channel) {
            return pingIndex.getPingNumber() < 5 ? super.getSampleInterval(pingIndex, channel) * 0.5f : super.getSampleInterval(pingIndex, channel);
         }
      };
      syntheticData.setFirstAndLastPingNumber(1, 10);
      return syntheticData;
   }

   private static SyntheticData increasingTransducerDepthDefinition(float svValue) {
      SyntheticData syntheticData = new ConstantSyntheticData(100, svValue) {
         @Override
         protected float getTransducerDepth(PingIndex pingIndex, int channel) {
            return pingIndex.getPingNumber() < 5 ? 7.5f : 15.0f;
         }
      };
      syntheticData.setFirstAndLastPingNumber(1, 10);
      return syntheticData;
   }

   private static SyntheticData decreasingTransducerDepthDefinition(float svValue) {
      SyntheticData syntheticData = new ConstantSyntheticData(100, svValue) {
         @Override
         protected float getTransducerDepth(PingIndex pingIndex, int channel) {
            return pingIndex.getPingNumber() < 5 ? 15.0f : 7.5f;
         }
      };
      syntheticData.setFirstAndLastPingNumber(1, 10);
      return syntheticData;
   }

   private static SyntheticData missingPowerDataDefinition(float svValue) {
      SyntheticData syntheticData = new ConstantSyntheticData(100, svValue) {
         @Override
         protected boolean hasPowerData(PingIndex pingIndex, int channel) {
            return (channel + pingIndex.getPingNumber()) % 2 == 0;
         }
      };
      syntheticData.setFirstAndLastPingNumber(1, 10);
      return syntheticData;
   }
}
