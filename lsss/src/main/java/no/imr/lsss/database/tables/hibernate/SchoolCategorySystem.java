package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@ColumnOrder({
      "categorySystem",

      // Properties:
      "categorySystemName",
      "categorySystemDescription",
})
public class SchoolCategorySystem implements BaseDatabaseObject {
   private short categorySystem;

   // Properties:
   private String categorySystemName;
   private String categorySystemDescription;

   // Referenced tables:
   // <none>

   public SchoolCategorySystem() {
   }

   public SchoolCategorySystem(short categorySystem, String categorySystemName, String categorySystemDescription) {
      this.categorySystem = categorySystem;
      this.categorySystemName = categorySystemName;
      this.categorySystemDescription = categorySystemDescription;
   }

   @Override
   public Object primaryKey() {
      return categorySystem;
   }

   @Id
   public short getCategorySystem() {
      return categorySystem;
   }

   public void setCategorySystem(short categorySystem) {
      this.categorySystem = categorySystem;
   }

   @Column(length = 40)
   public String getCategorySystemName() {
      return categorySystemName;
   }

   public void setCategorySystemName(String categorySystemName) {
      this.categorySystemName = DatabaseUtils.nullToEmpty(categorySystemName);
   }

   @Column(length = 40)
   public String getCategorySystemDescription() {
      return categorySystemDescription;
   }

   public void setCategorySystemDescription(String categorySystemDescription) {
      this.categorySystemDescription = DatabaseUtils.nullToEmpty(categorySystemDescription);
   }

   @Override
   public String toString() {
      return "SchoolCategorySystem{" +
            "categorySystem=" + categorySystem +
            ", categorySystemName='" + categorySystemName + '\'' +
            ", categorySystemDescription='" + categorySystemDescription + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolCategorySystem that
            && categorySystem == that.categorySystem
            && Objects.equals(categorySystemName, that.categorySystemName)
            && Objects.equals(categorySystemDescription, that.categorySystemDescription);
   }

   @Override
   public int hashCode() {
      int result = categorySystem;
      result = 31 * result + Objects.hashCode(categorySystemName);
      result = 31 * result + Objects.hashCode(categorySystemDescription);
      return result;
   }
}
