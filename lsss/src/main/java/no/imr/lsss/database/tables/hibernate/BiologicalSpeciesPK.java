package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class BiologicalSpeciesPK implements BaseNationPK {
   private short nation;
   private int biologicalSpecies;

   public BiologicalSpeciesPK() {
   }

   public BiologicalSpeciesPK(short nation, int biologicalSpecies) {
      this.nation = nation;
      this.biologicalSpecies = biologicalSpecies;
   }

   @Override
   public short getNation() {
      return nation;
   }

   @Override
   public void setNation(short nation) {
      this.nation = nation;
   }

   public int getBiologicalSpecies() {
      return biologicalSpecies;
   }

   public void setBiologicalSpecies(int biologicalSpecies) {
      this.biologicalSpecies = biologicalSpecies;
   }

   @Override
   public String toString() {
      return "BiologicalSpeciesPK{" +
            "nation=" + nation +
            ", biologicalSpecies=" + biologicalSpecies +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof BiologicalSpeciesPK that
            && nation == that.nation
            && biologicalSpecies == that.biologicalSpecies;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + biologicalSpecies;
      return result;
   }
}
