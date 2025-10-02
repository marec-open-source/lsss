package no.imr.tools.math.nnls;

/**
 * A Fortran style column ordered matrix.
 */
public final class ColumnOrderedMatrix {
   private final int m;
   private final int n;
   private final double[] values;

   public ColumnOrderedMatrix(int m, int n) {
      this.m = m;
      this.n = n;

      values = new double[m * n];
   }

   public ColumnOrderedMatrix(int m, int n, double[] values) {
      if (values.length != m * n) {
         throw new IllegalArgumentException();
      }
      this.m = m;
      this.n = n;
      this.values = values;
   }

   public int getM() {
      return m;
   }

   public int getN() {
      return n;
   }

   public int getSize() {
      return values.length;
   }

   public void set(int i, int j, double x) {
      values[i + j * m] = x;
   }

   public double get(int i, int j) {
      return values[i + j * m];
   }

   public double[] getValues() {
      return values;
   }

   public void multiply(double[] x, double[] result) {
      if (x.length != n) {
         throw new IllegalArgumentException();
      }
      if (result.length != m) {
         throw new IllegalArgumentException();
      }

      for (int i = 0; i < m; i++) {
         double y = 0;
         for (int j = 0; j < n; j++) {
            y += get(i, j) * x[j];
         }
         result[i] = y;
      }
   }
}
