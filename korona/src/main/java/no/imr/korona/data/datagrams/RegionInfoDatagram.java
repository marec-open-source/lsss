package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.math.GeometryUtils;
import no.imr.tools.time.NTDate;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

/**
 * Region info.
 */
public final class RegionInfoDatagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("RNF0", RegionInfoDatagram::new);

   private boolean accepted;
   private final int channel;
   private final float threshold;
   private final BoundingBox boundingBox = new BoundingBox();
   private final Values values = new Values();
   private final Statistics statistics = new Statistics();
   private final int[] borderIds;
   private final Histogram histogram = new Histogram();
   private final List<PerimeterPoint> perimeterPoints;
   private final List<MaskInterval> maskIntervals;

   public RegionInfoDatagram(Instant instant, boolean accepted, int channel, float threshold, int[] borderIds,
                             List<PerimeterPoint> perimeterPoints, List<MaskInterval> maskIntervals) {
      super(instant);

      this.accepted = accepted;
      this.channel = channel;
      this.threshold = threshold;
      this.borderIds = borderIds;
      this.perimeterPoints = perimeterPoints;
      this.maskIntervals = maskIntervals;
   }

   public RegionInfoDatagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      accepted = byteBuffer.getInt() != 0;
      channel = byteBuffer.getInt();
      threshold = byteBuffer.getFloat();

      boundingBox.x = byteBuffer.getFloat();
      boundingBox.y = byteBuffer.getFloat();
      boundingBox.width = byteBuffer.getFloat();
      boundingBox.height = byteBuffer.getFloat();

      values.area = byteBuffer.getFloat();
      values.sampleCount = byteBuffer.getFloat();
      values.sumLogSv = byteBuffer.getFloat();
      values.sumSv = byteBuffer.getFloat();
      values.logMeanSv = byteBuffer.getFloat();
      values.sa = byteBuffer.getDouble();
      values.perimeter = byteBuffer.getFloat();
      values.length = byteBuffer.getFloat();
      values.maxHeight = byteBuffer.getFloat();

      statistics.meanInside = byteBuffer.getDouble();
      statistics.stdDevInside = byteBuffer.getDouble();
      statistics.meanOutside = byteBuffer.getDouble();
      statistics.stdDevOutside = byteBuffer.getDouble();
      statistics.overlap = byteBuffer.getDouble();
      statistics.crossing = byteBuffer.getDouble();

      borderIds = ByteBufferUtils.readCountAndIntArray(byteBuffer);

      histogram.startValue = byteBuffer.getFloat();
      histogram.step = byteBuffer.getFloat();

      int maxIndex = byteBuffer.getInt();
      if (maxIndex < 0 || maxIndex >= byteBuffer.remaining() / 4) {
         throw new DatagramFormatException("maxIndex: " + maxIndex);
      }
      histogram.counts = new int[maxIndex + 1];
      for (int i = 0; i <= maxIndex; i++) {
         histogram.counts[i] = byteBuffer.getInt();
      }

      perimeterPoints = ByteBufferUtils.readCountAndList(byteBuffer, 8 + 4, PerimeterPoint::new);

      // Previously, with the old RegionModule, the next int was numInnerTraces,
      // which was required to be 0, since inner traces was not implemented.
      // Now, with the new SchoolDetectionModule, the next int is instead used for the mask.

      maskIntervals = ByteBufferUtils.readCountAndList(byteBuffer, 8 + 4 + 4, MaskInterval::new);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt(accepted ? 1 : 0);
      byteBuffer.putInt(channel);
      byteBuffer.putFloat(threshold);

      byteBuffer.putFloat(boundingBox.x);
      byteBuffer.putFloat(boundingBox.y);
      byteBuffer.putFloat(boundingBox.width);
      byteBuffer.putFloat(boundingBox.height);

      byteBuffer.putFloat(values.area);
      byteBuffer.putFloat(values.sampleCount);
      byteBuffer.putFloat(values.sumLogSv);
      byteBuffer.putFloat(values.sumSv);
      byteBuffer.putFloat(values.logMeanSv);
      byteBuffer.putDouble(values.sa);
      byteBuffer.putFloat(values.perimeter);
      byteBuffer.putFloat(values.length);
      byteBuffer.putFloat(values.maxHeight);

      byteBuffer.putDouble(statistics.meanInside);
      byteBuffer.putDouble(statistics.stdDevInside);
      byteBuffer.putDouble(statistics.meanOutside);
      byteBuffer.putDouble(statistics.stdDevOutside);
      byteBuffer.putDouble(statistics.overlap);
      byteBuffer.putDouble(statistics.crossing);

      // All ids in the region
      ByteBufferUtils.writeCountAndIntArray(byteBuffer, borderIds);

      // histogram of the region
      byteBuffer.putFloat(histogram.startValue);
      byteBuffer.putFloat(histogram.step);

      int maxIndex = histogram.counts.length - 1;
      byteBuffer.putInt(maxIndex);

      for (int i = 0; i <= maxIndex; i++) {
         byteBuffer.putInt(histogram.counts[i]);
      }

      ByteBufferUtils.writeCountAndList(byteBuffer, perimeterPoints, PerimeterPoint::write);

      ByteBufferUtils.writeCountAndList(byteBuffer, maskIntervals, MaskInterval::write);
   }

   public boolean isAccepted() {
      return accepted;
   }

   public void setAccepted(boolean accepted) {
      this.accepted = accepted;
   }

   public int getRegionId() {
      return borderIds[0];
   }

   public int[] getBorderIds() {
      return borderIds;
   }

   public float getThreshold() {
      return threshold;
   }

   public BoundingBox getBoundingBox() {
      return boundingBox;
   }

   public Values getValues() {
      return values;
   }

   public Statistics getStatistics() {
      return statistics;
   }

   public Histogram getHistogram() {
      return histogram;
   }

   public @Nullable List<PerimeterPoint> getPerimeterPoints() {
      return emptyToNull(perimeterPoints);
   }

   public @Nullable List<MaskInterval> getMaskIntervals() {
      return emptyToNull(maskIntervals);
   }

   private static <T> @Nullable List<T> emptyToNull(List<T> list) {
      return list.isEmpty() ? null : list;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   public static final class BoundingBox {
      public float x;      // Ping number relative to beginning of the file
      public float y;      // Upper depth [m]
      public float width;  // Number of pings
      public float height; // Height [m]

      private BoundingBox() {
      }
   }

   public static final class Values {
      public float area;            // [m²]
      public float sampleCount;     //
      public float sumLogSv;        // [dB]
      public float sumSv;           // [4π 1852² m²/m³]
      public float logMeanSv;       // [dB]
      public double sa;             // [m²/nmi²]
      public float perimeter;       // [m]
      public float length;          // [m]
      public float maxHeight;       // [m]

      private Values() {
      }

      public float getMeanSv() {
         return sampleCount > 0 ? sumSv / sampleCount : 0;
      }

      public float getCircleCompactness() {
         return (float) GeometryUtils.getCircleCompactness(area, perimeter);
      }
   }

   public static final class Statistics {
      public double meanInside;    // Mean LogSv for samples inside the region close to the border
      public double stdDevInside;  // Standard deviation for samples inside the region close to the border
      public double meanOutside;   // Mean LogSv for samples outside the region close to the border
      public double stdDevOutside; // Standard deviation for samples outside the region close to the border
      public double overlap;       // Overlap [%] between the Gauss curves of the two distributions
      public double crossing;      // Cross point (between the mean values) of the two distributions

      private Statistics() {
      }
   }

   public static final class Histogram {
      public float startValue;          // Start value [dB] of the first histogram bin
      public float step;                // Bin size [dB]
      public int[] counts = new int[1]; // Number of samples in each bin

      private Histogram() {
      }
   }

   public record PerimeterPoint(Instant instant, float depth) {
      private PerimeterPoint(ByteBuffer byteBuffer) {
         this(NTDate.ntDateToInstant(byteBuffer.getLong()), byteBuffer.getFloat());
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putLong(NTDate.instantToNTDate(instant()));
         byteBuffer.putFloat(depth());
      }
   }

   public record MaskInterval(Instant instant, float minDepth, float maxDepth) {
      private MaskInterval(ByteBuffer byteBuffer) {
         this(NTDate.ntDateToInstant(byteBuffer.getLong()),
               byteBuffer.getFloat(),
               byteBuffer.getFloat());
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putLong(NTDate.instantToNTDate(instant()));
         byteBuffer.putFloat(minDepth());
         byteBuffer.putFloat(maxDepth());
      }
   }
}
