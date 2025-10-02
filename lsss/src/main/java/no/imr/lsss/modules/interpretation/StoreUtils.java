package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.hibernate.BaseSurveyPK;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.database.hibernate.BaseDatabaseObject;
import no.imr.tools.database.queries.DeleteQuery;
import no.imr.tools.database.queries.QueryBuilder;
import no.imr.tools.range.FloatRange;
import no.marec.lsss.api.util.GeoPoint;

public final class StoreUtils {
   private StoreUtils() {
   }

   static ObservationPK createObservationPK(BaseSurveyPK baseSurveyPK, DatabaseTime databaseTime, ObservationTypeEnum observationTypeEnum) {
      return new ObservationPK(
            baseSurveyPK.getNation(),
            baseSurveyPK.getPlatform(),
            baseSurveyPK.getSurvey(),
            databaseTime.getDate(),
            databaseTime.getTime(),
            observationTypeEnum.getValue());
   }

   static ObservationPK createObservationPK(ScatterObject scatterObject) {
      return new ObservationPK(
            scatterObject.getCompId().getNation(),
            scatterObject.getCompId().getPlatform(),
            scatterObject.getCompId().getSurvey(),
            scatterObject.getObservationDate(),
            scatterObject.getObservationTime(),
            scatterObject.getObservationType());
   }

   static ObservationPK createObservationPK(Scatter scatter) {
      return new ObservationPK(
            scatter.getCompId().getNation(),
            scatter.getCompId().getPlatform(),
            scatter.getCompId().getSurvey(),
            scatter.getCompId().getObservationDate(),
            scatter.getCompId().getObservationTime(),
            scatter.getObservationType());
   }

   static Observation createObservation(ObservationPK observationPK, DataFileSet dataFileSet, PingIndex pingIndex) {
      float bottomPhysicalDepth;
      if (dataFileSet.getDataConfiguration().isSeabedMounted()) {
         bottomPhysicalDepth = dataFileSet.getDataConfiguration().getSeabedMountedSeabedPhysicalDepth();
      } else {
         bottomPhysicalDepth = dataFileSet.getCoordinatedDepth(pingIndex);
      }
      GeoPoint geographicalPosition = pingIndex.getGeographicalPosition();
      return new Observation(observationPK,
            (float) dataFileSet.getVesselDistanceUncorrectedForWrapAround(pingIndex),
            geographicalPosition != null ? (float) geographicalPosition.getLatitude() : 0f,
            geographicalPosition != null ? (float) geographicalPosition.getLongitude() : 0f,
            bottomPhysicalDepth);
   }

   static Observation createObservation(ScatterObject scatterObject, DataFileSet dataFileSet) {
      PingIndex pingIndex = dataFileSet.getClosestPingIndex(PingMapping.millisToTimeValue(DatabaseTime.toMillis(scatterObject)), PingMapping.TIME);

      ObservationPK observationPK = createObservationPK(scatterObject);
      return createObservation(observationPK, dataFileSet, pingIndex);
   }

   public static QueryBuilder.AfterTerm createDeleteQueryBuilder(Class<? extends BaseDatabaseObject> clazz, SurveyPK surveyPK, DatabaseTime min, DatabaseTime max) {
      return QueryBuilder.delete(clazz).where()
            .eq(DatabaseData.NATION, surveyPK.getNation()).and()
            .eq(DatabaseData.PLATFORM, surveyPK.getPlatform()).and()
            .eq(DatabaseData.SURVEY, surveyPK.getSurvey()).and()
            .parenthesisBegin()
            /**/.eq(DatabaseData.OBSERVATION_DATE, min.getDate()).and()
            /**/.gte(DatabaseData.OBSERVATION_TIME, min.getTime()).or()
            /**/.gt(DatabaseData.OBSERVATION_DATE, min.getDate())
            .parenthesisEnd()
            .and()
            .parenthesisBegin()
            /**/.eq(DatabaseData.OBSERVATION_DATE, max.getDate()).and()
            /**/.lte(DatabaseData.OBSERVATION_TIME, max.getTime()).or()
            /**/.lt(DatabaseData.OBSERVATION_DATE, max.getDate())
            .parenthesisEnd();
   }

   public static DeleteQuery createDeleteQuery(Class<? extends BaseDatabaseObject> clazz, SurveyPK surveyPK, DatabaseTime min, DatabaseTime max) {
      return new DeleteQuery(createDeleteQueryBuilder(clazz, surveyPK, min, max).getQuery());
   }

   public static FloatRange getStoredZRange(Scatter scatter) {
      float dz = scatter.getChannelThickness();
      float minZ = (float) (Math.floor(scatter.getUpperInterpretationDepth() / dz) * dz);
      float maxZ = (float) (Math.ceil(scatter.getLowerInterpretationDepth() / dz) * dz);
      return FloatRange.of(minZ, maxZ);
   }
}
