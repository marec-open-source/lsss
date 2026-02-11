package no.imr.tools;

import no.imr.tools.concurrent.Exec;
import no.imr.tools.misc.ThrowingRunnable;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

final class UtilsTest {
   @Test
   void awaitFuture() {
      Future<String> future = Exec.CACHED_THREAD_POOL.submit(() -> "ok");
      String result = Utils.awaitFuture(future);
      assertEquals("ok", result);
   }

   @Test
   void avoidInfinity() {
      assertEquals(0, Utils.avoidInfinity(0));

      assertEquals(Float.MAX_VALUE, Utils.avoidInfinity(Float.MAX_VALUE));
      assertEquals(Float.MAX_VALUE, Utils.avoidInfinity(Float.POSITIVE_INFINITY));

      assertEquals(-Float.MAX_VALUE, Utils.avoidInfinity(-Float.MAX_VALUE));
      assertEquals(-Float.MAX_VALUE, Utils.avoidInfinity(Float.NEGATIVE_INFINITY));
   }

   @Test
   void testModInt() {
      assertEquals(0, Utils.mod(10, 10));
      assertEquals(9, Utils.mod(9, 10));
      assertEquals(1, Utils.mod(1, 10));
      assertEquals(0, Utils.mod(0, 10));
      assertEquals(9, Utils.mod(-1, 10));
      assertEquals(1, Utils.mod(-9, 10));
      assertEquals(0, Utils.mod(-10, 10));

      for (int i = -3; i <= 3; i++) {
         assertEquals(0, Utils.mod(3 * i, 3));
         assertEquals(1, Utils.mod(3 * i + 1, 3));
         assertEquals(2, Utils.mod(3 * i - 1, 3));
      }
   }

   @Test
   void testModDouble() {
      assertEquals(0, Utils.mod(10.0, 10.0));
      assertEquals(1.1, Utils.mod(3.1, 2));
      assertEquals(1.1, Utils.mod(5.1, 2), 1e-15);
      for (int i = -3; i <= 3; i++) {
         assertEquals(1.1, Utils.mod(3 * i + 1.1, 3.0), 1e-15);
      }
   }

   @Test
   void testRoundFloat() {
      assertEquals(1.1f, Utils.round(1.11f, 10));
      assertEquals(Float.NaN, Utils.round(Float.NaN, 10));
      assertEquals(Float.NEGATIVE_INFINITY, Utils.round(Float.NEGATIVE_INFINITY, 10));
      assertEquals(Float.POSITIVE_INFINITY, Utils.round(Float.POSITIVE_INFINITY, 10));
   }

   @Test
   void testRoundDouble() {
      assertEquals(1.1, Utils.round(1.11, 10));
      assertEquals(Double.NaN, Utils.round(Double.NaN, 10));
      assertEquals(Double.NEGATIVE_INFINITY, Utils.round(Double.NEGATIVE_INFINITY, 10));
      assertEquals(Double.POSITIVE_INFINITY, Utils.round(Double.POSITIVE_INFINITY, 10));
   }

   @Test
   void roundToNumberOfDigits() {
      assertEquals(0, Utils.roundToNumberOfDigits(0, 1));

      assertEquals(100, Utils.roundToNumberOfDigits(111, 1));
      assertEquals(-100, Utils.roundToNumberOfDigits(-111, 1));

      assertEquals(110, Utils.roundToNumberOfDigits(111, 2));
      assertEquals(-110, Utils.roundToNumberOfDigits(-111, 2));

      assertEquals(111, Utils.roundToNumberOfDigits(111, 3));
      assertEquals(-111, Utils.roundToNumberOfDigits(-111, 3));

      assertEquals(111, Utils.roundToNumberOfDigits(111, 4));
      assertEquals(-111, Utils.roundToNumberOfDigits(-111, 4));

      assertEquals(1.23, Utils.roundToNumberOfDigits(1.23456, 3));
      assertEquals(-1.23, Utils.roundToNumberOfDigits(-1.23456, 3));

      assertEquals(1.235, Utils.roundToNumberOfDigits(1.23456, 4));
      assertEquals(-1.235, Utils.roundToNumberOfDigits(-1.23456, 4));

      assertEquals(1.235e6, Utils.roundToNumberOfDigits(1.2345e6, 4));
      assertEquals(-1.235e6, Utils.roundToNumberOfDigits(-1.2345e6, 4));

      assertEquals(Double.NaN, Utils.roundToNumberOfDigits(Double.NaN, 4));
      assertEquals(Double.NEGATIVE_INFINITY, Utils.roundToNumberOfDigits(Double.NEGATIVE_INFINITY, 4));
      assertEquals(Double.POSITIVE_INFINITY, Utils.roundToNumberOfDigits(Double.POSITIVE_INFINITY, 4));
   }

   @Test
   void greaterThan() {
      Predicate<Integer> predicate = Utils.greaterThan(Comparator.naturalOrder(), 5);
      assertFalse(predicate.test(4));
      assertFalse(predicate.test(5));
      assertTrue(predicate.test(6));
   }

