package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class SchoolDetect implements BaseSurveyObject<ScatterObjectPK> {
   private ScatterObjectPK compId;

   // Properties:
   private float detectionThreshold;
   private float detectionThresholdHole;
   private float minSizeHole;
   private float smoothVertical;
   private float smoothHorizontal;
   private String detectionSettings;
   private short manuallyCorrected;

   // Referenced tables:
   private ScatterObject scatterObject;

   public SchoolDetect() {
   }

   public SchoolDetect(ScatterObjectPK compId, float detectionThreshold, float detectionThresholdHole, float minSizeHole, float smoothVertical, float smoothHorizontal, String detectionSettings, short manuallyCorrected) {
      this.compId = compId;
      this.detectionThreshold = detectionThreshold;
      this.detectionThresholdHole = detectionThresholdHole;
      this.minSizeHole = minSizeHole;
      this.smoothVertical = smoothVertical;
      this.smoothHorizontal = smoothHorizontal;
      this.detectionSettings = detectionSettings;
      this.manuallyCorrected = manuallyCorrected;
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

   public float getDetectionThreshold() {
      return detectionThreshold;
   }

   public void setDetectionThreshold(float detectionThreshold) {
      this.detectionThreshold = detectionThreshold;
   }

   public float getDetectionThresholdHole() {
      return detectionThresholdHole;
   }

   public void setDetectionThresholdHole(float detectionThresholdHole) {
      this.detectionThresholdHole = detectionThresholdHole;
   }

   public float getMinSizeHole() {
      return minSizeHole;
   }

   public void setMinSizeHole(float minSizeHole) {
      this.minSizeHole = minSizeHole;
   }

   public float getSmoothVertical() {
      return smoothVertical;
   }

   public void setSmoothVertical(float smoothVertical) {
      this.smoothVertical = smoothVertical;
   }

   public float getSmoothHorizontal() {
      return smoothHorizontal;
   }

   public void setSmoothHorizontal(float smoothHorizontal) {
      this.smoothHorizontal = smoothHorizontal;
   }

   public String getDetectionSettings() {
      return detectionSettings;
   }

   public void setDetectionSettings(String detectionSettings) {
      this.detectionSettings = DatabaseUtils.nullToEmpty(detectionSettings);
   }

   public short getManuallyCorrected() {
      return manuallyCorrected;
   }

   public void setManuallyCorrected(short manuallyCorrected) {
      this.manuallyCorrected = manuallyCorrected;
   }

   //maybe not necessary
   //bug https://opensource.atlassian.com/projects/hibernate/browse/ANN-300
   public ScatterObject getScatterObject() {
      return scatterObject;
   }

   public void setScatterObject(ScatterObject scatterObject) {
      this.scatterObject = scatterObject;
   }

   @Override
   public String toString() {
      return "SchoolDetect{" +
            "compId=" + compId +
            ", detectionThreshold=" + detectionThreshold +
            ", detectionThresholdHole=" + detectionThresholdHole +
            ", minSizeHole=" + minSizeHole +
            ", smoothVertical=" + smoothVertical +
            ", smoothHorizontal=" + smoothHorizontal +
            ", detectionSettings='" + detectionSettings + '\'' +
            ", manuallyCorrected=" + manuallyCorrected +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolDetect that
            && Objects.equals(compId, that.compId)
            && Float.floatToIntBits(detectionThreshold) == Float.floatToIntBits(that.detectionThreshold)
            && Float.floatToIntBits(detectionThresholdHole) == Float.floatToIntBits(that.detectionThresholdHole)
            && Float.floatToIntBits(minSizeHole) == Float.floatToIntBits(that.minSizeHole)
            && Float.floatToIntBits(smoothVertical) == Float.floatToIntBits(that.smoothVertical)
            && Float.floatToIntBits(smoothHorizontal) == Float.floatToIntBits(that.smoothHorizontal)
            && Objects.equals(detectionSettings, that.detectionSettings)
            && manuallyCorrected == that.manuallyCorrected;
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + Float.floatToIntBits(detectionThreshold);
      result = 31 * result + Float.floatToIntBits(detectionThresholdHole);
      result = 31 * result + Float.floatToIntBits(minSizeHole);
      result = 31 * result + Float.floatToIntBits(smoothVertical);
      result = 31 * result + Float.floatToIntBits(smoothHorizontal);
      result = 31 * result + Objects.hashCode(detectionSettings);
      result = 31 * result + manuallyCorrected;
      return result;
   }
}
