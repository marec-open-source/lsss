package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class SurveyPK implements BaseSurveyPK {
   private short nation;
   private short platform;
   private int survey;

   public SurveyPK() {
   }

   public SurveyPK(short nation, short platform, int survey) {
      this.nation = nation;
      this.platform = platform;
      this.survey = survey;
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

   @Override
   public String toString() {
      return "SurveyPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SurveyPK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      return result;
   }
}
