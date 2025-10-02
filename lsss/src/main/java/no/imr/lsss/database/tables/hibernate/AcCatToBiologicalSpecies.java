package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class AcCatToBiologicalSpecies implements BasePlatformObject<AcCatToBiologicalSpeciesPK> {
   private AcCatToBiologicalSpeciesPK compId;

   // Properties:
   // <none>

   // Referenced tables:
   private BiologicalSpecies biologicalSpecies;
   private AcousticCategory acousticCategory;

   public AcCatToBiologicalSpecies() {
   }

   public AcCatToBiologicalSpecies(AcCatToBiologicalSpeciesPK compId) {
      this.compId = compId;
   }

   @EmbeddedId
   @Override
   public AcCatToBiologicalSpeciesPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(AcCatToBiologicalSpeciesPK compId) {
      this.compId = compId;
   }

   public BiologicalSpecies getBiologicalSpecies() {
      return biologicalSpecies;
   }

   public void setBiologicalSpecies(BiologicalSpecies biologicalSpecies) {
      this.biologicalSpecies = biologicalSpecies;
   }

   public AcousticCategory getAcousticCategory() {
      return acousticCategory;
   }

   public void setAcousticCategory(AcousticCategory acousticCategory) {
      this.acousticCategory = acousticCategory;
   }

   @Override
   public String toString() {
      return "AcCatToBiologicalSpecies{" +
            "compId=" + compId +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof AcCatToBiologicalSpecies that
            && Objects.equals(compId, that.compId);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(compId);
   }
}
