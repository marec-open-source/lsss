package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class ScatterData implements BaseSurveyObject<ScatterDataPK> {
   private ScatterDataPK compId;

   // Properties:
   private float sa;

   // Referenced tables:
   private Scatter scatter;
   private AcousticCategory acousticCategory;

   public ScatterData() {
   }

   public ScatterData(ScatterDataPK compId, float sa) {
      this.compId = compId;
      this.sa = sa;
   }

   @EmbeddedId
   @Override
   public ScatterDataPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(ScatterDataPK compId) {
      this.compId = compId;
   }

   public float getSa() {
      return sa;
   }

   public void setSa(float sa) {
      this.sa = sa;
   }

   public Scatter getScatter() {
      return scatter;
   }

   public void setScatter(Scatter scatter) {
      this.scatter = scatter;
   }

   public AcousticCategory getAcousticCategory() {
      return acousticCategory;
   }

   public void setAcousticCategory(AcousticCategory acousticCategory) {
      this.acousticCategory = acousticCategory;
   }

   @Override
   public String toString() {
      return "ScatterData{" +
            "compId=" + compId +
            ", sa=" + sa +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ScatterData that
            && Objects.equals(compId, that.compId)
            && Float.floatToIntBits(sa) == Float.floatToIntBits(that.sa);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + Float.floatToIntBits(sa);
      return result;
   }
}
