package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

final class ValueParameterTest {
   @Test
   void test() {
      AtomicInteger changeCounter = new AtomicInteger();
      ValueParameter<Optional<Float>> p = new ValueParameter<>(new Name("Test"),
            Optional.empty(), Unit.DIMENSIONLESS, ValueConverters.OPTIONAL_FLOAT);
      p.subscribe(__ -> changeCounter.incrementAndGet());
      p.setStringValue("2");
      assertEquals(Optional.of(2f), p.getValue());
      assertEquals(1, changeCounter.get());

      p.setValue(Optional.of(3f));
      assertEquals(Optional.of(3f), p.getValue());
      assertEquals(2, changeCounter.get());

      p.setStringValue("3");
      assertEquals(Optional.of(3f), p.getValue());
      assertEquals(2, changeCounter.get());

      p.setStringValue("");
      assertEquals(Optional.empty(), p.getValue());
      assertEquals(3, changeCounter.get());

      p.setValue(Optional.empty());
      assertEquals(Optional.empty(), p.getValue());
      assertEquals(3, changeCounter.get());
   }

   @Test
   void changeConstraint() {
      ValueParameter<Integer> p = new ValueParameter<>(new Name("Test"),
            0, Unit.DIMENSIONLESS, ValueConstraints.none(), ValueConverters.INTEGER);
      assertEquals(0, (int) p.getValue());
      p.setConstraintAndPossiblyValue(ValueConstraints.gte(0), 1);
      assertEquals(0, (int) p.getValue());
      p.setConstraintAndPossiblyValue(ValueConstraints.gte(1), 1);
      assertEquals(1, (int) p.getValue());
      p.setConstraintAndValue(ValueConstraints.gte(2), 2);
      assertEquals(2, (int) p.getValue());
   }

   @Test
   void initialValueInvalid() {
      assertThrows(ParameterException.class, () -> {
         new ValueParameter<>(new Name("Test"),
               0, Unit.DIMENSIONLESS, ValueConstraints.gt(0), ValueConverters.INTEGER);
      });
   }

   @Test
   void setValueInvalid() {
      ValueParameter<Integer> p = new ValueParameter<>(new Name("Test"),
            1, Unit.DIMENSIONLESS, ValueConstraints.gt(0), ValueConverters.INTEGER);
      assertThrows(ParameterException.class, () -> {
         p.setValue(0);
      });
   }

   @Test
   void setStringValueNotParsable() {
      ValueParameter<Integer> p = new ValueParameter<>(new Name("Test"),
            0, Unit.DIMENSIONLESS, ValueConverters.INTEGER);
      assertThrows(ParameterException.class, () -> {
         p.setStringValue("a");
      });
   }

   @Test
   void setStringValueInvalid() {
      ValueParameter<Integer> p = new ValueParameter<>(new Name("Test"),
            1, Unit.DIMENSIONLESS, ValueConstraints.gt(0), ValueConverters.INTEGER);
      assertThrows(ParameterException.class, () -> {
         p.setStringValue("0");
      });
   }
}
