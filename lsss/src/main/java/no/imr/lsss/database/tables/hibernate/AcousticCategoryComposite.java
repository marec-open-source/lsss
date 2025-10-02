package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

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

   public AcousticCategory getAcousticCategoryByNationAndPlatformAndAcousticCategoryMember() {
      return acousticCategoryByNationAndPlatformAndAcousticCategoryMember;
   }

   public void setAcousticCategoryByNationAndPlatformAndAcousticCategoryMember(AcousticCategory acousticCategoryByNationAndPlatformAndAcousticCategoryMember) {
      this.acousticCategoryByNationAndPlatformAndAcousticCategoryMember = acousticCategoryByNationAndPlatformAndAcousticCategoryMember;
   }

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
