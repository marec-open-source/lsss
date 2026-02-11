package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseTestUtils;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterData;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.SchoolData;
import no.imr.lsss.database.tables.hibernate.SchoolDetect;
import no.imr.lsss.database.tables.hibernate.SchoolMorphology;
import no.imr.lsss.database.types.JavaDBInMemoryDatabasePlugin;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.QueryBuilder;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class InterpretationModuleTest {
   private LSSS lsss;
   private InterpretationModule interpretationModule;

   @BeforeEach
   void beforeEach() {
      lsss = LsssTestUtils.start(List.of(RegionIntegrationModule.class), List.of(InterpretationModule.class));
      JavaDBInMemoryDatabasePlugin.install(lsss);
      DatabaseTestUtils.resetCompleteTestSurvey(lsss);
      interpretationModule = lsss.getModuleManager().getModule(InterpretationModule.class);
      interpretationModule.frequencies.setValue(List.of(38));
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   @Test
   void testStore() {
      double pingsPerNmi = 20;
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).withFirstAndLastPingNumber(0, 200).toSegmentHandle());

      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().pelagicMode.setBooleanValue(false);
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().topBoundaryOffset.setFloatValue(15);
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().bottomBoundaryOffset.setFloatValue(0.5f);
      lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().verticalGridSizePelagic.setFloatValue(10);
      lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().verticalGridSizeBottom.setFloatValue(5);
      lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().storeRawDataSpecies.setBooleanValue(true);

      //Test store of full ping range, no schools.
      lsss.getRegionManager().setupDefaultBoundaries();
      store(5);
      assertEquals(4, count(Scatter.class));
      long expectedScatterDataSize = 2 * (9 + 1 + 2 + 1); // 2 grid columns, cells: 9 pelagic, 2 bottom, summary
      assertEquals(expectedScatterDataSize, count(ScatterData.class)); // Only raw data acoustic category is stored
      assertEquals(1, count(ScatterObject.class));
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1),
            getObservationCounts());

      ScatterObject scatterObject = getScatterObjects().getFirst();
      assertEquals(40000, scatterObject.getDuration());

      assertEquals(0, count(SchoolMorphology.class));
      assertEquals(0, count(SchoolData.class));
      assertEquals(0, count(SchoolDetect.class));

      //Delete all.
      delete();
      assertEmptyDatabase();

      //Test pelagic mode.
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().pelagicMode.setBooleanValue(true);
      lsss.getRegionManager().setupDefaultBoundaries();
      store(5);
      assertEquals(2, count(Scatter.class));
      assertEquals(2 * 9, count(ScatterData.class));
      assertEquals(1, count(ScatterObject.class));
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1),
            getObservationCounts());

      assertEquals(0, count(SchoolMorphology.class));
      assertEquals(0, count(SchoolData.class));
      assertEquals(0, count(SchoolDetect.class));

      delete();
      assertEmptyDatabase();

      //Test partial delete
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().pelagicMode.setBooleanValue(false);
      lsss.getRegionManager().setupDefaultBoundaries();
      store(5);
      assertEquals(4, count(Scatter.class));
      expectedScatterDataSize = 2 * (9 + 1 + 2 + 1); // 2 grid columns, cells: 9 pelagic, 2 bottom, summary
      assertEquals(expectedScatterDataSize, count(ScatterData.class)); // Only raw data acoustic category is stored
      assertEquals(1, count(ScatterObject.class));
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1),
            getObservationCounts());
      lsss.getInterpretationSettings().setPingRange(PingRange.of(
            lsss.getDataManager().getDataFileSet().getPingIndex(0),
            lsss.getDataManager().getDataFileSet().getPingIndex(110)));
      delete();
      assertEquals(2, count(Scatter.class));
      assertEquals(9 + 1 + 2 + 1, count(ScatterData.class));
      assertEquals(1, count(ScatterObject.class));
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 1,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1),
            getObservationCounts());

      assertEquals(0, count(SchoolMorphology.class));
      assertEquals(0, count(SchoolData.class));
      assertEquals(0, count(SchoolDetect.class));

      ScatterObject scatterObject1 = getScatterObjects().getFirst();
      assertEquals(20000, scatterObject1.getDuration());
      lsss.getInterpretationSettings().setPingRange(lsss.getDataManager().getDataFileSet().getTotalRange());
      delete();
      assertEmptyDatabase();

      //Test interpretation
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().pelagicMode.setBooleanValue(false);
      lsss.getRegionManager().setupDefaultBoundaries();
      Region region = lsss.getRegionManager().getRegion(new EchogramPoint(lsss.getDataManager().getDataFileSet().getPingIndex(1), 50));
      assertNotNull(region);
      List<AcousticCategory> selectedSpecies = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories();
      region.getChannelInterpretation(lsss.getInterpretationSettings().getChannel()).setAssignment(selectedSpecies.getFirst().getCompId().getAcousticCategory(), 0.5f);
      store(5);
      assertEquals(4, count(Scatter.class));
      assertEquals(2 * expectedScatterDataSize, count(ScatterData.class)); // Twice as many since one acoustic category is interpreted.

      assertEquals(0, count(SchoolMorphology.class));
      assertEquals(0, count(SchoolData.class));
      assertEquals(0, count(SchoolDetect.class));

      delete();
      assertEmptyDatabase();

      //Test store of region
      lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolVerticalGridSizePelagic.setFloatValue(1);
      lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolVerticalGridSizeBottom.setFloatValue(1);
      lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolHorizontalGridSize.setDoubleValue(1);
      lsss.getInterpretationSettings().setPingRange(PingRange.of(
            lsss.getDataManager().getDataFileSet().getPingIndex(0),
            lsss.getDataManager().getDataFileSet().getPingIndex(101)));
      lsss.getRegionManager().setupDefaultBoundaries();
      addSchool(1, 40, 60, 50);
      Region school = lsss.getRegionManager().getRegion(new EchogramPoint(lsss.getDataManager().getDataFileSet().getPingIndex(5), 45));
      assertNotNull(school);
      selectedSpecies = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories();
      school.getChannelInterpretation(lsss.getInterpretationSettings().getChannel()).setAssignment(selectedSpecies.getFirst().getCompId().getAcousticCategory(), 0.5f);
      store(5);

      assertEquals(5, count(Scatter.class)); //3 in school, 1 pelagic and 1 bottom
      assertEquals(2, count(ScatterObject.class)); //one school, one background
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 4,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 1,
                  ObservationTypeEnum.SCHOOL_OF_FISH_DATA, 3,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_SCHOOL, 1),
            getObservationCounts());

      int scatterDataFromSchool = 2 * 3 * 11;
      assertEquals(scatterDataFromSchool + 12 + 3, count(ScatterData.class));

      assertEquals(1, count(SchoolMorphology.class));
      assertEquals(1, count(SchoolData.class));
      assertEquals(1, count(SchoolDetect.class));

      delete();
      assertEmptyDatabase();

      //Test store of school in two steps.
      lsss.getInterpretationSettings().setPingRange(lsss.getDataManager().getDataFileSet().getTotalRange());
      lsss.getRegionManager().setupDefaultBoundaries();
      addSchool(1, 40, 60, 50);
      lsss.getInterpretationSettings().setPingRange(PingRange.of(
            lsss.getDataManager().getDataFileSet().getPingIndex(0),
            lsss.getDataManager().getDataFileSet().getPingIndex(50)));
      school = lsss.getRegionManager().getRegion(new EchogramPoint(lsss.getDataManager().getDataFileSet().getPingIndex(5), 45));
      assertNotNull(school);
      selectedSpecies = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories();
      school.getChannelInterpretation(lsss.getInterpretationSettings().getChannel()).setAssignment(selectedSpecies.getFirst().getCompId().getAcousticCategory(), 0.5f);
      //Store first part of school
      store(2);

      assertEquals(4, count(Scatter.class)); //2 in school, 2 in background
      assertEquals(2, count(ScatterObject.class));
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 1,
                  ObservationTypeEnum.SCHOOL_OF_FISH_DATA, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_SCHOOL, 1),
            getObservationCounts());
      assertEquals(2 * 2 * 11 + 12 + 3, count(ScatterData.class));

      for (ScatterObject o : getScatterObjects()) {
         if (o.getObservationType() == ObservationTypeEnum.SCATTER_OBJECT_SCHOOL.getValue()) {
            assertEquals(2 * 4000, o.getDuration());
            assertEquals(19700101, o.getObservationDate());
            assertEquals(0, o.getObservationTime());
         }
      }

      lsss.getInterpretationSettings().setPingRange(PingRange.of(
            lsss.getDataManager().getDataFileSet().getPingIndex(30),
            lsss.getDataManager().getDataFileSet().getPingIndex(100)));

      //Store second part of school
      store(2);

      assertEquals(4 + 3, count(Scatter.class));
      assertEquals(3, count(ScatterObject.class));
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 4,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 2,
                  ObservationTypeEnum.SCHOOL_OF_FISH_DATA, 3,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_SCHOOL, 1),
            getObservationCounts());
      assertEquals(2 * 3 * 11 + 2 * (12 + 3), count(ScatterData.class));

      for (ScatterObject o : getScatterObjects()) {
         if (o.getObservationType() == ObservationTypeEnum.SCATTER_OBJECT_SCHOOL.getValue()) {
            assertEquals(3 * 4000, o.getDuration());
            assertEquals(19700101, o.getObservationDate());
            assertEquals(0, o.getObservationTime());
         }
      }

      lsss.getInterpretationSettings().setPingRange(PingRange.of(
            lsss.getDataManager().getDataFileSet().getPingIndex(0),
            lsss.getDataManager().getDataFileSet().getPingIndex(50)));
      delete();

      assertEquals(3, count(Scatter.class));
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 1,
                  ObservationTypeEnum.SCHOOL_OF_FISH_DATA, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_SCHOOL, 1),
            getObservationCounts());
      assertEquals(2, count(ScatterObject.class));
      assertEquals(2 * 11 + 12 + 3, count(ScatterData.class));

      for (ScatterObject o : getScatterObjects()) {
         if (o.getObservationType() == ObservationTypeEnum.SCATTER_OBJECT_SCHOOL.getValue()) {
            assertEquals(4000, o.getDuration());
            assertEquals(19700101, o.getObservationDate());
            assertEquals(12000, o.getObservationTime());
         }
      }

      lsss.getInterpretationSettings().setPingRange(lsss.getDataManager().getDataFileSet().getTotalRange());
      delete();
      assertEmptyDatabase();
   }

   private void assertEmptyDatabase() {
      assertEquals(0, count(Scatter.class));
      assertEquals(0, count(ScatterData.class));
      assertEquals(Map.of(), getObservationCounts());
      assertEquals(0, count(ScatterObject.class));

      assertEquals(0, count(SchoolMorphology.class));
      assertEquals(0, count(SchoolData.class));
      assertEquals(0, count(SchoolDetect.class));
   }

   @Test
   void testObjectNumber() {
      double dx = lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolHorizontalGridSize.getDoubleValue();
      float dy = lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolVerticalGridSizePelagic.getFloatValue();
      double pingsPerNmi = 10 / dx; // 10 pings per grid column
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).withFirstAndLastPingNumber(0, (int) Math.ceil(pingsPerNmi * 5)).toSegmentHandle());

      lsss.getRegionManager().setupDefaultBoundaries();

      float y = 20;
      addSchool(15, y, 25, y + 1.5f * dy);
      addSchool(25, y + 1.5f * dy, 35, y + 3 * dy);
      assertEquals(2, lsss.getRegionManager().visibleSchools().count());

      assertEquals(0, count(Scatter.class));
      assertEquals(0, count(ScatterObject.class));

      store(5);

      assertEquals(4, getScatterSetSchools().size());
      assertEquals(3, count(ScatterObject.class));

      assertEquals(2 + 4, count(Scatter.class)); // 2 = pelagic and bottom echogram scatters

      delete();
      assertEmptyDatabase();
   }

   @Test
   void twoSchoolsDeletingSameObservation() {
      double pingsPerNmi = 10;
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).withFirstAndLastPingNumber(0, 200).toSegmentHandle());

      lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolHorizontalGridSize.setDoubleValue(1);
      lsss.getRegionManager().setupDefaultBoundaries();

      addSchool(10, 10, 30, 20);
      addSchool(10, 30, 20, 40);
      assertEquals(2, lsss.getRegionManager().visibleSchools().count());

      store(1);

      assertEquals(3, getScatterSetSchools().size());
      assertEquals(3, count(ScatterObject.class));

      lsss.getInterpretationSettings().setPingRange(PingRange.of(
            lsss.getDataManager().getDataFileSet().getPingIndex(10),
            lsss.getDataManager().getDataFileSet().getPingIndex(20)));
      // When deleting now: One school will cause delete observation, the other will cause delete observation if empty.
      // This failed with org.hibernate.NonUniqueObjectException: A different object with the same identifier value was already associated with the session
      delete();
      assertEquals(1, getScatterSetSchools().size());
      assertEquals(2, count(ScatterObject.class));

      lsss.getInterpretationSettings().setPingRange(lsss.getDataManager().getDataFileSet().getTotalRange());
      delete();
      assertEmptyDatabase();
   }

   /**
    * Test for ticket #5.
    * <p>
    * Problem occurs when an interval is already stored to database and the user edits a school or creates a new school.
    * The user is allowed to store the part of a new school that does not overlap with any already stored schools.
    * Storing should only be allowed where nothing is stored before.
    */
   @Test
   void ticket5() {
      double dx = lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolHorizontalGridSize.getDoubleValue();
      double pingsPerNmi = 10 / dx; // 10 pings per grid column
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).withFirstAndLastPingNumber(0, (int) Math.ceil(pingsPerNmi * 10)).toSegmentHandle());

      lsss.getRegionManager().setupDefaultBoundaries();

      PingIndex pingIndex = lsss.getInterpretationSettings().getDataFileSet().getContainingPingIndex(6, PingMapping.DISTANCE);
      assertNotNull(pingIndex);
      lsss.getInterpretationSettings().setPingRange(PingRange.of(lsss.getDataManager().getDataFileSet().getTotalRange().begin(), pingIndex));

      store(5);
      assertEquals(2, count(Scatter.class));

      lsss.getInterpretationSettings().setPingRange(lsss.getDataManager().getDataFileSet().getTotalRange());
      lsss.getInterpretationSettings().waitUntilFinished();

      addSchool(15, 20, 25, 30);

      store(5);
      assertEquals(4, count(Scatter.class));
   }

   @Test
   void testInheritInterpretation() {
      int pingsPerNmi = 20;
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).withFirstAndLastPingNumber(0, 10).toSegmentHandle());
      lsss.getInterpretationSettings().setChannel(1);
      lsss.getRegionManager().setupDefaultBoundaries();

      Region region = lsss.getRegionManager().getRegion(createEchogramPoint(1, 20));
      assertNotNull(region);
      assertFalse(region.getChannelInterpretation(1).isInitialized());

      AcousticCategory acousticCategory = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories().getFirst();
      float assignment = 0.5f;
      region.getChannelInterpretation(1).setAssignment(acousticCategory.getCompId().getAcousticCategory(), assignment);
      assertTrue(region.getChannelInterpretation(1).isInitialized());
      assertFalse(region.getChannelInterpretation(2).isInitialized());

      lsss.getInterpretationSettings().setChannel(2);
      lsss.getInterpretationSettings().waitUntilFinished();
      assertTrue(region.getChannelInterpretation(2).isInitialized());
      assertEquals(assignment, region.getChannelInterpretation(2).getAssignment(acousticCategory.getCompId().getAcousticCategory()));
      assertEquals(0, region.getChannelInterpretation(3).getAssignment(acousticCategory.getCompId().getAcousticCategory()));
      assertFalse(region.getChannelInterpretation(3).isInitialized());

      lsss.getRegionManager().addVerticalDivider(lsss.getDataManager().getDataFileSet().getPingIndex(5));
      Region region2 = lsss.getRegionManager().getRegion(createEchogramPoint(7, 20));
      assertNotNull(region2);
      assertTrue(region.getChannelInterpretation(1).isInitialized());
      assertTrue(region.getChannelInterpretation(2).isInitialized());
      assertFalse(region.getChannelInterpretation(3).isInitialized());

      School school = addSchool(3, 20, 7, 40);
      assertNotNull(school);
      assertFalse(school.getChannelInterpretation(1).isInitialized());
      assertFalse(school.getChannelInterpretation(2).isInitialized());
      assertFalse(school.getChannelInterpretation(3).isInitialized());

      school.getChannelInterpretation(2).setAssignment(acousticCategory.getCompId().getAcousticCategory(), 0);
      interpretationModule.inheritVisibleInterpretation(List.of(2, 3));
      assertTrue(region.getChannelInterpretation(3).isInitialized());
      assertEquals(assignment, region.getChannelInterpretation(3).getAssignment(acousticCategory.getCompId().getAcousticCategory()));
      assertTrue(region2.getChannelInterpretation(3).isInitialized());
      assertEquals(assignment, region2.getChannelInterpretation(3).getAssignment(acousticCategory.getCompId().getAcousticCategory()));
      assertFalse(school.getChannelInterpretation(1).isInitialized());
      assertTrue(school.getChannelInterpretation(2).isInitialized());
      assertTrue(school.getChannelInterpretation(3).isInitialized());
      assertEquals(0, school.getChannelInterpretation(1).getAssignment(acousticCategory.getCompId().getAcousticCategory()));
      assertEquals(0, school.getChannelInterpretation(2).getAssignment(acousticCategory.getCompId().getAcousticCategory()));
      assertEquals(0, school.getChannelInterpretation(3).getAssignment(acousticCategory.getCompId().getAcousticCategory()));
   }

   private @Nullable School addSchool(long pingNumber1, float depth1, long pingNumber2, float depth2) {
      EchogramPoint startPoint = createEchogramPoint(pingNumber1, depth1);
      EchogramPoint endPoint = createEchogramPoint(pingNumber2, depth2);
      if (lsss.getRegionManager().isReadOnlyIncludingEnd(startPoint.pingIndex()) || lsss.getRegionManager().isReadOnlyIncludingEnd(endPoint.pingIndex())) {
         return null;
      }
      return lsss.getRegionManager().addSchool(startPoint, endPoint, IdentityDepthTransform.INSTANCE);
   }

   private EchogramPoint createEchogramPoint(long pingNumber, float depth) {
      return new EchogramPoint(lsss.getDataManager().getDataFileSet().getPingIndex(pingNumber), depth);
   }

   private void store(double horizontalGridSize) {
      lsss.getConfigurationManager().getGridConf().horizontalGridSize.setDoubleValue(horizontalGridSize);
      interpretationModule.store(InterpretationModule.StoreAction.ALL);
      lsss.getDatabaseManager().getDatabaseConnection().waitUntilFinished();
   }

   private void delete() {
      interpretationModule.delete(InterpretationModule.DeleteAction.ALL);
      lsss.getDatabaseManager().getDatabaseConnection().waitUntilFinished();
   }

   private Collection<Scatter> getScatterSetSchools() {
      return lsss.getInterpretationSummary().getScatterSet().getScatters(ScatterTypeEnum.PELAGIC_SCHOOL, lsss.getInterpretationSettings().getFrequency());
   }

   private long count(Class<? extends BaseDatabaseObject> clazz) {
      return lsss.getDatabaseManager().getDatabaseConnection().executeStatelessValuedQuery(QueryBuilder.count(clazz).build());
   }

   private List<ScatterObject> getScatterObjects() {
      return lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(ScatterObject.class));
   }

   private Map<ObservationTypeEnum, Integer> getObservationCounts() {
      return lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(Observation.class)).stream()
            .map(observation -> ObservationTypeEnum.valueToObservationTypeEnum(observation.getCompId().getObservationType()))
            .peek(Objects::requireNonNull)
            .filter(observationTypeEnum -> observationTypeEnum != ObservationTypeEnum.NAVIGATION_DATA_INPUT) // These are not deleted.
            .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(_ -> 1)));
   }

   private static final class TestSyntheticData extends SyntheticData {
      private static final float SV_VALUE = PowerData.logSvToSv(-56);
      private final double pingsPerNmi;

      private TestSyntheticData(double pingsPerNmi) {
         this.pingsPerNmi = pingsPerNmi;
      }

      @Override
      protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
         float[] sv = new float[200];
         Arrays.fill(sv, SV_VALUE);
         powerData.setSv(sv);
      }

      @Override
      protected double getVesselDistance(long pingNumber) {
         return pingNumber / pingsPerNmi;
      }

      @Override
      protected float getBottomDepth(PingIndex pingIndex, int channel) {
         return 100;
      }

      @Override
      protected float getSampleInterval(PingIndex pingIndex, int channel) {
         float sampleDistance = 1;
         return 2 * sampleDistance / getSoundVelocity(pingIndex, channel);
      }
   }
}
