package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import no.imr.tools.database.ColumnOrder;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@ColumnOrder({
      "nation",
      "platform",
      "survey",
      "person",

      // Properties:
      // <none>
})
public class Personnel implements BaseSurveyObject<PersonnelPK> {
   private PersonnelPK compId;

   // Properties:
   // <none>

   // Referenced tables:
   private Survey survey;

   public Personnel() {
   }

   public Personnel(PersonnelPK compId) {
      this.compId = compId;
   }

   public Personnel(Survey survey, String person) {
      this(new PersonnelPK(
            survey.getCompId().getNation(),
            survey.getCompId().getPlatform(),
            survey.getCompId().getSurvey(),
            person));
   }

   @EmbeddedId
   @Override
   public PersonnelPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(PersonnelPK compId) {
      this.compId = compId;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "platform", referencedColumnName = "platform"),
         @JoinColumn(name = "survey", referencedColumnName = "survey")
   })
   public Survey getSurvey() {
      return survey;
   }

   public void setSurvey(Survey survey) {
      this.survey = survey;
   }

   @Override
   public String toString() {
      return "Personnel{" +
            "compId=" + compId +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Personnel that
            && Objects.equals(compId, that.compId);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(compId);
   }
}
