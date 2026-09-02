package no.imr.lsss.modules.test;

import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.modules.interpretation.InterpretationSummary;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.queries.QueryBuilder;
import org.hibernate.StatelessSession;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

final class DatabaseConsistencyChecker {
   private DatabaseConsistencyChecker() {
   }

   static void assertConsistency(DatabaseConnection databaseConnection) {
      databaseConnection.waitUntilFinished();
      databaseConnection.executeStatelessQuery(session -> {
         for (Survey survey : LsssQuery.fetch(Survey.class).executeAndGetValue(session)) {
            assertCollectionSizes(session, survey);
            assertScatterObjectConsistency(session, survey);
         }
      });
   }

   private static void assertScatterObjectConsistency(StatelessSession session, Survey survey) {
      for (Observation observation : LsssQuery.fetch(Observation.class, survey).executeAndGetValue(session)) {
         short type = observation.getCompId().getObservationType();
         if (type == ObservationTypeEnum.SCATTER_OBJECT_PELAGIC.getValue() ||
               type == ObservationTypeEnum.SCATTER_OBJECT_SCHOOL.getValue()) {
            session.fetch(observation.getScatterObjects());
            assert !observation.getScatterObjects().isEmpty();
         }
      }
      for (ScatterObject scatterObject : LsssQuery.fetch(ScatterObject.class, survey).executeAndGetValue(session)) {
         if (scatterObject.getObservationType() != ObservationTypeEnum.SCATTER_OBJECT_DUMMY.getValue()) {
            session.fetch(scatterObject.getScatters());
            Scatter firstScatter = Collections.min(scatterObject.getScatters(), InterpretationSummary.SCATTER_COMPARATOR);
            Scatter lastScatter = Collections.max(scatterObject.getScatters(), InterpretationSummary.SCATTER_COMPARATOR);

            assert scatterObject.getObservationDate() == firstScatter.getCompId().getObservationDate()
                  && scatterObject.getObservationTime() == firstScatter.getCompId().getObservationTime()
                  : scatterObject + ", " + firstScatter;

            Instant firstTime = DatabaseTime.toInstant(firstScatter);
            Instant lastTime = DatabaseTime.toInstant(lastScatter);

            long expectedDuration = firstTime.until(lastTime, ChronoUnit.MILLIS) / 10 + lastScatter.getDuration();
            assert scatterObject.getDuration() == expectedDuration :
                  scatterObject + ", expected duration: " + expectedDuration;
         }
      }
   }

   private static void assertCollectionSizes(StatelessSession session, Survey survey) {
      List<Observation> observations = LsssQuery.fetch(Observation.class, survey).executeAndGetValue(session);
      long scatterObjectCount = LsssQuery.forSurvey(QueryBuilder.count(ScatterObject.class), survey.getCompId()).build().executeAndGetValue(session);
      long scatterCount = LsssQuery.forSurvey(QueryBuilder.count(Scatter.class), survey.getCompId()).build().executeAndGetValue(session);

      long scatterObservationCount = 0;
      long scatterObjectObservationCount = 0;

      for (Observation observation : observations) {
         short type = observation.getCompId().getObservationType();
         if (type == ObservationTypeEnum.SCHOOL_OF_FISH_DATA.getValue() ||
               type == ObservationTypeEnum.SCATTERED_FISH_DATA.getValue()) {
            scatterObservationCount++;
         } else if (type == ObservationTypeEnum.SCATTER_OBJECT_PELAGIC.getValue() ||
               type == ObservationTypeEnum.SCATTER_OBJECT_SCHOOL.getValue()) {
            scatterObjectObservationCount++;
         }
      }
      assert scatterObservationCount <= scatterCount :
            "scatterObservationCount: " + scatterObservationCount + ", scatterCount: " + scatterCount;
      assert scatterObjectObservationCount <= scatterObjectCount :
            "scatterObjectObservationCount: " + scatterObjectObservationCount + ", scatterObjectCount: " + scatterObjectCount;
      assert scatterObjectCount <= scatterCount :
            "scatterObjectCount: " + scatterObjectCount + ", scatterCount: " + scatterCount;
   }
}
