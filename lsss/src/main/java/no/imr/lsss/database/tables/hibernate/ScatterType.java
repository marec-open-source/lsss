package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Id;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public class ScatterType implements BaseDatabaseObject {
   private short scatterType;

   // Properties:
   private String scatterTypeName;

   // Referenced tables:
   private Set<Scatter> scatters;

   public ScatterType() {
   }

   public ScatterType(short scatterType, String scatterTypeName) {
      this.scatterType = scatterType;
      this.scatterTypeName = scatterTypeName;
   }

   @Override
   public Object primaryKey() {
      return scatterType;
   }

   @Id
   public short getScatterType() {
      return scatterType;
   }

   public void setScatterType(short scatterType) {
      this.scatterType = scatterType;
   }

   public String getScatterTypeName() {
      return scatterTypeName;
   }

   public void setScatterTypeName(String scatterTypeName) {
      this.scatterTypeName = DatabaseUtils.nullToEmpty(scatterTypeName);
   }

   public Set<Scatter> getScatters() {
      return scatters;
   }

   public void setScatters(Set<Scatter> scatters) {
      this.scatters = scatters;
   }

   @Override
   public String toString() {
      return "ScatterType{" +
            "scatterType=" + scatterType +
            ", scatterTypeName='" + scatterTypeName + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ScatterType that
            && scatterType == that.scatterType
            && Objects.equals(scatterTypeName, that.scatterTypeName);
   }

   @Override
   public int hashCode() {
      int result = scatterType;
      result = 31 * result + Objects.hashCode(scatterTypeName);
      return result;
   }
}
