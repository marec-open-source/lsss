package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import no.imr.tools.database.hibernate.BaseCompPK;
import org.jspecify.annotations.Nullable;

@Embeddable
public class PlatformTypePK implements BaseCompPK {
   private short platformType;
   private short platformSubType;

   public PlatformTypePK() {
   }

   public PlatformTypePK(short platformType, short platformSubType) {
      this.platformType = platformType;
      this.platformSubType = platformSubType;
   }

   public short getPlatformType() {
      return platformType;
   }

   public void setPlatformType(short platformType) {
      this.platformType = platformType;
   }

   public short getPlatformSubType() {
      return platformSubType;
   }

   public void setPlatformSubType(short platformSubType) {
      this.platformSubType = platformSubType;
   }

   @Override
   public String toString() {
      return "PlatformTypePK{" +
            "platformType=" + platformType +
            ", platformSubType=" + platformSubType +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PlatformTypePK that
            && platformType == that.platformType
            && platformSubType == that.platformSubType;
   }

   @Override
   public int hashCode() {
      int result = platformType;
      result = 31 * result + platformSubType;
      return result;
   }
}
