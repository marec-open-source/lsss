package no.imr.tools.parameter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CsvListParameterTest {
   @Test
   void test() {
      CsvListParameter<Integer> p = new IntCsvListParameter(new Name("p"),
            List.of(), Unit.DIMENSIONLESS);
      assertEquals(List.of(), p.getValue());
      assertEquals("", p.getStringValue());

      p = new IntCsvListParameter(new Name("p"),
            List.of(0, 2), Unit.DIMENSIONLESS);
      assertEquals(List.of(0, 2), p.getValue());
      assertEquals("0,2", p.getStringValue());

      p.setStringValue("1");
      assertEquals(List.of(1), p.getValue());
      assertEquals("1", p.getStringValue());

      p.setStringValue(" -1,5  , 7  ");
      assertEquals(List.of(-1, 5, 7), p.getValue());
      assertEquals("-1,5,7", p.getStringValue());

      p.setStringValue(" \n  \t ");
      assertEquals(List.of(), p.getValue());
      assertEquals("", p.getStringValue());
   }
}
