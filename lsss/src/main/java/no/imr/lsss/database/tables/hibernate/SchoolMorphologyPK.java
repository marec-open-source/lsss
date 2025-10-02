package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class SchoolMorphologyPK implements BaseSurveyPK {
   private short nation;
   private short platform;
   private int survey;
   private int object;
   private short schoolObjectType;

   public SchoolMorphologyPK() {
   }

   public SchoolMorphologyPK(short nation, short platform, int survey, int object, short schoolObjectType) {
      this.nation = nation;
      this.platform = platform;
      this.survey = survey;
      this.object = object;
      this.schoolObjectType = schoolObjectType;
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

   public short getSchoolObjectType() {
      return schoolObjectType;
   }

   public void setSchoolObjectType(short schoolObjectType) {
      this.schoolObjectType = schoolObjectType;
   }

   @Override
   public String toString() {
      return "SchoolMorphologyPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            ", object=" + object +
            ", schoolObjectType=" + schoolObjectType +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolMorphologyPK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey
            && object == that.object
            && schoolObjectType == that.schoolObjectType;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      result = 31 * result + object;
      result = 31 * result + schoolObjectType;
      return result;
   }
}
