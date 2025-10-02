package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class AcousticCategoryPK implements BasePlatformPK {
   private short nation;
   private short platform;
   private int acousticCategory;

   public AcousticCategoryPK() {
   }

   public AcousticCategoryPK(short nation, short platform, int acousticCategory) {
      this.nation = nation;
      this.platform = platform;
      this.acousticCategory = acousticCategory;
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

   @Override
   public String toString() {
      return "AcousticCategoryPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", acousticCategory=" + acousticCategory +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof AcousticCategoryPK that
            && nation == that.nation
            && platform == that.platform
            && acousticCategory == that.acousticCategory;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + acousticCategory;
      return result;
   }
}
