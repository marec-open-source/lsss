package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class AcousticCategoryCompositePK implements BasePlatformPK {
   private short nation;
   private short platform;
   private int acousticCategory;
   private int acousticCategoryMember;

   public AcousticCategoryCompositePK() {
   }

   public AcousticCategoryCompositePK(short nation, short platform, int acousticCategory, int acousticCategoryMember) {
      this.nation = nation;
      this.platform = platform;
      this.acousticCategory = acousticCategory;
      this.acousticCategoryMember = acousticCategoryMember;
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

   public int getAcousticCategoryMember() {
      return acousticCategoryMember;
   }

   public void setAcousticCategoryMember(int acousticCategoryMember) {
      this.acousticCategoryMember = acousticCategoryMember;
   }

   @Override
   public String toString() {
      return "AcousticCategoryCompositePK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", acousticCategory=" + acousticCategory +
            ", acousticCategoryMember=" + acousticCategoryMember +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof AcousticCategoryCompositePK that
            && nation == that.nation
            && platform == that.platform
            && acousticCategory == that.acousticCategory
            && acousticCategoryMember == that.acousticCategoryMember;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + acousticCategory;
      result = 31 * result + acousticCategoryMember;
      return result;
   }
}
