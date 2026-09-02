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
      "acousticCategory",
      "biologicalSpecies",

      // Properties:
      // <none>
})
@TableWithOnlyPrimaryKeyColumns
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

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "biologicalSpecies", referencedColumnName = "biologicalSpecies")
   })
   public BiologicalSpecies getBiologicalSpecies() {
      return biologicalSpecies;
   }

   public void setBiologicalSpecies(BiologicalSpecies biologicalSpecies) {
      this.biologicalSpecies = biologicalSpecies;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "platform", referencedColumnName = "platform"),
         @JoinColumn(name = "acousticCategory", referencedColumnName = "acousticCategory")
   })
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
