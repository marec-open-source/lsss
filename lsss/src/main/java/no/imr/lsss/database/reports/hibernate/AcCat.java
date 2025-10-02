package no.imr.lsss.database.reports.hibernate;

/**
 * Acoustic category on surveys.
 */
public record AcCat(
      int acousticCategory,
      short composite,
      String initials,
      String englishInitials,
      String commonName,
      String englishName) {
}
