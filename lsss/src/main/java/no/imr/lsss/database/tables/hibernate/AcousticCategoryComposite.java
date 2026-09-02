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
      "acousticCategoryMember",

      // Properties:
      // <none>
})
@TableWithOnlyPrimaryKeyColumns
public class AcousticCategoryComposite implements BasePlatformObject<AcousticCategoryCompositePK> {
   private AcousticCategoryCompositePK compId;

   // Properties:
   // <none>

   // Referenced tables:
   private AcousticCategory acousticCategoryByNationAndPlatformAndAcousticCategoryMember;
   private AcousticCategory acousticCategoryByNationAndPlatformAndAcousticCategory;

   public AcousticCategoryComposite() {
   }

   public AcousticCategoryComposite(AcousticCategoryCompositePK compId) {
      this.compId = compId;
   }

   @EmbeddedId
   @Override
   public AcousticCategoryCompositePK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(AcousticCategoryCompositePK compId) {
      this.compId = compId;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "platform", referencedColumnName = "platform"),
         @JoinColumn(name = "acousticCategoryMember", referencedColumnName = "acousticCategory")
   })
   public AcousticCategory getAcousticCategoryByNationAndPlatformAndAcousticCategoryMember() {
      return acousticCategoryByNationAndPlatformAndAcousticCategoryMember;
   }

   public void setAcousticCategoryByNationAndPlatformAndAcousticCategoryMember(AcousticCategory acousticCategoryByNationAndPlatformAndAcousticCategoryMember) {
      this.acousticCategoryByNationAndPlatformAndAcousticCategoryMember = acousticCategoryByNationAndPlatformAndAcousticCategoryMember;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "platform", referencedColumnName = "platform"),
         @JoinColumn(name = "acousticCategory", referencedColumnName = "acousticCategory")
   })
   public AcousticCategory getAcousticCategoryByNationAndPlatformAndAcousticCategory() {
      return acousticCategoryByNationAndPlatformAndAcousticCategory;
   }

   public void setAcousticCategoryByNationAndPlatformAndAcousticCategory(AcousticCategory acousticCategoryByNationAndPlatformAndAcousticCategory) {
      this.acousticCategoryByNationAndPlatformAndAcousticCategory = acousticCategoryByNationAndPlatformAndAcousticCategory;
   }

   @Override
   public String toString() {
      return "AcousticCategoryComposite{" +
            "compId=" + compId +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof AcousticCategoryComposite that
            && Objects.equals(compId, that.compId);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(compId);
   }
}
