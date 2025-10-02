package no.imr.tools.math.linalg;

import no.imr.tools.RandomUtils;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.Utils;
import no.imr.tools.test.JUnitUtils;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("PMD.SystemPrintln")
final class BoxIntersectionMain {
   private BoxIntersectionMain() {
   }

   public static void main(String[] args) {
      Random random = new Random();
      int n = 1_000_000;
      int intersectionCount = 0;
      int wrongCount = 0;
      Set<Long> failedSeeds = new LinkedHashSet<>();
      for (int i = 1; i <= n; i++) {
         long seed = RandomUtils.newSeed();
         random.setSeed(seed);
         Result result = testUnitBoxIntersectsTriangle(random);
         if (!result.ok()) {
            wrongCount++;
            failedSeeds.add(seed);
            System.out.println("FAILED   iteration: " + i +
                  ",  seed: " + JUnitUtils.seedToString(seed) +
                  ",  intersects=" + result.intersects +
                  ",  insideBox: " + result.hasPointInBox() +
                  ",  point: " + result.pointInBox);
         }
         if (result.intersects) {
            intersectionCount++;
         }
         if (i % 1000 == 0) {
            System.out.println("Iteration: " + i +
                  ",  wrong: " + wrongCount + " (" + (100f * wrongCount / i) + " %)" +
                  ",  intersections: " + Utils.round(100f * intersectionCount / i, 1) + " %");
         }
      }
      if (!failedSeeds.isEmpty()) {
         System.out.println("Failed seeds: " + failedSeeds);
      }
   }

   static Result testUnitBoxIntersectsTriangle(Random random) {
      Vec3 p1 = randomVector(random);
      Vec3 p2 = randomVector(random);
      Vec3 p3 = randomVector(random);

      boolean intersects = BoxIntersection.unitBoxIntersectsTriangle(p1, p2, p3);

      int iterationCount = intersects ? 1_000_000_000 : 10_000;
      Vec3 pointInBox = findPointInUnitBox(p1, p2, p3, iterationCount, random);
      return new Result(intersects, pointInBox);
   }

   private static @Nullable Vec3 findPointInUnitBox(Vec3 p1, Vec3 p2, Vec3 p3, int iterationCount, Random random) {
      Vec3 v12 = p2.minus(p1);
      Vec3 v13 = p3.minus(p1);
      for (int i = 0; i < iterationCount; i++) {
         float a;
         float b;
         switch (random.nextInt(4)) {
            case 0 -> { // Along one edge
               a = 0;
               b = random.nextFloat();
            }
            case 1 -> { // Along one edge
               a = random.nextFloat();
               b = 0;
            }
            case 2 -> { // Along one edge
               a = random.nextFloat();
               b = 1 - a;
            }
            case 3 -> { // Inside
               a = random.nextFloat();
               b = random.nextFloat(1 - a);
            }
            default -> {
               throw new ShouldNotHappenException();
            }
         }

         float x = p1.x() + a * v12.x() + b * v13.x();
         if (x < 0 || x > 1) {
            continue;
         }
         float y = p1.y() + a * v12.y() + b * v13.y();
         if (y < 0 || y > 1) {
            continue;
         }
         float z = p1.z() + a * v12.z() + b * v13.z();
         if (z < 0 || z > 1) {
            continue;
         }
         return new Vec3(x, y, z);
      }
      return null;
   }

   private static Vec3 randomVector(Random random) {
      float x = random.nextFloat(-1, 2);
      float y = random.nextFloat(-1, 2);
      float z = random.nextFloat(-1, 2);
      return new Vec3(x, y, z);
   }

   record Result(boolean intersects, @Nullable Vec3 pointInBox) {

      private boolean ok() {
         return intersects == hasPointInBox();
      }

      boolean hasPointInBox() {
         return pointInBox != null;
      }
   }
}
