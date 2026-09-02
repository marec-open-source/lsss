package no.imr.lsss.database.referencedata;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseData;
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
import no.imr.lsss.database.tables.hibernate.AcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.Area;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategory;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.AreaPK;
import no.imr.lsss.database.tables.hibernate.BasePlatformPK;
import no.imr.lsss.database.tables.hibernate.BiologicalSpecies;
import no.imr.lsss.database.tables.hibernate.BiologicalSpeciesPK;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformCodes;
import no.imr.lsss.database.tables.hibernate.PlatformCodesPK;
import no.imr.lsss.database.tables.hibernate.PlatformName;
import no.imr.lsss.database.tables.hibernate.PlatformNamePK;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.StandardCommentPK;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.JsonUtils;
import org.hibernate.StatelessSession;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DbReferenceDataImporter {
   private final Set<URI> importedURIs = new HashSet<>();
   private final Map<Short, NationData> nationDataMap = new HashMap<>();

   private DbReferenceDataImporter() {
   }

   public static void download(URI sourceUri, Path destinationFile, AsyncHandle asyncHandle) throws IOException {
      DbReferenceDataImporter importer = new DbReferenceDataImporter();
      importer.importContent(sourceUri, asyncHandle);
      importer.saveToFile(destinationFile);
   }

   public static void doImport(LSSS lsss, URI uri, AsyncHandle asyncHandle) throws IOException {
      DbReferenceDataImporter importer = new DbReferenceDataImporter();
      importer.importContent(uri, asyncHandle);
      importer.saveToDatabase(lsss.getDatabaseManager().getDatabaseConnection(), asyncHandle);
      lsss.getDatabaseManager().getConnectionManager().resetDatabaseData();
   }

   private void importContent(URI uri, AsyncHandle asyncHandle) throws IOException {
      if (asyncHandle.isCancelled()) {
         return;
      }
      if (!importedURIs.add(uri)) {
         return;
      }
      Log.global.info("Reading " + uri);
      PojoDatabaseReferenceData data = JsonUtils.readValue(uri.toURL(), PojoDatabaseReferenceData.class);
      for (String include : nullToEmpty(data.includes)) {
         URI includeURI = uri.resolve(include);
         importContent(includeURI, asyncHandle);
      }
      for (PojoAcousticCategory pojoAcousticCategory : nullToEmpty(data.acousticCategories)) {
         AcousticCategoryPK pk = new AcousticCategoryPK(
               pojoAcousticCategory.nation(),
               (short) 0,
               pojoAcousticCategory.acousticCategoryId()
         );
         getNationData(pk.getNation()).acousticCategories.put(pk, pojoAcousticCategory);
      }
      for (PojoArea pojoArea : nullToEmpty(data.areas)) {
         AreaPK pk = new AreaPK(pojoArea.nation(), pojoArea.areaId());
         getNationData(pk.getNation()).areas.put(pk, pojoArea);
      }
      for (PojoBiologicalSpecies pojoBiologicalSpecies : nullToEmpty(data.biologicalSpecies)) {
         BiologicalSpeciesPK pk = new BiologicalSpeciesPK(pojoBiologicalSpecies.nation(), pojoBiologicalSpecies.speciesId());
         getNationData(pk.getNation()).biologicalSpeciesMap.put(pk, pojoBiologicalSpecies);
      }
      for (PojoPlatform pojoPlatform : nullToEmpty(data.platforms)) {
         PlatformPK pk = new PlatformPK(pojoPlatform.nation(), (short) pojoPlatform.platformId());
         getNationData(pk.getNation()).platforms.put(pk, pojoPlatform);
      }
      for (PojoStandardComment pojoStandardComment : nullToEmpty(data.standardComments)) {
         StandardCommentPK pk = new StandardCommentPK(pojoStandardComment.nation(), (short) 0, pojoStandardComment.commentId());
         getNationData(pojoStandardComment.nation()).standardComments.put(pk, pojoStandardComment);
      }
   }

   private NationData getNationData(short nation) {
      return nationDataMap.computeIfAbsent(nation, NationData::new);
   }

   private void saveToFile(Path file) throws IOException {
      FileUtils.createDirectories(file.getParent());
      PojoDatabaseReferenceData data = new PojoDatabaseReferenceData();
      Collection<NationData> sorted = new TreeMap<>(nationDataMap).values();
      data.acousticCategories = emptyToNull(sorted.stream().flatMap(nationData -> nationData.acousticCategories.values().stream()).toList());
      data.areas = emptyToNull(sorted.stream().flatMap(nationData -> nationData.areas.values().stream()).toList());
      data.biologicalSpecies = emptyToNull(sorted.stream().flatMap(nationData -> nationData.biologicalSpeciesMap.values().stream()).toList());
      data.platforms = emptyToNull(sorted.stream().flatMap(nationData -> nationData.platforms.values().stream()).toList());
      data.standardComments = emptyToNull(sorted.stream().flatMap(nationData -> nationData.standardComments.values().stream()).toList());
      JsonUtils.writeValuePrettily(file, data);
   }

   private void saveToDatabase(DatabaseConnection databaseConnection, AsyncHandle asyncHandle) {
      Map<PlatformPK, DbPlatform> existingPlatforms = DbReferenceDataExporter.platforms(databaseConnection, true).values().stream()
            .flatMap(List::stream)
            .map(DbPlatform::from)
            .collect(Collectors.toMap(p -> p.platform.getCompId(), Function.identity()));

      for (NationData nationData : nationDataMap.values()) {
         saveToDatabase(nationData, existingPlatforms, databaseConnection, asyncHandle);
      }
   }

   private static void saveToDatabase(NationData nationData, Map<PlatformPK, DbPlatform> existingPlatforms, DatabaseConnection databaseConnection, AsyncHandle asyncHandle) {
      PlatformPK defaultPlatformPK = new PlatformPK(nationData.nation, (short) 0);
      if (!nationData.platforms.containsKey(defaultPlatformPK) && !existingPlatforms.containsKey(defaultPlatformPK)) {
         nationData.platforms.put(defaultPlatformPK, new PojoPlatform(
               nationData.nation,
               0,
               0,
               0,
               null,
               null,
               List.of(new PojoPlatform.Name("Default platform", null, null)),
               null
         ));
      }

      databaseConnection.executeStatelessQuery(session -> {
         for (Map.Entry<PlatformPK, PojoPlatform> entry : nationData.platforms.entrySet()) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            DbPlatform existingPlatform = existingPlatforms.get(entry.getKey());
            DbPlatform wantedPlatform = DbPlatform.from(entry.getValue());
            if (!wantedPlatform.equals(existingPlatform)) {
               Log.global.info("Updating platform for nation=" + nationData.nation + ", platform=" + entry.getKey().getPlatform());
               sync(wantedPlatform.platform, existingPlatform != null ? existingPlatform.platform : null, session);
               sync(wantedPlatform.names, existingPlatform != null ? existingPlatform.names : null, session);
               sync(wantedPlatform.codes, existingPlatform != null ? existingPlatform.codes : null, session);
            }
         }

         if (!nationData.biologicalSpeciesMap.isEmpty()) {
            Map<BiologicalSpeciesPK, BiologicalSpecies> existingBiologicalSpecies =
                  LsssQuery.fetch(BiologicalSpecies.class, DatabaseData.NATION, nationData.nation).createSelectionQuery(session).stream()
                        .collect(Collectors.toMap(BiologicalSpecies::getCompId, Function.identity()));

            List<BiologicalSpecies> toBeSaved = nationData.biologicalSpeciesMap.values().stream()
                  .map(DbReferenceDataExporter::fromPojoBiologicalSpecies)
                  .filter(b -> !b.equals(existingBiologicalSpecies.get(b.getCompId())))
                  .toList();
            if (!toBeSaved.isEmpty()) {
               Log.global.info("Updating " + toBeSaved.size() + " biological species for nation=" + nationData.nation);
               session.upsertMultiple(toBeSaved);
            }
         }

         if (!nationData.acousticCategories.isEmpty()) {
            Map<AcousticCategoryPK, AcousticCategory> existingAcousticCategories =
                  LsssQuery.fetch(AcousticCategory.class, DatabaseData.NATION, nationData.nation).createSelectionQuery(session).stream()
                        .collect(Collectors.toMap(AcousticCategory::getCompId, Function.identity()));

            Map<AcousticCategoryPK, Set<AcCatToBiologicalSpecies>> existingAcCatToBiologicalSpecies =
                  LsssQuery.fetch(AcCatToBiologicalSpecies.class, DatabaseData.NATION, nationData.nation).createSelectionQuery(session).stream()
                        .collect(Collectors.groupingBy(
                              a -> new AcousticCategoryPK(a.getCompId().getNation(), a.getCompId().getPlatform(), a.getCompId().getAcousticCategory()),
                              Collectors.toSet()
                        ));

            Map<AcousticCategoryPK, Set<AcousticCategoryComposite>> existingAcousticCategoryComposites =
                  LsssQuery.fetch(AcousticCategoryComposite.class, DatabaseData.NATION, nationData.nation).createSelectionQuery(session).stream()
                        .collect(Collectors.groupingBy(
                              a -> new AcousticCategoryPK(a.getCompId().getNation(), a.getCompId().getPlatform(), a.getCompId().getAcousticCategory()),
                              Collectors.toSet()
                        ));

            for (short platform : toPlatforms(existingAcousticCategories.keySet())) {
               List<DbAcousticCategory> toBeSaved = nationData.acousticCategories.values().stream()
                     .map(a -> DbAcousticCategory.from(a, platform))
                     .filter(a -> !a.acousticCategory.equals(existingAcousticCategories.get(a.acousticCategory.getCompId()))
                           || !a.biologicalSpecies.equals(existingAcCatToBiologicalSpecies.getOrDefault(a.acousticCategory.getCompId(), Set.of()))
                           || !a.composites.equals(existingAcousticCategoryComposites.getOrDefault(a.acousticCategory.getCompId(), Set.of()))
                     )
                     .toList();
               if (!toBeSaved.isEmpty()) {
                  Log.global.info("Updating " + toBeSaved.size() + " acoustic categories for nation=" + nationData.nation + ", platform=" + platform);
                  for (DbAcousticCategory dbAcousticCategory : toBeSaved) {
                     // Save all AcousticCategory before saving any AcousticCategoryComposite.
                     if (asyncHandle.isCancelled()) {
                        return;
                     }
                     sync(dbAcousticCategory.acousticCategory, existingAcousticCategories.get(dbAcousticCategory.acousticCategory.getCompId()), session);
                  }
                  for (DbAcousticCategory dbAcousticCategory : toBeSaved) {
                     if (asyncHandle.isCancelled()) {
                        return;
                     }
                     sync(dbAcousticCategory.biologicalSpecies, existingAcCatToBiologicalSpecies.get(dbAcousticCategory.acousticCategory.getCompId()), session);
                     sync(dbAcousticCategory.composites, existingAcousticCategoryComposites.get(dbAcousticCategory.acousticCategory.getCompId()), session);
                  }
               }
            }
         }

         if (!nationData.areas.isEmpty()) {
            Map<AreaPK, Area> existingAreas =
                  LsssQuery.fetch(Area.class, DatabaseData.NATION, nationData.nation).createSelectionQuery(session).stream()
                        .collect(Collectors.toMap(Area::getCompId, Function.identity()));

            Map<Short, Map<AreaPK, Set<AreaOfAcousticCategory>>> existingPlatformToAreaOfAcousticCategories =
                  LsssQuery.fetch(AreaOfAcousticCategory.class, DatabaseData.NATION, nationData.nation).createSelectionQuery(session).stream()
                        .collect(Collectors.groupingBy(
                              a -> a.getCompId().getPlatform(),
                              Collectors.groupingBy(
                                    a -> new AreaPK(a.getCompId().getNation(), a.getCompId().getArea()),
                                    Collectors.toSet()
                              )));

            List<Area> areasToBeSaved = nationData.areas.values().stream()
                  .map(a -> new Area(new AreaPK(a.nation(), a.areaId()), a.areaName()))
                  .filter(a -> !a.equals(existingAreas.get(a.getCompId())))
                  .toList();
            if (!areasToBeSaved.isEmpty()) {
               Log.global.info("Updating " + areasToBeSaved.size() + " areas for nation=" + nationData.nation);
               session.upsertMultiple(areasToBeSaved);
            }

            List<Short> platformsWithAcousticCategories = session
                  .createSelectionQuery("select distinct x.compId.platform from AcousticCategory x where x.compId.nation=" + nationData.nation, short.class)
                  .list();
            for (short platform : platformsWithAcousticCategories) {
               int updateCount = 0;
               for (PojoArea pojoArea : nationData.areas.values()) {
                  if (asyncHandle.isCancelled()) {
                     return;
                  }
                  Set<AreaOfAcousticCategory> wantedAreaOfAcousticCategories = nullToEmpty(pojoArea.acousticCategories()).stream()
                        .map(acousticCategory -> new AreaOfAcousticCategory(new AreaOfAcousticCategoryPK(nationData.nation, platform, acousticCategory, pojoArea.areaId())))
                        .collect(Collectors.toSet());
                  Set<AreaOfAcousticCategory> existing = existingPlatformToAreaOfAcousticCategories
                        .getOrDefault(platform, Map.of())
                        .get(new AreaPK(nationData.nation, pojoArea.areaId()));
                  if (!wantedAreaOfAcousticCategories.equals(existing)) {
                     updateCount++;
                     sync(wantedAreaOfAcousticCategories, existing, session);
                  }
               }
               if (updateCount > 0) {
                  Log.global.info("Updated " + updateCount + " areas with acoustic category mapping for nation=" + nationData.nation + ", platform=" + platform);
               }
            }
         }

         if (!nationData.standardComments.isEmpty()) {
            Map<StandardCommentPK, StandardComment> existingStandardComments =
                  LsssQuery.fetch(StandardComment.class, DatabaseData.NATION, nationData.nation).createSelectionQuery(session).stream()
                        .collect(Collectors.toMap(StandardComment::getCompId, Function.identity()));

            for (short platform : toPlatforms(existingStandardComments.keySet())) {
               if (asyncHandle.isCancelled()) {
                  return;
               }
               List<StandardComment> toBeSaved = nationData.standardComments.values().stream()
                     .map(c -> DbReferenceDataExporter.fromPojoStandardComment(c, platform))
                     .filter(c -> !c.equals(existingStandardComments.get(c.getCompId())))
                     .toList();
               if (!toBeSaved.isEmpty()) {
                  Log.global.info("Updating " + toBeSaved.size() + " standard comments for nation=" + nationData.nation + ", platform=" + platform);
                  session.upsertMultiple(toBeSaved);
               }
            }
         }
      });
   }

   private static List<Short> toPlatforms(Collection<? extends BasePlatformPK> platformPKs) {
      return Stream.concat(
                  Stream.of((short) 0),
                  platformPKs.stream().map(BasePlatformPK::getPlatform)
            )
            .distinct()
            .sorted()
            .toList();
   }

   private static <T> List<T> nullToEmpty(@Nullable List<T> list) {
      return list != null ? list : List.of();
   }

   private static <T> @Nullable List<T> emptyToNull(List<T> list) {
      return list.isEmpty() ? null : list;
   }

   private static <T extends BaseDatabaseObject> void sync(T wanted, @Nullable T existing, StatelessSession session) {
      if (!wanted.equals(existing)) {
         session.upsert(wanted);
      }
   }

   private static <T extends BaseDatabaseObject> void sync(Collection<T> wanted, @Nullable Collection<T> existing, StatelessSession session) {
      if (existing == null) {
         existing = Set.of();
      }
      Set<Object> existingPKs = existing.stream().map(BaseDatabaseObject::primaryKey).collect(Collectors.toSet());
      for (T w : wanted) {
         if (existingPKs.contains(w.primaryKey())) {
            if (!existing.contains(w)) {
               session.update(w);
            }
         } else {
            session.insert(w);
         }
      }
      Set<Object> wantedPKs = wanted.stream().map(BaseDatabaseObject::primaryKey).collect(Collectors.toSet());
      session.deleteMultiple(existing.stream().filter(e -> !wantedPKs.contains(e.primaryKey())).toList());
   }

   private record DbAcousticCategory(
         AcousticCategory acousticCategory,
         Set<AcCatToBiologicalSpecies> biologicalSpecies,
         Set<AcousticCategoryComposite> composites

   ) {
      private static DbAcousticCategory from(PojoAcousticCategory a, short platform) {
         Set<AcCatToBiologicalSpecies> biologicalSpecies = nullToEmpty(a.biologicalSpecies()).stream()
               .map(x -> new AcCatToBiologicalSpecies(new AcCatToBiologicalSpeciesPK(a.nation(), platform, a.acousticCategoryId(), x)))
               .filter(Utils.distinctBy(AcCatToBiologicalSpecies::getCompId))
               .collect(Collectors.toSet());
         Set<AcousticCategoryComposite> composites = nullToEmpty(a.compositeAcousticCategories()).stream()
               .map(compositeId -> new AcousticCategoryComposite(
                     new AcousticCategoryCompositePK(
                           a.nation(),
                           platform,
                           a.acousticCategoryId(),
                           compositeId)
               ))
               .filter(Utils.distinctBy(AcousticCategoryComposite::getCompId))
               .collect(Collectors.toSet());
         AcousticCategory acousticCategory = new AcousticCategory(
               new AcousticCategoryPK(a.nation(), platform, a.acousticCategoryId()),
               (short) (composites.isEmpty() ? 0 : 1),
               a.initials(),
               a.englishInitials(),
               a.commonName(),
               a.englishName()
         );
         return new DbAcousticCategory(acousticCategory, biologicalSpecies, composites);
      }
   }

   private record DbPlatform(
         Platform platform,
         Set<PlatformName> names,
         Set<PlatformCodes> codes
   ) {
      private static DbPlatform from(PojoPlatform p) {
         Set<PlatformName> names = nullToEmpty(p.names()).stream()
               .map(name -> new PlatformName(
                     new PlatformNamePK(
                           p.nation(),
                           (short) p.platformId(),
                           DbReferenceDataExporter.fromLocalDate(name.firstValidDate())
                     ),
                     DbReferenceDataExporter.fromLocalDate(name.lastValidDate()),
                     name.name()
               ))
               .filter(Utils.distinctBy(PlatformName::getCompId))
               .collect(Collectors.toSet());
         Set<PlatformCodes> codes = nullToEmpty(p.codes()).stream()
               .map(code -> new PlatformCodes(
                     new PlatformCodesPK(
                           p.nation(),
                           (short) p.platformId(),
                           code.codeName(),
                           DbReferenceDataExporter.fromLocalDate(code.firstValidDate())
                     ),
                     DbReferenceDataExporter.fromLocalDate(code.lastValidDate()),
                     code.codeValue()
               ))
               .filter(Utils.distinctBy(PlatformCodes::getCompId))
               .collect(Collectors.toSet());
         Platform platform = new Platform(
               new PlatformPK(p.nation(), (short) p.platformId()),
               (short) p.platformType(),
               (short) p.platformSubType(),
               DbReferenceDataExporter.fromLocalDate(p.firstValidDate()),
               DbReferenceDataExporter.fromLocalDate(p.lastValidDate())
         );
         return new DbPlatform(platform, names, codes);
      }
   }

   private static final class NationData {
      private final short nation;
      private final Map<AcousticCategoryPK, PojoAcousticCategory> acousticCategories = new LinkedHashMap<>();
      private final Map<AreaPK, PojoArea> areas = new LinkedHashMap<>();
      private final Map<BiologicalSpeciesPK, PojoBiologicalSpecies> biologicalSpeciesMap = new LinkedHashMap<>();
      private final Map<PlatformPK, PojoPlatform> platforms = new LinkedHashMap<>();
      private final Map<StandardCommentPK, PojoStandardComment> standardComments = new LinkedHashMap<>();

      private NationData(short nation) {
         this.nation = nation;
      }
   }
}
