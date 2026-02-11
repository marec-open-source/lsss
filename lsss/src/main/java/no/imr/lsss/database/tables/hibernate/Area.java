package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.text.Collator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Entity
@ColumnOrder({
      "nation",
      "area",

      // Properties:
      "areaName",
})
public class Area implements BaseNationObject<AreaPK>, Comparable<Area> {
   private AreaPK compId;

   // Properties:
   private String areaName;

   // Referenced tables:
   private Nation nation;
   private Set<AreaOfAcousticCategory> areaOfAcousticCategory;

   public Area() {
   }

   public Area(AreaPK compId) {
      this.compId = compId;
   }

   public Area(AreaPK compId, String areaName) {
      this.compId = compId;
      this.areaName = areaName;
   }

   @EmbeddedId
   @Override
   public AreaPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(AreaPK compId) {
      this.compId = compId;
   }

   @Column(length = 80)
   public String getAreaName() {
      return areaName;
   }

   public void setAreaName(String areaName) {
      this.areaName = DatabaseUtils.nullToEmpty(areaName);
   }

   @OneToMany(mappedBy = "area")
   public Set<AreaOfAcousticCategory> getAreaOfAcousticCategory() {
      return areaOfAcousticCategory;
   }

   public void setAreaOfAcousticCategory(Set<AreaOfAcousticCategory> areaOfAcousticCategory) {
      this.areaOfAcousticCategory = areaOfAcousticCategory;
   }

   @ManyToOne(fetch = FetchType.LAZY)
   @MapsId("compId")
   @JoinColumns({
         @JoinColumn(name = "nation", referencedColumnName = "nation")
   })
   public Nation getNation() {
      return nation;
   }

   public void setNation(Nation nation) {
      this.nation = nation;
   }

   @Override
   public String toString() {
      return areaName;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Area that
            && Objects.equals(compId, that.compId)
            && Objects.equals(areaName, that.areaName);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + Objects.hashCode(areaName);
      return result;
   }

   @Override
   public int compareTo(Area area) {
      return Collator.getInstance(Locale.ENGLISH).compare(areaName, area.areaName);
   }
}
