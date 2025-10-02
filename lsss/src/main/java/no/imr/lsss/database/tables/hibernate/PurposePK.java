package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class PurposePK implements BaseSurveyPK {
   private short nation;
   private short platform;
   private int survey;
   private int acousticCategory;

   public PurposePK(short nation, short platform, int survey, int acousticCategory) {
      this.nation = nation;
      this.platform = platform;
      this.survey = survey;
      this.acousticCategory = acousticCategory;
   }

   public PurposePK() {
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

   public int getAcousticCategory() {
      return acousticCategory;
   }

   public void setAcousticCategory(int acousticCategory) {
      this.acousticCategory = acousticCategory;
   }

   @Override
   public String toString() {
      return "PurposePK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            ", acousticCategory=" + acousticCategory +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PurposePK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey
            && acousticCategory == that.acousticCategory;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      result = 31 * result + acousticCategory;
      return result;
   }
}
