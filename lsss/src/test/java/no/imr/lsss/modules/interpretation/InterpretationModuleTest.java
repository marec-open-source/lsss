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
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.database.queries.FetchQuery;
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
      DatabaseTestUtils.connectToInMemoryDatabase(lsss);
      interpretationModule = lsss.getModuleManager().getModule(InterpretationModule.class);
      interpretationModule.frequencies.setValue(List.of(38));
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   @Test
   void testStore() {
      Survey survey = DatabaseTestUtils.resetCompleteTestSurvey(lsss);
      double pingsPerNmi = 20;
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).toSegmentHandle(0, 200));

      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().pelagicMode.setBooleanValue(false);
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().topBoundaryOffset.setFloatValue(15);
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().bottomBoundaryOffset.setFloatValue(0.5f);
      lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().verticalGridSizePelagic.setFloatValue(10);
      lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().verticalGridSizeBottom.setFloatValue(5);
      lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().storeRawDataSpecies.setBooleanValue(true);

      //Test store of full ping range, no schools.
      lsss.getRegionManager().setupDefaultBoundaries();
      store(5);
      assertEquals(4, getScatters(survey).size());
      long expectedScatterDataSize = 2 * (9 + 1 + 2 + 1); // 2 grid columns, cells: 9 pelagic, 2 bottom, summary
      assertEquals(expectedScatterDataSize, getScatterDatas(survey).size()); // Only raw data acoustic category is stored
      assertEquals(1, getScatterObjects(survey).size());
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1),
            getObservationCounts(survey));

      ScatterObject scatterObject = getScatterObjects(survey).getFirst();
      assertEquals(40000, scatterObject.getDuration());

      List<SchoolMorphology> schoolMorphologies = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolMorphology.class));
      List<SchoolData> schoolDatas = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolData.class));
      List<SchoolDetect> schoolDetects = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolDetect.class));

      assertEquals(0, schoolMorphologies.size());
      assertEquals(0, schoolDatas.size());
      assertEquals(0, schoolDetects.size());

      //Delete all.
      delete();
      assertEmptyDatabase(survey);

      //Test pelagic mode.
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().pelagicMode.setBooleanValue(true);
      lsss.getRegionManager().setupDefaultBoundaries();
      store(5);
      assertEquals(2, getScatters(survey).size());
      assertEquals(2 * 9, getScatterDatas(survey).size());
      assertEquals(1, getScatterObjects(survey).size());
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1),
            getObservationCounts(survey));

      schoolMorphologies = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolMorphology.class));
      lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolData.class));
      schoolDetects = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolDetect.class));

      assertEquals(0, schoolMorphologies.size());
      assertEquals(0, schoolDatas.size());
      assertEquals(0, schoolDetects.size());

      delete();
      assertEmptyDatabase(survey);

      //Test partial delete
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().pelagicMode.setBooleanValue(false);
      lsss.getRegionManager().setupDefaultBoundaries();
      store(5);
      assertEquals(4, getScatters(survey).size());
      expectedScatterDataSize = 2 * (9 + 1 + 2 + 1); // 2 grid columns, cells: 9 pelagic, 2 bottom, summary
      assertEquals(expectedScatterDataSize, getScatterDatas(survey).size()); // Only raw data acoustic category is stored
      assertEquals(1, getScatterObjects(survey).size());
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1),
            getObservationCounts(survey));
      lsss.getInterpretationSettings().setPingRange(PingRange.of(
            lsss.getDataManager().getDataFileSet().getPingIndex(0),
            lsss.getDataManager().getDataFileSet().getPingIndex(110)));
      delete();
      assertEquals(2, getScatters(survey).size());
      assertEquals(9 + 1 + 2 + 1, getScatterDatas(survey).size());
      List<ScatterObject> scatterObjectList = getScatterObjects(survey);
      assertEquals(1, scatterObjectList.size());
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 1,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1),
            getObservationCounts(survey));

      schoolMorphologies = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolMorphology.class));
      lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolData.class));
      schoolDetects = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolDetect.class));

      assertEquals(0, schoolMorphologies.size());
      assertEquals(0, schoolDatas.size());
      assertEquals(0, schoolDetects.size());

      ScatterObject scatterObject1 = getScatterObjects(survey).getFirst();
      assertEquals(20000, scatterObject1.getDuration());
      lsss.getInterpretationSettings().setPingRange(lsss.getDataManager().getDataFileSet().getTotalRange());
      delete();
      assertEmptyDatabase(survey);

      //Test interpretation
      lsss.getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().pelagicMode.setBooleanValue(false);
      lsss.getRegionManager().setupDefaultBoundaries();
      Region region = lsss.getRegionManager().getRegion(new EchogramPoint(lsss.getDataManager().getDataFileSet().getPingIndex(1), 50));
      assertNotNull(region);
      List<AcousticCategory> selectedSpecies = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories();
      region.getChannelInterpretation(lsss.getInterpretationSettings().getChannel()).setAssignment(selectedSpecies.getFirst().getCompId().getAcousticCategory(), 0.5f);
      store(5);
      assertEquals(4, getScatters(survey).size());
      assertEquals(2 * expectedScatterDataSize, // twice as many since one acoustic category is interpreted
            getScatterDatas(survey).size());

      schoolMorphologies = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolMorphology.class));
      lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolData.class));
      schoolDetects = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolDetect.class));

      assertEquals(0, schoolMorphologies.size());
      assertEquals(0, schoolDatas.size());
      assertEquals(0, schoolDetects.size());

      delete();
      assertEmptyDatabase(survey);

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

      assertEquals(5, getScatters(survey).size()); //3 in school, 1 pelagic and 1 bottom
      assertEquals(2, getScatterObjects(survey).size()); //one school, one background
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 4,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 1,
                  ObservationTypeEnum.SCHOOL_OF_FISH_DATA, 3,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_SCHOOL, 1),
            getObservationCounts(survey));

      int scatterDataFromSchool = 2 * 3 * 11;
      assertEquals(scatterDataFromSchool + 12 + 3, getScatterDatas(survey).size());

      schoolMorphologies = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolMorphology.class));
      schoolDatas = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolData.class));
      schoolDetects = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolDetect.class));

      assertEquals(1, schoolMorphologies.size());
      assertEquals(1, schoolDatas.size());
      assertEquals(1, schoolDetects.size());

      delete();
      assertEmptyDatabase(survey);

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

      assertEquals(4, getScatters(survey).size()); //2 in school, 2 in background
      assertEquals(2, getScatterObjects(survey).size());
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 1,
                  ObservationTypeEnum.SCHOOL_OF_FISH_DATA, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_SCHOOL, 1),
            getObservationCounts(survey));
      assertEquals(2 * 2 * 11 + 12 + 3, getScatterDatas(survey).size());

      for (ScatterObject o : getScatterObjects(survey)) {
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

      assertEquals(4 + 3, getScatters(survey).size());
      assertEquals(3, getScatterObjects(survey).size());
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 4,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 2,
                  ObservationTypeEnum.SCHOOL_OF_FISH_DATA, 3,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 2,
                  ObservationTypeEnum.SCATTER_OBJECT_SCHOOL, 1),
            getObservationCounts(survey));
      assertEquals(2 * 3 * 11 + 2 * (12 + 3), getScatterDatas(survey).size());

      for (ScatterObject o : getScatterObjects(survey)) {
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

      assertEquals(3, getScatters(survey).size());
      assertEquals(Map.of(
                  //ObservationTypeEnum.NAVIGATION_DATA_INPUT, 2,
                  ObservationTypeEnum.SCATTERED_FISH_DATA, 1,
                  ObservationTypeEnum.SCHOOL_OF_FISH_DATA, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_PELAGIC, 1,
                  ObservationTypeEnum.SCATTER_OBJECT_SCHOOL, 1),
            getObservationCounts(survey));
      assertEquals(2, getScatterObjects(survey).size());
      assertEquals(2 * 11 + 12 + 3, getScatterDatas(survey).size());

      for (ScatterObject o : getScatterObjects(survey)) {
         if (o.getObservationType() == ObservationTypeEnum.SCATTER_OBJECT_SCHOOL.getValue()) {
            assertEquals(4000, o.getDuration());
            assertEquals(19700101, o.getObservationDate());
            assertEquals(12000, o.getObservationTime());
         }
      }

      lsss.getInterpretationSettings().setPingRange(lsss.getDataManager().getDataFileSet().getTotalRange());
      delete();
      assertEmptyDatabase(survey);
   }

   private void assertEmptyDatabase(Survey survey) {
      assertEquals(0, getScatters(survey).size());
      assertEquals(0, getScatterDatas(survey).size());
      assertEquals(Map.<ObservationTypeEnum, Integer>of(), getObservationCounts(survey));
      assertEquals(0, getScatterObjects(survey).size());

      List<SchoolMorphology> schoolMorphologies = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolMorphology.class));
      List<SchoolData> schoolDatas = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolData.class));
      List<SchoolDetect> schoolDetects = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolDetect.class));

      assertEquals(0, schoolMorphologies.size());
      assertEquals(0, schoolDatas.size());
      assertEquals(0, schoolDetects.size());
   }

   @Test
   void testObjectNumber() {
      double dx = lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolHorizontalGridSize.getDoubleValue();
      float dy = lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolVerticalGridSizePelagic.getFloatValue();

      Survey survey = DatabaseTestUtils.resetCompleteTestSurvey(lsss);
      double pingsPerNmi = 10 / dx; // 10 pings per grid column
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).toSegmentHandle(0, (int) Math.ceil(pingsPerNmi * 5)));

      lsss.getRegionManager().setupDefaultBoundaries();

      float y = 20;
      addSchool(15, y, 25, y + 1.5f * dy);
      addSchool(25, y + 1.5f * dy, 35, y + 3 * dy);
      assertEquals(2, lsss.getRegionManager().getVisibleSchools().size());

      assertEquals(0, getScatters(survey).size());
      assertEquals(0, getScatterObjects(survey).size());

      store(5);

      assertEquals(4, getScatterSetSchools().size());
      assertEquals(3, getScatterObjects(survey).size());

      assertEquals(2 + 4, getScatters(survey).size()); // 2 = pelagic and bottom echogram scatters

      delete();
      assertEmptyDatabase(survey);
   }

   @Test
   void twoSchoolsDeletingSameObservation() {
      Survey survey = DatabaseTestUtils.resetCompleteTestSurvey(lsss);
      double pingsPerNmi = 10;
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).toSegmentHandle(0, 200));

      lsss.getConfigurationManager().getSurveyConfiguration().getGridConf().schoolHorizontalGridSize.setDoubleValue(1);
      lsss.getRegionManager().setupDefaultBoundaries();

      addSchool(10, 10, 30, 20);
      addSchool(10, 30, 20, 40);
      assertEquals(2, lsss.getRegionManager().getVisibleSchools().size());

      store(1);

      assertEquals(3, getScatterSetSchools().size());
      assertEquals(3, getScatterObjects(survey).size());

      lsss.getInterpretationSettings().setPingRange(PingRange.of(
            lsss.getDataManager().getDataFileSet().getPingIndex(10),
            lsss.getDataManager().getDataFileSet().getPingIndex(20)));
      // When deleting now: One school will cause delete observation, the other will cause delete observation if empty.
      // This failed with org.hibernate.NonUniqueObjectException: A different object with the same identifier value was already associated with the session
      delete();
      assertEquals(1, getScatterSetSchools().size());
      assertEquals(2, getScatterObjects(survey).size());

      lsss.getInterpretationSettings().setPingRange(lsss.getDataManager().getDataFileSet().getTotalRange());
      delete();
      assertEmptyDatabase(survey);
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

      Survey survey = DatabaseTestUtils.resetCompleteTestSurvey(lsss);
      double pingsPerNmi = 10 / dx; // 10 pings per grid column
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).toSegmentHandle(0, (int) Math.ceil(pingsPerNmi * 10)));

      lsss.getRegionManager().setupDefaultBoundaries();

      PingIndex pingIndex = lsss.getInterpretationSettings().getDataFileSet().getContainingPingIndex(6, PingMapping.DISTANCE);
      assertNotNull(pingIndex);
      lsss.getInterpretationSettings().setPingRange(PingRange.of(lsss.getDataManager().getDataFileSet().getTotalRange().begin(), pingIndex));

      store(5);
      assertEquals(2, getScatters(survey).size());

      lsss.getInterpretationSettings().setPingRange(lsss.getDataManager().getDataFileSet().getTotalRange());
      lsss.getInterpretationSettings().waitUntilFinished();

      addSchool(15, 20, 25, 30);

      store(5);
      assertEquals(4, getScatters(survey).size());
   }

   @Test
   void testInheritInterpretation() {
      DatabaseTestUtils.resetCompleteTestSurvey(lsss);
      int pingsPerNmi = 20;
      LsssTestUtils.open(lsss, new TestSyntheticData(pingsPerNmi).toSegmentHandle(0, 10));
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

   private List<Scatter> getScatters(Survey survey) {
      return lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(Scatter.class, survey));
   }

   private List<ScatterData> getScatterDatas(Survey survey) {
      return lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(ScatterData.class, survey));
   }

   private List<ScatterObject> getScatterObjects(Survey survey) {
      return lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(ScatterObject.class, survey));
   }

   private Map<ObservationTypeEnum, Integer> getObservationCounts(Survey survey) {
      return lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(LsssQuery.fetch(Observation.class, survey)).stream()
            .map(observation -> ObservationTypeEnum.valueToObservationTypeEnum(observation.getCompId().getObservationType()))
            .peek(Objects::requireNonNull)
            .filter(observationTypeEnum -> observationTypeEnum != ObservationTypeEnum.NAVIGATION_DATA_INPUT) // These are not deleted.
            .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(__ -> 1)));
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
