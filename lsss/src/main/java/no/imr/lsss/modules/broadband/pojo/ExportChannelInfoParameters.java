package no.imr.lsss.modules.broadband.pojo;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Parameters for an exported channel.
 */
public final class ExportChannelInfoParameters {
   public float nominalFrequency;
   public float minFrequency;
   public float maxFrequency;
   public float transmitPower;
   public @Nullable String pulseType;
   public float pulseDuration;
   public float bandWidth;
   public float sampleInterval;
   public float slope;
   public @Nullable String serialNumber;

   public ExportChannelInfoParameters() {
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ExportChannelInfoParameters that
            && Float.floatToIntBits(nominalFrequency) == Float.floatToIntBits(that.nominalFrequency)
            && Float.floatToIntBits(minFrequency) == Float.floatToIntBits(that.minFrequency)
            && Float.floatToIntBits(maxFrequency) == Float.floatToIntBits(that.maxFrequency)
            && Float.floatToIntBits(transmitPower) == Float.floatToIntBits(that.transmitPower)
            && Objects.equals(pulseType, that.pulseType)
            && Float.floatToIntBits(pulseDuration) == Float.floatToIntBits(that.pulseDuration)
            && Float.floatToIntBits(bandWidth) == Float.floatToIntBits(that.bandWidth)
            && Float.floatToIntBits(sampleInterval) == Float.floatToIntBits(that.sampleInterval)
            && Float.floatToIntBits(slope) == Float.floatToIntBits(that.slope)
            && Objects.equals(serialNumber, that.serialNumber);
   }

   @Override
   public int hashCode() {
      int result = Float.floatToIntBits(nominalFrequency);
      result = 31 * result + Float.floatToIntBits(minFrequency);
      result = 31 * result + Float.floatToIntBits(maxFrequency);
      result = 31 * result + Float.floatToIntBits(transmitPower);
      result = 31 * result + Objects.hashCode(pulseType);
      result = 31 * result + Float.floatToIntBits(pulseDuration);
      result = 31 * result + Float.floatToIntBits(bandWidth);
      result = 31 * result + Float.floatToIntBits(sampleInterval);
      result = 31 * result + Float.floatToIntBits(slope);
      result = 31 * result + Objects.hashCode(serialNumber);
      return result;
   }
}
