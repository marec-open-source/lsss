package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class PlatformCodes implements BasePlatformObject<PlatformCodesPK> {
   private PlatformCodesPK compId;

   // Properties:
   private int lastValidDate;
   private String platformCode;

   // Referenced tables:
   private Platform platform;

   public PlatformCodes() {
   }

   public PlatformCodes(PlatformCodesPK compId) {
      this.compId = compId;
   }

   public PlatformCodes(PlatformCodesPK compId, int lastValidDate, String platformCode) {
      this.compId = compId;
      this.lastValidDate = lastValidDate;
      this.platformCode = platformCode;
   }

   @EmbeddedId
   @Override
   public PlatformCodesPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(PlatformCodesPK compId) {
      this.compId = compId;
   }

   public int getLastValidDate() {
      return lastValidDate;
   }

   public void setLastValidDate(int lastValidDate) {
      this.lastValidDate = lastValidDate;
   }

   public String getPlatformCode() {
      return platformCode;
   }

   public void setPlatformCode(String platformCode) {
      this.platformCode = DatabaseUtils.nullToEmpty(platformCode);
   }

   public Platform getPlatform() {
      return platform;
   }

   public void setPlatform(Platform platform) {
      this.platform = platform;
   }

   @Override
   public String toString() {
      return "PlatformCodes{" +
            "compId=" + compId +
            ", lastValidDate=" + lastValidDate +
            ", platformCode='" + platformCode + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PlatformCodes that
            && Objects.equals(compId, that.compId)
            && lastValidDate == that.lastValidDate
            && Objects.equals(platformCode, that.platformCode);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + lastValidDate;
      result = 31 * result + Objects.hashCode(platformCode);
      return result;
   }
}
