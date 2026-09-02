package no.imr.korona.computation.expression;

import no.imr.tools.Utils;
import no.imr.tools.compile.CompileException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class ExpressionApplierTest {
   @Test
   void testDiagnostics() {
      int[] kHz = Utils.EMPTY_INT_ARRAY;

      String[] validExpressions = {
            "0",
            "C1",
            "F200 + C1+ C4 - F200 * F18",
            "(((F34) * (-(5)) + 7) )",
            "sqrt(F18)",
            "C" + Integer.MAX_VALUE,
            "F" + Integer.MAX_VALUE,
      };
      for (String s : validExpressions) {
         try {
            new ExpressionApplier(s, kHz, 1);
         } catch (CompileException e) {
            fail(s, e);
         }
      }

      String[] invalidExpressions = {
            "",
            "+",
            "{",
            "i",
            "F",
            "C",
            "C0",
            "x + y",
            "0.5*(F38 + F120",
            "C" + (Integer.MAX_VALUE + 1L),
            "F" + (Integer.MAX_VALUE + 1L),
      };
      for (String s : invalidExpressions) {
         assertThrows(CompileException.class, () -> new ExpressionApplier(s, kHz, 1), s);
      }
   }

   @Test
   void duplicatedFrequency() throws CompileException {
      ExpressionApplier expressionApplier = new ExpressionApplier("C1 + C2", new int[]{38, 38}, 1);
      assertEquals(Set.of(), expressionApplier.getUnavailableVariables());
   }
}
