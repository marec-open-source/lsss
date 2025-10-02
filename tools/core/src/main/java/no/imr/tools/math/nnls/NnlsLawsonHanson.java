/*
 *  Produced by f2java.  f2java is part of the Fortran-
 *  -to-Java project at the University of Tennessee Netlib
 *  numerical software repository.
 *
 *  Original authorship for the BLAS and LAPACK numerical
 *  routines may be found in the Fortran source, available at
 *  http://www.netlib.org.
 *
 *  Fortran input file: NNLS.f
 *
 */

package no.imr.tools.math.nnls;

/**
 * <pre>
 *    SUBROUTINE NNLS  (A,MDA,M,N,B,X,RNORM,W,ZZ,INDEX,MODE)
 *
 * Algorithm NNLS: NONNEGATIVE LEAST SQUARES
 *
 * The original version of this code was developed by
 * Charles L. Lawson and Richard J. Hanson at Jet Propulsion Laboratory
 *
 * 1973 JUN 15, and published in the book
 * "SOLVING LEAST SQUARES PROBLEMS", Prentice-HalL, 1974.
 * Revised FEB 1995 to accompany reprinting of the book by SIAM.
 *
 *    GIVEN AN M BY N MATRIX, A, AND AN M-VECTOR, B,  COMPUTE AN
 *    N-VECTOR, X, THAT SOLVES THE LEAST SQUARES PROBLEM
 *
 *                     A * X = B  SUBJECT TO X .GE. 0
 *    ------------------------------------------------------------------
 *                    Subroutine Arguments
 *
 *    A(),MDA,M,N     MDA IS THE FIRST DIMENSIONING PARAMETER FOR THE
 *                    ARRAY, A().   ON ENTRY A() CONTAINS THE M BY N
 *                    MATRIX, A.           ON EXIT A() CONTAINS
 *                    THE PRODUCT MATRIX, Q*A , WHERE Q IS AN
 *                    M BY M ORTHOGONAL MATRIX GENERATED IMPLICITLY BY
 *                    THIS SUBROUTINE.
 *    B()     ON ENTRY B() CONTAINS THE M-VECTOR, B.   ON EXIT B() CON-
 *            TAINS Q*B.
 *    X()     ON ENTRY X() NEED NOT BE INITIALIZED.  ON EXIT X() WILL
 *            CONTAIN THE SOLUTION VECTOR.
 *    RNORM   ON EXIT RNORM CONTAINS THE EUCLIDEAN NORM OF THE
 *            RESIDUAL VECTOR.
 *    W()     AN N-ARRAY OF WORKING SPACE.  ON EXIT W() WILL CONTAIN
 *            THE DUAL SOLUTION VECTOR.   W WILL SATISFY W(I) = 0.
 *            FOR ALL I IN SET P  AND W(I) .LE. 0. FOR ALL I IN SET Z
 *    ZZ()     AN M-ARRAY OF WORKING SPACE.
 *    INDEX()     AN INTEGER WORKING ARRAY OF LENGTH AT LEAST N.
 *                ON EXIT THE CONTENTS OF THIS ARRAY DEFINE THE SETS
 *                P AND Z AS FOLLOWS..
 *
 *                INDEX(1)   THRU INDEX(NSETP) = SET P.
 *                INDEX(IZ1) THRU INDEX(IZ2)   = SET Z.
 *                IZ1 = NSETP + 1 = NPP1
 *                IZ2 = N
 *    MODE    THIS IS A SUCCESS-FAILURE FLAG WITH THE FOLLOWING
 *            MEANINGS.
 *            1     THE SOLUTION HAS BEEN COMPUTED SUCCESSFULLY.
 *            2     THE DIMENSIONS OF THE PROBLEM ARE BAD.
 *                  EITHER M .LE. 0 OR N .LE. 0.
 *            3    ITERATION COUNT EXCEEDED.  MORE THAN 3*N ITERATIONS.
 *
 *    ------------------------------------------------------------------
 *    ------------------------------------------------------------------
 *    integer INDEX(N)
 *    double precision A(MDA,N), B(M), W(N), X(N), ZZ(M)
 *    ------------------------------------------------------------------
 * </pre>
 */
