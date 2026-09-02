package no.imr.lsss.database.referencedata.pojo;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class PojoDatabaseReferenceData {
   public @Nullable List<String> includes;
   public @Nullable List<PojoAcousticCategory> acousticCategories;
   public @Nullable List<PojoArea> areas;
   public @Nullable List<PojoBiologicalSpecies> biologicalSpecies;
   public @Nullable List<PojoPlatform> platforms;
   public @Nullable List<PojoStandardComment> standardComments;

   public PojoDatabaseReferenceData() {
   }
}
