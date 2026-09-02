package no.imr.korona.computation.region;

import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.annotations.ReflectionEntryPoint;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Synthetic data for school detection.
 * Example file name:
 * <pre>
 * no.imr.korona.computation.region.SyntheticSchoolData@case=circleWithHole@1-100.lsss-ss
 * </pre>
 */
public final class SyntheticSchoolData extends SyntheticData {
   private static final float[] FREQUENCIES = {38_000, 200_000};
   private static final Instant START_INSTANT = Instant.EPOCH;
   static final float SV_INSIDE = PowerData.logSvToSv(-40);

   static final float METER_PER_PING = 2;
   static final float SAMPLE_DISTANCE = 0.1f;

   static final String CASE_RECTANGULAR_SCHOOL = "rectangle";
   static final String CASE_RECTANGULAR_SCHOOL_WITH_HOLE = "rectangleWithHole";
   static final String CASE_CIRCULAR_SCHOOL = "circle";
   static final String CASE_CIRCULAR_SCHOOL_WITH_HOLE = "circleWithHole";
   static final String CASE_COMPLICATED_SCHOOL_DEFINITION = "complicated";

   private String theCase;

   @ReflectionEntryPoint
   public SyntheticSchoolData() {
      this(CASE_RECTANGULAR_SCHOOL);
   }

   public SyntheticSchoolData(String theCase) {
      this.theCase = theCase;
   }

   @Override
   public int getTransducerCount() {
      return FREQUENCIES.length;
   }

   @Override
   public float getFrequency(int channel) {
      return FREQUENCIES[channel - 1];
   }

   @Override
   public void addParameters(Map<String, String> parameters) {
      parameters.put("case", theCase);
   }

   @Override
   public void setParameter(String name, String value) {
      if (name.equals("case")) {
         theCase = value;
      } else {
         super.setParameter(name, value);
      }
   }

   @Override
   public Instant getInstant(long pingNumber) {
      // One ping per second.
      return START_INSTANT.plusSeconds(pingNumber);
   }

   @Override
   public double getVesselDistance(long pingNumber) {
      // 10 meter per ping.
      return pingNumber * KoronaUtils.meterToNmi(METER_PER_PING);
   }

   @Override
   public float getSampleInterval(PingIndex pingIndex, int channel) {
      // sampleDistance = sampleInterval * soundVelocity / 2
      return SAMPLE_DISTANCE * 2 / getSoundVelocity(pingIndex, channel);
   }

   @Override
   public float getBottomDepth(PingIndex pingIndex, int channel) {
      return 0;
   }

