package no.imr.lsss.modules.trawl.biotic.pojo;

import org.jspecify.annotations.Nullable;

@SuppressWarnings("SpellCheckingInspection")
public final class BioticIndividual {
   public float length = Float.NaN;
   public float individualweight = Float.NaN;
   public @Nullable String sex;

   public BioticIndividual() {
   }

   @Override
   public String toString() {
      return String.valueOf(length);
   }
}
