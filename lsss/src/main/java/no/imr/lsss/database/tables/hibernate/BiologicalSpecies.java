package no.imr.lsss.database.tables.hibernate;

import jakarta.persistence.EmbeddedId;
import no.imr.tools.Utils;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class BiologicalSpecies implements BaseNationObject<BiologicalSpeciesPK>, Comparable<BiologicalSpecies> {
   private BiologicalSpeciesPK compId;

   // Properties:
   private String initials;
   private String commonName;
   private String englishName;
   private String latinName;
   private String nodc;
   private int itis;

   // Referenced tables:
   private Nation nation;
   private Set<AcCatToBiologicalSpecies> acousticCategoryToBiologicalSpecies;

   public BiologicalSpecies() {
   }

   public BiologicalSpecies(BiologicalSpeciesPK compId) {
      this.compId = compId;
   }

   @EmbeddedId
   @Override
   public BiologicalSpeciesPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(BiologicalSpeciesPK compId) {
      this.compId = compId;
   }

   public String getInitials() {
      return initials;
   }

   public void setInitials(String initials) {
      this.initials = DatabaseUtils.nullToEmpty(initials);
   }

   public String getCommonName() {
      return commonName;
   }

   public void setCommonName(String commonName) {
      this.commonName = DatabaseUtils.nullToEmpty(commonName);
   }

   public String getEnglishName() {
      return englishName;
   }

   public void setEnglishName(String englishName) {
      this.englishName = DatabaseUtils.nullToEmpty(englishName);
   }

   public String getLatinName() {
      return latinName;
   }

   public void setLatinName(String latinName) {
      this.latinName = DatabaseUtils.nullToEmpty(latinName);
   }

   public String getNodc() {
      return nodc;
   }

   public void setNodc(String nodc) {
      this.nodc = DatabaseUtils.nullToEmpty(nodc);
   }

   public int getItis() {
      return itis;
   }

   public void setItis(int itis) {
      this.itis = itis;
   }

   public Nation getNation() {
      return nation;
   }

   public void setNation(Nation nation) {
      this.nation = nation;
   }

   public Set<AcCatToBiologicalSpecies> getAcousticCategoryToBiologicalSpecies() {
      return acousticCategoryToBiologicalSpecies;
   }

   public void setAcousticCategoryToBiologicalSpecies(Set<AcCatToBiologicalSpecies> acousticCategoryToBiologicalSpecies) {
      this.acousticCategoryToBiologicalSpecies = acousticCategoryToBiologicalSpecies;
   }

   @Override
   public String toString() {
      return initials + " (" + englishName + "/" + latinName + ")";
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof BiologicalSpecies that
            && Objects.equals(compId, that.compId)
            && Objects.equals(initials, that.initials)
            && Objects.equals(commonName, that.commonName)
            && Objects.equals(englishName, that.englishName)
            && Objects.equals(latinName, that.latinName)
            && Objects.equals(nodc, that.nodc)
            && itis == that.itis;
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + Objects.hashCode(initials);
      result = 31 * result + Objects.hashCode(commonName);
      result = 31 * result + Objects.hashCode(englishName);
      result = 31 * result + Objects.hashCode(latinName);
      result = 31 * result + Objects.hashCode(nodc);
      result = 31 * result + itis;
      return result;
   }

   @Override
   public int compareTo(BiologicalSpecies biologicalSpecies) {
      return Utils.compare(comparisonStrings(), biologicalSpecies.comparisonStrings(), Collator.getInstance(Locale.ENGLISH));
   }

   private List<String> comparisonStrings() {
      String str = toString();

      for (int i = 0; i < str.length(); i++) {
         if (Character.isLetter(str.charAt(i))) {
            return List.of(str.substring(i), str.substring(0, i));
         }
      }
      return List.of(str);
   }
}
