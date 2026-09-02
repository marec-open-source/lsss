package no.imr.tools;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.math.MathUtils;
import no.imr.tools.misc.ThrowingRunnable;
import no.imr.tools.misc.test.UniqueTmpDir;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.UiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.Image;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

/**
 * Various utility functions.
 */
public final class Utils {
   public static final Instant START_TIME = Instant.now();

   // Substituted during build:
   public static final boolean IS_BUILT_VERSION = /* @END_BLOCK_COMMENT@ true;                                         @LINE_COMMENT@ */ false;
   public static final Instant BUILD_TIME =       /* @END_BLOCK_COMMENT@ Instant.ofEpochSecond(@BUILD_EPOCH_SECOND@L); @LINE_COMMENT@ */ Instant.now().truncatedTo(ChronoUnit.SECONDS);
   public static final String GIT_COMMIT =        /* @END_BLOCK_COMMENT@ "@GIT_COMMIT@";                               @LINE_COMMENT@ */ "XXX";

   public static final boolean IS_DIST_VERSION = ResourceUtils.getUrl("no/imr/tools/resources/images/icons/Empty.svg")
         .toString().contains("/lib/jar/marec-tools-core.jar!/no/");

   private static boolean mainRun;
   private static boolean debugRun;
   private static boolean smokeTestRun;

   // public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
   // public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
   // public static final short[] EMPTY_SHORT_ARRAY = new short[0];
   public static final int[] EMPTY_INT_ARRAY = new int[0];
   public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
   public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
   // public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

   public static final Charset UTF_8 = StandardCharsets.UTF_8;
   public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;
   private static Charset nativeCharset = UTF_8; // Use UTF-8 for tests unless Utils.init is called.

   private static final Interner<String> STRING_INTERNER = Interners.newWeakInterner();

   private Utils() {
   }

   public static Charset nativeCharset() {
      return nativeCharset;
   }

   /**
    * Wrapper for {@link Thread#sleep(long)} that catches and ignores InterruptedException.
    *
    * @param duration the duration to sleep
    */
   public static void sleep(Duration duration) {
      try {
         Thread.sleep(duration);
      } catch (InterruptedException _) {
         Thread.currentThread().interrupt();
      }
   }

