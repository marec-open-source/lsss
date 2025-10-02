package no.imr.korona.computation.datareduction;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PingThinningModuleTest {
   @Test
   void test() throws IOException {
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      SimpleSyntheticData syntheticData = new SimpleSyntheticData();

      PingThinningModule pingThinningModule = moduleContainer.addModule(new PingThinningModule());
      pingThinningModule.pingsToSkipInitially.setIntValue(0);

      pingThinningModule.pingsToSkipPeriodically.setIntValue(0);
      check(moduleContainer, syntheticData, List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

      pingThinningModule.pingsToSkipPeriodically.setIntValue(1);
      check(moduleContainer, syntheticData, List.of(0, 2, 4, 6, 8));

      pingThinningModule.pingsToSkipPeriodically.setIntValue(2);
      check(moduleContainer, syntheticData, List.of(0, 3, 6, 9));

      pingThinningModule.pingsToSkipPeriodically.setIntValue(3);
      check(moduleContainer, syntheticData, List.of(0, 4, 8));

      pingThinningModule.pingsToSkipPeriodically.setIntValue(4);
      check(moduleContainer, syntheticData, List.of(0, 5));

      // ---
      pingThinningModule.pingsToSkipPeriodically.setIntValue(3);

      pingThinningModule.pingsToSkipInitially.setIntValue(1);
      check(moduleContainer, syntheticData, List.of(1, 5, 9));

      pingThinningModule.pingsToSkipInitially.setIntValue(2);
      check(moduleContainer, syntheticData, List.of(2, 6));

      pingThinningModule.pingsToSkipInitially.setIntValue(3);
      check(moduleContainer, syntheticData, List.of(3, 7));

      pingThinningModule.pingsToSkipInitially.setIntValue(9);
      check(moduleContainer, syntheticData, List.of(9));

      pingThinningModule.pingsToSkipInitially.setIntValue(10);
      check(moduleContainer, syntheticData, List.of());
   }

   private static void check(ModuleContainer moduleContainer, SimpleSyntheticData syntheticData, List<Integer> expectedNTDates) throws IOException {
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.toPingReader())) {
         List<Integer> actualNTDates = new ArrayList<>();
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            actualNTDates.add((int) ping.getNTDate());
            assertEquals(actualNTDates.size(), ping.getPingNumber());
         }
         assertEquals(expectedNTDates, actualNTDates);
      }
   }

   private static final class SimpleSyntheticData extends SyntheticData {
      private SimpleSyntheticData() {
         setFirstAndLastPingNumber(0, 9);
      }

      @Override
      protected long getNTDate(long pingNumber) {
         return pingNumber;
      }

      @Override
      protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
         float[] logSv = new float[1];
         powerData.setLogSv(logSv);
      }
   }
}
