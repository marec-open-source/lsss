package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class ScatterObjectPK implements BaseSurveyPK {
   private short nation;
   private short platform;
   private int survey;
   private int object;

   public ScatterObjectPK() {
   }

   public ScatterObjectPK(short nation, short platform, int survey, int object) {
      this.nation = nation;
      this.platform = platform;
      this.survey = survey;
      this.object = object;
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

   @Override
   public String toString() {
      return "ScatterObjectPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            ", object=" + object +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ScatterObjectPK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey
            && object == that.object;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      result = 31 * result + object;
      return result;
   }
}
