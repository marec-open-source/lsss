package no.imr.korona.computation.feature;

import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Class for extracting features.
 */
public abstract class FeatureExtractor implements Comparable<FeatureExtractor> {
   public static final String FREQUENCY_FEATURE_PREFIX = "R";
   public static final String ADDITIONAL_FEATURE_SV38 = "Sv38";
   public static final String ADDITIONAL_FEATURE_DEPTH = "depth";
   public static final List<String> ALL_ADDITIONAL_FEATURES = List.of(
         ADDITIONAL_FEATURE_SV38,
         ADDITIONAL_FEATURE_DEPTH);

   /**
    * The name of the feature that is extracted by this feature extractor.
    */
   private final String featureName;
   private final boolean usable;
   private boolean active = true;
   private boolean enabled = true;

   private FeatureExtractor(String featureName, boolean usable) {
      this.featureName = featureName;
      this.usable = usable;
   }

   /**
    * Returns the name of the feature that is extracted by this feature extractor.
    *
    * @return the name of the feature that is extracted by this feature extractor
    */
   public String getFeatureName() {
      return featureName;
   }

   /**
    * Returns whether this FeatureExtractor is operational.
    * Operational means active and applicable to current data.
    *
    * @return {@code true} if this FeatureExtractor is operational
    */
   public boolean isOperational() {
      return usable && active;
   }

   /**
    * Returns {@code true} if this feature is active and {@code false} if not.
    *
    * @return {@code true} if this feature is active and {@code false} if not
    */
   public boolean isActive() {
      return active;
   }

   /**
    * Sets the active state of this feature extractor.
    *
    * @param active the new active state of this feature extractor
    */
   public void setActive(boolean active) {
      if (enabled) {
         this.active = active;
      }
   }

   /**
    * Returns whether this FeatureExtractor is enabled.
    *
    * @return {@code true} if this FeatureExtractor is enabled
    */
   public boolean isEnabled() {
      return enabled;
   }

