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
      "observationType",

      // Properties:
      "observationTypeName",
})
public class ObservationType implements BaseDatabaseObject {
   private short observationType;

   // Properties:
   private String observationTypeName;

   // Referenced tables:
   private Set<Observation> observations;

   public ObservationType() {
   }

   public ObservationType(short observationType, String observationTypeName) {
      this.observationType = observationType;
      this.observationTypeName = observationTypeName;
   }

   @Override
   public Object primaryKey() {
      return observationType;
   }

   @Id
   public short getObservationType() {
      return observationType;
   }

   public void setObservationType(short observationType) {
      this.observationType = observationType;
   }

   @Column(length = 80)
   public String getObservationTypeName() {
      return observationTypeName;
   }

   public void setObservationTypeName(String observationTypeName) {
      this.observationTypeName = DatabaseUtils.nullToEmpty(observationTypeName);
   }

   @OneToMany(mappedBy = "observationType")
   public Set<Observation> getObservations() {
      return observations;
   }

   public void setObservations(Set<Observation> observations) {
      this.observations = observations;
   }

   @Override
   public String toString() {
      return "ObservationType{" +
            "observationType=" + observationType +
            ", observationTypeName='" + observationTypeName + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ObservationType that
            && observationType == that.observationType
            && Objects.equals(observationTypeName, that.observationTypeName);
   }

   @Override
   public int hashCode() {
      int result = observationType;
      result = 31 * result + Objects.hashCode(observationTypeName);
      return result;
   }
}
