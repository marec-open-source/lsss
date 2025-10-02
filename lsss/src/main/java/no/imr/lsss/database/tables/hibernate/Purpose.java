package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class Purpose implements BaseSurveyObject<PurposePK> {
   private PurposePK compId;

   // Properties:
   private short purpose;

   // Referenced tables:
   private Survey survey;
   private AcousticCategory acousticCategory;

   public Purpose() {
   }

   public Purpose(PurposePK compId) {
      this.compId = compId;
   }

   public Purpose(PurposePK compId, short purpose, Survey survey, AcousticCategory acousticCategory) {
      this.compId = compId;
      this.purpose = purpose;
      this.survey = survey;
      this.acousticCategory = acousticCategory;
   }

   public Purpose(Survey survey, AcousticCategory acousticCategory, short purpose) {
      this(new PurposePK(
                  survey.getCompId().getNation(),
                  survey.getCompId().getPlatform(),
                  survey.getCompId().getSurvey(),
                  acousticCategory.getCompId().getAcousticCategory()),
            purpose,
            survey,
            acousticCategory);
   }

   @EmbeddedId
   @Override
   public PurposePK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(PurposePK compId) {
      this.compId = compId;
   }

   public short getPurpose() {
      return purpose;
   }

   public void setPurpose(short purpose) {
      this.purpose = purpose;
   }

   public Survey getSurvey() {
      return survey;
   }

   public void setSurvey(Survey survey) {
      this.survey = survey;
   }

   public AcousticCategory getAcousticCategory() {
      return acousticCategory;
   }

   public void setAcousticCategory(AcousticCategory acousticCategory) {
      this.acousticCategory = acousticCategory;
   }

   @Override
   public String toString() {
      return "Purpose{" +
            "compId=" + compId +
            ", purpose=" + purpose +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Purpose that
            && Objects.equals(compId, that.compId)
            && purpose == that.purpose;
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + purpose;
      return result;
   }
}
