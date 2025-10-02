package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public class Observation implements BaseSurveyObject<ObservationPK> {
   private ObservationPK compId;

   // Properties:
   private float distance;
   private float latitude;
   private float longitude;
   private float bottomDepth;

   // Referenced tables:
   private ObservationComment observationComment;
   private Survey survey;
   private ObservationType observationType;
   private Set<Scatter> scatters;
   private Set<ScatterObject> scatterObjects;

   public Observation() {
   }

   public Observation(ObservationPK compId) {
      this.compId = compId;
   }

   public Observation(ObservationPK compId, float distance, float latitude, float longitude, float bottomDepth) {
      this.compId = compId;
      this.distance = distance;
      this.latitude = latitude;
      this.longitude = longitude;
      this.bottomDepth = bottomDepth;
   }

   @EmbeddedId
   @Override
   public ObservationPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(ObservationPK compId) {
      this.compId = compId;
   }

   public float getDistance() {
      return distance;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }

   public float getLatitude() {
      return latitude;
   }

   public void setLatitude(float latitude) {
      this.latitude = latitude;
   }

   public float getLongitude() {
      return longitude;
   }

   public void setLongitude(float longitude) {
      this.longitude = longitude;
   }

   public float getBottomDepth() {
      return bottomDepth;
   }

   public void setBottomDepth(float bottomDepth) {
      this.bottomDepth = bottomDepth;
   }

   public ObservationComment getObservationComment() {
      return observationComment;
   }

   public void setObservationComment(ObservationComment observationComment) {
      this.observationComment = observationComment;
   }

   public Survey getSurvey() {
      return survey;
   }

   public void setSurvey(Survey survey) {
      this.survey = survey;
   }

   public ObservationType getObservationType() {
      return observationType;
   }

   public void setObservationType(ObservationType observationType) {
      this.observationType = observationType;
   }

   public Set<Scatter> getScatters() {
      return scatters;
   }

   public void setScatters(Set<Scatter> scatters) {
      this.scatters = scatters;
   }

   public Set<ScatterObject> getScatterObjects() {
      return scatterObjects;
   }

   public void setScatterObjects(Set<ScatterObject> scatterObjects) {
      this.scatterObjects = scatterObjects;
   }

   @Override
   public String toString() {
      return "Observation{" +
            "compId=" + compId +
            ", distance=" + distance +
            ", latitude=" + latitude +
            ", longitude=" + longitude +
            ", bottomDepth=" + bottomDepth +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Observation that
            && Objects.equals(compId, that.compId)
            && Float.floatToIntBits(distance) == Float.floatToIntBits(that.distance)
            && Float.floatToIntBits(latitude) == Float.floatToIntBits(that.latitude)
            && Float.floatToIntBits(longitude) == Float.floatToIntBits(that.longitude)
            && Float.floatToIntBits(bottomDepth) == Float.floatToIntBits(that.bottomDepth);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + Float.floatToIntBits(distance);
      result = 31 * result + Float.floatToIntBits(latitude);
      result = 31 * result + Float.floatToIntBits(longitude);
      result = 31 * result + Float.floatToIntBits(bottomDepth);
      return result;
   }
}
