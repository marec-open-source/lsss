package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.Test;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class ByteBufferUtilsTest {
   @Test
   void test() {
      test("");
      test("-+.;/\\ 123 abc");
   }

   private static void test(String string) {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[string.length() + 10]);
      ByteBufferUtils.writeCString(buffer, string);
      assertEquals(string.length() + 1, buffer.position());

      buffer.flip();
      assertEquals(string, ByteBufferUtils.readCString(buffer));
      assertEquals(string.length() + 1, buffer.position());
      assertEquals(0, buffer.remaining());
   }

   @Test
   void read0() {
      assertThrows(BufferUnderflowException.class, () -> {
         ByteBufferUtils.readCString(ByteBuffer.wrap(new byte[]{65, 66}));
      });
   }

   @Test
   void read1() {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[]{65, 66, 0});
      assertEquals("AB", ByteBufferUtils.readCString(buffer));
      assertEquals(0, buffer.remaining());
   }

   @Test
   void read2() {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[]{65, 66});
      assertEquals("A", ByteBufferUtils.readCString(buffer, 1));
      assertEquals(1, buffer.remaining());
   }

   @Test
   void write1() {
      byte[] bytes = {-1, -1, -1, -1};
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      ByteBufferUtils.writeCString(buffer, "AB");
      assertEquals(3, buffer.position());
      assertArrayEquals(new byte[]{65, 66, 0, -1}, bytes);
   }

   @Test
   void write2() {
      testWrite(new byte[]{-1, -1, -1, -1}, "AB", 0);
      testWrite(new byte[]{65, -1, -1, -1}, "AB", 1);
      testWrite(new byte[]{65, 66, -1, -1}, "AB", 2);
      testWrite(new byte[]{65, 66, 0, -1}, "AB", 3);
      testWrite(new byte[]{65, 66, 0, 0}, "AB", 4);
   }

   private static void testWrite(byte[] expectedBytes, String string, int length) {
      byte[] bytes = {-1, -1, -1, -1};
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      ByteBufferUtils.writeCString(buffer, string, length);
      assertEquals(length, buffer.position());
      assertArrayEquals(expectedBytes, bytes);
   }

   @Test
   void utf16String() {
      String s = "A å ★";
      int size = 2 + 2 * s.length(); // Length is written as a short.
      ByteBuffer buffer = ByteBufferUtils.allocate(size + 10);
      ByteBufferUtils.writeLengthAndUtf16String(buffer, s);
      assertEquals(size, buffer.position());
      buffer.flip();
      assertEquals(s, ByteBufferUtils.readLengthAndUtf16String(buffer));
      assertEquals(size, buffer.position());
   }

   @Test
   void shortArray() throws DatagramFormatException {
      short[] values = {1, 0, -100, 999, Short.MIN_VALUE, Short.MAX_VALUE, -6};
      int size = 4 + 2 * values.length;
      ByteBuffer buffer = ByteBufferUtils.allocate(size + 10);
      ByteBufferUtils.writeCountAndShortArray(buffer, values);
      assertEquals(size, buffer.position());
      buffer.flip();
      assertArrayEquals(values, ByteBufferUtils.readCountAndShorArray(buffer));
      assertEquals(size, buffer.position());
   }

   @Test
   void intArray() throws DatagramFormatException {
      int[] values = {1, 0, -100, 999, Integer.MIN_VALUE, Integer.MAX_VALUE, -6};
      int size = 4 + 4 * values.length;
      ByteBuffer buffer = ByteBufferUtils.allocate(size + 10);
      ByteBufferUtils.writeCountAndIntArray(buffer, values);
      assertEquals(size, buffer.position());
      buffer.flip();
      assertArrayEquals(values, ByteBufferUtils.readCountAndIntArray(buffer));
      assertEquals(size, buffer.position());
   }

   @Test
   void longArray() throws DatagramFormatException {
      long[] values = {1, 0, -100, 999, Long.MIN_VALUE, Long.MAX_VALUE, -6};
      int size = 4 + 8 * values.length;
      ByteBuffer buffer = ByteBufferUtils.allocate(size + 10);
      ByteBufferUtils.writeCountAndLongArray(buffer, values);
      assertEquals(size, buffer.position());
      buffer.flip();
      assertArrayEquals(values, ByteBufferUtils.readCountAndLongArray(buffer));
      assertEquals(size, buffer.position());
   }

   @Test
   void floatArray() throws DatagramFormatException {
      float[] values = {1, 0, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN, Float.MIN_VALUE, Float.MAX_VALUE, -6};
      int size = 4 + 4 * values.length;
      ByteBuffer buffer = ByteBufferUtils.allocate(size + 10);
      ByteBufferUtils.writeCountAndFloatArray(buffer, values);
      assertEquals(size, buffer.position());
      buffer.flip();
      assertArrayEquals(values, ByteBufferUtils.readCountAndFloatArray(buffer));
      assertEquals(size, buffer.position());
   }

   @Test
   void doubleArray() throws DatagramFormatException {
      double[] values = {1, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN, Double.MIN_VALUE, Double.MAX_VALUE, -6};
      int size = 4 + 8 * values.length;
      ByteBuffer buffer = ByteBufferUtils.allocate(size + 10);
      ByteBufferUtils.writeCountAndDoubleArray(buffer, values);
      assertEquals(size, buffer.position());
      buffer.flip();
      assertArrayEquals(values, ByteBufferUtils.readCountAndDoubleArray(buffer));
      assertEquals(size, buffer.position());
   }

   @Test
   void list() throws DatagramFormatException {
      List<Integer> list = List.of(-1, 0, 1);
      int size = 4 + 4 * list.size();
      ByteBuffer buffer = ByteBufferUtils.allocate(size + 10);
      ByteBufferUtils.writeCountAndList(buffer, list, (item, byteBuffer) -> byteBuffer.putInt(item));
      assertEquals(size, buffer.position());
      buffer.flip();
      assertEquals(list, ByteBufferUtils.readCountAndList(buffer, 4, ByteBuffer::getInt));
      assertEquals(size, buffer.position());
   }

   @Test
   void json() throws DatagramFormatException {
      Object obj = Map.of("a", 1, "b", List.of("x", 0));
      ByteBuffer buffer = ByteBufferUtils.allocate(100);
      ByteBufferUtils.writeJson(buffer, obj);
      buffer.flip();
      assertEquals(obj, ByteBufferUtils.readJson(buffer, Map.class));
   }

   @Test
   void vec3() {
      Vec3 vec = new Vec3(1, 0, Float.POSITIVE_INFINITY);
      int size = 12;
      ByteBuffer buffer = ByteBufferUtils.allocate(size + 10);
      ByteBufferUtils.writeVec3(buffer, vec);
      assertEquals(size, buffer.position());
      buffer.flip();
      assertEquals(vec, ByteBufferUtils.readVec3(buffer));
      assertEquals(size, buffer.position());
   }

   @Test
   void floatRange() throws DatagramFormatException {
      FloatRange range = FloatRange.of(1, 2);
      int size = 8;
      ByteBuffer buffer = ByteBufferUtils.allocate(size + 10);
      ByteBufferUtils.writeFloatRange(buffer, range);
      assertEquals(size, buffer.position());
      buffer.flip();
      assertEquals(range, ByteBufferUtils.readFloatRange(buffer));
      assertEquals(size, buffer.position());
   }
}
