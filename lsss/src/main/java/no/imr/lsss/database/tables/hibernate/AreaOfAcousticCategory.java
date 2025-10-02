package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

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

   public Area getArea() {
      return area;
   }

   public void setArea(Area area) {
      this.area = area;
   }

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
