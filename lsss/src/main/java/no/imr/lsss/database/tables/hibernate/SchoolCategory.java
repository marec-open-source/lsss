package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class SchoolCategory implements BaseSurveyObject<SchoolCategoryPK> {
   private SchoolCategoryPK compId;

   // Properties:
   // <none>

   // Referenced tables:
   private SchoolCategorySystem schoolCategorySystem;
   private ScatterObject scatterObject;

   public SchoolCategory() {
   }

   public SchoolCategory(SchoolCategoryPK compId) {
      this.compId = compId;
   }

   @EmbeddedId
   @Override
   public SchoolCategoryPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(SchoolCategoryPK compId) {
      this.compId = compId;
   }

   public SchoolCategorySystem getSchoolCategorySystem() {
      return schoolCategorySystem;
   }

   public void setSchoolCategorySystem(SchoolCategorySystem schoolCategorySystem) {
      this.schoolCategorySystem = schoolCategorySystem;
   }

   public ScatterObject getScatterObject() {
      return scatterObject;
   }

   public void setScatterObject(ScatterObject scatterObject) {
      this.scatterObject = scatterObject;
   }

   @Override
   public String toString() {
      return "SchoolCategory{" +
            "compId=" + compId +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolCategory that
            && Objects.equals(compId, that.compId);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(compId);
   }
}
