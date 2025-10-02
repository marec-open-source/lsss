package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class ScatterDataPK implements BaseSurveyPK, BaseObservationTimeContainer {
   private short nation;
   private short platform;
   private int survey;
   private int object;
   private int observationDate;
   private int observationTime;
   private int frequency;
   private short transceiver;
   private short scatterType;
   private int channelNumber;
   private int acousticCategory;

   public ScatterDataPK() {
   }

   public ScatterDataPK(ScatterPK scatterPK, int channelNumber, int acousticCategory) {
      nation = scatterPK.getNation();
      platform = scatterPK.getPlatform();
      survey = scatterPK.getSurvey();
      object = scatterPK.getObject();
      observationDate = scatterPK.getObservationDate();
      observationTime = scatterPK.getObservationTime();
      frequency = scatterPK.getFrequency();
      transceiver = scatterPK.getTransceiver();
      scatterType = scatterPK.getScatterType();
      this.channelNumber = channelNumber;
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

   public int getFrequency() {
      return frequency;
   }

   public void setFrequency(int frequency) {
      this.frequency = frequency;
   }

   public short getTransceiver() {
      return transceiver;
   }

   public void setTransceiver(short transceiver) {
      this.transceiver = transceiver;
   }

   public short getScatterType() {
      return scatterType;
   }

   public void setScatterType(short scatterType) {
      this.scatterType = scatterType;
   }

   public int getChannelNumber() {
      return channelNumber;
   }

   public void setChannelNumber(int channelNumber) {
      this.channelNumber = channelNumber;
   }

   public int getAcousticCategory() {
      return acousticCategory;
   }

   public void setAcousticCategory(int acousticCategory) {
      this.acousticCategory = acousticCategory;
   }

   @Override
   public String toString() {
      return "ScatterDataPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            ", object=" + object +
            ", observationDate=" + observationDate +
            ", observationTime=" + observationTime +
            ", frequency=" + frequency +
            ", transceiver=" + transceiver +
            ", scatterType=" + scatterType +
            ", channelNumber=" + channelNumber +
            ", acousticCategory=" + acousticCategory +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ScatterDataPK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey
            && object == that.object
            && observationDate == that.observationDate
            && observationTime == that.observationTime
            && frequency == that.frequency
            && transceiver == that.transceiver
            && scatterType == that.scatterType
            && channelNumber == that.channelNumber
            && acousticCategory == that.acousticCategory;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      result = 31 * result + object;
      result = 31 * result + observationDate;
      result = 31 * result + observationTime;
      result = 31 * result + frequency;
      result = 31 * result + transceiver;
      result = 31 * result + scatterType;
      result = 31 * result + channelNumber;
      result = 31 * result + acousticCategory;
      return result;
   }
}
