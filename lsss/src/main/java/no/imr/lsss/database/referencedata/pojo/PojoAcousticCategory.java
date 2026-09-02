package no.imr.lsss.database.referencedata.pojo;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record PojoAcousticCategory(
      short nation,
      int acousticCategoryId,
      String initials,
      String englishInitials,
      String commonName,
      String englishName,
      @Nullable List<Integer> biologicalSpecies,
      @Nullable List<Integer> compositeAcousticCategories
) {
}
