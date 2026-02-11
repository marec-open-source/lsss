package no.imr.korona.computation.datareduction;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.PowerData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class DownsamplingModuleTest {
   @Test
   void test() throws IOException {
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      SyntheticDataFile syntheticDataFile = new SimpleSyntheticData().withFirstAndLastPingNumber(0, 9);

      DownsamplingModule downsamplingModule = moduleContainer.addModule(new DownsamplingModule());
      downsamplingModule.downsamplingMethod.setValue(DownsamplingModule.Method.FACTOR);
      downsamplingModule.downsamplingFactor.setValue(2);

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         Ping ping = computation.nextPing();
         assertNotNull(ping);
         PowerData powerData = ping.getPowerData(1);
         assertNotNull(powerData);
         assertEquals(5, powerData.getCount());
         assertArrayEquals(new float[]{10_000, 10_000, 10_000, 10_000, 10_000}, powerData.getSv());
         AngleData angleData = powerData.getAngleData();
         assertNotNull(angleData);
         assertArrayEquals(new float[]{0, 0, 2, 2, 4, 4, 6, 6, 8, 8}, angleData.getElectricalAngles());
      }

      downsamplingModule.downsamplingFactor.setValue(3);

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         Ping ping = computation.nextPing();
         assertNotNull(ping);
         PowerData powerData = ping.getPowerData(1);
         assertNotNull(powerData);
         assertEquals(3, powerData.getCount());
         assertArrayEquals(new float[]{10_000, 10_000, 10_000}, powerData.getSv());
         AngleData angleData = powerData.getAngleData();
         assertNotNull(angleData);
         assertArrayEquals(new float[]{1, 1, 4, 4, 7, 7}, angleData.getElectricalAngles());
      }
   }

   private static final class SimpleSyntheticData extends SyntheticData {
      private SimpleSyntheticData() {
      }

      @Override
      protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
         float[] sv = new float[10];
         Arrays.fill(sv, 10_000);
         powerData.setSv(sv);

         float[] angles = new float[2 * powerData.getCount()];
         for (int i = 0; i < powerData.getCount(); i++) {
            angles[2 * i] = i;
            angles[2 * i + 1] = i;
         }
         powerData.setElectricAngles(angles);
      }
   }
}
