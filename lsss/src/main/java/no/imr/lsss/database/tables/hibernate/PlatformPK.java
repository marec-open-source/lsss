package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class PlatformPK implements BasePlatformPK {
   private short nation;
   private short platform;

   public PlatformPK() {
   }

   public PlatformPK(short nation, short platform) {
      this.nation = nation;
      this.platform = platform;
   }

   @Override
   public short getNation() {
      return nation;
   }

   @Override
   public void setNation(short nation) {
      this.nation = nation;
   }

   @Override
   public short getPlatform() {
      return platform;
   }

   @Override
   public void setPlatform(short platform) {
      this.platform = platform;
   }

   @Override
   public String toString() {
      return "PlatformPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PlatformPK that
            && nation == that.nation
            && platform == that.platform;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      return result;
   }
}
