package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.time.NTDate;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.util.ByteBufferBackedInputStream;
import tools.jackson.databind.util.ByteBufferBackedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * ByteBuffer utilities.
 */
public final class ByteBufferUtils {
   public static final int INITIAL_CAPACITY = 8 * 1024;
   private static final ThreadLocal<@Nullable SoftReference<ByteBuffer>> THREAD_LOCAL_BYTE_BUFFER = new ThreadLocal<>();

   private ByteBufferUtils() {
   }

   public static ByteBuffer toByteBuffer(Path file) throws IOException {
      return FileUtils.toByteBuffer(file, ByteOrder.LITTLE_ENDIAN);
   }

   public static ByteBuffer getThreadLocalByteBuffer(int size) {
      SoftReference<ByteBuffer> reference = THREAD_LOCAL_BYTE_BUFFER.get();
      ByteBuffer byteBuffer = reference != null ? reference.get() : null;
      if (byteBuffer == null || byteBuffer.capacity() < size) {
         byteBuffer = allocate((int) Math.min(Integer.MAX_VALUE, (long) size + (size >> 1))); // Allocate a little bit extra
         THREAD_LOCAL_BYTE_BUFFER.set(new SoftReference<>(byteBuffer));
      }
      byteBuffer.position(0);
      byteBuffer.limit(size);
      return byteBuffer;
   }

   public static ByteBuffer allocate(int capacity) {
      return ByteBuffer.allocate(capacity)
            .order(ByteOrder.LITTLE_ENDIAN);
   }

   public static ByteBuffer toByteBuffer(Consumer<ByteBuffer> writer) {
      int capacity = INITIAL_CAPACITY;
      while (true) {
         try {
            ByteBuffer byteBuffer = allocate(capacity);
            writer.accept(byteBuffer);
            byteBuffer.flip();
            return byteBuffer;
         } catch (BufferOverflowException _) {
            capacity *= 2;
         }
      }
   }

   public static void skip(ByteBuffer byteBuffer, int bytesToSkip) {
      if (byteBuffer.remaining() < bytesToSkip) {
         throw new BufferUnderflowException();
      }
      byteBuffer.position(byteBuffer.position() + bytesToSkip);
   }

   public static int readCount(ByteBuffer byteBuffer, int minimumBytesPerItem) throws DatagramFormatException {
      int count = byteBuffer.getInt();
      checkCount(count, byteBuffer, minimumBytesPerItem);
      return count;
   }

   public static void checkCount(int count, ByteBuffer byteBuffer, int minimumBytesPerItem) throws DatagramFormatException {
      if (count < 0 || count > byteBuffer.remaining() / minimumBytesPerItem) {
         throw new DatagramFormatException("count: " + count);
      }
   }

   public static int readByteSize(ByteBuffer byteBuffer) throws DatagramFormatException {
      int size = byteBuffer.getInt();
      if (size < 0 || size > byteBuffer.remaining() + 4) {
         throw new DatagramFormatException("size: " + size);
      }
      return size;
   }

   //-------- String

