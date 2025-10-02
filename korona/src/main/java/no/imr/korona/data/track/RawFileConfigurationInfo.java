package no.imr.korona.data.track;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public final class RawFileConfigurationInfo {
   private static final Interner<RawFileConfigurationInfo> INTERNER = Interners.newWeakInterner();

   public final float[] frequencies;

   private RawFileConfigurationInfo(float[] frequencies) {
      this.frequencies = frequencies;
   }

   public static RawFileConfigurationInfo of(float[] frequencies) {
      return INTERNER.intern(new RawFileConfigurationInfo(frequencies));
   }

   public static RawFileConfigurationInfo of(RawFileConfiguration rawFileConfiguration) {
      List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
      float[] frequencies = new float[transducers.size()];
      for (int i = 0; i < transducers.size(); i++) {
         frequencies[i] = transducers.get(i).getFrequency();
      }
      return of(frequencies);
   }

   @Override
   public String toString() {
      return Arrays.toString(frequencies);
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof RawFileConfigurationInfo that
            && Arrays.equals(frequencies, that.frequencies);
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(frequencies);
   }
}
