package no.imr.korona.util;

import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.parameter.Name;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ChannelPredicateParameterTest {
   @Test
   void test() {
      ChannelPredicateParameter parameter = new ChannelPredicateParameter(new Name("Test"), true);
      parameter.setStringValue("f < 50 || c >= n");

      CompiledChannelPredicate predicate = parameter.getValue().second();
      assertTrue(predicate.test(38, 1, 10));
      assertFalse(predicate.test(70, 1, 10));
      assertTrue(predicate.test(70, 10, 10));

      assertEquals(List.of(1, 2, 6), parameter.selectedChannels(new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1).getRawFileConfiguration()));
   }
}
