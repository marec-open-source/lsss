package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.viewer.variables.plankton.PlanktonVariable;
import no.imr.tools.Utils;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Plankton inversion data.
 */
public final class Pid0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("PID0", Pid0Datagram::new);

   private NavigableSet<PlanktonSample> internalPlanktonSamples = new TreeSet<>();
   private @Nullable List<PlanktonSample> planktonSamples;

   private int count;
   private float sampleDistance;
   private float firstDepth;

   public Pid0Datagram(PowerData referenceDatagram, long ntDate) {
      super(ntDate);
      count = referenceDatagram.getCount();
      sampleDistance = referenceDatagram.getSampleDistance();
      float offset = referenceDatagram.getOffset()
            + (int) (referenceDatagram.getHeaveCorrectedTransducerDepth()
            / referenceDatagram.getSampleDistance());
      firstDepth = offset * sampleDistance;
   }

   public Pid0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      count = byteBuffer.getInt();
      sampleDistance = byteBuffer.getFloat();
      firstDepth = byteBuffer.getFloat();

      int numberOfSamples = byteBuffer.getInt();
      for (int i = 0; i < numberOfSamples; i++) {
         internalPlanktonSamples.add(new PlanktonSample(byteBuffer));
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt(count);
      byteBuffer.putFloat(sampleDistance);
      byteBuffer.putFloat(firstDepth);

      byteBuffer.putInt(internalPlanktonSamples.size());
      for (PlanktonSample internalPlanktonSample : internalPlanktonSamples) {
         internalPlanktonSample.putBuffer(byteBuffer);
      }
   }

   public void setCount(int count) {
      this.count = count;
   }

   public int getCount() {
      return count;
   }

   public float getSampleDistance() {
      return sampleDistance;
   }

   public float indexToDepth(int index) {
      return firstDepth + index * sampleDistance;
   }

   /**
    * Converts a depth to a pixel index.
    *
    * @param depth a depth
    * @return the corresponding pixel index
    */
   public int depthToIndex(float depth) {
      return Math.round((depth - firstDepth) / sampleDistance);
   }

   public FloatRange getDepthRange() {
      return FloatRange.ofMinAndSize(firstDepth, count * sampleDistance);
   }

   /**
    * Set plankton distributions valid from startIndex to the start index of the next set of plankton
    * distributions.
    *
    * @param startIndex    start index
    * @param distributions distributions
    */
   public void setCategorySamples(int startIndex, List<PlanktonData> distributions) {
      internalPlanktonSamples.add(new PlanktonSample(startIndex, distributions));
   }

   /**
    * Get a list of plankton samples with the same resolution as the original raw datagram.
    * The first sample in the list should correspond to the first sample in the raw datagram.
    *
    * @param pic0Datagram plankton configuration datagram
    * @return a list of plankton samples
    */
   public List<PlanktonSample> getPlanktonSamples(Pic0Datagram pic0Datagram) {
      if (planktonSamples != null) {
         return planktonSamples;
      }

      List<PlanktonSample> samples = new ArrayList<>();

      if (internalPlanktonSamples.isEmpty()) {
         //fill with 'filler' category
         for (int i = 0; i < count; i++) {
            samples.add(new PlanktonSample(i, List.of(new PlanktonData(Pic0Datagram.getFillerCategory(), LengthDistribution.EMPTY_DISTRIBUTION, 1.0f, 0))));
         }
         planktonSamples = samples;
         return samples;
      }

      int firstSample = internalPlanktonSamples.first().startSample;
      if (firstSample != 0) {
         //fill with 'filler' category
         for (int i = 0; i < firstSample; i++) {
            samples.add(new PlanktonSample(i, List.of(new PlanktonData(Pic0Datagram.getFillerCategory(), LengthDistribution.EMPTY_DISTRIBUTION, 1.0f, 0))));
         }
      }

      PlanktonSample sample = internalPlanktonSamples.first();
      convertIdToPlanktonCategory(sample.planktonDatas, pic0Datagram);
      for (PlanktonSample nextSample : internalPlanktonSamples) {
         if (nextSample == sample) {
            continue; //skip first element
         }
         //fill with plankton sample between the sample indices for sample and nextSample
         for (int i = sample.startSample; i < nextSample.startSample; i++) {
            samples.add(sample);
         }
         sample = nextSample;
         convertIdToPlanktonCategory(sample.planktonDatas, pic0Datagram);
      }
      //last sample
      for (int i = sample.startSample; i < count; i++) {
         samples.add(sample);
      }

      planktonSamples = samples; // Update member variable at end because this function may be called from multiple threads
      return samples;
   }

   private static void convertIdToPlanktonCategory(Collection<PlanktonData> planktonDatas, Pic0Datagram pic0Datagram) {
      for (PlanktonData planktonData : planktonDatas) {
         planktonData.planktonCategory = pic0Datagram.getIdToCategoryMap().get(planktonData.planktonNumber);
      }
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public String toStringExtra() {
      StringBuilder sb = new StringBuilder();
      for (PlanktonSample internalPlanktonSample : internalPlanktonSamples) {
         sb.append("sample ").append(internalPlanktonSample.startSample).append(" cat");
         for (PlanktonData planktonData : internalPlanktonSample.planktonDatas) {
            sb.append(' ').append(planktonData.planktonNumber);
         }
         sb.append("; ");
      }
      return sb.toString();
   }

   public static final class LengthDistribution {
      private static final LengthDistribution EMPTY_DISTRIBUTION = new LengthDistribution();

      private final float[] dividers;
      private final float[] abundances;

      private LengthDistribution() {
         dividers = Utils.EMPTY_FLOAT_ARRAY;
         abundances = Utils.EMPTY_FLOAT_ARRAY;
      }

      public LengthDistribution(double[] allDividers, double[] allAbundances) {
         if (allDividers.length != 2 * allAbundances.length) {
            throw new IllegalArgumentException(Arrays.toString(allDividers) + ' ' + Arrays.toString(allAbundances));
         }

         int cellCount = 0;
         boolean previousEmpty = true;
         for (int i = 0; i < allAbundances.length; i++) {
            float abundance = (float) allAbundances[i];
            boolean empty = abundance == 0;
            if (!empty) {
               if (i > 0 && allDividers[2 * i] != allDividers[2 * i - 1]) { // gap between cells
                  previousEmpty = true;
               }
               if (previousEmpty && cellCount > 0) {
                  cellCount++;
               }
               cellCount++;
            }
            previousEmpty = empty;
         }

         dividers = new float[cellCount + 1];
         abundances = new float[cellCount];

         cellCount = 0;
         previousEmpty = true;
         for (int i = 0; i < allAbundances.length; i++) {
            float abundance = (float) allAbundances[i];
            boolean empty = abundance == 0;
            if (!empty) {
               if (i > 0 && allDividers[2 * i] != allDividers[2 * i - 1]) { // gap between cells
                  previousEmpty = true;
               }
               if (previousEmpty) {
                  if (cellCount > 0) {
                     // Insert empty cell (possibly several cells accumulated)
                     cellCount++;
                  }
                  dividers[cellCount] = (float) allDividers[2 * i]; // lower limit for this cell
               }

               // Non-empty cell
               dividers[cellCount + 1] = (float) allDividers[2 * i + 1]; // upper limit for this cell
               abundances[cellCount] = abundance;
               cellCount++;
            }
            previousEmpty = empty;
         }

         if (!dividersConsistent()) {
            throw new IllegalArgumentException("Dividers are not monotonously increasing: " + Arrays.toString(dividers));
         }
      }

      private LengthDistribution(ByteBuffer byteBuffer, int size) throws DatagramFormatException {
         dividers = new float[size + 1];
         abundances = new float[size];

         FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
         floatBuffer.get(dividers);
         floatBuffer.get(abundances);

         // Update position
         int noByte = floatBuffer.position() * 4;
         byteBuffer.position(byteBuffer.position() + noByte);

         if (!dividersConsistent()) {
            throw new DatagramFormatException("Dividers are not monotonously increasing: " + Arrays.toString(dividers));
         }
      }

      private void putBuffer(ByteBuffer byteBuffer) {
         FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
         floatBuffer.put(dividers);
         floatBuffer.put(abundances);

         // Update position
         int noByte = floatBuffer.position() * 4;
         byteBuffer.position(byteBuffer.position() + noByte);
      }

      public float[] getDividers() {
         return dividers;
      }

      public float[] getAbundances() {
         return abundances;
      }

      public boolean isEmpty() {
         return abundances.length == 0;
      }

      private boolean dividersConsistent() {
         float prevDivider = dividers[0];
         for (float divider : dividers) {
            if (divider < prevDivider) {
               return false;
            }
            prevDivider = divider;
         }
         return true;
      }
   }

   /**
    * Data about a plankton category.
    */
   public static final class PlanktonData implements Comparable<PlanktonData> {
      private final byte planktonNumber;
      private final LengthDistribution lengthDistribution;
      private final float fraction;
      private final float residual;

      private Pic0Datagram.@Nullable PlanktonCategory planktonCategory; //may or may not be set, depending on whether Pic0Datagram.PlanktonCategory
      //has been available.

      public PlanktonData(Pic0Datagram.PlanktonCategory planktonCategory, LengthDistribution lengthDistribution, float fraction, float residual) {
         this.planktonCategory = planktonCategory;
         planktonNumber = planktonCategory.getNumber();
         this.lengthDistribution = lengthDistribution;
         this.fraction = fraction;
         this.residual = residual;
      }

      public PlanktonData(byte planktonNumber, LengthDistribution lengthDistribution, float fraction, float residual) {
         this.planktonNumber = planktonNumber;
         this.lengthDistribution = lengthDistribution;
         this.fraction = fraction;
         this.residual = residual;
      }

      public PlanktonData(ByteBuffer byteBuffer) throws DatagramFormatException {
         planktonNumber = byteBuffer.get();
         int lengthDistributionSize = ByteBufferUtils.readCount(byteBuffer, 8);
         if (lengthDistributionSize > 0) {
            lengthDistribution = new LengthDistribution(byteBuffer, lengthDistributionSize);
         } else {
            lengthDistribution = LengthDistribution.EMPTY_DISTRIBUTION;
         }
         fraction = byteBuffer.getFloat();
         residual = byteBuffer.getFloat();
      }

      private void putBuffer(ByteBuffer byteBuffer) {
         byteBuffer.put(planktonNumber);
         if (lengthDistribution.isEmpty()) {
            byteBuffer.putInt(0);
         } else {
            byteBuffer.putInt(lengthDistribution.abundances.length); //number of bins
            lengthDistribution.putBuffer(byteBuffer);
         }
         byteBuffer.putFloat(fraction);
         byteBuffer.putFloat(residual);
      }

      public Pic0Datagram.@Nullable PlanktonCategory getPlanktonCategory() {
         return planktonCategory;
      }

      public LengthDistribution getLengthDistribution() {
         return lengthDistribution;
      }

      public float getFraction() {
         return fraction;
      }

      public float getResidual() {
         return residual;
      }

      @Override
      public int compareTo(PlanktonData other) {
         return Float.compare(fraction, other.fraction);
      }
   }

   /**
    * A collection of the plankton data for a sample.
    */
   public static final class PlanktonSample implements Comparable<PlanktonSample> {
      private final int startSample;
      private final NavigableSet<PlanktonData> planktonDatas = new TreeSet<>();

      public PlanktonSample(int startSample, List<PlanktonData> planktonDistributions) {
         this.startSample = startSample;
         planktonDatas.addAll(planktonDistributions);
      }

      public PlanktonSample(ByteBuffer byteBuffer) throws DatagramFormatException {
         startSample = byteBuffer.getInt();
         int numberOfDistributions = byteBuffer.getInt();
         for (int i = 0; i < numberOfDistributions; i++) {
            planktonDatas.add(new PlanktonData(byteBuffer));
         }
      }

      private void putBuffer(ByteBuffer byteBuffer) {
         byteBuffer.putInt(startSample);
         byteBuffer.putInt(planktonDatas.size());
         for (PlanktonData planktonData : planktonDatas) {
            planktonData.putBuffer(byteBuffer);
         }
      }

      public PlanktonData getBestPlanktonData() {
         return planktonDatas.first();
      }

      public @Nullable PlanktonData getBestPlanktonData(PlanktonVariable planktonVariable) {
         for (PlanktonData planktonData : planktonDatas) {
            Pic0Datagram.PlanktonCategory planktonCategory = planktonData.getPlanktonCategory();
            if (planktonCategory != null && planktonVariable.getSettings().isPlottable(planktonCategory)) {
               return planktonData;
            }
         }
         return null;
      }

      public Set<PlanktonData> getPlanktonData() {
         return planktonDatas;
      }

      @Override
      public int compareTo(PlanktonSample other) {
         return Integer.compare(startSample, other.startSample);
      }
   }
}
