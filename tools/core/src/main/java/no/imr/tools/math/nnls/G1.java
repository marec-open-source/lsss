/*
 *  Produced by f2java.  f2java is part of the Fortran-
 *  -to-Java project at the University of Tennessee Netlib
 *  numerical software repository.
 *
 *  Original authorship for the BLAS and LAPACK numerical
 *  routines may be found in the Fortran source, available at
 *  http://www.netlib.org.
 *
 *  Fortran input file: G1.f
 *
 */

package no.imr.tools.math.nnls;

/**
 * <pre>
 *
 *    COMPUTE ORTHOGONAL ROTATION MATRIX.
 *
 * The original version of this code was developed by
 * Charles L. Lawson and Richard J. Hanson at Jet Propulsion Laboratory
 *
 * 1973 JUN 12, and published in the book
 * "SOLVING LEAST SQUARES PROBLEMS", Prentice-HalL, 1974.
 * Revised FEB 1995 to accompany reprinting of the book by SIAM.
 *
 *    COMPUTE.. MATRIX   (C, S) SO THAT (C, S)(A) = (SQRT(A**2+B**2))
 *                       (-S,C)         (-S,C)(B)   (   0          )
 *    COMPUTE SIG = SQRT(A**2+B**2)
 *       SIG IS COMPUTED LAST TO ALLOW FOR THE POSSIBILITY THAT
 *       SIG MAY BE IN THE SAME LOCATION AS A OR B .
 *    ------------------------------------------------------------------
 *    ------------------------------------------------------------------
 * </pre>
 */
final class G1 {
   double cterm;
   double sterm;
   double sig;

   G1() {
   }

   void g1(double a, double b) {
      double as = Math.abs(a);
      double bs = Math.abs(b);

      if (as > bs) {
         double xr = b / a;
         double yr = Math.sqrt(1.0 + xr * xr);
         cterm = dsign(1.0 / yr, a);
         sterm = cterm * xr;
         sig = as * yr;
         return;
      }
//
      if (b != 0.0) {
         double xr = a / b;
         double yr = Math.sqrt(1.0 + xr * xr);
         sterm = dsign(1.0 / yr, b);
         cterm = sterm * xr;
         sig = bs * yr;
         return;
      }
//
      sig = 0.0;
      cterm = 0.0;
      sterm = 1.0;
   }

   private static double dsign(double a, double b) {
      if (b >= 0) {
         return Math.abs(a);
      } else {
         return -Math.abs(a);
      }
   }
}