   /**
    * Sets whether this FeatureExtractor is enabled.
    *
    * @param enabled whether this FeatureExtractor is enabled
    */
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      if (!enabled) {
         active = false;
      }
   }

   /**
    * Returns {@code true} if this feature is additional and {@code false} if not.
    * If it is not additional, then it is a frequency feature extractor.
    *
    * @return {@code true} if this feature is additional and {@code false} if not
    */
   public boolean isAdditional() {
      return !(this instanceof FrequencyFeatureExtractor);
   }

   @Override
   public String toString() {
      return featureName;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof FeatureExtractor that
            && featureName.equals(that.featureName);
   }

   @Override
   public int hashCode() {
      return featureName.hashCode();
   }

   @Override
   public int compareTo(FeatureExtractor other) {
      if (isAdditional()) {
         if (other.isAdditional()) {
            return featureName.compareTo(other.featureName);
         } else {
            return 1;
         }
      } else {
         if (other.isAdditional()) {
            return -1;
         } else {
            String kHz = featureName.substring(FREQUENCY_FEATURE_PREFIX.length());
            String otherKHz = other.featureName.substring(FREQUENCY_FEATURE_PREFIX.length());
            return Integer.parseInt(kHz) - Integer.parseInt(otherKHz);
         }
      }
   }

   /**
    * Extracts a feature from an echogram window for specified indices.
    *
    * @param echogramWindow the echogram window
    * @param i              the horizontal index
    * @param j              the vertical index
    * @return the feature
    */
   public abstract @Nullable Feature extract(EchogramWindow echogramWindow, int i, int j);

   /**
    * Extracts a feature from datagrams for a specified sample index.
    *
    * @param resampledFloatArrays resampled arrays over <em>all</em> frequency,
    *                             including the reference frequency. The entry for the reference frequency
    *                             is not used, and may thus be {@code null}.
    * @param referenceDatagram    the reference datagram
    * @param sampleIndex          the sample index
    * @param ping                 the ping
    * @return the feature
    */
   public abstract @Nullable Feature extract(@Nullable ResampledFloatArray[] resampledFloatArrays,
                                             PowerData referenceDatagram, int sampleIndex, Ping ping);

   //------------------- Factory methods: -------------------------

   /**
    * Returns a feature extractor for a specified feature name.
    *
    * @param featureName  a feature name
    * @param configurator the configurator
    * @return the feature extractor
    */
   public static @Nullable FeatureExtractor createFeatureExtractor(String featureName, Configurator configurator) {
      if (featureName.matches(FREQUENCY_FEATURE_PREFIX + "\\d+")) {
         int kHz = Integer.parseInt(featureName.substring(FREQUENCY_FEATURE_PREFIX.length()));
         return new FrequencyFeatureExtractor(featureName, configurator.getFrequencyIndex(kHz),
               configurator.getReferenceChannel() - 1, kHz);
      } else if (ADDITIONAL_FEATURE_SV38.equals(featureName)) {
         return new Sv38FeatureExtractor(configurator.getReferenceChannel() - 1);
      } else if (ADDITIONAL_FEATURE_DEPTH.equals(featureName)) {
         return new DepthFeatureExtractor();
      } else {
         return null;
      }
   }

   /**
    * Returns a feature extractor for a specified frequency.
    *
    * @param kHz          the frequency in kHz
    * @param configurator the configurator
    * @return a collection of feature extractors for all frequencies
    */
   public static FrequencyFeatureExtractor createFrequencyFeatureExtractor(int kHz, Configurator configurator) {
      return new FrequencyFeatureExtractor(FREQUENCY_FEATURE_PREFIX + kHz,
            configurator.getFrequencyIndex(kHz),
            configurator.getReferenceChannel() - 1, kHz);
   }

   /**
    * Returns all additional feature extractors.
    *
    * @param configurator the configurator
    * @return all additional feature extractors
    */
   public static List<FeatureExtractor> createAllAdditionalFeatureExtractors(Configurator configurator) {
      return ALL_ADDITIONAL_FEATURES.stream()
            .map(additionalFeature -> createFeatureExtractor(additionalFeature, configurator))
            .filter(Objects::nonNull)
            .toList();
   }

   //--------------------- Implementations of feature extractors: ----------------------

   /**
    * Extracts relative frequency response in dB.
    */
   public static final class FrequencyFeatureExtractor extends FeatureExtractor {
      private final int frequencyIndex;
      private final int referenceFrequencyIndex;
      private final int kHz;
      private FloatRange range = FloatRange.ALL;

      private FrequencyFeatureExtractor(String featureName, int frequencyIndex,
                                        int referenceFrequencyIndex, int kHz) {
         super(featureName, frequencyIndex >= 0 && referenceFrequencyIndex >= 0);

         this.frequencyIndex = frequencyIndex;
         this.referenceFrequencyIndex = referenceFrequencyIndex;
         this.kHz = kHz;
      }

      /**
       * Returns the frequency of this feature extractor.
       *
       * @return the frequency in kHz
       */
      public int getKHz() {
         return kHz;
      }

      public FloatRange getRange() {
         return range;
      }

      /**
       * Sets the range where this feature is valid.
       *
       * @param range maximum range
       */
      public void setRange(FloatRange range) {
         this.range = range;
      }

      @Override
      public @Nullable Feature extract(EchogramWindow echogramWindow, int i, int j) {
         PowerData powerData = echogramWindow.getPings().get(i).getPowerData(frequencyIndex + 1);
         if (powerData != null && range.contains(powerData.depthToRange(echogramWindow.getDepth(j)))) {
            float[] sv = echogramWindow.getSv(i, j);
            return createFrequencyFeature(sv[frequencyIndex], sv[referenceFrequencyIndex]);
         } else {
            return null;
         }
      }

      @Override
      public @Nullable Feature extract(@Nullable ResampledFloatArray[] resampledFloatArrays,
                                       PowerData referenceDatagram, int sampleIndex, Ping ping) {
         ResampledFloatArray resampledFloatArray = resampledFloatArrays[frequencyIndex];
         if (resampledFloatArray == null) {
            return null;
         }
         if (sampleIndex < resampledFloatArray.getBeginReferenceIndex()) {
            return null;
         }
         if (sampleIndex >= resampledFloatArray.getEndReferenceIndex()) {
            return null;
         }
         PowerData powerData = ping.getPowerData(frequencyIndex + 1);
         if (powerData == null || !range.contains(powerData.depthToRange(referenceDatagram.getSampleDepth(sampleIndex)))) {
            return null;
         }
         ResampledFloatArray referenceResampledFloatArray = resampledFloatArrays[referenceFrequencyIndex];
         if (referenceResampledFloatArray == null) {
            return null;
         }
         float referenceValue = referenceResampledFloatArray.getValueForReferenceIndex(sampleIndex);
         return createFrequencyFeature(resampledFloatArray.getValueForReferenceIndex(sampleIndex), referenceValue);
      }

      private @Nullable Feature createFrequencyFeature(float sv, float referenceSv) {
         if (referenceSv == 0) {
            return null;
         } else {
            return new Feature(getFeatureName(), KoronaUtils.toDB(sv / referenceSv));
         }
      }
   }

   /**
    * Extracts signal strength of the reference frequency in dB.
    */
   private static final class Sv38FeatureExtractor extends FeatureExtractor {
      private final int referenceChannelIndex;

      private Sv38FeatureExtractor(int referenceChannelIndex) {
         super(ADDITIONAL_FEATURE_SV38, referenceChannelIndex >= 0);

         this.referenceChannelIndex = referenceChannelIndex;
      }

      @Override
      public Feature extract(EchogramWindow echogramWindow, int i, int j) {
         float[] sv = echogramWindow.getSv(i, j);
         return new Feature(getFeatureName(), KoronaUtils.toDB(sv[referenceChannelIndex]));
      }

      @Override
      public @Nullable Feature extract(@Nullable ResampledFloatArray[] resampledFloatArrays,
                                       PowerData referenceDatagram, int sampleIndex, Ping ping) {
         ResampledFloatArray resampledFloatArray = resampledFloatArrays[referenceChannelIndex];
         if (resampledFloatArray == null) {
            return null;
         }
         float value = resampledFloatArray.getValueForReferenceIndex(sampleIndex);
         return new Feature(getFeatureName(), KoronaUtils.toDB(value));
      }
   }

   /**
    * Extracts sample depth.
    */
   private static final class DepthFeatureExtractor extends FeatureExtractor {
      private DepthFeatureExtractor() {
         super(ADDITIONAL_FEATURE_DEPTH, true);
      }

      @Override
      public Feature extract(EchogramWindow echogramWindow, int i, int j) {
         return new Feature(getFeatureName(), echogramWindow.getDepth(j));
      }

      @Override
      public Feature extract(@Nullable ResampledFloatArray[] resampledFloatArrays,
                             PowerData referenceDatagram, int sampleIndex, Ping ping) {
         float value = referenceDatagram.getSampleDepth(sampleIndex);
         return new Feature(getFeatureName(), value);
      }
   }
}
