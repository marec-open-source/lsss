package no.imr.lsss.database.referencedata;

import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.referencedata.pojo.PojoAcousticCategory;
import no.imr.lsss.database.referencedata.pojo.PojoArea;
import no.imr.lsss.database.referencedata.pojo.PojoBiologicalSpecies;
import no.imr.lsss.database.referencedata.pojo.PojoDatabaseReferenceData;
import no.imr.lsss.database.referencedata.pojo.PojoPlatform;
import no.imr.lsss.database.referencedata.pojo.PojoStandardComment;
import no.imr.lsss.database.tables.hibernate.AcCatToBiologicalSpecies;
import no.imr.lsss.database.tables.hibernate.AcCatToBiologicalSpeciesPK;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryComposite;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryCompositePK;
import no.imr.lsss.database.tables.hibernate.Area;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategory;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.AreaPK;
import no.imr.lsss.database.tables.hibernate.BiologicalSpecies;
import no.imr.lsss.database.tables.hibernate.BiologicalSpeciesPK;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformCodes;
import no.imr.lsss.database.tables.hibernate.PlatformName;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.StandardCommentPK;
import no.imr.tools.Utils;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.time.DateTimeMillis;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DbReferenceDataExporter {
   private DbReferenceDataExporter() {
   }

   public static void export(DatabaseConnection databaseConnection, Path dir) throws IOException {
      FileUtils.createDirectories(dir);

      List<String> includes = new ArrayList<>();

      for (Map.Entry<Short, List<PojoAcousticCategory>> entry : acousticCategories(databaseConnection).entrySet()) {
         PojoDatabaseReferenceData data = new PojoDatabaseReferenceData();
         data.acousticCategories = entry.getValue();
         String fileName = "AcousticCategory-Nation-" + entry.getKey() + ".json";
         includes.add(fileName);
         JsonUtils.writeValuePrettily(dir.resolve(fileName), data);
      }

      for (Map.Entry<Short, List<PojoArea>> entry : areas(databaseConnection).entrySet()) {
         PojoDatabaseReferenceData data = new PojoDatabaseReferenceData();
         data.areas = entry.getValue();
         String fileName = "Area-Nation-" + entry.getKey() + ".json";
         includes.add(fileName);
         JsonUtils.writeValuePrettily(dir.resolve(fileName), data);
      }

      for (Map.Entry<Short, List<PojoBiologicalSpecies>> entry : biologicalSpecies(databaseConnection).entrySet()) {
         PojoDatabaseReferenceData data = new PojoDatabaseReferenceData();
         data.biologicalSpecies = entry.getValue();
         String fileName = "BiologicalSpecies-Nation-" + entry.getKey() + ".json";
         includes.add(fileName);
         JsonUtils.writeValuePrettily(dir.resolve(fileName), data);
      }

      for (Map.Entry<Short, List<PojoPlatform>> entry : platforms(databaseConnection, false).entrySet()) {
         PojoDatabaseReferenceData platforms = new PojoDatabaseReferenceData();
         platforms.platforms = entry.getValue();
         String fileName = "Platform-Nation-" + entry.getKey() + ".json";
         includes.add(fileName);
         JsonUtils.writeValuePrettily(dir.resolve(fileName), platforms);
      }

      for (Map.Entry<Short, List<PojoStandardComment>> entry : standardComments(databaseConnection).entrySet()) {
         PojoDatabaseReferenceData data = new PojoDatabaseReferenceData();
         data.standardComments = entry.getValue();
         String fileName = "StandardComment-Nation-" + entry.getKey() + ".json";
         includes.add(fileName);
         JsonUtils.writeValuePrettily(dir.resolve(fileName), data);
      }

      PojoDatabaseReferenceData data = new PojoDatabaseReferenceData();
      data.includes = includes;
      JsonUtils.writeValuePrettily(dir.resolve("index.json"), data);
   }

   private static Map<Short, List<PojoAcousticCategory>> acousticCategories(DatabaseConnection databaseConnection) {
      Map<NationAndAcousticCategory, List<Integer>> acCatToBiological =
            databaseConnection.executeFetchQuery(LsssQuery.fetch(AcCatToBiologicalSpecies.class)).stream()
                  .map(AcCatToBiologicalSpecies::getCompId)
                  .filter(Utils.distinctBy(pk -> List.of(pk.getNation(), pk.getAcousticCategory(), pk.getBiologicalSpecies())))
                  .collect(Collectors.groupingBy(
                        pk -> new NationAndAcousticCategory(pk.getNation(), pk.getAcousticCategory()),
                        Collectors.mapping(AcCatToBiologicalSpeciesPK::getBiologicalSpecies, Utils.toSortedListCollector())
                  ));
      Map<NationAndAcousticCategory, List<Integer>> acCatToComposite =
            databaseConnection.executeFetchQuery(LsssQuery.fetch(AcousticCategoryComposite.class)).stream()
                  .map(AcousticCategoryComposite::getCompId)
                  .filter(Utils.distinctBy(pk -> List.of(pk.getNation(), pk.getAcousticCategory(), pk.getAcousticCategoryMember())))
                  .collect(Collectors.groupingBy(
                        pk -> new NationAndAcousticCategory(pk.getNation(), pk.getAcousticCategory()),
                        Collectors.mapping(AcousticCategoryCompositePK::getAcousticCategoryMember, Utils.toSortedListCollector())
                  ));
      return databaseConnection.executeFetchQuery(LsssQuery.fetch(AcousticCategory.class)).stream()
            .sorted(Comparator.comparingInt(a -> a.getCompId().getPlatform())) // Sort so that filter uses the smallest platform id.
            .filter(Utils.distinctBy(a -> List.of(a.getCompId().getNation(), a.getCompId().getAcousticCategory())))
            .map(a -> toPojoAcousticCategory(a, acCatToBiological, acCatToComposite))
            .collect(Collectors.groupingBy(
                  PojoAcousticCategory::nation,
                  Utils.toSortedListCollector(Comparator.comparingInt(PojoAcousticCategory::acousticCategoryId))
            ));
   }

   private static PojoAcousticCategory toPojoAcousticCategory(AcousticCategory acousticCategory,
                                                              Map<NationAndAcousticCategory, List<Integer>> acCatToBiological,
                                                              Map<NationAndAcousticCategory, List<Integer>> acCatToComposite) {
      NationAndAcousticCategory nationAndAcousticCategory = new NationAndAcousticCategory(
            acousticCategory.getCompId().getNation(),
            acousticCategory.getCompId().getAcousticCategory()
      );
      return new PojoAcousticCategory(
            acousticCategory.getCompId().getNation(),
            acousticCategory.getCompId().getAcousticCategory(),
            acousticCategory.getInitials(),
            acousticCategory.getEnglishInitials(),
            acousticCategory.getCommonName(),
            acousticCategory.getEnglishName(),
            acCatToBiological.get(nationAndAcousticCategory),
            acCatToComposite.get(nationAndAcousticCategory)
      );
   }

   private static Map<Short, List<PojoArea>> areas(DatabaseConnection databaseConnection) {
      Map<AreaPK, List<Integer>> areaToCategories = databaseConnection.executeFetchQuery(LsssQuery.fetch(AreaOfAcousticCategory.class)).stream()
            .map(AreaOfAcousticCategory::getCompId)
            .filter(Utils.distinctBy(pk -> List.of(pk.getNation(), pk.getArea(), pk.getAcousticCategory())))
            .collect(Collectors.groupingBy(
                  pk -> new AreaPK(pk.getNation(), pk.getArea()),
                  Collectors.mapping(AreaOfAcousticCategoryPK::getAcousticCategory, Utils.toSortedListCollector())
            ));
      return databaseConnection.executeFetchQuery(LsssQuery.fetch(Area.class)).stream()
            .map(area -> toPojoArea(area, areaToCategories))
            .collect(Collectors.groupingBy(
                  PojoArea::nation,
                  Utils.toSortedListCollector(Comparator.comparingInt(PojoArea::areaId))
            ));
   }

   private static PojoArea toPojoArea(Area area, Map<AreaPK, List<Integer>> areaToCategories) {
      return new PojoArea(
            area.getCompId().getNation(),
            area.getCompId().getArea(),
            area.getAreaName(),
            areaToCategories.get(area.getCompId())
      );
   }

   private static Map<Short, List<PojoBiologicalSpecies>> biologicalSpecies(DatabaseConnection databaseConnection) {
      return databaseConnection.executeFetchQuery(LsssQuery.fetch(BiologicalSpecies.class)).stream()
            .map(DbReferenceDataExporter::toPojoBiologicalSpecies)
            .collect(Collectors.groupingBy(
                  PojoBiologicalSpecies::nation,
                  Utils.toSortedListCollector(Comparator.comparingInt(PojoBiologicalSpecies::speciesId))
            ));
   }

   private static PojoBiologicalSpecies toPojoBiologicalSpecies(BiologicalSpecies species) {
      return new PojoBiologicalSpecies(
            species.getCompId().getNation(),
            species.getCompId().getBiologicalSpecies(),
            species.getInitials(),
            species.getCommonName(),
            species.getEnglishName(),
            species.getLatinName(),
            species.getNodc(),
            species.getItis()
      );
   }

   static BiologicalSpecies fromPojoBiologicalSpecies(PojoBiologicalSpecies species) {
      return new BiologicalSpecies(
            new BiologicalSpeciesPK(
                  species.nation(),
                  species.speciesId()
            ),
            species.initials(),
            species.commonName(),
            species.englishName(),
            species.latinName(),
            species.nodc(),
            species.itis()
      );
   }

   static Map<Short, List<PojoPlatform>> platforms(DatabaseConnection databaseConnection, boolean includeDefaultPlatform) {
      Map<PlatformPK, List<PojoPlatform.Name>> platformToNames = databaseConnection.executeFetchQuery(LsssQuery.fetch(PlatformName.class)).stream()
            .collect(Collectors.groupingBy(
                  PlatformPK::new,
                  Collectors.mapping(
                        platformName -> new PojoPlatform.Name(
                              platformName.getPlatformName(),
                              toLocalDate(platformName.getCompId().getFirstValidDate()),
                              toLocalDate(platformName.getLastValidDate())
                        ), Utils.toSortedListCollector(Comparator.comparing(PojoPlatform.Name::firstValidDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                  )
            ));
      Map<PlatformPK, List<PojoPlatform.Code>> platformToCodes = databaseConnection.executeFetchQuery(LsssQuery.fetch(PlatformCodes.class)).stream()
            .collect(Collectors.groupingBy(
                  PlatformPK::new,
                  Collectors.mapping(
                        platformCodes -> new PojoPlatform.Code(
                              platformCodes.getCompId().getPlatformCodeSysName(),
                              platformCodes.getPlatformCode(),
                              toLocalDate(platformCodes.getCompId().getFirstValidDate()),
                              toLocalDate(platformCodes.getLastValidDate())
                        ), Utils.toSortedListCollector(Comparator.comparing(PojoPlatform.Code::codeName)
                              .thenComparing(PojoPlatform.Code::firstValidDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        )
                  )
            ));
      return databaseConnection.executeFetchQuery(LsssQuery.fetch(Platform.class)).stream()
            .filter(platform -> includeDefaultPlatform || platform.getCompId().getPlatform() != 0)
            .map(platform -> toPojoPlatform(platform, platformToNames, platformToCodes))
            .collect(Collectors.groupingBy(
                  PojoPlatform::nation,
                  Utils.toSortedListCollector(Comparator.comparingInt(PojoPlatform::platformId))
            ));
   }

   private static PojoPlatform toPojoPlatform(Platform platform,
                                              Map<PlatformPK, List<PojoPlatform.Name>> platformToNames,
                                              Map<PlatformPK, List<PojoPlatform.Code>> platformToCodes) {
      return new PojoPlatform(
            platform.getCompId().getNation(),
            platform.getCompId().getPlatform(),
            platform.getPlatformType(),
            platform.getPlatformSubType(),
            toLocalDate(platform.getFirstValidDate()),
            toLocalDate(platform.getLastValidDate()),
            platformToNames.get(platform.getCompId()),
            platformToCodes.get(platform.getCompId())
      );
   }

   private static Map<Short, List<PojoStandardComment>> standardComments(DatabaseConnection databaseConnection) {
      return databaseConnection.executeFetchQuery(LsssQuery.fetch(StandardComment.class)).stream()
            .sorted(Comparator.comparingInt(c -> c.getCompId().getPlatform())) // Sort so that filter uses the smallest platform id.
            .filter(Utils.distinctBy(c -> List.of(c.getCompId().getNation(), c.getCompId().getStandardComment())))
            .map(DbReferenceDataExporter::toPojoStandardComment)
            .collect(Collectors.groupingBy(
                  PojoStandardComment::nation,
                  Utils.toSortedListCollector(Comparator.comparingInt(PojoStandardComment::commentId))
            ));
   }

   private static PojoStandardComment toPojoStandardComment(StandardComment standardComment) {
      return new PojoStandardComment(
            standardComment.getCompId().getNation(),
            standardComment.getCompId().getStandardComment(),
            standardComment.getText()
      );
   }

   static StandardComment fromPojoStandardComment(PojoStandardComment pojoStandardComment, short platform) {
      return new StandardComment(
            new StandardCommentPK(
                  pojoStandardComment.nation(),
                  platform,
                  pojoStandardComment.commentId()
            ),
            pojoStandardComment.text()
      );
   }

   private static @Nullable LocalDate toLocalDate(int date) {
      return DateTimeMillis.toLocalDate(date).orElse(null);
   }

   static int fromLocalDate(@Nullable LocalDate localDate) {
      return localDate != null ? DateTimeMillis.localDateToInt(localDate) : 0;
   }

   private record NationAndAcousticCategory(short nation, int acousticCategory) {
   }
}
