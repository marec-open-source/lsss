package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public class ScatterObject implements BaseSurveyObject<ScatterObjectPK>, BaseObservationTimeContainer {
   private ScatterObjectPK compId;

   // Properties:
   private int observationDate;
   private int observationTime;
   private short observationType;
   private int duration;

   // Referenced tables:
   private Observation observation;
   private SchoolDetect schoolDetect;
   private Set<Scatter> scatters;
   private Set<SchoolMorphology> schoolMorphologies;
   private Set<SchoolCategory> schoolCategories;

   public ScatterObject() {
   }

   public ScatterObject(ScatterObjectPK compId, int observationDate, int observationTime, short observationType, int duration) {
      this.compId = compId;
      this.observationDate = observationDate;
      this.observationTime = observationTime;
      this.observationType = observationType;
      this.duration = duration;
   }

   public ScatterObject(ScatterObject scatterObject) {
      compId = scatterObject.compId;
      observationDate = scatterObject.observationDate;
      observationTime = scatterObject.observationTime;
      observationType = scatterObject.observationType;
      duration = scatterObject.duration;
   }

   @EmbeddedId
   @Override
   public ScatterObjectPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(ScatterObjectPK compId) {
      this.compId = compId;
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

   public int getDuration() {
      return duration;
   }

   public void setDuration(int duration) {
      this.duration = duration;
   }

   public Set<Scatter> getScatters() {
      return scatters;
   }

   public void setScatters(Set<Scatter> scatters) {
      this.scatters = scatters;
   }

   public Set<SchoolMorphology> getSchoolMorphologies() {
      return schoolMorphologies;
   }

   public void setSchoolMorphologies(Set<SchoolMorphology> schoolMorphologies) {
      this.schoolMorphologies = schoolMorphologies;
   }

   public Observation getObservation() {
      return observation;
   }

   public void setObservation(Observation observation) {
      this.observation = observation;
   }

   public SchoolDetect getSchoolDetect() {
      return schoolDetect;
   }

   public void setSchoolDetect(SchoolDetect schoolDetect) {
      this.schoolDetect = schoolDetect;
   }

   public Set<SchoolCategory> getSchoolCategories() {
      return schoolCategories;
   }

   public void setSchoolCategories(Set<SchoolCategory> schoolCategories) {
      this.schoolCategories = schoolCategories;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ScatterObject that
            && Objects.equals(compId, that.compId)
            && observationDate == that.observationDate
            && observationTime == that.observationTime
            && observationType == that.observationType
            && duration == that.duration;
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + observationDate;
      result = 31 * result + observationTime;
      result = 31 * result + observationType;
      result = 31 * result + duration;
      return result;
   }

   @Override
   public String toString() {
      return "ScatterObject{" +
            "compId=" + compId +
            ", observationDate=" + observationDate +
            ", observationTime=" + observationTime +
            ", observationType=" + observationType +
            ", duration=" + duration +
            '}';
   }
}
