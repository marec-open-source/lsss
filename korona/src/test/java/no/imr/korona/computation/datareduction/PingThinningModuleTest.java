package no.imr.korona.computation.datareduction;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PingThinningModuleTest {
   @Test
   void test() throws IOException {
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      SyntheticDataFile syntheticDataFile = new SimpleSyntheticData().withFirstAndLastPingNumber(0, 9);

      PingThinningModule pingThinningModule = moduleContainer.addModule(new PingThinningModule());
      pingThinningModule.pingsToSkipInitially.setIntValue(0);

      pingThinningModule.pingsToSkipPeriodically.setIntValue(0);
      check(moduleContainer, syntheticDataFile, List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

      pingThinningModule.pingsToSkipPeriodically.setIntValue(1);
      check(moduleContainer, syntheticDataFile, List.of(0, 2, 4, 6, 8));

      pingThinningModule.pingsToSkipPeriodically.setIntValue(2);
      check(moduleContainer, syntheticDataFile, List.of(0, 3, 6, 9));

      pingThinningModule.pingsToSkipPeriodically.setIntValue(3);
      check(moduleContainer, syntheticDataFile, List.of(0, 4, 8));

      pingThinningModule.pingsToSkipPeriodically.setIntValue(4);
      check(moduleContainer, syntheticDataFile, List.of(0, 5));

      // ---
      pingThinningModule.pingsToSkipPeriodically.setIntValue(3);

      pingThinningModule.pingsToSkipInitially.setIntValue(1);
      check(moduleContainer, syntheticDataFile, List.of(1, 5, 9));

      pingThinningModule.pingsToSkipInitially.setIntValue(2);
      check(moduleContainer, syntheticDataFile, List.of(2, 6));

      pingThinningModule.pingsToSkipInitially.setIntValue(3);
      check(moduleContainer, syntheticDataFile, List.of(3, 7));

      pingThinningModule.pingsToSkipInitially.setIntValue(9);
      check(moduleContainer, syntheticDataFile, List.of(9));

      pingThinningModule.pingsToSkipInitially.setIntValue(10);
      check(moduleContainer, syntheticDataFile, List.of());
   }

   private static void check(ModuleContainer moduleContainer, SyntheticDataFile syntheticDataFile, List<Integer> expectedEpochSeconds) throws IOException {
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         List<Integer> actualEpochSeconds = new ArrayList<>();
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            actualEpochSeconds.add((int) ping.getInstant().getEpochSecond());
            assertEquals(actualEpochSeconds.size(), ping.getPingNumber());
         }
         assertEquals(expectedEpochSeconds, actualEpochSeconds);
      }
   }

   private static final class SimpleSyntheticData extends SyntheticData {
      private SimpleSyntheticData() {
      }

      @Override
      public Instant getInstant(long pingNumber) {
         return Instant.ofEpochSecond(pingNumber);
      }

      @Override
      public void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
         float[] logSv = new float[1];
         powerData.setLogSv(logSv);
      }
   }
}
