package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.TableWithOnlyPrimaryKeyColumns;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@ColumnOrder({
      "nation",
      "platform",
      "survey",
      "object",
      "categorySystem",
      "category",

      // Properties:
      // <none>
})
@TableWithOnlyPrimaryKeyColumns
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

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "categorySystem", referencedColumnName = "categorySystem")
   })
   public SchoolCategorySystem getSchoolCategorySystem() {
      return schoolCategorySystem;
   }

   public void setSchoolCategorySystem(SchoolCategorySystem schoolCategorySystem) {
      this.schoolCategorySystem = schoolCategorySystem;
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
