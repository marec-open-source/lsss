package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import org.jspecify.annotations.Nullable;

@Embeddable
public class PlatformNamePK implements BasePlatformPK {
   private short nation;
   private short platform;
   private int firstValidDate;

   public PlatformNamePK() {
   }

   public PlatformNamePK(short nation, short platform, int firstValidDate) {
      this.nation = nation;
      this.platform = platform;
      this.firstValidDate = firstValidDate;
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

   public int getFirstValidDate() {
      return firstValidDate;
   }

   public void setFirstValidDate(int firstValidDate) {
      this.firstValidDate = firstValidDate;
   }

   @Override
   public String toString() {
      return "PlatformNamePK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", firstValidDate=" + firstValidDate +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PlatformNamePK that
            && nation == that.nation
            && platform == that.platform
            && firstValidDate == that.firstValidDate;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + firstValidDate;
      return result;
   }
}
