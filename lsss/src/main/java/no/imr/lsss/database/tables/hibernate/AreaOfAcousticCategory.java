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
      "area",

      // Properties:
      // <none>
})
@TableWithOnlyPrimaryKeyColumns
public class AreaOfAcousticCategory implements BasePlatformObject<AreaOfAcousticCategoryPK> {
   private AreaOfAcousticCategoryPK compId;

   // Properties:
   // <none>

   // Referenced tables:
   private Area area;
   private AcousticCategory acousticCategory;

   public AreaOfAcousticCategory() {
   }

   public AreaOfAcousticCategory(AreaOfAcousticCategoryPK compId) {
      this.compId = compId;
   }

   @EmbeddedId
   @Override
   public AreaOfAcousticCategoryPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(AreaOfAcousticCategoryPK compId) {
      this.compId = compId;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "area", referencedColumnName = "area")
   })
   public Area getArea() {
      return area;
   }

   public void setArea(Area area) {
      this.area = area;
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
      return "AreaOfAcousticCategory{" +
            "compId=" + compId +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof AreaOfAcousticCategory that
            && Objects.equals(compId, that.compId);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(compId);
   }
}
