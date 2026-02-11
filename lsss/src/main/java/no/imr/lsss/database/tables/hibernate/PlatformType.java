package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.hibernate.BaseCompDatabaseObject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@Entity
@ColumnOrder({
      "platformType",
      "platformSubType",

      // Properties:
      "platformTypeName",
      "platformSubTypeName",
})
public class PlatformType implements BaseCompDatabaseObject<PlatformTypePK> {
   private PlatformTypePK compId;

   // Properties:
   private String platformTypeName;
   private String platformSubTypeName;

   // Referenced tables:
   private Set<Platform> platforms;

   public PlatformType() {
   }

   public PlatformType(PlatformTypePK compId, String platformTypeName, String platformSubTypeName) {
      this.compId = compId;
      this.platformTypeName = platformTypeName;
      this.platformSubTypeName = platformSubTypeName;
   }

   @EmbeddedId
   @Override
   public PlatformTypePK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(PlatformTypePK compId) {
      this.compId = compId;
   }

   @Column(length = 80)
   public String getPlatformTypeName() {
      return platformTypeName;
   }

   public void setPlatformTypeName(String platformTypeName) {
      this.platformTypeName = DatabaseUtils.nullToEmpty(platformTypeName);
   }

   @Column(length = 80)
   public String getPlatformSubTypeName() {
      return platformSubTypeName;
   }

   public void setPlatformSubTypeName(String platformSubTypeName) {
      this.platformSubTypeName = DatabaseUtils.nullToEmpty(platformSubTypeName);
   }

   @OneToMany(mappedBy = "referencedPlatformType")
   public Set<Platform> getPlatforms() {
      return platforms;
   }

   public void setPlatforms(Set<Platform> platforms) {
      this.platforms = platforms;
   }

   @Override
   public String toString() {
      return "PlatformType{" +
            "compId=" + compId +
            ", platformTypeName='" + platformTypeName + '\'' +
            ", platformSubTypeName='" + platformSubTypeName + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PlatformType that
            && Objects.equals(compId, that.compId)
            && Objects.equals(platformTypeName, that.platformTypeName)
            && Objects.equals(platformSubTypeName, that.platformSubTypeName);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + Objects.hashCode(platformTypeName);
      result = 31 * result + Objects.hashCode(platformSubTypeName);
      return result;
   }
}
