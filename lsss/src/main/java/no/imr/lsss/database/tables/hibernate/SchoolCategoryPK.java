package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class SchoolCategoryPK implements BaseSurveyPK {
   private short nation;
   private short platform;
   private int survey;
   private int object;
   private short categorySystem;
   private int category;

   public SchoolCategoryPK() {
   }

   public SchoolCategoryPK(short nation, short platform, int survey, int object, short categorySystem, int category) {
      this.nation = nation;
      this.platform = platform;
      this.survey = survey;
      this.object = object;
      this.categorySystem = categorySystem;
      this.category = category;
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

   @Override
   public int getSurvey() {
      return survey;
   }

   @Override
   public void setSurvey(int survey) {
      this.survey = survey;
   }

   public int getObject() {
      return object;
   }

   public void setObject(int object) {
      this.object = object;
   }

   public short getCategorySystem() {
      return categorySystem;
   }

   public void setCategorySystem(short categorySystem) {
      this.categorySystem = categorySystem;
   }

   public int getCategory() {
      return category;
   }

   public void setCategory(int category) {
      this.category = category;
   }

   @Override
   public String toString() {
      return "SchoolCategoryPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            ", object=" + object +
            ", categorySystem=" + categorySystem +
            ", category=" + category +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolCategoryPK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey
            && object == that.object
            && categorySystem == that.categorySystem
            && category == that.category;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      result = 31 * result + object;
      result = 31 * result + categorySystem;
      result = 31 * result + category;
      return result;
   }
}
