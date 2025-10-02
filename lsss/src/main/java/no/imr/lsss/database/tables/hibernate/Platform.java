package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public class Platform implements BasePlatformObject<PlatformPK> {
   private PlatformPK compId;

   // Properties:
   private short platformType;
   private short platformSubType;
   private int firstValidDate;
   private int lastValidDate;

   // Referenced tables:
   private Nation nation;
   private Set<AcousticCategory> acousticCategories;
   private Set<Survey> surveys;
   private Set<PlatformName> platformNames;
   private Set<PlatformCodes> platformCodes;

   public Platform() {
   }

   public Platform(PlatformPK compId, short platformType, short platformSubType, int firstValidDate, int lastValidDate) {
      this.compId = compId;
      this.platformType = platformType;
      this.platformSubType = platformSubType;
      this.firstValidDate = firstValidDate;
      this.lastValidDate = lastValidDate;
   }

   @EmbeddedId
   @Override
   public PlatformPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(PlatformPK compId) {
      this.compId = compId;
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

   public int getFirstValidDate() {
      return firstValidDate;
   }

   public void setFirstValidDate(int firstValidDate) {
      this.firstValidDate = firstValidDate;
   }

   public int getLastValidDate() {
      return lastValidDate;
   }

   public void setLastValidDate(int lastValidDate) {
      this.lastValidDate = lastValidDate;
   }

   public Nation getNation() {
      return nation;
   }

   public void setNation(Nation nation) {
      this.nation = nation;
   }

   public Set<AcousticCategory> getAcousticCategories() {
      return acousticCategories;
   }

   public void setAcousticCategories(Set<AcousticCategory> acousticCategories) {
      this.acousticCategories = acousticCategories;
   }

   public Set<Survey> getSurveys() {
      return surveys;
   }

   public void setSurveys(Set<Survey> surveys) {
      this.surveys = surveys;
   }

   public Set<PlatformName> getPlatformNames() {
      return platformNames;
   }

   public void setPlatformNames(Set<PlatformName> platformNames) {
      this.platformNames = platformNames;
   }

   public Set<PlatformCodes> getPlatformCodes() {
      return platformCodes;
   }

   public void setPlatformCodes(Set<PlatformCodes> platformCodes) {
      this.platformCodes = platformCodes;
   }

   public String findLatestPlatformName() {
      PlatformName latestPlatformName = null;
      for (PlatformName platformName : platformNames) {
         int last = platformName.getLastValidDate();
         if (last == 0) {
            return platformName.getPlatformName();
         }
         if (latestPlatformName == null || last > platformName.getLastValidDate()) {
            latestPlatformName = platformName;
         }
      }
      if (latestPlatformName != null) {
         return latestPlatformName.getPlatformName();
      }

      Log.global.warning("No names for platform " + compId.getPlatform());
      return findUnknownPlatformName();
   }

   public String findPlatformName(Survey survey) {
      return findPlatformName(survey.getStartDate());
   }

   public String findPlatformName(int date) {
      for (PlatformName platformName : platformNames) {
         int first = platformName.getCompId().getFirstValidDate();
         int last = platformName.getLastValidDate();
         if ((date >= first || first == 0) && (date <= last || last == 0)) {
            return platformName.getPlatformName();
         }
      }

      Log.global.warning("No name for platform " + compId.getPlatform() + " for date " + date);
      return findUnknownPlatformName();
   }

   private String findUnknownPlatformName() {
      return "?-" + compId.getPlatform();
   }

   @Override
   public String toString() {
      return "Platform{" +
            "compId=" + compId +
            ", platformType=" + platformType +
            ", platformSubType=" + platformSubType +
            ", firstValidDate=" + firstValidDate +
            ", lastValidDate=" + lastValidDate +
            ", nation=" + nation +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Platform that
            && Objects.equals(compId, that.compId)
            && platformType == that.platformType
            && platformSubType == that.platformSubType
            && firstValidDate == that.firstValidDate
            && lastValidDate == that.lastValidDate;
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + platformType;
      result = 31 * result + platformSubType;
      result = 31 * result + firstValidDate;
      result = 31 * result + lastValidDate;
      return result;
   }
}