   public static <T extends @Nullable Object> @Nullable T awaitFuture(Future<T> future) {
      try {
         return future.get();
      } catch (CancellationException _) {
         // Cancelled.
      } catch (InterruptedException _) {
         Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
      return null;
   }

   public static String intern(String string) {
      return STRING_INTERNER.intern(string);
   }

   public static boolean startsWithIgnoringCase(String string, String prefix) {
      return string.regionMatches(true, 0, prefix, 0, prefix.length());
   }

   public static boolean endsWithIgnoringCase(String string, String suffix) {
      return string.regionMatches(true, string.length() - suffix.length(), suffix, 0, suffix.length());
   }

   public static boolean containsIgnoringCase(String string, String infix) {
      if (infix.isEmpty()) {
         return true;
      }
      char firstChar = infix.charAt(0);
      int i = indexOfIgnoringCase(string, firstChar, 0);
      while (i >= 0) {
         if (string.regionMatches(true, i, infix, 0, infix.length())) {
            return true;
         }
         i = indexOfIgnoringCase(string, firstChar, i + 1);
      }
      return false;
   }

   public static int commonPrefixLength(String a, String b) {
      int n = Math.min(a.length(), b.length());
      int i = 0;
      while (i < n && a.charAt(i) == b.charAt(i)) {
         i++;
      }
      return i;
   }

   public static int indexOfIgnoringCase(List<String> list, String s) {
      for (int i = 0; i < list.size(); i++) {
         if (list.get(i).equalsIgnoreCase(s)) {
            return i;
         }
      }
      return -1;
   }

   public static int indexOfIgnoringCase(String string, char ch, int beginIndex) {
      char upperCase = Character.toUpperCase(ch);
      char lowerCase = Character.toLowerCase(ch);
      for (int i = beginIndex; i < string.length(); i++) {
         char c = string.charAt(i);
         if (c == upperCase || c == lowerCase) {
            return i;
         }
      }
      return -1;
   }

   public static <T> Comparator<T> comparingIgnoringCase(Function<T, String> keyExtractor) {
      return Comparator.comparing(keyExtractor, String.CASE_INSENSITIVE_ORDER);
   }

   public static String trimmedSubstring(String s, int begin, int end) {
      while (begin < end && Character.isWhitespace(s.charAt(begin))) {
         begin++;
      }
      while (end > begin && Character.isWhitespace(s.charAt(end - 1))) {
         end--;
      }
      return s.substring(begin, end);
   }

   public static int indexOfFirstLetter(String s) {
      for (int i = 0; i < s.length(); i++) {
         if (Character.isLetter(s.charAt(i))) {
            return i;
         }
      }
      return -1;
   }

   /**
    * Returns the count of true values in a boolean array.
    *
    * @param booleans a boolean array
    * @return the count of true values in a boolean array
    */
   public static int count(boolean[] booleans) {
      int n = 0;
      for (boolean bool : booleans) {
         if (bool) {
            n++;
         }
      }
      return n;
   }

   /**
    * Extended logical or.
    *
    * @param booleans   a boolean array
    * @param beginIndex the start index
    * @param endIndex   the end index
    * @return {@code true} if and only if one or more of the entries are {@code true}
    */
   public static boolean any(boolean[] booleans, int beginIndex, int endIndex) {
      for (int i = beginIndex; i < endIndex; i++) {
         if (booleans[i]) {
            return true;
         }
      }
      return false;
   }

   /**
    * Extended logical or.
    *
    * @param booleans a boolean array
    * @return {@code true} if and only if one or more of the entries are {@code true}
    */
   public static boolean any(boolean[] booleans) {
      return any(booleans, 0, booleans.length);
   }

   /**
    * Extended logical and.
    *
    * @param booleans   a boolean array
    * @param beginIndex the start index
    * @param endIndex   the end index
    * @return {@code true} if and only if all the entries are {@code true}
    */
   public static boolean all(boolean[] booleans, int beginIndex, int endIndex) {
      for (int i = beginIndex; i < endIndex; i++) {
         if (!booleans[i]) {
            return false;
         }
      }
      return true;
   }

   /**
    * Extended logical and.
    *
    * @param booleans a boolean array
    * @return {@code true} if and only if all the entries are {@code true}
    */
   public static boolean all(boolean[] booleans) {
      return all(booleans, 0, booleans.length);
   }

   public static void invert(boolean[] booleans) {
      for (int i = 0; i < booleans.length; i++) {
         booleans[i] = !booleans[i];
      }
   }

   public static void invert(boolean[][] booleans) {
      for (boolean[] a : booleans) {
         invert(a);
      }
   }

   public static void fill(boolean[][] booleans, boolean value) {
      for (boolean[] a : booleans) {
         Arrays.fill(a, value);
      }
   }

   public static void fill(float[][] floats, float value) {
      for (float[] a : floats) {
         Arrays.fill(a, value);
      }
   }

   public static void fill(double[][] doubles, double value) {
      for (double[] a : doubles) {
         Arrays.fill(a, value);
      }
   }

   public static void fill(int[][] ints, int value) {
      for (int[] a : ints) {
         Arrays.fill(a, value);
      }
   }

   public static float[][] copy(float[][] array) {
      float[][] result = new float[array.length][];
      for (int i = 0; i < array.length; i++) {
         result[i] = array[i].clone();
      }
      return result;
   }

   public static float[][] toFloats(double[][] array) {
      float[][] result = new float[array.length][];
      for (int i = 0; i < array.length; i++) {
         result[i] = toFloats(array[i]);
      }
      return result;
   }

   public static float[] toFloats(double[] array) {
      float[] result = new float[array.length];
      for (int i = 0; i < array.length; i++) {
         result[i] = (float) array[i];
      }
      return result;
   }

   public static float[] toFloats(int[] array) {
      float[] result = new float[array.length];
      for (int i = 0; i < array.length; i++) {
         result[i] = array[i];
      }
      return result;
   }

   public static int[] toInts(Collection<? extends Number> numbers) {
      int[] result = new int[numbers.size()];
      int i = 0;
      for (Number number : numbers) {
         result[i++] = number.intValue();
      }
      return result;
   }

   public static long[] toLongs(Collection<? extends Number> numbers) {
      long[] result = new long[numbers.size()];
      int i = 0;
      for (Number number : numbers) {
         result[i++] = number.longValue();
      }
      return result;
   }

   public static float[] toFloats(Collection<? extends Number> numbers) {
      float[] result = new float[numbers.size()];
      int i = 0;
      for (Number number : numbers) {
         result[i++] = number.floatValue();
      }
      return result;
   }

   public static double[] toDoubles(Collection<? extends Number> numbers) {
      double[] result = new double[numbers.size()];
      int i = 0;
      for (Number number : numbers) {
         result[i++] = number.doubleValue();
      }
      return result;
   }

   public static double[] toDoubles(float[] array) {
      double[] result = new double[array.length];
      for (int i = 0; i < array.length; i++) {
         result[i] = array[i];
      }
      return result;
   }

   public static <T> List<Optional<T>> toOptionals(Collection<@Nullable T> nullableValues) {
      return nullableValues.stream()
            .map(Optional::ofNullable)
            .toList();
   }

   public static <T> List<T> toList(Collection<? extends T> a, Collection<? extends T> b) {
      return Stream.concat(a.stream(), b.stream()).toList();
   }

   /**
    * Compare two collections.
    *
    * @param a a collection
    * @param b another collection
    * @param c the comparator to use for comparing elements from a with elements from b
    * @return the comparison of the first pair of elements different from 0, or comparison based on sizes
    */
   public static <T> int compare(Collection<? extends T> a, Collection<? extends T> b, Comparator<? super T> c) {
      Iterator<? extends T> aIterator = a.iterator();
      Iterator<? extends T> bIterator = b.iterator();

      while (aIterator.hasNext() && bIterator.hasNext()) {
         int comparison = c.compare(aIterator.next(), bIterator.next());
         if (comparison != 0) {
            return comparison;
         }
      }

      return Integer.compare(a.size(), b.size());
   }

   public static <T extends Comparable<? super T>> Collection<T> sorted(Collection<T> collection) {
      if (collection.size() < 2) {
         return collection;
      }
      List<T> sorted = new ArrayList<>(collection);
      sorted.sort(null);
      return sorted;
   }

   /**
    * Returns a shifted value.
    *
    * @param allValues all values
    * @param value     the reference value
    * @param shift     the shift amount
    * @return the shifted value
    */
   public static <T> T shift(List<? extends T> allValues, T value, int shift) {
      int i = allValues.indexOf(value);
      int iResult;
      if (i >= 0) {
         iResult = i + shift;
      } else {
         if (allValues.isEmpty() || shift == 0) {
            return value;
         }
         iResult = shift > 0 ? shift - 1 : shift;
      }
      return allValues.get(MathUtils.mod(iResult, allValues.size()));
   }

   /**
    * Returns another enum value.
    *
    * @param value the reference enum value
    * @param shift the shift amount
    * @return the shifted enum value
    */
   public static <T extends Enum<T>> T shift(T value, int shift) {
      T[] values = value.getDeclaringClass().getEnumConstants();
      return values[MathUtils.mod(value.ordinal() + shift, values.length)];
   }

   public static <T extends Comparable<? super T>> T clamp(T value, T min, T max) {
      if (value.compareTo(min) < 0) {
         return min;
      }
      if (value.compareTo(max) > 0) {
         return max;
      }
      return value;
   }

   public static <T> Consumer<T> emptyConsumer() {
      return _ -> {
      };
   }

   public static <T> Predicate<T> greaterThan(Comparator<? super T> comparator, T referenceValue) {
      return value -> comparator.compare(value, referenceValue) > 0;
   }

   public static <T> void toggle(Set<T> set, Collection<T> items) {
      items.forEach(item -> {
         toggle(set, item);
      });
   }

   public static <T> void toggle(Set<T> set, T item) {
      if (!set.add(item)) {
         set.remove(item);
      }
   }

   public static <T> Predicate<T> distinctBy(Function<? super T, ?> extractor) {
      Set<Object> set = ConcurrentHashMap.newKeySet();
      return item -> set.add(extractor.apply(item));
   }

   public static <T> T getFirstOrThrow(Collection<?> collection, Class<T> clazz) {
      T result = getFirstOrNull(collection, clazz);
      if (result == null) {
         throw new NoSuchElementException(clazz.getName());
      }
      return result;
   }

   public static <T> @Nullable T getFirstOrNull(Collection<?> collection, Class<T> clazz) {
      for (Object object : collection) {
         if (clazz.isInstance(object)) {
            return clazz.cast(object);
         }
      }
      return null;
   }

   public static <R> Gatherer<@Nullable Object, ?, R> allOfType(Class<R> clazz) {
      return Gatherer.of(Gatherer.Integrator.ofGreedy((_, element, downstream) -> {
         return !clazz.isInstance(element) || downstream.push(clazz.cast(element));
      }));
   }

   public static <T> Stream<T> getAllOfType(Collection<?> collection, Class<T> clazz) {
      return collection.stream()
            .gather(allOfType(clazz));
   }

   public static <T> Stream<T> recursiveStream(T top, Function<T, Collection<? extends T>> childrenExtractor) {
      return Stream.concat(
            Stream.of(top),
            childrenExtractor.apply(top).stream().flatMap(child -> recursiveStream(child, childrenExtractor)));
   }

   public static <T> Collector<T, ?, List<T>> toSortedListCollector() {
      return Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), list -> {
         list.sort(null);
         return list;
      });
   }

   public static <T> Collector<T, ?, List<T>> toSortedListCollector(Comparator<? super T> c) {
      return Collectors.collectingAndThen(Collectors.toCollection(ArrayList::new), list -> {
         list.sort(c);
         return list;
      });
   }

   public static <T> @Nullable T nextOrNull(Iterator<T> iterator) {
      return iterator.hasNext() ? iterator.next() : null;
   }

   public static <T extends @Nullable Object> @Nullable T getOrNull(T[] array, int index) {
      return index >= 0 && index < array.length ? array[index] : null;
   }

   public static <T> T getOrDefault(T[] array, int index, T defaultValue) {
      return index >= 0 && index < array.length ? array[index] : defaultValue;
   }

   public static <T> int binarySearchForInt(List<T> list, int value, ToIntFunction<T> function) {
      return Collections.<@Nullable T>binarySearch(list, null, Comparator.comparingInt(a -> a != null ? function.applyAsInt(a) : value));
   }

   public static <T> int binarySearchForLong(List<T> list, long value, ToLongFunction<T> function) {
      return Collections.<@Nullable T>binarySearch(list, null, Comparator.comparingLong(a -> a != null ? function.applyAsLong(a) : value));
   }

   public static <T> int binarySearchForDouble(List<T> list, double value, ToDoubleFunction<T> function) {
      return Collections.<@Nullable T>binarySearch(list, null, Comparator.comparingDouble(a -> a != null ? function.applyAsDouble(a) : value));
   }

   public static <T, K extends Comparable<? super K>> int binarySearch(List<T> list, K value, Function<T, K> function) {
      return Collections.<@Nullable T>binarySearch(list, null, Comparator.comparing(a -> a != null ? function.apply(a) : value));
   }

   /**
    * Returns a format string on the form "%.xf" suitable to use with String.format.
    *
    * @param x a value
    * @return a format string
    * @see #getPrecisionString(double)
    */
   public static String getPrecisionString(float x) {
      return getPrecisionString((double) Math.nextUp(Math.abs(x)));
   }

   /**
    * Returns a format string on the form "%.xf" suitable to use with String.format.
    *
    * @param x a value
    * @return a format string
    */
   public static String getPrecisionString(double x) {
      int precision = Math.max(0, (int) -Math.floor(Math.log10(Math.nextUp(Math.abs(x)))));
      return "%." + precision + "f";
   }

   /**
    * Reformats a number by removing trailing zeros and possibly the decimal point.
    *
    * @param number a number
    * @return the reformatted number
    */
   public static String removeTrailingZeros(String number) {
      for (int i = 0; i < number.length(); i++) {
         char ci = number.charAt(i);
         if (ci == '.' || ci == ',') {
            int removeBegin = i;
            for (int j = i + 1; j < number.length(); j++) {
               char cj = number.charAt(j);
               if (cj == 'E' || cj == 'e') {
                  return removeBegin == j
                        ? number
                        : number.substring(0, removeBegin) + number.substring(j);
               } else if (cj != '0') {
                  removeBegin = j + 1;
               }
            }
            return removeBegin == number.length()
                  ? number
                  : number.substring(0, removeBegin);
         }
      }
      return number;
   }

   /**
    * Formats a number nicely.
    *
    * @param value a value
    * @return the corresponding string value
    */
   public static String numberToString(double value) {
      return numberToString(value, true);
   }

   /**
    * Formats a number nicely.
    *
    * @param value               a value
    * @param removeTrailingZeros if trailing zeros should be removed
    * @return the corresponding string value
    */
   public static String numberToString(double value, boolean removeTrailingZeros) {
      if (value == 0) {
         return "0";
      }

      int precision;
      double d = Math.log10(Math.abs(value));
      if (d >= 0) {
         precision = Math.max(0, 2 - (int) Math.floor(d));
      } else {
         precision = Math.max(2, (int) Math.floor(-d) + 1);
      }

      String result = format("%." + precision + "f", value);
      return removeTrailingZeros ? removeTrailingZeros(result) : result;
   }

   /**
    * Converts a string to a double without throwing NumberFormatException.
    *
    * @param string a string
    * @return the parsed double, or {@code null} if parse error
    */
   public static @Nullable Double stringToNumber(String string) {
      try {
         string = string.replace(',', '.');
         return Double.valueOf(string);
      } catch (NumberFormatException _) {
         return null;
      }
   }

   /**
    * Converts a float to a string without the trailing ".0" returned by {@link Float#toString(float)}.
    *
    * @param value a value
    * @return the corresponding string
    */
   public static String toString(float value) {
      if (value == 0) {
         return "0";
      }
      return removeTrailingZeros(Float.toString(value));
   }

   /**
    * Converts a double to a string without the trailing ".0" returned by {@link Double#toString(double)}.
    *
    * @param value a value
    * @return the corresponding string
    */
   public static String toString(double value) {
      if (value == 0) {
         return "0";
      }
      return removeTrailingZeros(Double.toString(value));
   }

   /**
    * Wrapper method in order to call {@link String#format(Locale, String, Object...)} with no localization applied.
    *
    * @param format format string
    * @param args   arguments
    * @return a formatted string
    */
   public static String format(String format, Object... args) {
      return String.format(Locale.ROOT, format, args);
   }

   /**
    * Creates a non-localized instance of {@link DecimalFormatSymbols} with decimal separator "." and minus sign "-".
    *
    * @return a new instance of {@link DecimalFormatSymbols}
    */
   public static DecimalFormatSymbols createDecimalFormatSymbols() {
      return new DecimalFormatSymbols(Locale.ROOT);
   }

   public static DecimalFormat createDecimalFormat(String pattern) {
      return new DecimalFormat(pattern, createDecimalFormatSymbols());
   }

   public static String nameAndUnit(String name, Unit unit) {
      return nameAndUnit(name, unit.text());
   }

   public static String nameAndUnit(String name, String unit) {
      return unit.isEmpty() ? name : name + " [" + unit + ']';
   }

   /**
    * Returns a human-readable size string, rounded to about 3 significant digits, with a unit.
    *
    * @param bytes number of bytes
    * @return a string with size and unit
    */
   public static String getByteSizeString(long bytes) {
      if (bytes < 1000) {
         return bytes + " B";
      }

      String units = "KMGTPE"; // Kilo, Mega, Giga, Tera, Peta, Exa. Long.MAX_VALUE = 8 EB
      double x = bytes / 1024.0;
      int i = 0;
      while (x > 999.5) {
         x /= 1024;
         i++;
      }

      return numberToString(x, false) + ' ' + units.charAt(i) + 'B';
   }

   public static MessageDigest getSha256() {
      try {
         return MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException e) {
         throw new ShouldNotHappenException(e);
      }
   }

   public static URL toURL(String url) {
      try {
         return URI.create(url).toURL();
      } catch (MalformedURLException e) {
         throw new IllegalArgumentException(url, e);
      }
   }

   public static @Nullable String getSystemPropertyOrEnv(String key) {
      String property = System.getProperty(key);
      if (property != null) {
         return property;
      }
      return System.getenv(key);
   }

   public static String getSystemProperties() {
      StringBuilder sb = new StringBuilder(16384).append("System properties:");
      System.getProperties().forEach((key, value) -> {
         sb.append('\n').append(key).append('=').append(value);
      });
      sb
            .append("\nGit commit=").append(GIT_COMMIT)
            .append("\nMax memory=").append(Runtime.getRuntime().maxMemory())
            .append("\nProcessors=").append(Runtime.getRuntime().availableProcessors())
            .append("\nNow=").append(Instant.now());
      return sb.toString();
   }

   public static String getUserName() {
      return System.getProperty("user.name");
   }

   public static Path getUserHome() {
      return Path.of(System.getProperty("user.home"));
   }

   public static Path getTmpDir() {
      if (isTestRun()) {
         return UniqueTmpDir.get().resolve("marec");
      } else {
         return Path.of(System.getProperty("java.io.tmpdir"), "marec");
      }
   }

   public static String stackTraceToString(Throwable throwable) {
      StringWriter stringWriter = new StringWriter();
      try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
         throwable.printStackTrace(printWriter);
      }
      return stringWriter.toString();
   }

   @SafeVarargs
   public static <E extends Throwable> void closeAll(ThrowingRunnable<? extends E>... closeables) throws E {
      Throwable exception = null;
      for (ThrowingRunnable<? extends E> closeable : closeables) {
         try {
            closeable.run();
         } catch (Throwable e) {
            if (exception == null) {
               exception = e;
            } else {
               exception.addSuppressed(e);
            }
         }
      }
      if (exception != null) {
         @SuppressWarnings("unchecked")
         E e = (E) exception;
         throw e;
      }
   }

   public static void closeOrSuppress(Throwable e, @Nullable AutoCloseable closeable) {
      if (closeable != null) {
         try {
            closeable.close();
         } catch (Throwable suppressed) {
            e.addSuppressed(suppressed);
         }
      }
   }

   public static <E extends Throwable> void tryAndCleanup(ThrowingRunnable<? extends E> task, ThrowingRunnable<? extends E> cleanup) throws E {
      try {
         task.run();
      } catch (Throwable e) {
         try {
            cleanup.run();
         } catch (Throwable suppressed) {
            e.addSuppressed(suppressed);
         }
         throw e;
      }
      cleanup.run();
   }

   public static String readBuildVersion(String key) {
      if (IS_BUILT_VERSION) {
         throw new IllegalStateException();
      }
      try {
         String s = Files.readString(LoggingManager.getTopInstallationDir().resolve("buildSrc/src/main/kotlin/no/marec/gradle/BuildVersions.kt"));
         Matcher matcher = Pattern.compile("\\b" + key + "\\b[^=]*= \"([^\"]+)\"").matcher(s);
         if (matcher.find()) {
            return matcher.group(1);
         }
         throw new IllegalStateException(key);
      } catch (IOException e) {
         throw new IllegalStateException(key, e);
      }
   }

   public static void waitForEnter() throws IOException {
      while (true) {
         int b = System.in.read();
         if (b == '\n' || b == -1) {
            break;
         }
      }
   }

   public static boolean isMainRun() {
      return mainRun;
   }

   public static boolean isSmokeTestRun() {
      return smokeTestRun;
   }

   public static boolean isTestRun() {
      return !mainRun || smokeTestRun;
   }

   public static boolean useTestFeatures() {
      return isTestRun() || debugRun;
   }

   public static void init(String[] args) {
      nativeCharset = Charset.forName(System.getProperty("native.encoding", ""), UTF_8);
      List<String> argsList = Arrays.asList(args);
      mainRun = true;
      debugRun = argsList.contains("--debug");
      smokeTestRun = argsList.contains("--smoke-test");
   }

   public static void init(String[] args, Image taskbarIcon) {
      init(args);
      SwingUtilities.invokeLater(() -> {
         UiUtils.initDefaults();
         GuiUtils.taskbarSetIconImage(taskbarIcon);
      });
   }
}
