package no.imr.tools.math.nnls;

import no.imr.tools.Utils;
import no.imr.tools.time.Stopwatch;

import java.util.Arrays;

@SuppressWarnings("PMD.SystemPrintln")
final class NnlsTimer {
   private final ColumnOrderedMatrix matrix0 = new ColumnOrderedMatrix(10, 6, new double[]{
         1, 4, -4, -6, 1, 0, 0, 0, 0, 1,
         2, 5, 7, 0, 1, -1, -1, 1, 1, -1,
         3, 6, 1, 2, -1, 4, 4, -4, -4, -4,
         1, 5, -6, -1, 1, -2, -2, -2, 2, 2,
         -6, 0, 0, -3, 0, 1, -1, -1, -1, -1,
         8, -7, 0, -4, 1, 7, 7, 7, -7, 7,
   });
   private final double[] x0 = {0, 0, 0, 0, 0, 0};
   private final double[] b0 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
   /* C++ version: 0   0.660591728843043846097771166   0   0.351755208545684316945312275  0  0.494067762004208044235298303  */

   private final ColumnOrderedMatrix matrix1 = new ColumnOrderedMatrix(16, 6, new double[]{
         0.4592, 0.5221, 0.5962, 0.0941, 0.4428, 0.0389, 0.6074, 0.0469, 0.8386, 0.0645, 1, 0, 0, 0, 0, 0,
         0.8817, 0.6663, 0.6834, 0.8612, 0.5547, 0.5796, 0.0737, 0.9753, 0.7335, 0.5500, 0, 1, 0, 0, 0, 0,
         0.3113, 0.4289, 0.4998, 0.5743, 0.0441, 0.2603, 0.5132, 0.2678, 0.4851, 0.8041, 0, 0, 1, 0, 0, 0,
         0.7460, 0.7454, 0.3400, 0.5031, 0.3989, 0.9696, 0.7035, 0.7288, 0.5697, 0.7290, 0, 0, 0, 1, 0, 0,
         0.7807, 0.5910, 0.2063, 0.2683, 0.6968, 0.5934, 0.6796, 0.2996, 0.4429, 0.3393, 0, 0, 0, 0, 1, 0,
         0.1489, 0.9787, 0.4195, 0.3864, 0.6665, 0.0688, 0.5966, 0.7012, 0.6455, 0.1108, 0, 0, 0, 0, 0, 1,
   });
   private final double[] x1 = {0, 0, 0, 0, 0, 0};
   private final double[] b1 = {0.8912, 0.0183, 0.2851, 0.8702, 0.6046, 0.2688, 0.4731, 0.3316, 0.8206, 0.7202, 0, 0, 0, 0, 0, 0};
   //Matlab: 0.1211    0.2840    0.2424    0.0981    0.1950         0

   private NnlsTimer() {
   }

   public static void main(String[] args) {
      new NnlsTimer().run();
   }

   private void run() {
      for (int i = 0; i < 5; i++) {
         run(matrix0, x0, b0, 100000);
      }

      System.out.println("\n---------------\n");

      for (int i = 0; i < 5; i++) {
         run(matrix1, x1, b1, 100000);
      }
   }

   private static void run(ColumnOrderedMatrix matrix, double[] x, double[] b, int n) {
      NNLS nnls = new NNLS(matrix);
      Stopwatch stopwatch = Stopwatch.createStarted();
      for (int i = 0; i < n; i++) {
         Arrays.fill(x, 0);
         nnls.solve(b, x);
      }
      stopwatch.stop();

      System.out.println(Utils.format("%.3f", stopwatch.seconds()) + " seconds,   x = " + Arrays.toString(x));
   }
}
