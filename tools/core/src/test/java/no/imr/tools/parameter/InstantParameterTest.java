package no.imr.tools.parameter;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class InstantParameterTest {
   @Test
   void test() {
      InstantParameter p = new InstantParameter(new Name("Test"));
      assertEquals(Optional.empty(), p.getValue());
      assertEquals("", p.getStringValue());

      String text = "2017-10-12T14:02:00Z";
      Instant instant = Instant.parse(text);
      p.setStringValue(text);
      assertEquals(Optional.of(instant), p.getValue());
      assertEquals(text, p.getStringValue());

      p.setEmpty();
      assertEquals(Optional.empty(), p.getValue());
      assertEquals("", p.getStringValue());

      p.setInstant(instant);
      assertEquals(Optional.of(instant), p.getValue());
      assertEquals(text, p.getStringValue());
   }
}
