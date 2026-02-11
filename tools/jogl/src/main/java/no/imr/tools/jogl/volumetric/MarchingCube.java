package no.imr.tools.jogl.volumetric;

import no.imr.tools.math.linalg.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * DESCRIPTION <br>
 * ----------- <br>
 * This implementation is based on the original Marching Cubes algorithm
 * by William E. Lorenzen and Harvey E. Cline in Computer Graphics,
 * Volume 21, Number 4, July 1987. <br><br>
 * <p>
 * The algorithm does not generate triangles, instead it creates polygons.
 * This approach simplifies the algorithm and reduces number of polygons. <br><br>
 * <p>
 * The flaw in the original algorithm (not dealing with poly ambiguities,
 * where cases 3, 6, 7, 10, 12, 13 are ambiguous) are handled by explicitly
 * defining polygons for the complementary cases. For the non-ambiguous cases,
 * the polygons are simply reversed.  Note that the complement of cases 8-14,
 * are the cases themselves in a different rotation. These cases are
 * therefore eliminated, and we need 23 rather than 15 cases. <br><br>
 * <p>
 * All polygons are represented as triangles, using fan triangulation.
 * <p>
 * Cube mapping:
 * <pre>
 *
 *         V8 _______e7________ V7
 *           /|               /|
 *        e11 |            e12 |
 *       /    |           /    |
 *   V4 +-------e3-------+ V3  |
 *      |     |          |     |
 *      |     e8         |     e6
 *      |     |          |     |
 *      e4    |          e2    |
 *      |     |          |     |
 *      |  V5 +------e5--|-----+ V6
 *      |    /           |    /
 *      |  e9            | e10
 *      |/               |/
 *   V1 +-------e1-------+ V2
 *
 * </pre>
 */
public final class MarchingCube {
   private static final int V1 = 0;
   private static final int V2 = 1;
   private static final int V3 = 2;
   private static final int V4 = 3;
   private static final int V5 = 4;
   private static final int V6 = 5;
   private static final int V7 = 6;
   private static final int V8 = 7;

   private static final int P1 = 1;
   private static final int P2 = 2;
   private static final int P3 = 4;
   private static final int P4 = 8;
   private static final int P5 = 16;
   private static final int P6 = 32;
   private static final int P7 = 64;
   private static final int P8 = 128;

   private static final int NUM_CASES = 23;
   private static final int[] CASE_PATTERN = {
         /*  0  */   0,
         /*  1  */   P1,
         /*  2  */   P1 | P2,
         /*  3  */   P1 | P3,
         /*  4  */   P1 | P7,
         /*  5  */   P2 | P5 | P6,
         /*  6  */   P1 | P2 | P7,
         /*  7  */   P2 | P4 | P7,
         /*  8  */   P1 | P2 | P5 | P6,
         /*  9  */   P1 | P5 | P6 | P8,
         /* 10  */   P1 | P4 | P6 | P7,
         /* 11  */   P1 | P5 | P6 | P7,
         /* 12  */   P2 | P4 | P5 | P6,
         /* 13  */   P1 | P3 | P6 | P8,
         /* 14  */   P2 | P5 | P6 | P8,
         /*  7c */ ~(P2 | P4 | P7) & 0xff,
         /*  6c */ ~(P1 | P2 | P7) & 0xff,
         /*  5c */ ~(P2 | P5 | P6) & 0xff,
         /*  4c */ ~(P1 | P7) & 0xff,
         /*  3c */ ~(P1 | P3) & 0xff,
         /*  2c */ ~(P1 | P2) & 0xff,
         /*  1c */ ~P1 & 0xff,
         /*  0c */   0xff
   };

