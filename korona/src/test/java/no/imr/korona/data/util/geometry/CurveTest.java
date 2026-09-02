package no.imr.korona.data.util.geometry;

import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

final class CurveTest {
   @Test
   void copyOf() {
      Curve curve = newCurve(10, 20, 10, 20);
      assertArrayEquals(new float[]{0, 0}, curve.copyOf(newPingRange(8, 10)).getDepths());
      assertArrayEquals(new float[]{0, 0, 10, 11}, curve.copyOf(newPingRange(8, 12)).getDepths());
      assertArrayEquals(new float[]{12, 13, 14}, curve.copyOf(newPingRange(12, 15)).getDepths());
      assertArrayEquals(new float[]{19, 0}, curve.copyOf(newPingRange(19, 21)).getDepths());
      assertArrayEquals(new float[]{0, 0, 0}, curve.copyOf(newPingRange(22, 25)).getDepths());
   }

   @Test
   void adjustByCurve() {
      Curve curve = newCurve(10, 20, -1);
      curve.adjust(newCurve(5, 12, 5, 12));
      curve.adjust(newCurve(15, 17, 15, 17));
      curve.adjust(newCurve(19, 25, 19, 25));
      assertArrayEquals(new float[]{10, 11, -1, -1, -1, 15, 16, -1, -1, 19}, curve.getDepths());
   }

   @Test
   void adjustByCurveAndOperator() {
      Curve curve = newCurve(10, 20, 5);
      curve.adjust(newCurve(5, 10, 7), Float::max);
      curve.adjust(newCurve(5, 15, 6), Float::max);
      curve.adjust(newCurve(18, 22, 4), Float::min);
      assertArrayEquals(new float[]{6, 6, 6, 6, 6, 5, 5, 5, 4, 4}, curve.getDepths());
   }

   private static PingIndex newPingIndex(long pingNumber) {
      return new DefaultPingIndex(Instant.ofEpochSecond(pingNumber), pingNumber, 0, null);
   }

   private static PingRange newPingRange(long beginPingNumber, long endPingNumber) {
      return PingRange.of(newPingIndex(beginPingNumber), newPingIndex(endPingNumber));
   }

   private static Curve newCurve(long beginPingNumber, long endPingNumber, float depth) {
      Curve curve = new Curve(newPingRange(beginPingNumber, endPingNumber));
      Arrays.fill(curve.getDepths(), depth);
      return curve;
   }

   private static Curve newCurve(long beginPingNumber, long endPingNumber, float startDepth, float endDepth) {
      Curve curve = new Curve(newPingRange(beginPingNumber, endPingNumber));
      float[] depths = curve.getDepths();
      int n = depths.length;
      for (int i = 0; i < n; i++) {
         depths[i] = ((n - i) * startDepth + i * endDepth) / n;
      }
      return curve;
   }
}