   /**
    * Read data as a null terminated 'C' string, length unknown.
    *
    * @param byteBuffer buffer to read from
    * @return a string
    */
   public static String readCString(ByteBuffer byteBuffer) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      while (true) {
         byte b = byteBuffer.get();
         if (b == 0) {
            break;
         }
         out.write(b);
      }
      return out.toString(Utils.ISO_8859_1);
   }

   /**
    * Read data as a null terminated 'C' string, length known.
    *
    * @param byteBuffer buffer to read from
    * @param length     number of characters to read
    * @return a string
    */
   public static String readCString(ByteBuffer byteBuffer, int length) {
      byte[] bytes = new byte[length];

      // Read all bytes
      byteBuffer.get(bytes);

      // Find null terminator
      int k = 0;
      while (k < length && bytes[k] != 0) {
         k++;
      }
      return new String(bytes, 0, k, Utils.ISO_8859_1);
   }

   /**
    * Write string as null terminated 'C' string, write only until length.
    *
    * @param byteBuffer buffer to put data
    * @param string     string to write
    */
   public static void writeCString(ByteBuffer byteBuffer, String string) {
      writeCString(byteBuffer, string, string.length() + 1); // + 1 to make room for 0 terminator
   }

   /**
    * Write string as null terminated 'C' string with given length.
    *
    * @param byteBuffer buffer to put data
    * @param string     string to write
    * @param length     length to be written
    */
   public static void writeCString(ByteBuffer byteBuffer, String string, int length) {
      // Use getBytes instead of casting char to byte because of encoding of char > 255.
      byte[] bytes = string.getBytes(Utils.ISO_8859_1);
      int stringLength = Math.min(bytes.length, length);
      byteBuffer.put(bytes, 0, stringLength);

      // Pad with zeros
      for (int i = stringLength; i < length; i++) {
         byteBuffer.put((byte) 0);
      }
   }

   public static String readLengthAndUtf16String(ByteBuffer byteBuffer) {
      int length = 0xffff & byteBuffer.getShort();
      char[] chars = new char[length];
      byteBuffer.asCharBuffer().get(chars);
      byteBuffer.position(byteBuffer.position() + 2 * length);
      return new String(chars);
   }

   public static void writeLengthAndUtf16String(ByteBuffer byteBuffer, String string) {
      byteBuffer.putShort((short) string.length());
      byteBuffer.asCharBuffer().put(string);
      byteBuffer.position(byteBuffer.position() + 2 * string.length());
   }

   //-------- Byte

   public static byte[] readByteArray(ByteBuffer byteBuffer, int count) {
      byte[] values = new byte[count];
      byteBuffer.get(values);
      return values;
   }

   //-------- Short

   public static short[] readShortArray(ByteBuffer byteBuffer, int count) {
      short[] values = new short[count];
      readShortArray(byteBuffer, values);
      return values;
   }

   public static void readShortArray(ByteBuffer byteBuffer, short[] values) {
      byteBuffer.asShortBuffer().get(values);
      byteBuffer.position(byteBuffer.position() + 2 * values.length);
   }

   public static void writeShortArray(ByteBuffer byteBuffer, short[] values) {
      byteBuffer.asShortBuffer().put(values);
      byteBuffer.position(byteBuffer.position() + 2 * values.length);
   }

   public static short[] readCountAndShortArray(ByteBuffer byteBuffer) throws DatagramFormatException {
      int count = readCount(byteBuffer, 2);
      return readShortArray(byteBuffer, count);
   }

   public static void writeCountAndShortArray(ByteBuffer byteBuffer, short[] values) {
      byteBuffer.putInt(values.length);
      writeShortArray(byteBuffer, values);
   }

   //-------- Int

   public static int[] readIntArray(ByteBuffer byteBuffer, int count) {
      int[] values = new int[count];
      readIntArray(byteBuffer, values);
      return values;
   }

   public static void readIntArray(ByteBuffer byteBuffer, int[] values) {
      byteBuffer.asIntBuffer().get(values);
      byteBuffer.position(byteBuffer.position() + 4 * values.length);
   }

   public static void writeIntArray(ByteBuffer byteBuffer, int[] values) {
      byteBuffer.asIntBuffer().put(values);
      byteBuffer.position(byteBuffer.position() + 4 * values.length);
   }

   public static int[] readCountAndIntArray(ByteBuffer byteBuffer) throws DatagramFormatException {
      int count = readCount(byteBuffer, 4);
      return readIntArray(byteBuffer, count);
   }

   public static void writeCountAndIntArray(ByteBuffer byteBuffer, int[] values) {
      byteBuffer.putInt(values.length);
      writeIntArray(byteBuffer, values);
   }

   //-------- Long

   public static long[] readLongArray(ByteBuffer byteBuffer, int count) {
      long[] values = new long[count];
      readLongArray(byteBuffer, values);
      return values;
   }

   public static void readLongArray(ByteBuffer byteBuffer, long[] values) {
      byteBuffer.asLongBuffer().get(values);
      byteBuffer.position(byteBuffer.position() + 8 * values.length);
   }

   public static void writeLongArray(ByteBuffer byteBuffer, long[] values) {
      byteBuffer.asLongBuffer().put(values);
      byteBuffer.position(byteBuffer.position() + 8 * values.length);
   }

   public static long[] readCountAndLongArray(ByteBuffer byteBuffer) throws DatagramFormatException {
      int count = readCount(byteBuffer, 8);
      return readLongArray(byteBuffer, count);
   }

   public static void writeCountAndLongArray(ByteBuffer byteBuffer, long[] values) {
      byteBuffer.putInt(values.length);
      writeLongArray(byteBuffer, values);
   }

   //-------- Float

   public static float[] readFloatArray(ByteBuffer byteBuffer, int count) {
      float[] values = new float[count];
      readFloatArray(byteBuffer, values);
      return values;
   }

   public static void readFloatArray(ByteBuffer byteBuffer, float[] values) {
      byteBuffer.asFloatBuffer().get(values);
      byteBuffer.position(byteBuffer.position() + 4 * values.length);
   }

   public static void writeFloatArray(ByteBuffer byteBuffer, float[] values) {
      byteBuffer.asFloatBuffer().put(values);
      byteBuffer.position(byteBuffer.position() + 4 * values.length);
   }

   public static float[] readCountAndFloatArray(ByteBuffer byteBuffer) throws DatagramFormatException {
      int count = readCount(byteBuffer, 4);
      return readFloatArray(byteBuffer, count);
   }

   public static void writeCountAndFloatArray(ByteBuffer byteBuffer, float[] values) {
      byteBuffer.putInt(values.length);
      writeFloatArray(byteBuffer, values);
   }

   //-------- Double

   public static double[] readDoubleArray(ByteBuffer byteBuffer, int count) {
      double[] values = new double[count];
      readDoubleArray(byteBuffer, values);
      return values;
   }

   public static void readDoubleArray(ByteBuffer byteBuffer, double[] values) {
      byteBuffer.asDoubleBuffer().get(values);
      byteBuffer.position(byteBuffer.position() + 8 * values.length);
   }

   public static void writeDoubleArray(ByteBuffer byteBuffer, double[] values) {
      byteBuffer.asDoubleBuffer().put(values);
      byteBuffer.position(byteBuffer.position() + 8 * values.length);
   }

   public static double[] readCountAndDoubleArray(ByteBuffer byteBuffer) throws DatagramFormatException {
      int count = readCount(byteBuffer, 8);
      return readDoubleArray(byteBuffer, count);
   }

   public static void writeCountAndDoubleArray(ByteBuffer byteBuffer, double[] values) {
      byteBuffer.putInt(values.length);
      writeDoubleArray(byteBuffer, values);
   }

   //-------- List

   public static <T> List<T> readCountAndList(ByteBuffer byteBuffer, int minimumBytesPerItem, ByteBufferReader<T> reader) throws DatagramFormatException {
      int count = readCount(byteBuffer, minimumBytesPerItem);
      List<T> list = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
         list.add(reader.read(byteBuffer));
      }
      return list;
   }

   public static <T> void writeCountAndList(ByteBuffer byteBuffer, List<T> list, ByteBufferWriter<T> writer) {
      byteBuffer.putInt(list.size());
      list.forEach(item -> writer.write(item, byteBuffer));
   }

   //-------- JSON

   public static <T> T readJson(ByteBuffer byteBuffer, Class<T> clazz) throws DatagramFormatException {
      try {
         return JsonUtils.JSON_MAPPER.readValue(new ByteBufferBackedInputStream(byteBuffer), clazz);
      } catch (Exception e) {
         throw new DatagramFormatException(e);
      }
   }

   public static void writeJson(ByteBuffer byteBuffer, Object value) {
      JsonUtils.JSON_MAPPER.writeValue(new ByteBufferBackedOutputStream(byteBuffer), value);
   }

   //-------- Vec3

   public static Vec3 readVec3(ByteBuffer byteBuffer) {
      float x = byteBuffer.getFloat();
      float y = byteBuffer.getFloat();
      float z = byteBuffer.getFloat();
      return new Vec3(x, y, z);
   }

   public static void writeVec3(ByteBuffer byteBuffer, Vec3 vec3) {
      byteBuffer.putFloat(vec3.x());
      byteBuffer.putFloat(vec3.y());
      byteBuffer.putFloat(vec3.z());
   }

   //-------- FloatRange

   public static FloatRange readFloatRange(ByteBuffer byteBuffer) throws DatagramFormatException {
      float min = byteBuffer.getFloat();
      float max = byteBuffer.getFloat();
      if (min > max) {
         throw new DatagramFormatException(min + " > " + max);
      }
      return FloatRange.of(min, max);
   }

   public static void writeFloatRange(ByteBuffer byteBuffer, FloatRange range) {
      byteBuffer.putFloat(range.min());
      byteBuffer.putFloat(range.max());
   }

   //-------- Instants

   public static List<Instant> readInstantsAsNTDates(ByteBuffer byteBuffer) throws DatagramFormatException {
      long[] ntDates = readCountAndLongArray(byteBuffer);
      return Arrays.stream(ntDates)
            .mapToObj(NTDate::ntDateToInstant)
            .toList();
   }

   public static void writeInstantsAsNTDates(ByteBuffer byteBuffer, List<Instant> instants) {
      long[] ntDates = instants.stream()
            .mapToLong(NTDate::instantToNTDate)
            .toArray();
      writeCountAndLongArray(byteBuffer, ntDates);
   }
}
