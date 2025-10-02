package no.imr.tools.math.nnls;

import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

final class NnlsTest {
   @Test
   void test1() throws IOException {
      String s = """
            10 6

             1     2     3     1    -6     8
             4     5     6     5     0    -7
            -4     7     1    -6     0     0
            -6     0     2    -1    -3    -4
             1     1    -1     1     0     1
             0    -1     4    -2     1     7
             0    -1     4    -2    -1     7
             0     1    -4    -2    -1     7
             0     1    -4     2    -1    -7
             1    -1    -4     2    -1     7

            0.0000000000000000
            0.6605917288430440
            0.0000000000000000
            0.3517552085456845
            0.0000000000000000
            0.4940677620581394

            1
            2
            3
            4
            5
            6
            7
            8
            9
            10
            """;

      Problem problem = createProblem(s);
      assertEquals(4, problem.matrix.getValues()[1]);
      assertEquals(1, problem.b[0]);
      assertEquals(0, problem.x0[0]);
      assertEquals(0.6605917288430440, problem.x0[1]);
      assertEquals(0.0000000000000000, problem.x[0], 1e-15);
      assertEquals(0.6605917288430440, problem.x[1], 1e-15);
      assertEquals(0.0000000000000000, problem.x[2], 1e-15);
      assertEquals(0.3517552085456845, problem.x[3], 1e-15);
      assertEquals(0.0000000000000000, problem.x[4], 1e-15);
      assertEquals(0.4940677620581394, problem.x[5], 1e-15);
      assertEquals(0, problem.dx, 1e-15);
   }

   @Test
   void test2() throws IOException {
      String s = """
            7.0000000000000000e+000  4.0000000000000000e+000

            2.9080576405049841e-002  9.6533326602136066e-002  5.7966795486617850e-001  8.9866950521381250e-001
            1.5648564741432333e-001  7.8182177236024775e-001  1.0024126767694434e-001  3.7265687471939590e-001
            3.0059098116982941e-001  2.2141790586763885e-001  2.9985467172474067e-001  9.0637878199420996e-001
            1.0000000000000000e+000  0.0000000000000000e+000  0.0000000000000000e+000  0.0000000000000000e+000
            0.0000000000000000e+000  1.0000000000000000e+000  0.0000000000000000e+000  0.0000000000000000e+000
            0.0000000000000000e+000  0.0000000000000000e+000  1.0000000000000000e+000  0.0000000000000000e+000
            0.0000000000000000e+000  0.0000000000000000e+000  0.0000000000000000e+000  1.0000000000000000e+000

            7.2948889161669292e-002
            1.1114989471117821e-001
            9.0995875072774790e-002
            2.4542702233557845e-001

            3.2751947983299423e-001
            2.8037268885780658e-001
            4.9254098319046358e-001
            0.0000000000000000e+000
            0.0000000000000000e+000
            0.0000000000000000e+000
            0.0000000000000000e+000
            """;

      Problem problem = createProblem(s);
      assertEquals(1.5648564741432333e-001, problem.matrix.getValues()[1]);
      assertEquals(3.2751947983299423e-001, problem.b[0]);
      assertEquals(7.2948889161669292e-002, problem.x0[0]);
      assertEquals(7.2948889161669292e-002, problem.x[0], 1e-15);
      assertEquals(1.1114989471117821e-001, problem.x[1], 1e-15);
      assertEquals(9.0995875072774790e-002, problem.x[2], 1e-15);
      assertEquals(2.4542702233557845e-001, problem.x[3], 1e-15);
      assertEquals(0, problem.dx, 1e-15);
   }

   @Test
   void test3() throws IOException {
      String s = """
            7.0000000000000000e+000  4.0000000000000000e+000

            2.9080576405049841e-002  9.6533326602136066e-002  5.7966795486617850e-001  8.9866950521381250e-001
            1.5648564741432333e-001  7.8182177236024775e-001  1.0024126767694434e-001  3.7265687471939590e-001
            3.0059098116982941e-001  2.2141790586763885e-001  2.9985467172474067e-001  9.0637878199420996e-001
            1.0000000000000000e+000  0.0000000000000000e+000  0.0000000000000000e+000  0.0000000000000000e+000
            0.0000000000000000e+000  1.0000000000000000e+000  0.0000000000000000e+000  0.0000000000000000e+000
            0.0000000000000000e+000  0.0000000000000000e+000  1.0000000000000000e+000  0.0000000000000000e+000
            0.0000000000000000e+000  0.0000000000000000e+000  0.0000000000000000e+000  1.0000000000000000e+000

            0
            0
            0
            0

            0
            0
            0
            0
            0
            0
            0
            """;

      Problem problem = createProblem(s);
      assertArrayEquals(new double[]{0, 0, 0, 0}, problem.x);
   }

   private static Problem createProblem(String s) throws IOException {
      try (BufferedReader reader = new BufferedReader(new StringReader(s))) {
         Problem problem = Problem.read(reader);
         assertNotNull(problem);
         assertNull(reader.readLine());
         return problem;
      }
   }

   /**
    * Reads and solves a {@code Ax=b} problem.
    */
   static final class Problem {
      // input
      private final ColumnOrderedMatrix matrix;
      private final double[] x0;
      private final double[] b;

      // output
      final double[] x;
      final double dx;

      private Problem(ColumnOrderedMatrix matrix, double[] x0, double[] b) {
         this.matrix = matrix;
         this.x0 = x0;
         this.b = b;

         x = new double[x0.length];

         NNLS nnls = new NNLS(matrix);
         nnls.solve(b, x);

         double dx2 = 0;
         for (int i = 0; i < x0.length; i++) {
            dx2 += Utils.sq(x[i] - x0[i]);
         }
         dx = Math.sqrt(dx2);
      }

      static @Nullable Problem read(BufferedReader reader) throws IOException {
         String line = reader.readLine();
         if (line == null) {
            return null;
         }
         String[] words = line.trim().split("\\s+");
         int m = (int) Double.parseDouble(words[0]);
         int n = (int) Double.parseDouble(words[1]);

         ColumnOrderedMatrix matrix = readMatrix(reader, m, n);
         double[] x0 = readVector(reader, n);
         double[] b = readVector(reader, m);
         return new Problem(matrix, x0, b);
      }

      private static ColumnOrderedMatrix readMatrix(BufferedReader reader, int m, int n) throws IOException {
         ColumnOrderedMatrix matrix = new ColumnOrderedMatrix(m, n);
         for (int i = 0; i < m; i++) {
            String line = nextLine(reader);
            String[] words = line.split("\\s+");
            for (int j = 0; j < n; j++) {
               matrix.set(i, j, Double.parseDouble(words[j]));
            }
         }
         return matrix;
      }

      private static double[] readVector(BufferedReader reader, int n) throws IOException {
         double[] x = new double[n];
         for (int i = 0; i < n; i++) {
            String line = nextLine(reader);
            x[i] = Double.parseDouble(line);
         }
         return x;
      }

      private static String nextLine(BufferedReader reader) throws IOException {
         while (true) {
            String line = reader.readLine();
            if (line == null) {
               throw new EOFException("Unexpected end of input");
            }
            line = line.trim();
            if (!line.isEmpty()) {
               return line;
            }
         }
      }
   }
}