   @Test
   void testStartsWithIgnoringCase() {
      assertTrue(Utils.startsWithIgnoringCase("a", "A"));
      assertTrue(Utils.startsWithIgnoringCase("a", "a"));
      assertFalse(Utils.startsWithIgnoringCase("a", "b"));
      assertTrue(Utils.startsWithIgnoringCase("ba", "B"));
      assertFalse(Utils.startsWithIgnoringCase("ba", "a"));
      assertFalse(Utils.startsWithIgnoringCase("ba", "baba"));
   }

   @Test
   void testEndsWithIgnoringCase() {
      assertTrue(Utils.endsWithIgnoringCase("a", "A"));
      assertTrue(Utils.endsWithIgnoringCase("a", "a"));
      assertFalse(Utils.endsWithIgnoringCase("a", "b"));
      assertTrue(Utils.endsWithIgnoringCase("ba", "A"));
      assertFalse(Utils.endsWithIgnoringCase("ba", "b"));
      assertFalse(Utils.endsWithIgnoringCase("ba", "baba"));
   }

   @Test
   void testContainsIgnoringCase() {
      assertTrue(Utils.containsIgnoringCase("", ""));
      assertFalse(Utils.containsIgnoringCase("", "a"));
      assertTrue(Utils.containsIgnoringCase("ab", "aB"));
      assertTrue(Utils.containsIgnoringCase("ab", "Ab"));
      assertTrue(Utils.containsIgnoringCase("cab", "Ab"));
      assertTrue(Utils.containsIgnoringCase("cabc", "Ab"));
      assertTrue(Utils.containsIgnoringCase("abc", "Ab"));
      assertFalse(Utils.containsIgnoringCase("ab", "bb"));
   }

   @Test
   void trimmedSubstring() {
      assertEquals("a 12 b", Utils.trimmedSubstring("a 12 b", 0, 6));
      assertEquals("12", Utils.trimmedSubstring("a 12 b", 1, 5));
      assertEquals("12", Utils.trimmedSubstring("a 12 b", 2, 4));
      assertEquals("1", Utils.trimmedSubstring("a 12 b", 1, 3));
      assertEquals("", Utils.trimmedSubstring("a 12 b", 1, 2));
      assertEquals("", Utils.trimmedSubstring("a 12 b", 1, 1));
   }

   @Test
   void shift() {
      List<String> list = List.of("a", "b", "c", "d", "e");
      assertEquals("c", Utils.shift(list, "c", 0));
      assertEquals("d", Utils.shift(list, "c", 1));
      assertEquals("a", Utils.shift(list, "d", 2));

      assertEquals("b", Utils.shift(list, "c", -1));
      assertEquals("e", Utils.shift(list, "b", -2));

      assertEquals("x", Utils.shift(list, "x", 0));
      assertEquals("a", Utils.shift(list, "x", 1));
      assertEquals("b", Utils.shift(list, "x", 2));
      assertEquals("e", Utils.shift(list, "x", -1));
      assertEquals("d", Utils.shift(list, "x", -2));

      assertEquals("x", Utils.shift(List.of(), "x", 1));
   }

   @Test
   void allOfType() {
      assertEquals(List.of(1, 2, 3),
            Utils.getAllOfType(List.of(1.0f, 1, 1.0d, 2, "", 'c', "3", 3), Integer.class)
                  .toList()
      );
      assertEquals(List.of("", "a"),
            Stream.of(null, "", 0, "a", null, 1)
                  .gather(Utils.allOfType(String.class))
                  .toList()
      );
   }

   @Test
   void binarySearchForInt() {
      List<int[]> list = List.of(
            new int[]{1},
            new int[]{3},
            new int[]{4},
            new int[]{7}
      );
      ToIntFunction<int[]> f = x -> x[0];

      assertEquals(0, Utils.binarySearchForInt(list, 1, f));
      assertEquals(1, Utils.binarySearchForInt(list, 3, f));
      assertEquals(2, Utils.binarySearchForInt(list, 4, f));
      assertEquals(3, Utils.binarySearchForInt(list, 7, f));

      assertEquals(insertionPointToBinarySearchReturnValue(0), Utils.binarySearchForInt(list, 0, f));
      assertEquals(insertionPointToBinarySearchReturnValue(1), Utils.binarySearchForInt(list, 2, f));
      assertEquals(insertionPointToBinarySearchReturnValue(3), Utils.binarySearchForInt(list, 5, f));
      assertEquals(insertionPointToBinarySearchReturnValue(3), Utils.binarySearchForInt(list, 6, f));
      assertEquals(insertionPointToBinarySearchReturnValue(4), Utils.binarySearchForInt(list, 8, f));
   }

   private static int insertionPointToBinarySearchReturnValue(int i) {
      return -i - 1;
   }