   @Override
   public void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
      float[] sv = switch (theCase) {
         case CASE_RECTANGULAR_SCHOOL -> rectangularSchool(powerData, pingIndex, false);
         case CASE_RECTANGULAR_SCHOOL_WITH_HOLE -> rectangularSchool(powerData, pingIndex, true);
         case CASE_CIRCULAR_SCHOOL -> circularSchool(powerData, pingIndex, false);
         case CASE_CIRCULAR_SCHOOL_WITH_HOLE -> circularSchool(powerData, pingIndex, true);
         case CASE_COMPLICATED_SCHOOL_DEFINITION -> complicatedSchool(powerData, pingIndex);
         default -> throw new IllegalStateException("case: " + theCase);
      };
      powerData.setSv(sv);
   }

   static final long RECTANGULAR_SCHOOL_START_PING = 10;
   static final long RECTANGULAR_SCHOOL_PING_WIDTH = (long) (100 / METER_PER_PING);
   static final float RECTANGULAR_SCHOOL_MIN_DEPTH = 40;
   static final float RECTANGULAR_SCHOOL_HEIGHT = 10;

   static final long RECTANGULAR_SCHOOL_HOLE_PING_WIDTH = (long) (60 / METER_PER_PING);
   static final float RECTANGULAR_SCHOOL_HOLE_HEIGHT = 6;

   private static float[] rectangularSchool(PowerData powerData, PingIndex pingIndex, boolean hole) {
      int n = powerData.depthToSampleIndex(RECTANGULAR_SCHOOL_MIN_DEPTH + 2 * RECTANGULAR_SCHOOL_HEIGHT);
      float[] sv = new float[n];
      long p = pingIndex.getPingNumber() - RECTANGULAR_SCHOOL_START_PING;
      if (p >= 0 && p < RECTANGULAR_SCHOOL_PING_WIDTH) {
         float minDepth = RECTANGULAR_SCHOOL_MIN_DEPTH;
         float maxDepth = RECTANGULAR_SCHOOL_MIN_DEPTH + RECTANGULAR_SCHOOL_HEIGHT;
         fill(powerData, sv, minDepth, maxDepth, SV_INSIDE);
         if (hole) {
            long pingMargin = (RECTANGULAR_SCHOOL_PING_WIDTH - RECTANGULAR_SCHOOL_HOLE_PING_WIDTH) / 2;
            if (p >= pingMargin && p < RECTANGULAR_SCHOOL_PING_WIDTH - pingMargin) {
               float depthMargin = (RECTANGULAR_SCHOOL_HEIGHT - RECTANGULAR_SCHOOL_HOLE_HEIGHT) / 2;
               fill(powerData, sv, minDepth + depthMargin, maxDepth - depthMargin, 0);
            }
         }
      }
      return sv;
   }

   static final float CIRCULAR_SCHOOL_RADIUS = 49;
   static final long CIRCULAR_SCHOOL_PING_CENTER = (long) (1.5 * CIRCULAR_SCHOOL_RADIUS / METER_PER_PING);
   static final float CIRCULAR_SCHOOL_CENTER_DEPTH = 100;

   static final float CIRCULAR_SCHOOL_HOLE_RADIUS = 39;

   private static float[] circularSchool(PowerData powerData, PingIndex pingIndex, boolean hole) {
      int n = powerData.depthToSampleIndex(CIRCULAR_SCHOOL_CENTER_DEPTH + 2 * CIRCULAR_SCHOOL_RADIUS);
      float[] sv = new float[n];
      float dx = Math.abs(pingIndex.getPingNumber() - CIRCULAR_SCHOOL_PING_CENTER) * METER_PER_PING;
      if (dx < CIRCULAR_SCHOOL_RADIUS) {
         float dy = (float) Math.sqrt(CIRCULAR_SCHOOL_RADIUS * CIRCULAR_SCHOOL_RADIUS - dx * dx);
         fill(powerData, sv, CIRCULAR_SCHOOL_CENTER_DEPTH - dy, CIRCULAR_SCHOOL_CENTER_DEPTH + dy, SV_INSIDE);
      }
      if (hole && dx < CIRCULAR_SCHOOL_HOLE_RADIUS) {
         float dy = (float) Math.sqrt(CIRCULAR_SCHOOL_HOLE_RADIUS * CIRCULAR_SCHOOL_HOLE_RADIUS - dx * dx);
         fill(powerData, sv, CIRCULAR_SCHOOL_CENTER_DEPTH - dy, CIRCULAR_SCHOOL_CENTER_DEPTH + dy, 0);
      }
      return sv;
   }

   static final int COMPLICATED_SCHOOL_FIRST_PING = 10;
   static final float COMPLICATED_SCHOOL_FIRST_DEPTH = 40;
   static final List<String> COMPLICATED_SCHOOL_DEFINITION = """
         ████····················████████████████████████████████████████████
         ··██████············██████·········································█
         ████··██············██··████··███████████████████████████████████··█
         ······██████····██████········█·································█··█
         ████··██··██····██··██··████··█··█████████████████████████████··█··█
         ··██████··██····██··██████····█··█···························█··█··█
         ████······██····██······████··█··█··███████████████████████··█··█··█
         ··········████████············█··█··█··█·················█···█··█··█
         ████······██····██······████··█··█··█··████████████████████··█··█··█
         ··██████··██····██··██████····█··█··█························█··█··█
         ████··██··██····██··██··████··█··█··██████████████████████████··█··█
         ······██████····██████········█··█······························█··█
         ████··██············██··████··█··████████████████████████████████··█
         ··██████············██████····█····································█
         ████····················████··██████████████████████████████████████
         """.lines().toList();
   static final int COMPLICATED_SCHOOL_PING_WIDTH = COMPLICATED_SCHOOL_DEFINITION.getFirst().length();
   static final int COMPLICATED_SCHOOL_HOLE_SAMPLE_COUNT = 17;

   private static float[] complicatedSchool(PowerData powerData, PingIndex pingIndex) {
      int firstSampleIndex = powerData.depthToSampleIndex(COMPLICATED_SCHOOL_FIRST_DEPTH);
      int n = firstSampleIndex + 2 * COMPLICATED_SCHOOL_DEFINITION.size();
      float[] sv = new float[n];
      int i = (int) (pingIndex.getPingNumber() - COMPLICATED_SCHOOL_FIRST_PING);
      if (i >= 0 && i < COMPLICATED_SCHOOL_PING_WIDTH) {
         for (int j = 0; j < COMPLICATED_SCHOOL_DEFINITION.size(); j++) {
            String line = COMPLICATED_SCHOOL_DEFINITION.get(j);
            if (i < line.length() && line.charAt(i) == '█') {
               sv[firstSampleIndex + j] = SV_INSIDE;
            }
         }
      }
      return sv;
   }

   private static void fill(PowerData powerData, float[] sv, float minDepth, float maxDepth, float fillValue) {
      int iBegin = powerData.depthToSampleIndex(minDepth);
      int iEnd = powerData.depthToSampleIndex(maxDepth);
      Arrays.fill(sv, iBegin, iEnd, fillValue);
   }
}
