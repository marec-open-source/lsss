package no.imr.lsss.database;

import com.google.common.collect.ImmutableList;
import no.imr.lsss.database.tables.hibernate.AcCatToBiologicalSpecies;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryComposite;
import no.imr.lsss.database.tables.hibernate.Area;
import no.imr.lsss.database.tables.hibernate.AreaOfAcousticCategory;
import no.imr.lsss.database.tables.hibernate.BaseNationPK;
import no.imr.lsss.database.tables.hibernate.BasePlatformPK;
import no.imr.lsss.database.tables.hibernate.BiologicalSpecies;
import no.imr.lsss.database.tables.hibernate.DBParameter;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.tools.database.DatabaseColumn;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.hibernate.BaseCompDatabaseObject;
import no.imr.tools.database.queries.QueryBuilder;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Database tables that are static data.
 */
public final class DatabaseData {
   public static final DatabaseColumn ACOUSTIC_CATEGORY = new DatabaseColumn("acousticCategory");
   public static final DatabaseColumn AREA = new DatabaseColumn("area");
   public static final DatabaseColumn INFO_KEY = new DatabaseColumn("infoKey");
   public static final DatabaseColumn NATION = new DatabaseColumn("nation", Nation.class);
   public static final DatabaseColumn OBJECT = new DatabaseColumn("object");
   public static final DatabaseColumn OBSERVATION_DATE = new DatabaseColumn("observationDate");
   public static final DatabaseColumn OBSERVATION_TIME = new DatabaseColumn("observationTime");
   public static final DatabaseColumn OBSERVATION_TYPE = new DatabaseColumn("observationType");
   public static final DatabaseColumn PAR_NAME = new DatabaseColumn("parName", DBParameter.class);
   public static final DatabaseColumn PLATFORM = new DatabaseColumn("platform");
   public static final DatabaseColumn SURVEY = new DatabaseColumn("survey");

   public static final int MAX_COMMENT_LENGTH = 200;
   public static final int MAX_PLATFORM_NAME_LENGTH = 80;
   public static final int MAX_PLATFORM_CODE_LENGTH = 80;
   public static final int MAX_SURVEY_TITLE_LENGTH = 80;
   public static final int MAX_SURVEY_INFO_VALUE_LENGTH = 30_000;
   public static final int MAX_AREA_LENGTH = 80;
   public static final int MAX_AC_CAT_NAME_LENGTH = 80;
   public static final int MAX_AC_CAT_INITIALS_LENGTH = 5;

   private final DatabaseConnection databaseConnection;

   private @Nullable Nations nations;
   private final Object nationsLock = new Object();

   private @Nullable Areas areas;
   private final Object areasLock = new Object();

   private @Nullable StandardComments standardComments;
   private final Object standardCommentsLock = new Object();

   private @Nullable AcousticCategories acousticCategories;
   private final Object acousticCategoriesLock = new Object();

   DatabaseData(DatabaseConnection databaseConnection) {
      this.databaseConnection = databaseConnection;
   }

   public Nations getNations() {
      synchronized (nationsLock) {
         if (nations == null) {
            nations = new Nations(databaseConnection);
         }
         return nations;
      }
   }

   public @Nullable Nation getNation(String nationName) {
      for (Nation nation : getNations().getAll()) {
         if (nation.getNationName().equals(nationName)) {
            return nation;
         }
      }
      return null;
   }

   public Areas getAreas(@Nullable Nation nation) {
      synchronized (areasLock) {
         if (areas == null || !Objects.equals(areas.nation, nation)) {
            areas = new Areas(databaseConnection, nation);
         }
         return areas;
      }
   }

   public void refreshAreas() {
      synchronized (areasLock) {
         areas = null;
      }
   }

   public StandardComments getStandardComments(@Nullable Platform platform) {
      PlatformPK platformPK = platform != null ? platform.getCompId() : null;
      synchronized (standardCommentsLock) {
         if (standardComments == null || !Objects.equals(standardComments.platformPK, platformPK)) {
            standardComments = new StandardComments(databaseConnection, platformPK);
         }
         return standardComments;
      }
   }

   public AcousticCategories getAcousticCategories(@Nullable Platform platform) {
      PlatformPK platformPK = platform != null ? platform.getCompId() : null;
      synchronized (acousticCategoriesLock) {
         if (acousticCategories == null || !Objects.equals(acousticCategories.platformPK, platformPK)) {
            acousticCategories = new AcousticCategories(databaseConnection, platformPK);
         }
         return acousticCategories;
      }
   }

   public void refreshAcousticCategories() {
      synchronized (acousticCategoriesLock) {
         acousticCategories = null;
      }
   }

