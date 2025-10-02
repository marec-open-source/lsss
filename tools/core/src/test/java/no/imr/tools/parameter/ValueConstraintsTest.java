package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ValueConstraintsTest {
   @Test
   void maxLength() {
      ValueConstraint<String> c = ValueConstraints.maxLength(2);
      assertNull(c.validate("a"));
      assertNull(c.validate("ab"));
      assertNotNull(c.validate("abc"));

      assertThrows(IllegalArgumentException.class, () -> {
         ValueConstraints.maxLength(-1);
      });
   }

   @Test
   void gt() {
      ValueConstraint<Integer> c = ValueConstraints.gt(2);
      assertEquals("> 2", c.getAllowedValuesDescription(ValueConverters.INTEGER));
      assertNotNull(c.validate(1));
      assertNotNull(c.validate(2));
      assertNull(c.validate(3));
   }

   @Test
   void gte() {
      ValueConstraint<Integer> c = ValueConstraints.gte(2);
      assertEquals("≥ 2", c.getAllowedValuesDescription(ValueConverters.INTEGER));
      assertNotNull(c.validate(1));
      assertNull(c.validate(2));
      assertNull(c.validate(3));
   }

   @Test
   void lt() {
      ValueConstraint<Integer> c = ValueConstraints.lt(2);
      assertEquals("< 2", c.getAllowedValuesDescription(ValueConverters.INTEGER));
      assertNull(c.validate(1));
      assertNotNull(c.validate(2));
      assertNotNull(c.validate(3));
   }

   @Test
   void lte() {
      ValueConstraint<Integer> c = ValueConstraints.lte(2);
      assertEquals("≤ 2", c.getAllowedValuesDescription(ValueConverters.INTEGER));
      assertNull(c.validate(1));
      assertNull(c.validate(2));
      assertNotNull(c.validate(3));
   }

   @Test
   void gtLt() {
      ValueConstraint<Integer> c = ValueConstraints.gtLt(2, 4);
      assertEquals("(2, 4)", c.getAllowedValuesDescription(ValueConverters.INTEGER));
      assertNotNull(c.validate(1));
      assertNotNull(c.validate(2));
      assertNull(c.validate(3));
      assertNotNull(c.validate(4));
      assertNotNull(c.validate(5));
   }

   @Test
   void gtLte() {
      ValueConstraint<Integer> c = ValueConstraints.gtLte(2, 4);
      assertEquals("(2, 4]", c.getAllowedValuesDescription(ValueConverters.INTEGER));
      assertNotNull(c.validate(1));
      assertNotNull(c.validate(2));
      assertNull(c.validate(3));
      assertNull(c.validate(4));
      assertNotNull(c.validate(5));
   }

   @Test
   void gteLt() {
      ValueConstraint<Integer> c = ValueConstraints.gteLt(2, 4);
      assertEquals("[2, 4)", c.getAllowedValuesDescription(ValueConverters.INTEGER));
      assertNotNull(c.validate(1));
      assertNull(c.validate(2));
      assertNull(c.validate(3));
      assertNotNull(c.validate(4));
      assertNotNull(c.validate(5));
   }

   @Test
   void gteLte() {
      ValueConstraint<Integer> c = ValueConstraints.gteLte(2, 4);
      assertEquals("[2, 4]", c.getAllowedValuesDescription(ValueConverters.INTEGER));
      assertNotNull(c.validate(1));
      assertNull(c.validate(2));
      assertNull(c.validate(3));
      assertNull(c.validate(4));
      assertNotNull(c.validate(5));
   }
}