   private static final int[][] POLY_EDGE = {
         /*  0  */ {0, 0},
         /*  1  */ {1, 4, 9, 0, 0},
         /*  2  */ {4, 9, 10, 0, 4, 10, 2, 0, 0},
         /*  3A */ {1, 4, 9, 0, 2, 12, 3, 0, 0},
         /*  4  */ {1, 4, 9, 0, 6, 7, 12, 0, 0},
         /*  5  */ {1, 9, 8, 0, 1, 8, 6, 0, 1, 6, 2, 0, 0},
         /*  6A */ {4, 9, 10, 0, 4, 10, 2, 0, 6, 7, 12, 0, 0},
         /*  7A */ {1, 10, 2, 0, 3, 11, 4, 0, 6, 7, 12, 0, 0},
         /*  8  */ {2, 4, 8, 0, 2, 8, 6, 0, 0},
         /*  9  */ {1, 4, 11, 0, 1, 11, 7, 0, 1, 7, 6, 0, 1, 6, 10, 0, 0},
         /* 10A */ {1, 3, 11, 0, 1, 11, 9, 0, 5, 7, 12, 0, 5, 12, 10, 0, 0},
         /* 11  */ {1, 4, 8, 0, 1, 8, 7, 0, 1, 7, 12, 0, 1, 12, 10, 0, 0},
         /* 12A */ {1, 9, 8, 0, 1, 8, 6, 0, 1, 6, 2, 0, 4, 3, 11, 0, 0},
         /* 13A */ {1, 4, 9, 0, 2, 12, 3, 0, 8, 11, 7, 0, 5, 6, 10, 0, 0},
         /* 14  */ {1, 9, 11, 0, 1, 11, 7, 0, 1, 7, 6, 0, 1, 6, 2, 0, 0},
         /*  7B */ {1, 4, 11, 0, 1, 11, 7, 0, 1, 7, 6, 0, 1, 6, 10, 0, 2, 12, 3, 0, 0},
         /*  6B */ {12, 7, 6, 0, 12, 6, 10, 0, 12, 10, 9, 0, 12, 9, 4, 0, 12, 4, 2, 0, 0},
         /*  5r */ {2, 6, 8, 0, 2, 8, 9, 0, 2, 9, 1, 0, 0},
         /*  4r */ {9, 4, 1, 0, 12, 7, 6, 0, 0},
         /*  3B */ {9, 4, 3, 0, 9, 3, 12, 0, 9, 12, 2, 0, 9, 2, 1, 0, 0},
         /*  2r */ {2, 10, 9, 0, 2, 9, 4, 0, 0},
         /*  1r */ {9, 4, 1, 0, 0},
         /*  0r */ {0, 0}
   };

   private static final int[][] EDGE_VERTEX = {
         {0, 0},
         /* e1-e4 */ {V1, V2}, {V2, V3}, {V4, V3}, {V1, V4},
         /* e5-e8 */ {V5, V6}, {V6, V7}, {V8, V7}, {V5, V8},
         /* e9-e12*/ {V1, V5}, {V2, V6}, {V4, V8}, {V3, V7}
   };

   private record Info(
         int caseNumber, // The case number 0-22 for bit pattern 0-255.
         int[] transform // Maps cube vertices into normal cube view.
   ) {
   }

   private static final class CubeTransform {
      private final Info[] info;

      private CubeTransform() {
         info = new Info[256];
         int[][] pTransform = new int[24][8];
         pTransform[0] = new int[]{P1, P2, P3, P4, P5, P6, P7, P8};
         pTransform[1] = new int[]{P6, P5, P8, P7, P2, P1, P4, P3};
         pTransform[2] = new int[]{P2, P1, P5, P6, P3, P4, P8, P7};
         pTransform[3] = new int[]{P4, P3, P7, P8, P1, P2, P6, P5};
         pTransform[4] = new int[]{P5, P1, P4, P8, P6, P2, P3, P7};
         pTransform[5] = new int[]{P2, P6, P7, P3, P1, P5, P8, P4};

         // Make the different rotation for each transformation:
         //
         for (int i = 1; i < 4; ++i) {
            for (int j = 0; j < 6; ++j) {
               for (int k = 0; k < 4; ++k) {
                  pTransform[i * 6 + j][k] = pTransform[(i - 1) * 6 + j][(k + 1) % 4];
                  pTransform[i * 6 + j][k + 4] = pTransform[(i - 1) * 6 + j][(k + 1) % 4 + 4];
               }
            }
         }
         // Copy and convert p_transform values from pattern type to vertex indices:
         //
         int[] conv = new int[P8 + 1];
         conv[P1] = V1;
         conv[P2] = V2;
         conv[P3] = V3;
         conv[P4] = V4;
         conv[P5] = V5;
         conv[P6] = V6;
         conv[P7] = V7;
         conv[P8] = V8;
         int[][] transform = new int[24][8];
         for (int i = 0; i < 24; ++i) {
            for (int j = 0; j < 8; ++j) {
               transform[i][j] = conv[pTransform[i][j]];
            }
         }

         // Compute the mapping from all 8-bit patterns to one of the 23 cases:
         //
         for (int i = 0; i < 256; ++i) {
            // Test all possible rotations of the cube
            //
            for (int j = 0; j < 24; ++j) {
               boolean shouldBreak = false;
               int p = ((i & pTransform[j][V1]) != 0 ? P1 : 0)
                     | ((i & pTransform[j][V2]) != 0 ? P2 : 0)
                     | ((i & pTransform[j][V3]) != 0 ? P3 : 0)
                     | ((i & pTransform[j][V4]) != 0 ? P4 : 0)
                     | ((i & pTransform[j][V5]) != 0 ? P5 : 0)
                     | ((i & pTransform[j][V6]) != 0 ? P6 : 0)
                     | ((i & pTransform[j][V7]) != 0 ? P7 : 0)
                     | ((i & pTransform[j][V8]) != 0 ? P8 : 0);

               for (int k = 0; k < NUM_CASES; ++k) {
                  if (p == CASE_PATTERN[k]) {
                     info[i] = new Info(k, transform[j]);
                     shouldBreak = true;
                     break;
                  }
               }
               if (shouldBreak) {
                  break;
               }
            }
         }
      }
   }

