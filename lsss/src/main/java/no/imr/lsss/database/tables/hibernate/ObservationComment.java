package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class ObservationComment implements BaseSurveyObject<ObservationPK> {
   private ObservationPK compId;

   // Properties:
   private int standardComment;
   private int mantissa;
   private int exp;
   private String text;

   // Referenced tables:
   private Observation observation;
   private StandardComment referencedStandardComment;

   public ObservationComment() {
   }

   public ObservationComment(ObservationPK compId, int standardComment, int mantissa, int exp, String text) {
      this.compId = compId;
      this.standardComment = standardComment;
      this.mantissa = mantissa;
      this.exp = exp;
      this.text = text;
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

   public int getStandardComment() {
      return standardComment;
   }

   public void setStandardComment(int standardComment) {
      this.standardComment = standardComment;
   }

   public int getMantissa() {
      return mantissa;
   }

   public void setMantissa(int mantissa) {
      this.mantissa = mantissa;
   }

   public int getExp() {
      return exp;
   }

   public void setExp(int exp) {
      this.exp = exp;
   }

   public String getText() {
      return text;
   }

   public void setText(String text) {
      this.text = DatabaseUtils.nullToEmpty(text);
   }

   public Observation getObservation() {
      return observation;
   }

   public void setObservation(Observation observation) {
      this.observation = observation;
   }

   public StandardComment getReferencedStandardComment() {
      return referencedStandardComment;
   }

   public void setReferencedStandardComment(StandardComment referencedStandardComment) {
      this.referencedStandardComment = referencedStandardComment;
   }

   @Override
   public String toString() {
      return "ObservationComment{" +
            "compId=" + compId +
            ", standardComment=" + standardComment +
            ", mantissa=" + mantissa +
            ", exp=" + exp +
            ", text='" + text + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ObservationComment that
            && Objects.equals(compId, that.compId)
            && standardComment == that.standardComment
            && mantissa == that.mantissa
            && exp == that.exp
            && Objects.equals(text, that.text);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + standardComment;
      result = 31 * result + mantissa;
      result = 31 * result + exp;
      result = 31 * result + Objects.hashCode(text);
      return result;
   }
}
