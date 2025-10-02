package no.imr.tools.math.nnls;

/**
 * Solves a non-negative least squares problem.
 */
public final class NNLS {
   private final NnlsLawsonHanson nnls = new NnlsLawsonHanson();
   private final ColumnOrderedMatrix matrix;

   private final double[] tempA;
   private final double[] tempB;
   private final double[] w;
   private final double[] zz;
   private final int[] index;

   public NNLS(ColumnOrderedMatrix matrix) {
      this.matrix = matrix;
      tempA = new double[matrix.getSize()];
      tempB = new double[matrix.getM()];
      w = new double[matrix.getN()];
      zz = new double[matrix.getM()];
      index = new int[matrix.getN()];
   }

   public void solve(double[] b, double[] x) {
      if (b.length != matrix.getM()) {
         throw new IllegalArgumentException("Wrong length of b");
      }
      if (x.length != matrix.getN()) {
         throw new IllegalArgumentException("Wrong length of x");
      }

      System.arraycopy(matrix.getValues(), 0, tempA, 0, tempA.length);
      System.arraycopy(b, 0, tempB, 0, b.length);

      nnls.nnls(tempA, matrix.getM(), matrix.getN(), tempB, x, w, zz, index);
   }
}