   @Test
   void testFloatToString() {
      assertEquals("0", Utils.toString(0f));
      assertEquals("0", Utils.toString(-0.0f));

      assertEquals("1.1", Utils.toString(1.1f));
      assertEquals("-1.1", Utils.toString(-1.10f));

      assertEquals("1", Utils.toString(1.0f));
      assertEquals("-1", Utils.toString(-1.0f));

      float x = 1.123456789123456789123456789123456789123456789f;
      assertEquals(Float.toString(x), Utils.toString(x));

      assertEquals("1E25", Utils.toString(1.0E25f));
      assertEquals("1E-35", Utils.toString(1.0E-35f));

      assertEquals("1.2E10", Utils.toString(1.2E10f));
      assertEquals("0.008", Utils.toString(0.008));
   }

   @Test
   void testDoubleToString() {
      assertEquals("0", Utils.toString(0.0));
      assertEquals("0", Utils.toString(-0.0));

      assertEquals("1.1", Utils.toString(1.1));
      assertEquals("-1.1", Utils.toString(-1.10));

      assertEquals("1", Utils.toString(1.0));
      assertEquals("-1", Utils.toString(-1.0));

      double x = 1.123456789123456789123456789123456789123456789;
      assertEquals(Double.toString(x), Utils.toString(x));

      assertEquals("1E25", Utils.toString(1.0E25));
      assertEquals("1E-35", Utils.toString(1.0E-35));

      assertEquals("1.2E10", Utils.toString(1.2E10));
      assertEquals("0.008", Utils.toString(0.008));
   }

   @Test
   void testRemoveTrailingZeros() {
      assertEquals("0", Utils.removeTrailingZeros("0."));
      assertEquals("0", Utils.removeTrailingZeros("0.0000"));
      assertEquals("0", Utils.removeTrailingZeros("0,0000"));
      assertEquals("1E10", Utils.removeTrailingZeros("1.0000E10"));
      assertEquals("1e10", Utils.removeTrailingZeros("1,0000e10"));
      assertEquals("1.2E10", Utils.removeTrailingZeros("1.2E10"));
   }

   @Test
   void createDecimalFormatSymbols() {
      DecimalFormatSymbols symbols = Utils.createDecimalFormatSymbols();
      assertEquals('.', symbols.getDecimalSeparator());
      assertEquals('-', symbols.getMinusSign()); // Returned 'MINUS SIGN' (U+2212) since Java 9 when using localized default constructor of DecimalFormatSymbols
   }

   @Test
   void testByteSizeString() {
      assertEquals("1 B", Utils.getByteSizeString(1));
      assertEquals("10 B", Utils.getByteSizeString(10));
      assertEquals("100 B", Utils.getByteSizeString(100));
      assertEquals("999 B", Utils.getByteSizeString(999));
      assertEquals("0.98 KB", Utils.getByteSizeString(1000));
      assertEquals("9.77 KB", Utils.getByteSizeString(10000));
      assertEquals("97.7 KB", Utils.getByteSizeString(100000));
      assertEquals("977 KB", Utils.getByteSizeString(1000000));
      assertEquals("9.54 MB", Utils.getByteSizeString(10000000));
      assertEquals("1.00 KB", Utils.getByteSizeString(1024L));
      assertEquals("1.00 MB", Utils.getByteSizeString(1024L * 1024));
      assertEquals("1.00 GB", Utils.getByteSizeString(1024L * 1024 * 1024));
      assertEquals("1.00 TB", Utils.getByteSizeString(1024L * 1024 * 1024 * 1024));
      assertEquals("1.00 PB", Utils.getByteSizeString(1024L * 1024 * 1024 * 1024 * 1024));
      assertEquals("1.00 EB", Utils.getByteSizeString(1024L * 1024 * 1024 * 1024 * 1024 * 1024));
      assertEquals("8.00 EB", Utils.getByteSizeString(Long.MAX_VALUE));
   }

   @Test
   void testRun() {
      assertTrue(Utils.isTestRun());
   }

   @Test
   void interpolateDegrees() {
      assertEquals(45, Utils.interpolateDegrees(0, 90, 0.5));
      assertEquals(90, Utils.interpolateDegrees(0, 90, 0));
      assertEquals(0, Utils.interpolateDegrees(45, 360 - 45, 0.5));
      assertEquals(1, Utils.interpolateDegrees(46, 360 - 44, 0.5));
      assertEquals(359, Utils.interpolateDegrees(44, 360 - 46, 0.5));
   }

   @Test
   void tryAndCleanup() {
      AtomicInteger normalCount = new AtomicInteger();
      AtomicInteger throwCount = new AtomicInteger();
      ThrowingRunnable<RuntimeException> runNormally = normalCount::incrementAndGet;
      ThrowingRunnable<Exception> runThrowing = () -> {
         throwCount.incrementAndGet();
         throw new TestException();
      };
      Utils.tryAndCleanup(runNormally, runNormally);
      assertThrows(TestException.class, () -> Utils.tryAndCleanup(runNormally, runThrowing));
      assertThrows(TestException.class, () -> Utils.tryAndCleanup(runThrowing, runNormally));
      assertThrows(TestException.class, () -> Utils.tryAndCleanup(runThrowing, runThrowing));
      assertEquals(4, normalCount.get());
      assertEquals(4, throwCount.get());
   }

   private static final class TestException extends Exception {
   }
}
