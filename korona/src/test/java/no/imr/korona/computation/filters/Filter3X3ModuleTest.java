package no.imr.korona.computation.filters;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.math.ArrayMath;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class Filter3X3ModuleTest {
   @Test
   void userDefined() throws IOException {
      float logSv = -50;
      int count = 10;
      ConstantSyntheticData syntheticData = new ConstantSyntheticData(count, PowerData.logSvToSv(logSv));

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      Filter3X3Module filter3X3Module = moduleContainer.addModule(new Filter3X3Module());
      filter3X3Module.filterType.setValue("User defined");

      double sumLogSv = 0;
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.withFirstAndLastPingNumber(1, 3).toPingReader())) {
         assertNotNull(computation.nextPing());
         Ping ping = computation.nextPing();
         assertNotNull(ping);
         PowerData powerData = ping.getPowerData(1);
         assertNotNull(powerData);
         sumLogSv += ArrayMath.sum(powerData.getLogSv(), 1, count - 1);
      }
      assertEquals(logSv, sumLogSv / (count - 2));
   }

   @Test
   void sobelSkewEdge() throws IOException {
      float logSv = -50;
      int count = 10;
      ConstantSyntheticData syntheticData = new ConstantSyntheticData(count, PowerData.logSvToSv(logSv));

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      Filter3X3Module filter3X3Module = moduleContainer.addModule(new Filter3X3Module());
      filter3X3Module.filterType.setValue("/*Sobel skew edge  / */{{-2,-1,0},{-1,0,1},{0,1,2}}}");

      double sumLogSv = 0;
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticData.withFirstAndLastPingNumber(1, 3).toPingReader())) {
         assertNotNull(computation.nextPing());
         Ping ping = computation.nextPing();
         assertNotNull(ping);
         PowerData powerData = ping.getPowerData(1);
         assertNotNull(powerData);
         sumLogSv += ArrayMath.sum(powerData.getLogSv(), 1, count - 1);
      }
      assertEquals(0, sumLogSv / (count - 2));
   }
}
