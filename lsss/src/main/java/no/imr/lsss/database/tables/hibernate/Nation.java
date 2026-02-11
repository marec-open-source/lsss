package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

@Entity
@ColumnOrder({
      "nation",

      // Properties:
      "nationName",
})
public class Nation implements BaseDatabaseObject, Comparable<Nation> {
   private short nation;

   // Properties:
   private String nationName;

   // Referenced tables:
   private Set<Platform> platforms;
   private Set<BiologicalSpecies> biologicalSpecies;

   public Nation() {
   }

   public Nation(short nation, String nationName) {
      this.nation = nation;
      this.nationName = nationName;
   }

   @Override
   public Object primaryKey() {
      return nation;
   }

   @Id
   public short getNation() {
      return nation;
   }

   public void setNation(short nation) {
      this.nation = nation;
   }

   @Column(length = 80)
   public String getNationName() {
      return nationName;
   }

   public void setNationName(String nationName) {
      this.nationName = DatabaseUtils.nullToEmpty(nationName);
   }

   @OneToMany(mappedBy = "nation")
   public Set<Platform> getPlatforms() {
      return platforms;
   }

   public void setPlatforms(Set<Platform> platforms) {
      this.platforms = platforms;
   }

   @OneToMany(mappedBy = "nation")
   public Set<BiologicalSpecies> getBiologicalSpecies() {
      return biologicalSpecies;
   }

   public void setBiologicalSpecies(Set<BiologicalSpecies> biologicalSpecies) {
      this.biologicalSpecies = biologicalSpecies;
   }

   @Override
   public String toString() {
      return nationName + " (" + nation + ")";
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof Nation that
            && nation == that.nation
            && Objects.equals(nationName, that.nationName);
   }

   @Override
   public int hashCode() {
      int result = nation;
      result = 31 * result + Objects.hashCode(nationName);
      return result;
   }

   @Override
   public int compareTo(Nation nation) {
      return nationName.compareTo(nation.nationName);
   }
}