final class NnlsLawsonHanson {
   int mode;
   double rnorm;

   NnlsLawsonHanson() {
   }

   void nnls(double[] a,
             int m,
             int n,
             double[] b,
             double[] x,
             double[] w,
             double[] zz,
             int[] index) {
      if (m <= 0 || n <= 0) {
         mode = 2;
         rnorm = Double.NaN;
      }

      int j;
      int jj = -1;
      double asave;
      G1 g1 = new G1();
      H12 h12 = new H12();
      double[] dummy_c = new double[1];

      int iter = 0;
      int itmax = 3 * n;

      int iz2 = n - 1;
      int iz1 = 0;
      int nsetp = 0;
      int npp1 = 0;
// C
// C                    INITIALIZE THE ARRAYS INDEX() AND X().
// C
      for (int i = 0; i < n; i++) {
         x[i] = 0.0;
         index[i] = i;
      }
// C                             ******  MAIN LOOP BEGINS HERE  ******
      labelMainLoop:
      while (true) {
// C                  QUIT IF ALL COEFFICIENTS ARE ALREADY IN THE SOLUTION.
// C                        OR IF M COLS OF A HAVE BEEN TRIANGULARIZED.
// C
         if (iz1 > iz2 || nsetp >= m) {
            mode = 1;
            rnorm = computeRnorm(npp1, m, b, n, w);
            return;
         }
// C
// C         COMPUTE COMPONENTS OF THE DUAL (NEGATIVE GRADIENT) VECTOR W().
// C
         for (int iz = iz1; iz <= iz2; iz++) {
            j = index[iz];
            int jm = j * m;
            double sm = 0.0;
            for (int l = npp1; l < m; l++) {
               sm += a[l + jm] * b[l];
            }
            w[j] = sm;
         }
// C                                   FIND LARGEST POSITIVE W(J).
         int iz;
         while (true) {
            double wmax = 0.0;
            int izmax = -1;
            for (int iza = iz1; iza <= iz2; iza++) {
               j = index[iza];
               if (w[j] > wmax) {
                  wmax = w[j];
                  izmax = iza;
               }
            }
// C
// C             IF WMAX .LE. 0. GO TO TERMINATION.
// C             THIS INDICATES SATISFACTION OF THE KUHN-TUCKER CONDITIONS.
// C
            if (izmax == -1) {
               mode = 1;
               rnorm = computeRnorm(npp1, m, b, n, w);
               return;
            }
            iz = izmax;
            j = index[iz];
// C
// C     THE SIGN OF W(J) IS OK FOR J TO BE MOVED TO SET P.
// C     BEGIN THE TRANSFORMATION AND CHECK NEW DIAGONAL ELEMENT TO AVOID
// C     NEAR LINEAR DEPENDENCE.
// C
            asave = a[npp1 + j * m];
            h12.h12(1, npp1, npp1 + 1, m, a, j * m, 1, dummy_c, 0, 1, 1, 0);
            double unorm = 0.0;
            if (nsetp != 0) {
               for (int l = 0; l < nsetp; l++) {
                  double a_lj = a[l + j * m];
                  unorm += a_lj * a_lj;
               }
            }
            unorm = Math.sqrt(unorm);
// C inge introducing temp to eliminate function diff
            double tempA = unorm + Math.abs(a[npp1 + j * m]) * 0.01;
            if (tempA - unorm > 0.0) {
               // C
// C        COL J IS SUFFICIENTLY INDEPENDENT.  COPY B INTO ZZ, UPDATE ZZ
// C        AND SOLVE FOR ZTEST ( = PROPOSED NEW VALUE FOR X(J) ).
// C
               System.arraycopy(b, 0, zz, 0, m);

               h12.h12(2, npp1, npp1 + 1, m, a, j * m, 1, zz, 0, 1, 1, 1);
               double ztest = zz[npp1] / a[npp1 + j * m];
// C
// C                                     SEE IF ZTEST IS POSITIVE
// C
               if (ztest > 0.0) {
                  break;
               }
            }
// C
// C     REJECT J AS A CANDIDATE TO BE MOVED FROM SET Z TO SET P.
// C     RESTORE A(NPP1,J), SET W(J)=0., AND LOOP BACK TO TEST DUAL
// C     COEFFS AGAIN.
// C
            a[npp1 + j * m] = asave;
            w[j] = 0.0;
         }
// C
// C     THE INDEX  J=INDEX(IZ)  HAS BEEN SELECTED TO BE MOVED FROM
// C     SET Z TO SET P.    UPDATE B,  UPDATE INDICES,  APPLY HOUSEHOLDER
// C     TRANSFORMATIONS TO COLS IN NEW SET Z,  ZERO SUBDIAGONAL ELTS IN
// C     COL J,  SET W(J)=0.
// C
         System.arraycopy(zz, 0, b, 0, m);
// C
         index[iz] = index[iz1];
         index[iz1] = j;
         iz1++;
         nsetp = npp1 + 1;
         npp1++;
// C
         if (iz1 <= iz2) {
            for (int jz = iz1; jz <= iz2; jz++) {
               jj = index[jz];
               h12.h12(2, nsetp - 1, npp1, m, a, j * m, 1, a, jj * m, 1, m, 1);
            }
         }
// C
         if (nsetp != m) {
            int jm = j * m;
            for (int l = npp1; l < m; l++) {
               a[l + jm] = 0.0;
            }
         }
// C
         w[j] = 0.0;
// C                                SOLVE THE TRIANGULAR SYSTEM.
// C                                STORE THE SOLUTION TEMPORARILY IN ZZ().

         solveTriag(nsetp, a, jj, m, zz, index);

         while (true) {
// C
// C                       ******  SECONDARY LOOP BEGINS HERE ******
// C
// C                          ITERATION COUNTER.
// C
            iter++;
            if (iter > itmax) {
// C          write (*,'(/a)') ' NNLS quitting on iteration count.'
               mode = 3;
               rnorm = computeRnorm(npp1, m, b, n, w);
               return;
            }
// C
// C                    SEE IF ALL NEW CONSTRAINED COEFFS ARE FEASIBLE.
// C                                  IF NOT COMPUTE ALPHA.
// C
            double alpha = 2.0;
            for (int ip = 0; ip < nsetp; ip++) {
               int l = index[ip];
               if (zz[ip] <= 0.0) {
                  double x_l = x[l];
                  double t = -(x_l / (zz[ip] - x_l));
                  if (alpha > t) {
                     alpha = t;
                     jj = ip;
                  }
               }
            }
// C
// C          IF ALL NEW CONSTRAINED COEFFS ARE FEASIBLE THEN ALPHA WILL
// C          STILL = 2.    IF SO EXIT FROM SECONDARY LOOP TO MAIN LOOP.
// C
            if (alpha == 2.0) {
               for (int ip = 0; ip < nsetp; ip++) {
                  int i = index[ip];
                  x[i] = zz[ip];
               }
               continue labelMainLoop;
            }
// C
// C          OTHERWISE USE ALPHA WHICH WILL BE BETWEEN 0. AND 1. TO
// C          INTERPOLATE BETWEEN THE OLD X AND THE NEW ZZ.
// C
            for (int ip = 0; ip < nsetp; ip++) {
               int l = index[ip];
               x[l] += alpha * (zz[ip] - x[l]);
            }
// C
// C        MODIFY A AND B AND THE INDEX ARRAYS TO MOVE COEFFICIENT I
// C        FROM SET P TO SET Z.
// C
            label260:
            for (int i = index[jj]; true; ) {
               x[i] = 0.0;
// C
               if (jj + 1 != nsetp) {
                  jj++;
                  for (j = jj; j < nsetp; j++) {
                     int ii = index[j];
                     index[j - 1] = ii;
                     {
                        int a_index = (j - 1) + ii * m;
                        g1.g1(a[a_index], a[a_index + 1]);
                        a[a_index] = g1.sig;
                     }
                     a[j + ii * m] = 0.0;

                     for (int l = 0; l < n; l++) {
                        if (l != ii) {
                           // c
// c                 Apply procedure G2 (CC,SS,A(J-1,L),A(J,L))
// c
                           int a_index = j + l * m;
                           double a_i = a[a_index];
                           double a_im1 = a[a_index - 1];
                           a[a_index - 1] = g1.cterm * a_im1 + g1.sterm * a_i;
                           a[a_index] = -(g1.sterm * a_im1) + g1.cterm * a_i;
                        }
                     }
// c
// c                 Apply procedure G2 (CC,SS,B(J-1),B(J))
// c
                     double b_j = b[j];
                     double b_jm1 = b[j - 1];
                     b[j - 1] = g1.cterm * b_jm1 + g1.sterm * b_j;
                     b[j] = -(g1.sterm * b_jm1) + g1.cterm * b_j;
                  }
               }
// c
               npp1 = nsetp - 1;
               nsetp--;
               iz1--;
               index[iz1] = i;
// C
// C        SEE IF THE REMAINING COEFFS IN SET P ARE FEASIBLE.  THEY SHOULD
// C        BE BECAUSE OF THE WAY ALPHA WAS DETERMINED.
// C        IF ANY ARE INFEASIBLE IT IS DUE TO ROUND-OFF ERROR.  ANY
// C        THAT ARE NONPOSITIVE WILL BE SET TO ZERO
// C        AND MOVED FROM SET P TO SET Z.
// C
               for (jj = 0; jj < nsetp; jj++) {
                  i = index[jj];
                  if (x[i] <= 0.0) {
                     continue label260;
                  }
               }
               break;
            }
// C
// C         COPY B( ) INTO ZZ( ).  THEN SOLVE AGAIN AND LOOP BACK.
// C
            System.arraycopy(b, 0, zz, 0, m);

            solveTriag(nsetp, a, jj, m, zz, index);
         }
// C                      ******  END OF SECONDARY LOOP  ******
// C
// C        ALL NEW COEFFS ARE POSITIVE.  LOOP BACK TO BEGINNING.
      }
   }

