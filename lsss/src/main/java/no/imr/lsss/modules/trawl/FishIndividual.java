package no.imr.lsss.modules.trawl;

import no.imr.lsss.modules.trawl.biotic.pojo.BioticIndividual;

record FishIndividual(
      FishSex sex,
      float length, // meter
      float weight  // kg
) {
   FishIndividual(BioticIndividual individual) {
      this(
            individual.sex != null && !individual.sex.isEmpty() ? FishSex.of(individual.sex.charAt(0)) : FishSex.UNSPECIFIED,
            individual.length,
            individual.individualweight
      );
   }

   float lengthCm() {
      return length * 100;
   }
}
