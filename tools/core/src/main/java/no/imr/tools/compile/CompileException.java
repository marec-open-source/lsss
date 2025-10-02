package no.imr.tools.compile;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;

public final class CompileException extends Exception {
   public CompileException(String message) {
      super(message);
   }

   public CompileException(String message, Throwable cause) {
      super(message, cause);
   }

   public CompileException(DiagnosticCollector<?> diagnosticCollector, String output) {
      super(toString(diagnosticCollector, output));
   }

   private static String toString(DiagnosticCollector<?> diagnosticCollector, String output) {
      StringBuilder s = new StringBuilder();
      for (Diagnostic<?> diagnostic : diagnosticCollector.getDiagnostics()) {
         s.append(diagnostic.getMessage(null)).append('\n');
      }
      s.append(output).append('\n');
      return s.toString();
   }
}
