package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@Entity
@ColumnOrder({
      "schoolObjectType",

      // Properties:
      "schoolObjectTypeName",
      "schoolObjectTypeDescription",
})
public class SchoolObjectType implements BaseDatabaseObject {
   private short schoolObjectType;

   // Properties:
   private String schoolObjectTypeName;
   private String schoolObjectTypeDescription;

   // Referenced tables:
   private Set<SchoolMorphology> schoolMorphologies;

   public SchoolObjectType() {
   }

   public SchoolObjectType(short schoolObjectType, String schoolObjectTypeName, String schoolObjectTypeDescription) {
      this.schoolObjectType = schoolObjectType;
      this.schoolObjectTypeName = schoolObjectTypeName;
      this.schoolObjectTypeDescription = schoolObjectTypeDescription;
   }

   @Override
   public Object primaryKey() {
      return schoolObjectType;
   }

   @Id
   public short getSchoolObjectType() {
      return schoolObjectType;
   }

   public void setSchoolObjectType(short schoolObjectType) {
      this.schoolObjectType = schoolObjectType;
   }

   @Column(length = 40)
   public String getSchoolObjectTypeName() {
      return schoolObjectTypeName;
   }

   public void setSchoolObjectTypeName(String schoolObjectTypeName) {
      this.schoolObjectTypeName = DatabaseUtils.nullToEmpty(schoolObjectTypeName);
   }

   @Column(length = 80)
   public String getSchoolObjectTypeDescription() {
      return schoolObjectTypeDescription;
   }

   public void setSchoolObjectTypeDescription(String schoolObjectTypeDescription) {
      this.schoolObjectTypeDescription = DatabaseUtils.nullToEmpty(schoolObjectTypeDescription);
   }

   @OneToMany(mappedBy = "schoolObjectType")
   public Set<SchoolMorphology> getSchoolMorphologies() {
      return schoolMorphologies;
   }

   public void setSchoolMorphologies(Set<SchoolMorphology> schoolMorphologies) {
      this.schoolMorphologies = schoolMorphologies;
   }

   @Override
   public String toString() {
      return "SchoolObjectType{" +
            "schoolObjectType=" + schoolObjectType +
            ", schoolObjectTypeName='" + schoolObjectTypeName + '\'' +
            ", schoolObjectTypeDescription='" + schoolObjectTypeDescription + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolObjectType that
            && schoolObjectType == that.schoolObjectType
            && Objects.equals(schoolObjectTypeName, that.schoolObjectTypeName)
            && Objects.equals(schoolObjectTypeDescription, that.schoolObjectTypeDescription);
   }

   @Override
   public int hashCode() {
      int result = schoolObjectType;
      result = 31 * result + Objects.hashCode(schoolObjectTypeName);
      result = 31 * result + Objects.hashCode(schoolObjectTypeDescription);
      return result;
   }
}