   private static double computeRnorm(int npp1, int m, double[] b, int n, double[] w) {
// C
// C                        ******  END OF MAIN LOOP  ******
// C
// C                        COME TO HERE FOR TERMINATION.
// C                     COMPUTE THE NORM OF THE FINAL RESIDUAL VECTOR.
// C
      double sm = 0.0;
      if (npp1 < m) {
         for (int i = npp1; i < m; i++) {
            double b_i = b[i];
            sm += b_i * b_i;
         }
      } else {
         for (int j = 0; j < n; j++) {
            w[j] = 0.0;
         }
      }
      return Math.sqrt(sm);
   }

   private static void solveTriag(int nsetp, double[] a, int jj, int m, double[] zz, int[] index) {
// C
// C     THE FOLLOWING BLOCK OF CODE IS USED AS AN INTERNAL SUBROUTINE
// C     TO SOLVE THE TRIANGULAR SYSTEM, PUTTING THE SOLUTION IN ZZ().
// C
      for (int l = 0; l < nsetp; l++) {
         int ip = nsetp - l;
         if (l != 0) {
            for (int ii = 0; ii < ip; ii++) {
               zz[ii] += -a[ii + jj * m] * zz[ip];
            }
         }
         int ip_m1 = ip - 1;
         jj = index[ip_m1];
         zz[ip_m1] /= a[ip_m1 + jj * m];
      }
   }
}
