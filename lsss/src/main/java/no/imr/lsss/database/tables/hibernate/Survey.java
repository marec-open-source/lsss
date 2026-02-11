package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@Entity
@ColumnOrder({
      "nation",
      "platform",
      "survey",

      // Properties:
      "surveyTitle",
      "startDate",
      "startTime",
      "stopDate",
      "stopTime",
      "surveyDescription",
      "boundaryNorth",
      "boundarySouth",
      "boundaryWest",
      "boundaryEast",
})
public class Survey implements BaseSurveyObject<SurveyPK> {
   private SurveyPK compId;

   // Properties:
   private String surveyTitle;
   private int startDate;
   private int startTime;
   private int stopDate;
   private int stopTime;
   private String surveyDescription;
   private float boundaryNorth;
   private float boundarySouth;
   private float boundaryWest;
   private float boundaryEast;

   // Referenced tables:
   private Platform platform;
   private Set<Observation> observations;
   private Set<Purpose> purposes;

   public Survey() {
   }

   public Survey(SurveyPK compId, String surveyTitle,
                 int startDate, int startTime,
                 int stopDate, int stopTime,
                 String surveyDescription,
                 float boundaryNorth, float boundarySouth, float boundaryWest, float boundaryEast) {
      this.compId = compId;
      this.surveyTitle = surveyTitle;
      this.startDate = startDate;
      this.startTime = startTime;
      this.stopDate = stopDate;
      this.stopTime = stopTime;
      this.surveyDescription = surveyDescription;
      this.boundaryNorth = boundaryNorth;
      this.boundarySouth = boundarySouth;
      this.boundaryWest = boundaryWest;
      this.boundaryEast = boundaryEast;
   }

   @EmbeddedId
   @Override
   public SurveyPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(SurveyPK compId) {
      this.compId = compId;
   }

   @Column(length = 80)
   public String getSurveyTitle() {
      return surveyTitle;
   }

   public void setSurveyTitle(String surveyTitle) {
      this.surveyTitle = DatabaseUtils.nullToEmpty(surveyTitle);
   }

   public int getStartDate() {
      return startDate;
   }

   public void setStartDate(int startDate) {
      this.startDate = startDate;
   }

   public int getStartTime() {
      return startTime;
   }

   public void setStartTime(int startTime) {
      this.startTime = startTime;
   }

   public int getStopDate() {
      return stopDate;
   }

   public void setStopDate(int stopDate) {
      this.stopDate = stopDate;
   }

   public int getStopTime() {
      return stopTime;
   }

   public void setStopTime(int stopTime) {
      this.stopTime = stopTime;
   }

   @Column(length = 200)
   public String getSurveyDescription() {
      return surveyDescription;
   }

   public void setSurveyDescription(String surveyDescription) {
      this.surveyDescription = DatabaseUtils.nullToEmpty(surveyDescription);
   }

   public float getBoundaryNorth() {
      return boundaryNorth;
   }

   public void setBoundaryNorth(float boundaryNorth) {
      this.boundaryNorth = boundaryNorth;
   }

   public float getBoundarySouth() {
      return boundarySouth;
   }

   public void setBoundarySouth(float boundarySouth) {
      this.boundarySouth = boundarySouth;
   }

   public float getBoundaryWest() {
      return boundaryWest;
   }

   public void setBoundaryWest(float boundaryWest) {
      this.boundaryWest = boundaryWest;
   }

   public float getBoundaryEast() {
      return boundaryEast;
   }

   public void setBoundaryEast(float boundaryEast) {
      this.boundaryEast = boundaryEast;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "platform", referencedColumnName = "platform")
   })
   public Platform getPlatform() {
      return platform;
   }

   public void setPlatform(Platform platform) {
      this.platform = platform;
   }

   @OneToMany(mappedBy = "survey")
   public Set<Observation> getObservations() {
      return observations;
   }

   public void setObservations(Set<Observation> observations) {
      this.observations = observations;
   }

   @OneToMany(mappedBy = "survey")
   public Set<Purpose> getPurposes() {
      return purposes;
   }

   public void setPurposes(Set<Purpose> purposes) {
      this.purposes = purposes;
   }

   @Override
   public String toString() {
      return "Survey{" +
            "compId=" + compId +
            ", surveyTitle='" + surveyTitle + '\'' +
            ", startDate=" + startDate +
            ", startTime=" + startTime +
            ", stopDate=" + stopDate +
            ", stopTime=" + stopTime +
            ", surveyDescription='" + surveyDescription + '\'' +
            ", boundaryNorth=" + boundaryNorth +
            ", boundarySouth=" + boundarySouth +
            ", boundaryWest=" + boundaryWest +
            ", boundaryEast=" + boundaryEast +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Survey that
            && Objects.equals(compId, that.compId)
            && Objects.equals(surveyTitle, that.surveyTitle)
            && startDate == that.startDate
            && startTime == that.startTime
            && stopDate == that.stopDate
            && stopTime == that.stopTime
            && Objects.equals(surveyDescription, that.surveyDescription)
            && Float.floatToIntBits(boundaryNorth) == Float.floatToIntBits(that.boundaryNorth)
            && Float.floatToIntBits(boundarySouth) == Float.floatToIntBits(that.boundarySouth)
            && Float.floatToIntBits(boundaryWest) == Float.floatToIntBits(that.boundaryWest)
            && Float.floatToIntBits(boundaryEast) == Float.floatToIntBits(that.boundaryEast);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + Objects.hashCode(surveyTitle);
      result = 31 * result + startDate;
      result = 31 * result + startTime;
      result = 31 * result + stopDate;
      result = 31 * result + stopTime;
      result = 31 * result + Objects.hashCode(surveyDescription);
      result = 31 * result + Float.floatToIntBits(boundaryNorth);
      result = 31 * result + Float.floatToIntBits(boundarySouth);
      result = 31 * result + Float.floatToIntBits(boundaryWest);
      result = 31 * result + Float.floatToIntBits(boundaryEast);
      return result;
   }
}