   public static <T extends BaseCompDatabaseObject<? extends BaseNationPK>> List<T> copyFromDefaultNation(DatabaseConnection databaseConnection, Class<T> clazz, short nation) {
      List<T> defaultEntries = databaseConnection.executeFetchQuery(LsssQuery.fetch(clazz, NATION, 0));
      for (T entry : defaultEntries) {
         entry.getCompId().setNation(nation);
      }
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.upsert(defaultEntries));
      return defaultEntries;
   }

   private static <T extends BaseCompDatabaseObject<? extends BasePlatformPK>> List<T> copyFromDefaultPlatform(DatabaseConnection databaseConnection, Class<T> clazz, PlatformPK platformPK) {
      List<T> defaultEntries = databaseConnection.executeFetchQuery(
            LsssQuery.forPlatform(QueryBuilder.fetch(clazz), (short) 0, (short) 0).build());
      for (T entry : defaultEntries) {
         entry.getCompId().setNation(platformPK.getNation());
         entry.getCompId().setPlatform(platformPK.getPlatform());
      }
      databaseConnection.executeStatelessQuery(StatelessDatabaseQuery.upsert(defaultEntries));
      return defaultEntries;
   }

   //-------------------------------------------------------------------------------------------------------------------

   public static final class Nations {
      private final ImmutableList<Nation> all;

      private Nations(DatabaseConnection databaseConnection) {
         List<Nation> nations = databaseConnection.executeFetchQuery(LsssQuery.fetch(Nation.class));
         Nation defaultNation = getDefaultNation(nations);
         all = nations.stream()
               .filter(nation -> !nation.equals(defaultNation))
               .sorted()
               .collect(ImmutableList.toImmutableList());
      }

      public List<Nation> getAll() {
         return all;
      }

      private static @Nullable Nation getDefaultNation(List<Nation> nations) {
         for (Nation nation : nations) {
            if (nation.getNation() == 0) {
               return nation;
            }
         }
         // No warning here. Survey local database may not have the default nation.
         return null;
      }
   }

   //-------------------------------------------------------------------------------------------------------------------

   public static final class Areas {
      private final @Nullable Nation nation;
      private final ImmutableList<Area> all;

      private Areas(DatabaseConnection databaseConnection, @Nullable Nation nation) {
         this.nation = nation;
         if (nation != null) {
            List<Area> areaTemp = databaseConnection.executeFetchQuery(LsssQuery.fetch(Area.class, NATION, nation.getNation()));
            if (areaTemp.isEmpty()) {
               areaTemp = copyFromDefaultNation(databaseConnection, Area.class, nation.getNation());
            }
            all = areaTemp.stream()
                  .sorted()
                  .collect(ImmutableList.toImmutableList());
         } else {
            all = ImmutableList.of();
         }
      }

      public List<Area> getAll() {
         return all;
      }
   }

   //-------------------------------------------------------------------------------------------------------------------

   public static final class StandardComments {
      private final @Nullable PlatformPK platformPK;
      private final ImmutableList<StandardComment> all;

      private StandardComments(DatabaseConnection databaseConnection, @Nullable PlatformPK platformPK) {
         this.platformPK = platformPK;
         if (platformPK != null) {
            List<StandardComment> standardComments = databaseConnection.executeFetchQuery(
                  LsssQuery.forPlatform(QueryBuilder.fetch(StandardComment.class), platformPK).build());
            if (standardComments.isEmpty()) {
               standardComments = copyFromDefaultPlatform(databaseConnection, StandardComment.class, platformPK);
            }
            all = ImmutableList.copyOf(standardComments);
         } else {
            all = ImmutableList.of();
         }
      }

      public List<StandardComment> getAll() {
         return all;
      }

      public @Nullable StandardComment getFreeTextStandardComment() {
         for (StandardComment standardComment : all) {
            if (standardComment.getCompId().getStandardComment() == 0) {
               return standardComment;
            }
         }
         return null;
      }
   }

   //-------------------------------------------------------------------------------------------------------------------

   public static final class AcousticCategories {
      private final @Nullable PlatformPK platformPK;
      private final ImmutableList<AcousticCategory> all;
      private final @Nullable AcousticCategory rawDataAcousticCategory;

      private AcousticCategories(DatabaseConnection databaseConnection, @Nullable PlatformPK platformPK) {
         this.platformPK = platformPK;
         if (platformPK != null) {
            List<AcousticCategory> defaultCategories = databaseConnection.executeFetchQuery(
                  LsssQuery.forPlatform(QueryBuilder.fetch(AcousticCategory.class), platformPK).build());
            if (defaultCategories.isEmpty()) {
               defaultCategories = copyFromDefaultPlatform(databaseConnection, AcousticCategory.class, platformPK);
               // Check if BiologicalSpecies exists for current nation.
               long biologicalSpeciesCount = databaseConnection.executeStatelessValuedQuery(QueryBuilder.count(BiologicalSpecies.class).where()
                     .eq(NATION, platformPK.getNation())
                     .build());
               if (biologicalSpeciesCount == 0) {
                  copyFromDefaultNation(databaseConnection, BiologicalSpecies.class, platformPK.getNation());
               }
               copyFromDefaultPlatform(databaseConnection, AcCatToBiologicalSpecies.class, platformPK);
               copyFromDefaultPlatform(databaseConnection, AcousticCategoryComposite.class, platformPK);
               copyFromDefaultPlatform(databaseConnection, AreaOfAcousticCategory.class, platformPK);
            }
            rawDataAcousticCategory = getRawDataAcousticCategory(defaultCategories);
            all = defaultCategories.stream()
                  .filter(acousticCategory -> !acousticCategory.equals(rawDataAcousticCategory))
                  .sorted()
                  .collect(ImmutableList.toImmutableList());
         } else {
            all = ImmutableList.of();
            rawDataAcousticCategory = null;
         }
      }

      private static @Nullable AcousticCategory getRawDataAcousticCategory(List<AcousticCategory> acousticCategories) {
         for (AcousticCategory acousticCategory : acousticCategories) {
            if (acousticCategory.rawData()) {
               return acousticCategory;
            }
         }
         if (!acousticCategories.isEmpty()) {
            Log.global.warning("Cannot find raw data acoustic category");
         }
         return null;
      }

      public List<AcousticCategory> getAll() {
         return all;
      }

      public @Nullable AcousticCategory getRawDataAcousticCategory() {
         return rawDataAcousticCategory;
      }
   }

   //-------------------------------------------------------------------------------------------------------------------

   public static final class Purpose {
      public static final short MAIN = 1;
      public static final short USABLE = 2;
      public static final short OTHER = 3;

      private Purpose() {
      }
   }
}
