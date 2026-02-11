package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Embeddable
public class SurveyInfoPK implements BaseSurveyPK {
   private short nation;
   private short platform;
   private int survey;
   private String infoKey;
   private int infoIndex;

   public SurveyInfoPK() {
   }

   public SurveyInfoPK(SurveyPK surveyPK, String infoKey, int infoIndex) {
      nation = surveyPK.getNation();
      platform = surveyPK.getPlatform();
      survey = surveyPK.getSurvey();
      this.infoKey = infoKey;
      this.infoIndex = infoIndex;
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

   @Column(length = 40)
   public String getInfoKey() {
      return infoKey;
   }

   public void setInfoKey(String infoKey) {
      this.infoKey = DatabaseUtils.nullToEmpty(infoKey);
   }

   public int getInfoIndex() {
      return infoIndex;
   }

   public void setInfoIndex(int infoIndex) {
      this.infoIndex = infoIndex;
   }

   @Override
   public String toString() {
      return "SurveyInfoPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            ", infoKey='" + infoKey + '\'' +
            ", infoIndex='" + infoIndex + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SurveyInfoPK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey
            && Objects.equals(infoKey, that.infoKey)
            && infoIndex == that.infoIndex;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      result = 31 * result + Objects.hashCode(infoKey);
      result = 31 * result + infoIndex;
      return result;
   }
}
