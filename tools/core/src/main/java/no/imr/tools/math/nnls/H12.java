/*
 *  Produced by f2java.  f2java is part of the Fortran-
 *  -to-Java project at the University of Tennessee Netlib
 *  numerical software repository.
 *
 *  Original authorship for the BLAS and LAPACK numerical
 *  routines may be found in the Fortran source, available at
 *  http://www.netlib.org.
 *
 *  Fortran input file: H12.f
 *
 */

package no.imr.tools.math.nnls;

/**
 * <pre>
 *    SUBROUTINE H12 (MODE,LPIVOT,L1,M,U,IUE,UP,C,ICE,ICV,NCV)
 *
 * CONSTRUCTION AND/OR APPLICATION OF A SINGLE
 * HOUSEHOLDER TRANSFORMATION..     Q = I + U*(U**T)/B
 *
 * The original version of this code was developed by
 * Charles L. Lawson and Richard J. Hanson at Jet Propulsion Laboratory
 *
 * 1973 JUN 12, and published in the book
 * "SOLVING LEAST SQUARES PROBLEMS", Prentice-HalL, 1974.
 * Revised FEB 1995 to accompany reprinting of the book by SIAM.
 *    ------------------------------------------------------------------
 *                    Subroutine Arguments
 *
 *    MODE   = 1 OR 2   Selects Algorithm H1 to construct and apply a
 *           Householder transformation, or Algorithm H2 to apply a
 *           previously constructed transformation.
 *    LPIVOT IS THE INDEX OF THE PIVOT ELEMENT.
 *    L1,M   IF L1 .LE. M   THE TRANSFORMATION WILL BE CONSTRUCTED TO
 *           ZERO ELEMENTS INDEXED FROM L1 THROUGH M.   IF L1 GT. M
 *           THE SUBROUTINE DOES AN IDENTITY TRANSFORMATION.
 *    U(),IUE,UP    On entry with MODE = 1, U() contains the pivot
 *           vector.  IUE is the storage increment between elements.
 *           On exit when MODE = 1, U() and UP contain quantities
 *           defining the vector U of the Householder transformation.
 *           on entry with MODE = 2, U() and UP should contain
 *           quantities previously computed with MODE = 1.  These will
 *           not be modified during the entry with MODE = 2.
 *    C()    ON ENTRY with MODE = 1 or 2, C() CONTAINS A MATRIX WHICH
 *           WILL BE REGARDED AS A SET OF VECTORS TO WHICH THE
 *           HOUSEHOLDER TRANSFORMATION IS TO BE APPLIED.
 *           ON EXIT C() CONTAINS THE SET OF TRANSFORMED VECTORS.
 *    ICE    STORAGE INCREMENT BETWEEN ELEMENTS OF VECTORS IN C().
 *    ICV    STORAGE INCREMENT BETWEEN VECTORS IN C().
 *    NCV    NUMBER OF VECTORS IN C() TO BE TRANSFORMED. IF NCV .LE. 0
 *           NO OPERATIONS WILL BE DONE ON C().
 *    ------------------------------------------------------------------
 *    ------------------------------------------------------------------
 *    double precision U(IUE,M)
 *    ------------------------------------------------------------------
 * </pre>
 */
final class H12 {
   private double up;

   H12() {
   }

   void h12(int mode,
            int lpivot,
            int l1,
            int m,
            double[] u, int u_offset,
            int iue,
            double[] c, int c_offset,
            int ice,
            int icv,
            int ncv) {
      if (0 > lpivot || lpivot >= l1 || l1 >= m) {
         return;
      }

      int tmp_lpivot_iue = lpivot * iue + u_offset;

      double cl = Math.abs(u[tmp_lpivot_iue]);

      if (mode == 2) {
         if (cl <= 0) {
            return;
         }
      } else {
// C                            ****** CONSTRUCT THE TRANSFORMATION. ******
         for (int j = l1; j < m; j++) {
            cl = Math.max(Math.abs(u[j * iue + u_offset]), cl);
         }

         if (cl <= 0) {
            return;
         }

         double clinv = 1.0 / cl;
         double u_tmp_lpivot_minus1 = u[tmp_lpivot_iue] * clinv;
         double sm = u_tmp_lpivot_minus1 * u_tmp_lpivot_minus1;
         for (int j = l1; j < m; j++) {
            double u_j_iue = u[j * iue + u_offset] * clinv;
            sm += u_j_iue * u_j_iue;
         }
         cl *= Math.sqrt(sm);

         if (u[tmp_lpivot_iue] > 0) {
            cl = -cl;
         }

         up = u[tmp_lpivot_iue] - cl;
         u[tmp_lpivot_iue] = cl;
      }

// C            ****** APPLY THE TRANSFORMATION  I+U*(U**T)/B  TO C. ******
// C

      if (ncv <= 0) {
         return;
      }

      double b = up * u[tmp_lpivot_iue];
// C                       B  MUST BE NONPOSITIVE HERE.  IF B = 0., RETURN.
// C
      if (b >= 0) {
         return;
      }

      b = 1.0 / b;
      int i2 = ice * lpivot - icv + c_offset;
      int incr = ice * (l1 - lpivot);
      for (int j = 0; j < ncv; j++) {
         i2 += icv;
         int i3 = i2 + incr;
         int i4 = i3;
         double sm = c[i2] * up;

         for (int i = l1; i < m; i++) {
            sm += c[i3] * u[i * iue + u_offset];
            i3 += ice;
         }

         if (sm == 0) {
            continue;
         }

         sm *= b;
         c[i2] += sm * up;

         for (int i = l1; i < m; i++) {
            c[i4] += sm * u[i * iue + u_offset];
            i4 += ice;
         }
      }
   }
}
