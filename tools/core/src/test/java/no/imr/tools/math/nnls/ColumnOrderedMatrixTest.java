package no.imr.tools.math.nnls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ColumnOrderedMatrixTest {
   @Test
   void multiply() {
      //  | 1 3 5 |     | 1 |   | 22 |
      //  | 2 4 6 |  *  | 2 | = | 28 |
      //                | 3 |

      ColumnOrderedMatrix matrix = new ColumnOrderedMatrix(2, 3, new double[]{1, 2, 3, 4, 5, 6});
      double[] x = {1, 2, 3};
      double[] result = new double[2];
      matrix.multiply(x, result);
      assertEquals(22, result[0]);
      assertEquals(28, result[1]);
   }
}
