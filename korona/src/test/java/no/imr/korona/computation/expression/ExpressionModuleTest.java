package no.imr.korona.computation.expression;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ExpressionModuleTest {
   private static final float C1 = 1;
   private static final float C2 = 4.5f;
   private static final float C3 = 6.4f;
   private static final float C4 = 106;
   private static final float C5 = 0.1f;
   private static final float C6 = 17;

   private static final float F18 = C1;
   private static final float F38 = C2;
   private static final float F70 = C3;
   private static final float F120 = C4;
   private static final float F200 = C5;
   private static final float F364 = C6;

   private static SyntheticData createSyntheticData() {
      float[] values = {Float.NaN, C1, C2, C3, C4, C5, C6};

      SyntheticData syntheticData = new SyntheticData() {
         @Override
         protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
            float[] sv = new float[10];
            Arrays.fill(sv, values[powerData.getChannel()]);
            powerData.setSv(sv);
         }
      };
      syntheticData.setFirstAndLastPingNumber(0, 0);
      return syntheticData;
   }

   @Test
   void singleExpressions() throws IOException {
      check(F38, "F38");
      check(0.5 * (F38 + F120), "0.5 * (F38 + F120)");
      check(F38 + C2, "F38 + C2");
      check((F18 + F38 + F200) / 3, "(F18 + F38 + F200) / 3");
      check(C1 / C5 + F70 - C4, "C1 / C5 + F70 - C4");
      check(C6 * Math.log10(C2) / Math.pow(C3, F364), "C6 * Math.log10(C2) / Math.pow(C3, F364)");
      check(C6 * Math.log10(C2) / Math.pow(C3, F364), "C6 * log10(C2) / pow(C3, F364)");
   }

   @Test
   void multipleExpressions() throws IOException {
      check(C1, "C7", "C1");
      check(F18 + C2, "C7 + C2", "F17 + C2", "F18 + C2");
   }

   @Test
   void unavailableVariables() {
      assertThrows(ModuleConfigurationException.class, () -> {
         check(C1, "C7", "F17");
      });
   }

   private static void check(double expected, String... expressions) throws IOException {
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());

      ExpressionModule expressionModule = moduleContainer.addModule(new ExpressionModule());
      expressionModule.expressions.setValue(List.of(expressions));

      try (ModuleContainerComputation computation = moduleContainer.createComputation(createSyntheticData().toPingReader())) {
         RawFileConfiguration rawFileConfiguration = computation.getPingConfiguration().getRawFileConfiguration();
         assertEquals(7, rawFileConfiguration.getTransducerCount());

         Ping ping = computation.nextPing();
         assertNotNull(ping);
         PowerData result = ping.getPowerData(7);
         assertNotNull(result);
         assertEquals((float) expected, result.getSv()[0]);
         assertEquals((float) expected, result.getSv()[1]);
         assertEquals((float) expected, result.getSv()[8]);
         //todo assertEquals((float) expected, result.getSv()[9]);
      }
   }
}
