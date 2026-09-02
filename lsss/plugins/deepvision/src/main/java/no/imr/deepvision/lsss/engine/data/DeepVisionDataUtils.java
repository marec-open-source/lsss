package no.imr.deepvision.lsss.engine.data;

import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFrame;
import no.imr.tools.time.DateTimeMillis;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public final class DeepVisionDataUtils {
   private DeepVisionDataUtils() {
   }

   public static Instant time(DeepVisionFrame frame) {
      long deepVisionTime = frame.time;
      int date = (int) (deepVisionTime / 1_00_00_00_000L); // hh_mm_ss_SSS
      int time = (int) (deepVisionTime % 1_00_00_00_000L);
      return DateTimeMillis.toInstant(date, time);
   }

   public static @Nullable GeoPoint geoPoint(DeepVisionFrame frame) {
      return validPosition(frame) ? new GeoPoint(frame.lon, frame.lat) : null;
   }

   public static boolean validPosition(DeepVisionFrame frame) {
      return validPosition(frame.lon, frame.lat);
   }

   private static boolean validPosition(double lon, double lat) {
      return !Double.isNaN(lon) && !Double.isNaN(lat);
   }
}
