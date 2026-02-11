package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import no.imr.tools.database.ColumnOrder;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@Entity
@ColumnOrder({
      "nation",
      "platform",
      "survey",
      "object",
      "schoolObjectType",

      // Properties:
      "observationDate",
      "observationTime",
      "boxDuration",
      "boxThickness",
      "boxLength",
      "maxThickness",
      "maxLength",
      "perimeter",
      "area",
      "roughness",
      "circularity",
      "rectangularity",
      "elongation",
      "distanceToBottom",
      "distanceToSurface",
      "bottomDepth",
      "nmbAcousticCat",
      "nmbHoles",
      "nmbKernels",
})
public class SchoolMorphology implements BaseSurveyObject<SchoolMorphologyPK>, BaseObservationTimeContainer {
   private SchoolMorphologyPK compId;

   // Properties:
   private int observationDate;
   private int observationTime;
   private int boxDuration;
   private float boxThickness;
   private float boxLength;
   private float maxThickness;
   private float maxLength;
   private float perimeter;
   private float area;
   private float roughness;
   private float circularity;
   private float rectangularity;
   private float elongation;
   private float distanceToBottom;
   private float distanceToSurface;
   private float bottomDepth;
   private int nmbAcousticCat;
   private int nmbHoles;
   private int nmbKernels;

   // Referenced tables:
   private ScatterObject scatterObject;
   private SchoolObjectType schoolObjectType;
   private Set<SchoolData> schoolData;

   public SchoolMorphology() {
   }

   public SchoolMorphology(SchoolMorphologyPK compId) {
      this.compId = compId;
   }

   public SchoolMorphology(SchoolMorphologyPK compId, int observationDate, int observationTime, int boxDuration, float boxThickness, float boxLength, float maxThickness, float maxLength, float perimeter, float area, float roughness, float circularity, float rectangularity, float elongation, float distanceToBottom, float distanceToSurface, float bottomDepth, int nmbAcousticCat, int nmbHoles, int nmbKernels) {
      this.compId = compId;
      this.observationDate = observationDate;
      this.observationTime = observationTime;
      this.boxDuration = boxDuration;
      this.boxThickness = boxThickness;
      this.boxLength = boxLength;
      this.maxThickness = maxThickness;
      this.maxLength = maxLength;
      this.perimeter = perimeter;
      this.area = area;
      this.roughness = roughness;
      this.circularity = circularity;
      this.rectangularity = rectangularity;
      this.elongation = elongation;
      this.distanceToBottom = distanceToBottom;
      this.distanceToSurface = distanceToSurface;
      this.bottomDepth = bottomDepth;
      this.nmbAcousticCat = nmbAcousticCat;
      this.nmbHoles = nmbHoles;
      this.nmbKernels = nmbKernels;
   }

   @EmbeddedId
   @Override
   public SchoolMorphologyPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(SchoolMorphologyPK compId) {
      this.compId = compId;
   }

   @Override
   public int getObservationDate() {
      return observationDate;
   }

   @Override
   public void setObservationDate(int observationDate) {
      this.observationDate = observationDate;
   }

   @Override
   public int getObservationTime() {
      return observationTime;
   }

   @Override
   public void setObservationTime(int observationTime) {
      this.observationTime = observationTime;
   }

   public int getBoxDuration() {
      return boxDuration;
   }

   public void setBoxDuration(int boxDuration) {
      this.boxDuration = boxDuration;
   }

   public float getBoxThickness() {
      return boxThickness;
   }

   public void setBoxThickness(float boxThickness) {
      this.boxThickness = boxThickness;
   }

   public float getBoxLength() {
      return boxLength;
   }

   public void setBoxLength(float boxLength) {
      this.boxLength = boxLength;
   }

   public float getMaxThickness() {
      return maxThickness;
   }

   public void setMaxThickness(float maxThickness) {
      this.maxThickness = maxThickness;
   }

   public float getMaxLength() {
      return maxLength;
   }

   public void setMaxLength(float maxLength) {
      this.maxLength = maxLength;
   }

   public float getPerimeter() {
      return perimeter;
   }

   public void setPerimeter(float perimeter) {
      this.perimeter = perimeter;
   }

   public float getArea() {
      return area;
   }

   public void setArea(float area) {
      this.area = area;
   }

   public float getRoughness() {
      return roughness;
   }

   public void setRoughness(float roughness) {
      this.roughness = roughness;
   }

   public float getCircularity() {
      return circularity;
   }

   public void setCircularity(float circularity) {
      this.circularity = circularity;
   }

   public float getRectangularity() {
      return rectangularity;
   }

   public void setRectangularity(float rectangularity) {
      this.rectangularity = rectangularity;
   }

   public float getElongation() {
      return elongation;
   }

