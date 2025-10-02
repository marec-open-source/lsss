package no.imr.korona.computation.misc;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class TimeIntervalModuleTest {
   @Test
   void test() throws IOException {
      SyntheticData syntheticData = new ConstantSyntheticData();
      syntheticData.setFirstAndLastPingNumber(2, 20);

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      TimeIntervalModule timeIntervalModule = moduleContainer.addModule(new TimeIntervalModule());
      timeIntervalModule.startDate.setValue(Optional.of(syntheticData.createPingIndex(5).getInstant()));
      timeIntervalModule.endDate.setValue(Optional.of(syntheticData.createPingIndex(15).getInstant()));

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.toPingReader())) {
         List<Ping> pings = getPings(computation);
         assertEquals(11, pings.size());
         assertEquals(5, pings.getFirst().getPingNumber());
         assertEquals(15, pings.getLast().getPingNumber());
      }

      timeIntervalModule.startRelativePingNumber.setIntValue(7);
      timeIntervalModule.endRelativePingNumber.setIntValue(12);

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.toPingReader())) {
         List<Ping> pings = getPings(computation);
         assertEquals(6, pings.size());
         assertEquals(8, pings.getFirst().getPingNumber());
         assertEquals(13, pings.getLast().getPingNumber());
      }
   }

   private static List<Ping> getPings(ModuleContainerComputation computation) throws IOException {
      List<Ping> pings = new ArrayList<>();
      while (true) {
         Ping ping = computation.nextPing();
         if (ping == null) {
            return pings;
         } else {
            pings.add(ping);
         }
      }
   }
}
