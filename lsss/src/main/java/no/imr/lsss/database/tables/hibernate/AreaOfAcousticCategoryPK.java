package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class AreaOfAcousticCategoryPK implements BasePlatformPK {
   private short nation;
   private short platform;
   private int acousticCategory;
   private int area;

   public AreaOfAcousticCategoryPK() {
   }

   public AreaOfAcousticCategoryPK(short nation, short platform, int acousticCategory, int area) {
      this.nation = nation;
      this.platform = platform;
      this.acousticCategory = acousticCategory;
      this.area = area;
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

   public int getArea() {
      return area;
   }

   public void setArea(int area) {
      this.area = area;
   }

   @Override
   public String toString() {
      return "AreaOfAcousticCategoryPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", acousticCategory=" + acousticCategory +
            ", area=" + area +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof AreaOfAcousticCategoryPK that
            && nation == that.nation
            && platform == that.platform
            && acousticCategory == that.acousticCategory
            && area == that.area;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + acousticCategory;
      result = 31 * result + area;
      return result;
   }
}
