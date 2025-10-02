package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class SchoolDataPK implements BaseSurveyPK {
   private short nation;
   private short platform;
   private int survey;
   private int object;
   private short schoolObjectType;
   private int transceiver;
   private int frequency;

   public SchoolDataPK() {
   }

   public SchoolDataPK(short nation, short platform, int survey, int object, short schoolObjectType, int transceiver, int frequency) {
      this.nation = nation;
      this.platform = platform;
      this.survey = survey;
      this.object = object;
      this.schoolObjectType = schoolObjectType;
      this.transceiver = transceiver;
      this.frequency = frequency;
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

   public int getTransceiver() {
      return transceiver;
   }

   public void setTransceiver(int transceiver) {
      this.transceiver = transceiver;
   }

   public int getFrequency() {
      return frequency;
   }

   public void setFrequency(int frequency) {
      this.frequency = frequency;
   }

   @Override
   public String toString() {
      return "SchoolDataPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            ", object=" + object +
            ", schoolObjectType=" + schoolObjectType +
            ", transceiver=" + transceiver +
            ", frequency=" + frequency +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolDataPK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey
            && object == that.object
            && schoolObjectType == that.schoolObjectType
            && transceiver == that.transceiver
            && frequency == that.frequency;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      result = 31 * result + object;
      result = 31 * result + schoolObjectType;
      result = 31 * result + transceiver;
      result = 31 * result + frequency;
      return result;
   }
}
