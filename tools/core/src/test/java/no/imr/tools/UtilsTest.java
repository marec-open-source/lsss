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
   void clamp() {
      assertEquals("b", Utils.clamp("a", "b", "d"));
      assertEquals("b", Utils.clamp("b", "b", "d"));
      assertEquals("c", Utils.clamp("c", "b", "d"));
      assertEquals("d", Utils.clamp("d", "b", "d"));
      assertEquals("d", Utils.clamp("e", "b", "d"));
   }

   @Test
   void greaterThan() {
      Predicate<Integer> predicate = Utils.greaterThan(Comparator.naturalOrder(), 5);
      assertFalse(predicate.test(4));
      assertFalse(predicate.test(5));
      assertTrue(predicate.test(6));
   }

   @Test
   void startsWithIgnoringCase() {
      assertTrue(Utils.startsWithIgnoringCase("a", "A"));
      assertTrue(Utils.startsWithIgnoringCase("a", "a"));
      assertFalse(Utils.startsWithIgnoringCase("a", "b"));
      assertTrue(Utils.startsWithIgnoringCase("ba", "B"));
      assertFalse(Utils.startsWithIgnoringCase("ba", "a"));
      assertFalse(Utils.startsWithIgnoringCase("ba", "baba"));
   }

   @Test
   void endsWithIgnoringCase() {
      assertTrue(Utils.endsWithIgnoringCase("a", "A"));
      assertTrue(Utils.endsWithIgnoringCase("a", "a"));
      assertFalse(Utils.endsWithIgnoringCase("a", "b"));
      assertTrue(Utils.endsWithIgnoringCase("ba", "A"));
      assertFalse(Utils.endsWithIgnoringCase("ba", "b"));
      assertFalse(Utils.endsWithIgnoringCase("ba", "baba"));
   }

   @Test
   void containsIgnoringCase() {
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
   void commonPrefixLength() {
      assertEquals(0, Utils.commonPrefixLength("", ""));
      assertEquals(0, Utils.commonPrefixLength("x", "y"));
      assertEquals(0, Utils.commonPrefixLength("x", "X"));
      assertEquals(1, Utils.commonPrefixLength("x", "x"));
      assertEquals(1, Utils.commonPrefixLength("xx", "xy"));
      assertEquals(2, Utils.commonPrefixLength("xy", "xy"));
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
   void floatToString() {
      assertEquals("0", Utils.toString(0f));
      assertEquals("0", Utils.toString(-0.0f));

      assertEquals("1.1", Utils.toString(1.1f));
      assertEquals("-1.1", Utils.toString(-1.10f));

      assertEquals("1", Utils.toString(1.0f));
      assertEquals("-1", Utils.toString(-1.0f));

      float x = 1.123456789123456789123456789123456789123456789f;
      assertEquals(Float.toString(x), Utils.toString(x));

      assertEquals("1E25", Utils.toString(1.0e25f));
      assertEquals("1E-35", Utils.toString(1.0e-35f));

      assertEquals("1.2E10", Utils.toString(1.2e10f));
      assertEquals("0.008", Utils.toString(0.008));
   }

   @Test
   void doubleToString() {
      assertEquals("0", Utils.toString(0.0));
      assertEquals("0", Utils.toString(-0.0));

      assertEquals("1.1", Utils.toString(1.1));
      assertEquals("-1.1", Utils.toString(-1.10));

      assertEquals("1", Utils.toString(1.0));
      assertEquals("-1", Utils.toString(-1.0));

      double x = 1.123456789123456789123456789123456789123456789;
      assertEquals(Double.toString(x), Utils.toString(x));

      assertEquals("1E25", Utils.toString(1.0e25));
      assertEquals("1E-35", Utils.toString(1.0e-35));

      assertEquals("1.2E10", Utils.toString(1.2e10));
      assertEquals("0.008", Utils.toString(0.008));
   }

   @Test
   void removeTrailingZeros() {
      assertEquals("0", Utils.removeTrailingZeros("0"));
      assertEquals("0", Utils.removeTrailingZeros("0."));
      assertEquals("0", Utils.removeTrailingZeros("0.0000"));
      assertEquals("0", Utils.removeTrailingZeros("0,0000"));
      assertEquals("1.23", Utils.removeTrailingZeros("1.23"));
      assertEquals("1.23", Utils.removeTrailingZeros("1.230"));
      assertEquals("1.23", Utils.removeTrailingZeros("1.2300"));
      assertEquals("1E10", Utils.removeTrailingZeros("1.0000E10"));
      assertEquals("1e10", Utils.removeTrailingZeros("1,0000e10"));
      assertEquals("1.2E10", Utils.removeTrailingZeros("1.2E10"));
      assertEquals("1.2E10", Utils.removeTrailingZeros("1.20E10"));
      assertEquals("1.2E10", Utils.removeTrailingZeros("1.200E10"));
      assertEquals("-1.2E-10", Utils.removeTrailingZeros("-1.200E-10"));
   }

   @Test
   void numberToString() {
      assertEquals("0", Utils.numberToString(0));
      assertEquals("1", Utils.numberToString(1));
      assertEquals("-1", Utils.numberToString(-1));

      // Values > 1.
      assertEquals("1235", Utils.numberToString(1234.56));
      assertEquals("123", Utils.numberToString(123.456));
      assertEquals("12.3", Utils.numberToString(12.345));
      assertEquals("1.23", Utils.numberToString(1.2345));

      // Values in [0.1, 1).
      assertEquals("0.5", Utils.numberToString(0.5));
      assertEquals("0.98", Utils.numberToString(0.97656));

      // Values < 0.1.
      assertEquals("0.07", Utils.numberToString(0.07));
      assertEquals("0.008", Utils.numberToString(0.0081));
      assertEquals("0.005", Utils.numberToString(0.0045));
      assertEquals("0.0003", Utils.numberToString(0.0003));
      assertEquals("-0.00008", Utils.numberToString(-0.00008));
   }

   @Test
   void createDecimalFormatSymbols() {
      DecimalFormatSymbols symbols = Utils.createDecimalFormatSymbols();
      assertEquals('.', symbols.getDecimalSeparator());
      assertEquals('-', symbols.getMinusSign()); // Returned 'MINUS SIGN' (U+2212) since Java 9 when using localized default constructor of DecimalFormatSymbols
   }

   @Test
   void getByteSizeString() {
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
   void isTestRun() {
      assertTrue(Utils.isTestRun());
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
