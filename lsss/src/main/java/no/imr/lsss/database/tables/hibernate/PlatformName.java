package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@Entity
@ColumnOrder({
      "nation",
      "platform",
      "firstValidDate",

      // Properties:
      "lastValidDate",
      "platformName",
})
public class PlatformName implements BasePlatformObject<PlatformNamePK> {
   private PlatformNamePK compId;

   // Properties:
   private int lastValidDate;
   private String platformName;

   // Referenced tables:
   private Platform platform;

   public PlatformName() {
   }

   public PlatformName(PlatformNamePK compId) {
      this.compId = compId;
   }

   public PlatformName(PlatformNamePK compId, int lastValidDate, String platformName) {
      this.compId = compId;
      this.lastValidDate = lastValidDate;
      this.platformName = platformName;
   }

   @EmbeddedId
   @Override
   public PlatformNamePK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(PlatformNamePK compId) {
      this.compId = compId;
   }

   public int getLastValidDate() {
      return lastValidDate;
   }

   public void setLastValidDate(int lastValidDate) {
      this.lastValidDate = lastValidDate;
   }

   @Column(length = 80)
   public String getPlatformName() {
      return platformName;
   }

   public void setPlatformName(String platformName) {
      this.platformName = DatabaseUtils.nullToEmpty(platformName);
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation"),
         @JoinColumn(name = "platform", referencedColumnName = "platform")
   })
   public Platform getPlatform() {
      return platform;
   }

   public void setPlatform(Platform platform) {
      this.platform = platform;
   }

   @Override
   public String toString() {
      return "PlatformName{" +
            "compId=" + compId +
            ", lastValidDate=" + lastValidDate +
            ", platformName='" + platformName + '\'' +
            '}';
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PlatformName that
            && Objects.equals(compId, that.compId)
            && lastValidDate == that.lastValidDate
            && Objects.equals(platformName, that.platformName);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + lastValidDate;
      result = 31 * result + Objects.hashCode(platformName);
      return result;
   }
}
