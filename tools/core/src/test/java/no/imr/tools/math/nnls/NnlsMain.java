package no.imr.tools.math.nnls;

import no.imr.tools.Utils;
import no.imr.tools.time.Stopwatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("PMD.SystemPrintln")
final class NnlsMain {
   private NnlsMain() {
   }

   /**
    * Reads problems from a file.
    * <p>
    * Format:
    * <pre>
    * m n
    * a<sub>11</sub> a<sub>12</sub> ... a<sub>1n</sub>
    * ...
    * a<sub>m1</sub> a<sub>m2</sub> ... a<sub>mn</sub>
    * x<sub>1</sub>
    * ...
    * x<sub>n</sub>
    * b<sub>1</sub>
    * ...
    * b<sub>m</sub>
    * </pre>
    *
    * @param args file name. Example: .../lsss-data/nnls/matlab/data.txt
    */
   static void main(String[] args) throws IOException {
      Stopwatch stopwatch = Stopwatch.createStarted();

      double maxDx = Double.NEGATIVE_INFINITY;

      try (BufferedReader reader = Files.newBufferedReader(Path.of(args[0]), Utils.UTF_8)) {
         for (int iter = 0; true; iter++) {
            NnlsTest.Problem problem = NnlsTest.Problem.read(reader);
            if (problem == null) {
               break;
            }
            double dx = problem.dx;
            maxDx = Math.max(dx, maxDx);
            System.out.println(iter + ": dx = " + dx);
         }
      }
      System.out.println("max dx = " + maxDx);
      System.out.println("seconds = " + stopwatch.seconds());
   }
}
