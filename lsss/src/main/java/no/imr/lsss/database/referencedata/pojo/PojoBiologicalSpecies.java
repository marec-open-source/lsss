package no.imr.lsss.database.referencedata.pojo;

public record PojoBiologicalSpecies(
      short nation,
      int speciesId,
      String initials,
      String commonName,
      String englishName,
      String latinName,
      String nodc,
      int itis
) {
}
