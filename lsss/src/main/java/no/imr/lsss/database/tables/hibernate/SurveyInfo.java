package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class SurveyInfo implements BaseSurveyObject<SurveyInfoPK> {
   private SurveyInfoPK compId;

   // Properties:
   private String infoValue;

   // Referenced tables:
   private Survey survey;

   public SurveyInfo() {
   }

   public SurveyInfo(SurveyInfoPK compId, String infoValue) {
      this.compId = compId;
      this.infoValue = infoValue;
   }

   @EmbeddedId
   @Override
   public SurveyInfoPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(SurveyInfoPK compId) {
      this.compId = compId;
   }

   public String getInfoValue() {
      return infoValue;
   }

   public void setInfoValue(String infoValue) {
      this.infoValue = DatabaseUtils.nullToEmpty(infoValue);
   }

   public Survey getSurvey() {
      return survey;
   }

   public void setSurvey(Survey survey) {
      this.survey = survey;
   }

   @Override
   public String toString() {
      return "SurveyInfo{" +
            "compId=" + compId +
            ", infoValue='" + infoValue + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SurveyInfo that
            && Objects.equals(compId, that.compId)
            && Objects.equals(infoValue, that.infoValue);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + Objects.hashCode(infoValue);
      return result;
   }
}
