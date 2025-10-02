package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class AcCatToBiologicalSpeciesPK implements BasePlatformPK {
   private short nation;
   private short platform;
   private int acousticCategory;
   private int biologicalSpecies;

   public AcCatToBiologicalSpeciesPK() {
   }

   public AcCatToBiologicalSpeciesPK(short nation, short platform, int acousticCategory, int biologicalSpecies) {
      this.nation = nation;
      this.platform = platform;
      this.acousticCategory = acousticCategory;
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

   @Override
   public short getPlatform() {
      return platform;
   }

   @Override
   public void setPlatform(short platform) {
      this.platform = platform;
   }

   public int getAcousticCategory() {
      return acousticCategory;
   }

   public void setAcousticCategory(int acousticCategory) {
      this.acousticCategory = acousticCategory;
   }

   public int getBiologicalSpecies() {
      return biologicalSpecies;
   }

   public void setBiologicalSpecies(int biologicalSpecies) {
      this.biologicalSpecies = biologicalSpecies;
   }

   @Override
   public String toString() {
      return "AcCatToBiologicalSpeciesPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", acousticCategory=" + acousticCategory +
            ", biologicalSpecies=" + biologicalSpecies +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof AcCatToBiologicalSpeciesPK that
            && nation == that.nation
            && platform == that.platform
            && acousticCategory == that.acousticCategory
            && biologicalSpecies == that.biologicalSpecies;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + acousticCategory;
      result = 31 * result + biologicalSpecies;
      return result;
   }
}
