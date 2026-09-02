package no.imr.korona.color;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DiscreteColorMappingTest {
   @Test
   void getRGB() {
      List<DiscreteColor> discreteColors = List.of(
            new DiscreteColor(3, Color.RED, "Mackerel"),
            new DiscreteColor(5, Color.BLUE, "Small Fish"),
            new DiscreteColor(4, Color.GREEN, "Mackerel"),
            new DiscreteColor(7, Color.PINK, "rr")
      );
      DiscreteColorMapping dcm = new DiscreteColorMapping(discreteColors);

      assertEquals(Color.RED.getRGB(), dcm.getRGB(3));
      assertEquals(Color.PINK.getRGB(), dcm.getRGB(7));

      assertEquals(DiscreteColorMapping.INVALID_VALUE.getRGB(), dcm.getRGB(-1));
      assertEquals(DiscreteColorMapping.INVALID_VALUE.getRGB(), dcm.getRGB(6));
      assertEquals(DiscreteColorMapping.INVALID_VALUE.getRGB(), dcm.getRGB(8));
   }
}
