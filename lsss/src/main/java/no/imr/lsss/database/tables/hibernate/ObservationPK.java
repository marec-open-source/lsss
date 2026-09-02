package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class ObservationPK implements BaseSurveyPK, BaseObservationTimeContainer {
   private short nation;
   private short platform;
   private int survey;
   private int observationDate;
   private int observationTime;
   private short observationType;

   public ObservationPK() {
   }

   public ObservationPK(short nation, short platform, int survey, int observationDate, int observationTime, short observationType) {
      this.nation = nation;
      this.platform = platform;
      this.survey = survey;
      this.observationDate = observationDate;
      this.observationTime = observationTime;
      this.observationType = observationType;
   }

   public ObservationPK(BaseSurveyPK surveyPK, int observationDate, int observationTime, short observationType) {
      nation = surveyPK.getNation();
      platform = surveyPK.getPlatform();
      survey = surveyPK.getSurvey();
      this.observationDate = observationDate;
      this.observationTime = observationTime;
      this.observationType = observationType;
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
   public int getObservationDate() {
      return observationDate;
   }

   @Override
   public void setObservationDate(int observationDate) {
      this.observationDate = observationDate;
   }

   @Override
   public int getObservationTime() {
      return observationTime;
   }

   @Override
   public void setObservationTime(int observationTime) {
      this.observationTime = observationTime;
   }

   public short getObservationType() {
      return observationType;
   }

   public void setObservationType(short observationType) {
      this.observationType = observationType;
   }

   @Override
   public String toString() {
      return "ObservationPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            ", observationDate=" + observationDate +
            ", observationTime=" + observationTime +
            ", observationType=" + observationType +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ObservationPK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey
            && observationDate == that.observationDate
            && observationTime == that.observationTime
            && observationType == that.observationType;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      result = 31 * result + observationDate;
      result = 31 * result + observationTime;
      result = 31 * result + observationType;
      return result;
   }
}
