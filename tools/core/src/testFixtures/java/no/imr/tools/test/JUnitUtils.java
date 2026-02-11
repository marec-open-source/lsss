package no.imr.tools.test;

import no.imr.tools.RandomUtils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.logging.OneLineFormatter;
import no.imr.tools.math.linalg.Matrix3;
import no.imr.tools.math.linalg.Matrix4;
import no.imr.tools.math.linalg.Vec2;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.misc.FloatPredicate;
import no.imr.tools.misc.ThrowingConsumer;
import no.imr.tools.misc.test.TestUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

@SuppressWarnings("PMD.SystemPrintln")
public final class JUnitUtils {
   public static final Path OUTPUT_DIR = LoggingManager.getTopInstallationDir().resolve("build/marec-test");

   public static final Set<String> IGNORE_DIR_NAMES = Set.of(".angular", ".git", ".gradle", ".idea", "build", "node_modules", "out", "tmp");

   private JUnitUtils() {
   }

   public static void assertMaxLogLevel() {
      if (Log.getMaxLevel().intValue() >= Level.WARNING.intValue()) {
         Assertions.fail(new OneLineFormatter().format(Log.getMaxLevelLogRecord()));
      }
   }

   public static void set(double[] array, double... values) {
      Assertions.assertEquals(array.length, values.length);
      System.arraycopy(values, 0, array, 0, array.length);
   }

   public static void assertAllEquals(short expected, short[] actual) {
      for (int i = 0; i < actual.length; i++) {
         Assertions.assertEquals(expected, actual[i], Integer.toString(i));
      }
   }

   public static void assertAllEquals(float expected, float[] actual) {
      for (int i = 0; i < actual.length; i++) {
         Assertions.assertEquals(expected, actual[i], Integer.toString(i));
      }
   }

   public static void assertAllEquals(float expected, float[] actual, float delta) {
      for (int i = 0; i < actual.length; i++) {
         Assertions.assertEquals(expected, actual[i], delta, Integer.toString(i));
      }
   }

   public static void assertEquals(Vec2 expected, Vec2 actual) {
      Assertions.assertEquals(expected.x(), actual.x(), "x");
      Assertions.assertEquals(expected.y(), actual.y(), "y");
   }

   public static void assertEquals(Vec2 expected, Vec2 actual, float delta) {
      Assertions.assertEquals(expected.x(), actual.x(), delta, "x");
      Assertions.assertEquals(expected.y(), actual.y(), delta, "y");
   }

   public static void assertEquals(Vec3 expected, Vec3 actual) {
      Assertions.assertEquals(expected.x(), actual.x(), "x");
      Assertions.assertEquals(expected.y(), actual.y(), "y");
      Assertions.assertEquals(expected.z(), actual.z(), "z");
   }

   public static void assertEquals(Vec3 expected, Vec3 actual, float delta) {
      Assertions.assertEquals(expected.x(), actual.x(), delta, "x");
      Assertions.assertEquals(expected.y(), actual.y(), delta, "y");
      Assertions.assertEquals(expected.z(), actual.z(), delta, "z");
   }

   public static void assertEquals(Matrix3 expected, Matrix3 actual) {
      Assertions.assertArrayEquals(expected.toRowWiseArray(), actual.toRowWiseArray());
   }

   public static void assertEquals(Matrix3 expected, Matrix3 actual, float delta) {
      Assertions.assertArrayEquals(expected.toRowWiseArray(), actual.toRowWiseArray(), delta);
   }

   public static void assertEquals(Matrix4 expected, Matrix4 actual) {
      Assertions.assertArrayEquals(expected.toRowWiseArray(), actual.toRowWiseArray());
   }

   public static void assertEquals(Matrix4 expected, Matrix4 actual, float delta) {
      Assertions.assertArrayEquals(expected.toRowWiseArray(), actual.toRowWiseArray(), delta);
   }

   public static void assertLessThan(double small, double large) {
      Assertions.assertTrue(small < large, small + " < " + large);
   }

   public static void assertEquals(Element expected, Element actual) {
      boolean ok = XmlUtils.equalContent(expected, actual);
      if (!ok) {
         Path expectedFile = OUTPUT_DIR.resolve("JUnitUtils-expected.xml");
         Path actualFile = OUTPUT_DIR.resolve("JUnitUtils-actual.xml");
         try {
            FileUtils.createDirectories(OUTPUT_DIR);
            try (OutputStream expectedOut = Files.newOutputStream(expectedFile)) {
               new XMLWriter(expectedOut).write(expected);
            }
            try (OutputStream actualOut = Files.newOutputStream(actualFile)) {
               new XMLWriter(actualOut).write(actual);
            }
         } catch (IOException e) {
            e.printStackTrace(System.err);
         }
         Assertions.fail("\nSee files\n" + expectedFile + "\n" + actualFile + "\nExpected:\n" + XmlUtils.toCompactString(expected) + "\n\nActual:\n" + XmlUtils.toCompactString(actual));
      }
   }

   public static <T> void assertEquals(Set<T> expected, Set<T> actual) {
      if (expected.equals(actual)) {
         return;
      }
      String onlyInExpected = TestUtils.diff(expected, actual);
      String onlyInActual = TestUtils.diff(actual, expected);
      Assertions.assertEquals(onlyInExpected, onlyInActual);
   }

   public static void assertTrue(float[] values, FloatPredicate expectation) {
      for (int i = 0; i < values.length; i++) {
         float value = values[i];
         if (!expectation.test(value)) {
            Assertions.fail("Failure for value " + i + ": " + value);
         }
      }
   }

   public static <E extends Exception> void runWithRandom(ThrowingConsumer<Random, E> test) throws E {
      runWithRandom(1, test);
   }

   public static <E extends Exception> void runWithRandom(int runCount, ThrowingConsumer<Random, E> test) throws E {
      Random random = new Random();
      for (int i = 0; i < runCount; i++) {
         long seed = RandomUtils.newSeed();
         random.setSeed(seed);
         try {
            test.accept(random);
         } catch (Throwable e) {
            System.err.println("Test failed with seed = " + seedToString(seed));
            throw e;
         }
      }
   }

   public static String seedToString(long seed) {
      return "0x%016xL".formatted(seed);
   }

   public static float[] createRandomFloatArray(Random random, int length) {
      float[] values = new float[length];
      for (int i = 0; i < values.length; i++) {
         values[i] = random.nextFloat();
      }
      return values;
   }

   public static double[] createRandomDoubleArray(Random random, int length) {
      double[] values = new double[length];
      for (int i = 0; i < values.length; i++) {
         values[i] = random.nextDouble();
      }
      return values;
   }
}
