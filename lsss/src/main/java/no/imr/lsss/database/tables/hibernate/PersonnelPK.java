package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Embeddable
public class PersonnelPK implements BaseSurveyPK {
   private short nation;
   private short platform;
   private int survey;
   private String person;

   public PersonnelPK() {
   }

   public PersonnelPK(short nation, short platform, int survey, String person) {
      this.nation = nation;
      this.platform = platform;
      this.survey = survey;
      this.person = person;
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

   public String getPerson() {
      return person;
   }

   public void setPerson(String person) {
      this.person = DatabaseUtils.nullToEmpty(person);
   }

   @Override
   public String toString() {
      return "PersonnelPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", survey=" + survey +
            ", person='" + person + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PersonnelPK that
            && nation == that.nation
            && platform == that.platform
            && survey == that.survey
            && Objects.equals(person, that.person);
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + survey;
      result = 31 * result + Objects.hashCode(person);
      return result;
   }
}
