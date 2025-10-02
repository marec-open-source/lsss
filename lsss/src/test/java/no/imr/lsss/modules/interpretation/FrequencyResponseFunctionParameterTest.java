package no.imr.lsss.modules.interpretation;

import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class FrequencyResponseFunctionParameterTest {
   @Test
   void test() {
      FrequencyResponseFunctionParameter p = new FrequencyResponseFunctionParameter(new Name("test"));
      p.setStringValue("1");
      assertEquals("1", p.getStringValue());
      assertEquals(1, p.getValue().second().eval(200_000));

      p.setStringValue("F");
      assertEquals("F", p.getStringValue());
      assertEquals(200_000, p.getValue().second().eval(200_000));

      p.setStringValue("pow(38000 / F, 0.4)");
      assertEquals("pow(38000 / F, 0.4)", p.getStringValue());
      assertEquals(Math.pow(38000 / 200_000.0, 0.4), p.getValue().second().eval(200_000));

      List<String> invalidValues = List.of(
            "",
            "+",
            "x"
      );
      for (String s : invalidValues) {
         assertThrows(ParameterException.class, () -> p.setStringValue(s), s);
      }
   }
}
