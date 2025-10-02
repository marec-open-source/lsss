package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class SchoolData implements BaseSurveyObject<SchoolDataPK> {
   private SchoolDataPK compId;

   // Properties:
   private int frequencyRef;
   private int transceiverRef;
   private float sa;
   private float sl;
   private float rf;
   private float rf_sdev;
   private float sv_mean;
   private float sv_sdev;
   private float skewness;

   // Referenced tables:
   private SchoolMorphology schoolMorphology;

   public SchoolData() {
   }

   public SchoolData(SchoolDataPK compId) {
      this.compId = compId;
   }

   public SchoolData(SchoolDataPK compId, int frequencyRef, int transceiverRef, float sa, float sl, float rf, float rf_sdev, float sv_mean, float sv_sdev, float skewness) {
      this.compId = compId;
      this.frequencyRef = frequencyRef;
      this.transceiverRef = transceiverRef;
      this.sa = sa;
      this.sl = sl;
      this.rf = rf;
      this.rf_sdev = rf_sdev;
      this.sv_mean = sv_mean;
      this.sv_sdev = sv_sdev;
      this.skewness = skewness;
   }

   @EmbeddedId
   @Override
   public SchoolDataPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(SchoolDataPK compId) {
      this.compId = compId;
   }

   public int getFrequencyRef() {
      return frequencyRef;
   }

   public void setFrequencyRef(int frequencyRef) {
      this.frequencyRef = frequencyRef;
   }

   public int getTransceiverRef() {
      return transceiverRef;
   }

   public void setTransceiverRef(int transceiverRef) {
      this.transceiverRef = transceiverRef;
   }

   public float getSa() {
      return sa;
   }

   public void setSa(float sa) {
      this.sa = sa;
   }

   public float getSl() {
      return sl;
   }

   public void setSl(float sl) {
      this.sl = sl;
   }

   public float getRf() {
      return rf;
   }

   public void setRf(float rf) {
      this.rf = rf;
   }

   public float getRf_sdev() {
      return rf_sdev;
   }

   public void setRf_sdev(float rf_sdev) {
      this.rf_sdev = rf_sdev;
   }

   public float getSv_mean() {
      return sv_mean;
   }

   public void setSv_mean(float sv_mean) {
      this.sv_mean = sv_mean;
   }

   public float getSv_sdev() {
      return sv_sdev;
   }

   public void setSv_sdev(float sv_sdev) {
      this.sv_sdev = sv_sdev;
   }

   public float getSkewness() {
      return skewness;
   }

   public void setSkewness(float skewness) {
      this.skewness = skewness;
   }

   public SchoolMorphology getSchoolMorphology() {
      return schoolMorphology;
   }

   public void setSchoolMorphology(SchoolMorphology schoolMorphology) {
      this.schoolMorphology = schoolMorphology;
   }

   @Override
   public String toString() {
      return "SchoolData{" +
            "compId=" + compId +
            ", frequencyRef=" + frequencyRef +
            ", transceiverRef=" + transceiverRef +
            ", sa=" + sa +
            ", sl=" + sl +
            ", rf=" + rf +
            ", rf_sdev=" + rf_sdev +
            ", sv_mean=" + sv_mean +
            ", sv_sdev=" + sv_sdev +
            ", skewness=" + skewness +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolData that
            && Objects.equals(compId, that.compId)
            && frequencyRef == that.frequencyRef
            && transceiverRef == that.transceiverRef
            && Float.floatToIntBits(sa) == Float.floatToIntBits(that.sa)
            && Float.floatToIntBits(sl) == Float.floatToIntBits(that.sl)
            && Float.floatToIntBits(rf) == Float.floatToIntBits(that.rf)
            && Float.floatToIntBits(rf_sdev) == Float.floatToIntBits(that.rf_sdev)
            && Float.floatToIntBits(sv_mean) == Float.floatToIntBits(that.sv_mean)
            && Float.floatToIntBits(sv_sdev) == Float.floatToIntBits(that.sv_sdev)
            && Float.floatToIntBits(skewness) == Float.floatToIntBits(that.skewness);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + frequencyRef;
      result = 31 * result + transceiverRef;
      result = 31 * result + Float.floatToIntBits(sa);
      result = 31 * result + Float.floatToIntBits(sl);
      result = 31 * result + Float.floatToIntBits(rf);
      result = 31 * result + Float.floatToIntBits(rf_sdev);
      result = 31 * result + Float.floatToIntBits(sv_mean);
      result = 31 * result + Float.floatToIntBits(sv_sdev);
      result = 31 * result + Float.floatToIntBits(skewness);
      return result;
   }
}
