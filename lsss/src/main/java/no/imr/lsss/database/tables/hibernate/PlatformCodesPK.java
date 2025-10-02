package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Embeddable;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Embeddable
public class PlatformCodesPK implements BasePlatformPK {
   private short nation;
   private short platform;
   private String platformCodeSysName;
   private int firstValidDate;

   public PlatformCodesPK() {
   }

   public PlatformCodesPK(short nation, short platform, String platformCodeSysName, int firstValidDate) {
      this.nation = nation;
      this.platform = platform;
      this.platformCodeSysName = platformCodeSysName;
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

   public String getPlatformCodeSysName() {
      return platformCodeSysName;
   }

   public void setPlatformCodeSysName(String platformCodeSysName) {
      this.platformCodeSysName = DatabaseUtils.nullToEmpty(platformCodeSysName);
   }

   public int getFirstValidDate() {
      return firstValidDate;
   }

   public void setFirstValidDate(int firstValidDate) {
      this.firstValidDate = firstValidDate;
   }

   @Override
   public String toString() {
      return "PlatformCodesPK{" +
            "nation=" + nation +
            ", platform=" + platform +
            ", platformCodeSysName='" + platformCodeSysName + '\'' +
            ", firstValidDate=" + firstValidDate +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PlatformCodesPK that
            && nation == that.nation
            && platform == that.platform
            && Objects.equals(platformCodeSysName, that.platformCodeSysName)
            && firstValidDate == that.firstValidDate;
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + platform;
      result = 31 * result + Objects.hashCode(platformCodeSysName);
      result = 31 * result + firstValidDate;
      return result;
   }
}
