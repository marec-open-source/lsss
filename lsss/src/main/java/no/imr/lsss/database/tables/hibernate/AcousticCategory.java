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
import no.imr.tools.Utils;
import no.imr.tools.database.ColumnOrder;
import no.imr.tools.database.DatabaseUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@ColumnOrder({
      "nation",
      "platform",
      "acousticCategory",

      // Properties:
      "composite",
      "initials",
      "englishInitials",
      "commonName",
      "englishName",
})
public class AcousticCategory implements BasePlatformObject<AcousticCategoryPK>, Comparable<AcousticCategory> {
   public static final int RAW_DATA_CATEGORY = 0;

   private AcousticCategoryPK compId;

   // Properties:
   private short composite;
   private String initials;
   private String englishInitials;
   private String commonName;
   private String englishName;

   // Referenced tables:
   private Platform platform;
   private Set<AreaOfAcousticCategory> areaOfAcousticCategory;
   private Set<AcousticCategoryComposite> acousticCategoryCompositesByNationAndPlatformAndAcousticCategoryMember;
   private Set<AcousticCategoryComposite> acousticCategoryCompositesByNationAndPlatformAndAcousticCategory;
   private Set<ScatterData> scatterData;
   private Set<Purpose> purpose;
   private Set<AcCatToBiologicalSpecies> acousticCategoryToBiologicalSpecies;

   public AcousticCategory() {
   }

   public AcousticCategory(AcousticCategoryPK compId, short composite, String initials, String englishInitials, String commonName, String englishName) {
      this.compId = compId;
      this.composite = composite;
      this.initials = initials;
      this.englishInitials = englishInitials;
      this.commonName = commonName;
      this.englishName = englishName;
   }

   @EmbeddedId
   @Override
   public AcousticCategoryPK getCompId() {
      return compId;
   }

   @Override
   public void setCompId(AcousticCategoryPK compId) {
      this.compId = compId;
   }

   public short getComposite() {
      return composite;
   }

   public void setComposite(short composite) {
      this.composite = composite;
   }

   @Column(length = 5)
   public String getInitials() {
      return initials;
   }

   public void setInitials(String initials) {
      this.initials = DatabaseUtils.nullToEmpty(initials);
   }

   @Column(length = 5)
   public String getEnglishInitials() {
      return englishInitials;
   }

   public void setEnglishInitials(String englishInitials) {
      this.englishInitials = DatabaseUtils.nullToEmpty(englishInitials);
   }

   @Column(length = 80)
   public String getCommonName() {
      return commonName;
   }

   public void setCommonName(String commonName) {
      this.commonName = DatabaseUtils.nullToEmpty(commonName);
   }

   @Column(length = 80)
   public String getEnglishName() {
      return englishName;
   }

   public void setEnglishName(String englishName) {
      this.englishName = DatabaseUtils.nullToEmpty(englishName);
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

   @OneToMany(mappedBy = "acousticCategory")
   public Set<AreaOfAcousticCategory> getAreaOfAcousticCategory() {
      return areaOfAcousticCategory;
   }

   public void setAreaOfAcousticCategory(Set<AreaOfAcousticCategory> areaOfAcousticCategory) {
      this.areaOfAcousticCategory = areaOfAcousticCategory;
   }

   @OneToMany(mappedBy = "acousticCategoryByNationAndPlatformAndAcousticCategoryMember")
   public Set<AcousticCategoryComposite> getAcousticCategoryCompositesByNationAndPlatformAndAcousticCategoryMember() {
      return acousticCategoryCompositesByNationAndPlatformAndAcousticCategoryMember;
   }

   public void setAcousticCategoryCompositesByNationAndPlatformAndAcousticCategoryMember(Set<AcousticCategoryComposite> acousticCategoryCompositesByNationAndPlatformAndAcousticCategoryMember) {
      this.acousticCategoryCompositesByNationAndPlatformAndAcousticCategoryMember = acousticCategoryCompositesByNationAndPlatformAndAcousticCategoryMember;
   }

   @OneToMany(mappedBy = "acousticCategoryByNationAndPlatformAndAcousticCategory")
   public Set<AcousticCategoryComposite> getAcousticCategoryCompositesByNationAndPlatformAndAcousticCategory() {
      return acousticCategoryCompositesByNationAndPlatformAndAcousticCategory;
   }

   public void setAcousticCategoryCompositesByNationAndPlatformAndAcousticCategory(Set<AcousticCategoryComposite> acousticCategoryCompositesByNationAndPlatformAndAcousticCategory) {
      this.acousticCategoryCompositesByNationAndPlatformAndAcousticCategory = acousticCategoryCompositesByNationAndPlatformAndAcousticCategory;
   }

   @OneToMany(mappedBy = "acousticCategory")
   public Set<ScatterData> getScatterData() {
      return scatterData;
   }

   public void setScatterData(Set<ScatterData> scatterData) {
      this.scatterData = scatterData;
   }

   @OneToMany(mappedBy = "acousticCategory")
   public Set<Purpose> getPurpose() {
      return purpose;
   }

   public void setPurpose(Set<Purpose> purpose) {
      this.purpose = purpose;
   }

   @OneToMany(mappedBy = "acousticCategory")
   public Set<AcCatToBiologicalSpecies> getAcousticCategoryToBiologicalSpecies() {
      return acousticCategoryToBiologicalSpecies;
   }

   public void setAcousticCategoryToBiologicalSpecies(Set<AcCatToBiologicalSpecies> acousticCategoryToBiologicalSpecies) {
      this.acousticCategoryToBiologicalSpecies = acousticCategoryToBiologicalSpecies;
   }

   @Override
   public String toString() {
      return initials + " (" + commonName + ")";
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof AcousticCategory that
            && Objects.equals(compId, that.compId)
            && composite == that.composite
            && Objects.equals(initials, that.initials)
            && Objects.equals(englishInitials, that.englishInitials)
            && Objects.equals(commonName, that.commonName)
            && Objects.equals(englishName, that.englishName);
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(compId);
      result = 31 * result + composite;
      result = 31 * result + Objects.hashCode(initials);
      result = 31 * result + Objects.hashCode(englishInitials);
      result = 31 * result + Objects.hashCode(commonName);
      result = 31 * result + Objects.hashCode(englishName);
      return result;
   }

   /**
    * Test if this AcousticCategory is the special category for raw data.
    *
    * @return {@code true} if this category represents raw data
    */
   public boolean rawData() {
      return compId.getAcousticCategory() == RAW_DATA_CATEGORY;
   }

   @Override
   public int compareTo(AcousticCategory other) {
      return Utils.compare(comparisonStrings(), other.comparisonStrings(), String.CASE_INSENSITIVE_ORDER);
   }

   public int compareToEnglish(AcousticCategory acousticCategory) {
      return Utils.compare(englishComparisonStrings(), acousticCategory.englishComparisonStrings(), String.CASE_INSENSITIVE_ORDER);
   }

   private List<String> comparisonStrings() {
      return toComparisonStrings(initials, commonName);
   }

   private List<String> englishComparisonStrings() {
      return toComparisonStrings(englishInitials, englishName);
   }

   private static List<String> toComparisonStrings(String initials, String name) {
      int i = Utils.indexOfFirstLetter(initials);
      return i >= 0
            ? List.of(initials.substring(i), initials.substring(0, i), name)
            : List.of(initials, "", name);
   }
}
