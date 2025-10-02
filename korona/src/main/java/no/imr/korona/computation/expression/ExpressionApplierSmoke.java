package no.imr.korona.computation.expression;

import no.imr.tools.compile.CompileException;
import no.imr.tools.logging.Log;
import no.imr.tools.smoke.SmokeTestRunnable;

public final class ExpressionApplierSmoke extends SmokeTestRunnable {
   public ExpressionApplierSmoke() {
   }

   @Override
   public void run() throws CompileException {
      String expression = "(C1 + F200) / 2";
      new ExpressionApplier(expression, new int[]{38}, 2);
      Log.global.info(OK + "ExpressionApplier: " + expression);
   }
}