   /// Nested polygon class

   public static final class Polygon {
      private final List<Vec3> points = new ArrayList<>();

      private Polygon() {
      }

      public List<Vec3> getPoints() {
         return points;
      }

      @Override
      public String toString() {
         StringBuilder s = new StringBuilder();
         for (int i = 0; i < points.size(); i++) {
            Vec3 point = points.get(i);
            s.append(" p" + i + " = " + point);
         }
         return s.toString();
      }
   }

   private static final CubeTransform INITIALIZE = new CubeTransform();

   private static final int[] CUBE = {0, 1, 2, 3, 4, 5, 6, 7};
   //private static final int[] PRISM       = { 0,1,2,2,3,4,5,5 };
   //private static final int[] PYRAMID     = { 0,1,2,3,4,4,4,4 };
   //private static final int[] TETRAHEDRON = { 0,1,2,2,3,3,3,3 };

   private int numPolys;
   private final List<Polygon> polys = new ArrayList<>();

   public MarchingCube() {
      numPolys = 0;
   }

   /**
    * MarchingCube::MarchingCube().
    * <p>
    * - Initialize tables if not done before, and empties struct.
    * - Compute pattern based on isovalue and cube corner values (0-255)
    * - Compute case (0-21), (15-21 is a complement case of 1-7),
    * and the transformation needed for rotating original cube
    * to actual rotation.
    * - Compute bit pattern (range 0-255)
    * - Create polygons and poly points from
    * the iso value, scalar values, and corner
    * coordinates of the cube.
    */
   public MarchingCube(float value, Vec3[] coord, float[] param) {
      int[] mapping = CUBE;
      int pattern = computePattern(value, param, mapping);
      Info info = INITIALIZE.info[pattern];
      numPolys = 0;

      int[] polyEdge = POLY_EDGE[info.caseNumber];

      // compute the polygons:
      //
      for (int i = 0; polyEdge[i] != 0; ++i) {
         beginPoly();
         for (; polyEdge[i] != 0; ++i) {
            int v0 = mapping[info.transform[EDGE_VERTEX[polyEdge[i]][0]]];
            int v1 = mapping[info.transform[EDGE_VERTEX[polyEdge[i]][1]]];
            vertex(linePoint(coord, param, value, v0, v1));
         }
         endPoly();
      }
   }

   private void beginPoly() {
   }

   private void endPoly() {
      Polygon thePoly = polys.get(numPolys);

      if (thePoly.points.size() > 1 && thePoly.points.getLast().equals(thePoly.points.getFirst())) {
         thePoly.points.removeLast();
      }

      if (thePoly.points.size() < 3) {
         polys.remove(numPolys);
      } else {
         numPolys++; //polygon ok
      }

      /*
      Polygon& thePoly = m_poly[ m_numPolys ];

      if (thePoly.m_numPoints > 1 && thePoly.m_point[ thePoly.m_numPoints-1 ] == thePoly.m_point[ 0 ])
         --thePoly.m_numPoints;
      if (thePoly.m_numPoints < 3)
         thePoly.m_numPoints = 0;
      else
         ++m_numPolys;
         */
   }

   /**
    * MarchingCube::vertex().
    * <p>
    * - Add a vertex to the current poly
    */
   private void vertex(Vec3 point) {
      if (polys.size() < numPolys + 1) {
         polys.add(new Polygon());
      }
      Polygon thePoly = polys.get(numPolys);
      if (!thePoly.points.isEmpty() && point.equals(thePoly.points.getLast())) {
         return; //same point as last point
      }
      thePoly.points.add(point);
      /*
      Polygon& thePoly = m_poly[ m_numPolys ];

      if (thePoly.m_numPoints > 0 && point == thePoly.m_point[ thePoly.m_numPoints-1 ])
         return;
      thePoly.m_point[ thePoly.m_numPoints++ ] = point;
      */
   }

   public List<Polygon> getPolys() {
      return polys;
   }

   private static Vec3 linePoint(Vec3[] coords, float[] param, float value, int v0, int v1) {
      return coords[v0].plus(coords[v1].minus(coords[v0]).times((value - param[v0]) / (param[v1] - param[v0])));
   }

   /**
    * computePattern().
    * <p>
    * - Return 8-bit pattern describing which cube corner values
    * are higher than the value.
    */
   private static int computePattern(double value, float[] param, int[] mapping) {
      return (param[mapping[V1]] > value ? 1 : 0)
            | ((param[mapping[V2]] > value ? 1 : 0) << V2)
            | ((param[mapping[V3]] > value ? 1 : 0) << V3)
            | ((param[mapping[V4]] > value ? 1 : 0) << V4)
            | ((param[mapping[V5]] > value ? 1 : 0) << V5)
            | ((param[mapping[V6]] > value ? 1 : 0) << V6)
            | ((param[mapping[V7]] > value ? 1 : 0) << V7)
            | ((param[mapping[V8]] > value ? 1 : 0) << V8);
   }
}
