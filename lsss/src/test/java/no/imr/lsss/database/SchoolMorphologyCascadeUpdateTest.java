package no.imr.lsss.database;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.SchoolObjectTypeEnum;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.ScatterObjectPK;
import no.imr.lsss.database.tables.hibernate.SchoolData;
import no.imr.lsss.database.tables.hibernate.SchoolDataPK;
import no.imr.lsss.database.tables.hibernate.SchoolMorphology;
import no.imr.lsss.database.tables.hibernate.SchoolMorphologyPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.queries.SaveOrUpdateQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class SchoolMorphologyCascadeUpdateTest {
   private LSSS lsss;
   private DatabaseManager databaseManager;

   @BeforeEach
   void beforeEach() {
      lsss = LsssTestUtils.start(List.of(), List.of());
      databaseManager = lsss.getDatabaseManager();
      assertFalse(databaseManager.getDatabaseConnection().isConnected());
      DatabaseTestUtils.connectToInMemoryDatabase(lsss);
      assertTrue(databaseManager.getDatabaseConnection().isConnected());
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   private Survey createSurvey() {
      return DatabaseTestUtils.resetCompleteTestSurvey(lsss);
   }

   @Test
   void testInit() {
      assertTrue(databaseManager.getDatabaseConnection().isConnected());
   }

   @Test
   void testSchoolMorphologyCascade() {
      Survey survey = createSurvey();
      SurveyPK surveyPK = survey.getCompId();

      ObservationPK oPK = new ObservationPK(surveyPK.getNation(), surveyPK.getPlatform(), surveyPK.getSurvey(), survey.getStartDate(), survey.getStopTime(), ObservationTypeEnum.SCATTERED_FISH_DATA.getValue());
      Observation observation = new Observation(oPK);
      databaseManager.getDatabaseConnection().executeQuery(new SaveOrUpdateQuery(observation));

      //Create a scatterObject with one schoolMorphology and two schoolData objects
      ScatterObjectPK soPK = new ScatterObjectPK(oPK.getNation(), oPK.getPlatform(), oPK.getSurvey(), 1);
      ScatterObject scatterObject = new ScatterObject(
            soPK, oPK.getObservationDate(), oPK.getObservationTime(), oPK.getObservationType(), 0);

      SchoolMorphologyPK smPK = new SchoolMorphologyPK(soPK.getNation(), soPK.getPlatform(), soPK.getSurvey(), soPK.getObject(), SchoolObjectTypeEnum.SchoolDetectedUncorrected.getValue());
      SchoolMorphology morphology = new SchoolMorphology(smPK);

      SchoolDataPK sdPK = new SchoolDataPK(smPK.getNation(), smPK.getPlatform(), smPK.getSurvey(), smPK.getObject(), smPK.getSchoolObjectType(), 1, 38000);
      SchoolData schoolData = new SchoolData(sdPK);
      schoolData.setSchoolMorphology(morphology);

      SchoolDataPK sd2PK = new SchoolDataPK(smPK.getNation(), smPK.getPlatform(), smPK.getSurvey(), smPK.getObject(), smPK.getSchoolObjectType(), 2, 120000);
      SchoolData schoolData2 = new SchoolData(sd2PK);
      schoolData2.setSchoolMorphology(morphology);

      Set<SchoolData> schoolDatas = new HashSet<>();
      schoolDatas.add(schoolData);
      schoolDatas.add(schoolData2);

      morphology.setSchoolData(schoolDatas);

      Set<SchoolMorphology> morphologySet = new HashSet<>();
      morphologySet.add(morphology);
      morphology.setArea(2.0f);

      scatterObject.setSchoolMorphologies(morphologySet);
      morphology.setScatterObject(scatterObject);

      //Save the scatter object
      databaseManager.getDatabaseConnection().executeQuery(new SaveOrUpdateQuery(scatterObject));

      List<SchoolData> schoolDataList = databaseManager.getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolData.class));
      assertEquals(2, schoolDataList.size());

      List<SchoolMorphology> morphologies = databaseManager.getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolMorphology.class));
      assertEquals(2.0f, morphologies.getFirst().getArea(), 0.1);

      scatterObject.getSchoolMorphologies().clear();

      //Without this, the test fails further down, since it does not delete the SchoolData that is no longer there in the java object.
      databaseManager.getDatabaseConnection().executeQuery(new SaveOrUpdateQuery(scatterObject));
      morphologies = databaseManager.getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolMorphology.class));
      assertEquals(0, morphologies.size());

      // Associates a new school morphology and school data to the scatter object.

      SchoolMorphology morphology2 = new SchoolMorphology(smPK);
      morphology2.setArea(3.0f);

      scatterObject.getSchoolMorphologies().add(morphology2);
      morphology2.setScatterObject(scatterObject);

      Set<SchoolData> schoolDatas2 = new HashSet<>();
      SchoolData schoolData3 = new SchoolData(sdPK);
      schoolData3.setSchoolMorphology(morphology2);

      schoolDatas2.add(schoolData3);
      morphology2.setSchoolData(schoolDatas2);

      databaseManager.getDatabaseConnection().executeQuery(new SaveOrUpdateQuery(scatterObject));

      schoolDataList = databaseManager.getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolData.class));
      assertEquals(1, schoolDataList.size());
      morphologies = databaseManager.getDatabaseConnection().executeFetchQuery(new FetchQuery<>(SchoolMorphology.class));
      assertEquals(3.0f, morphologies.getFirst().getArea(), 0.1);
   }
}