   public void setElongation(float elongation) {
      this.elongation = elongation;
   }

   public float getDistanceToBottom() {
      return distanceToBottom;
   }

   public void setDistanceToBottom(float distanceToBottom) {
      this.distanceToBottom = distanceToBottom;
   }

   public float getDistanceToSurface() {
      return distanceToSurface;
   }

   public void setDistanceToSurface(float distanceToSurface) {
      this.distanceToSurface = distanceToSurface;
   }

   public float getBottomDepth() {
      return bottomDepth;
   }

   public void setBottomDepth(float bottomDepth) {
      this.bottomDepth = bottomDepth;
   }

   public int getNmbAcousticCat() {
      return nmbAcousticCat;
   }

   public void setNmbAcousticCat(int nmbAcousticCat) {
      this.nmbAcousticCat = nmbAcousticCat;
   }

   public int getNmbHoles() {
      return nmbHoles;
   }

   public void setNmbHoles(int nmbHoles) {
      this.nmbHoles = nmbHoles;
   }

   public int getNmbKernels() {
      return nmbKernels;
   }

   public void setNmbKernels(int nmbKernels) {
      this.nmbKernels = nmbKernels;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "platform", referencedColumnName = "platform"),
         @JoinColumn(name = "survey", referencedColumnName = "survey"),
         @JoinColumn(name = "object", referencedColumnName = "object")
   })
   public ScatterObject getScatterObject() {
      return scatterObject;
   }

   public void setScatterObject(ScatterObject scatterObject) {
      this.scatterObject = scatterObject;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "SchoolObjectType", referencedColumnName = "SchoolObjectType")
   })
   public SchoolObjectType getSchoolObjectType() {
      return schoolObjectType;
   }

   public void setSchoolObjectType(SchoolObjectType schoolObjectType) {
      this.schoolObjectType = schoolObjectType;
   }

   @OneToMany(mappedBy = "schoolMorphology")
   public Set<SchoolData> getSchoolData() {
      return schoolData;
   }

   public void setSchoolData(Set<SchoolData> schoolData) {
      this.schoolData = schoolData;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolMorphology that
            && Objects.equals(compId, that.compId)
            && observationDate == that.observationDate
            && observationTime == that.observationTime
            && boxDuration == that.boxDuration
            && Float.floatToIntBits(boxThickness) == Float.floatToIntBits(that.boxThickness)
            && Float.floatToIntBits(boxLength) == Float.floatToIntBits(that.boxLength)
            && Float.floatToIntBits(maxThickness) == Float.floatToIntBits(that.maxThickness)
            && Float.floatToIntBits(maxLength) == Float.floatToIntBits(that.maxLength)
            && Float.floatToIntBits(perimeter) == Float.floatToIntBits(that.perimeter)
            && Float.floatToIntBits(area) == Float.floatToIntBits(that.area)
            && Float.floatToIntBits(roughness) == Float.floatToIntBits(that.roughness)
            && Float.floatToIntBits(circularity) == Float.floatToIntBits(that.circularity)
            && Float.floatToIntBits(rectangularity) == Float.floatToIntBits(that.rectangularity)
            && Float.floatToIntBits(elongation) == Float.floatToIntBits(that.elongation)
            && Float.floatToIntBits(distanceToBottom) == Float.floatToIntBits(that.distanceToBottom)
            && Float.floatToIntBits(distanceToSurface) == Float.floatToIntBits(that.distanceToSurface)
            && Float.floatToIntBits(bottomDepth) == Float.floatToIntBits(that.bottomDepth)
            && nmbAcousticCat == that.nmbAcousticCat
            && nmbHoles == that.nmbHoles
            && nmbKernels == that.nmbKernels;
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + observationDate;
      result = 31 * result + observationTime;
      result = 31 * result + boxDuration;
      result = 31 * result + Float.floatToIntBits(boxThickness);
      result = 31 * result + Float.floatToIntBits(boxLength);
      result = 31 * result + Float.floatToIntBits(maxThickness);
      result = 31 * result + Float.floatToIntBits(maxLength);
      result = 31 * result + Float.floatToIntBits(perimeter);
      result = 31 * result + Float.floatToIntBits(area);
      result = 31 * result + Float.floatToIntBits(roughness);
      result = 31 * result + Float.floatToIntBits(circularity);
      result = 31 * result + Float.floatToIntBits(rectangularity);
      result = 31 * result + Float.floatToIntBits(elongation);
      result = 31 * result + Float.floatToIntBits(distanceToBottom);
      result = 31 * result + Float.floatToIntBits(distanceToSurface);
      result = 31 * result + Float.floatToIntBits(bottomDepth);
      result = 31 * result + nmbAcousticCat;
      result = 31 * result + nmbHoles;
      result = 31 * result + nmbKernels;
      return result;
   }
}
