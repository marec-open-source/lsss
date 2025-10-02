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
import no.imr.tools.database.queries.FetchQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DatabaseConsistencyChecker {
   private final DatabaseConnection databaseConnection;

   private DatabaseConsistencyChecker(DatabaseConnection databaseConnection) {
      this.databaseConnection = databaseConnection;
   }

   static void assertConsistency(DatabaseConnection databaseConnection) {
      new DatabaseConsistencyChecker(databaseConnection).assertConsistency();
   }

   private void assertConsistency() {
      databaseConnection.waitUntilFinished();

      for (Survey survey : databaseConnection.executeFetchQuery(new FetchQuery<>(Survey.class))) {
         assertCollectionSizes(survey);
         assertScatterObjectConsistency(survey);
      }
   }

   private void assertScatterObjectConsistency(Survey survey) {
      databaseConnection.executeQuery(session -> {
         LsssQuery.fetch(Observation.class, survey).executeAndGetValue(session).forEach(observation -> {
            short type = observation.getCompId().getObservationType();
            if (type == ObservationTypeEnum.SCATTER_OBJECT_PELAGIC.getValue() ||
                  type == ObservationTypeEnum.SCATTER_OBJECT_SCHOOL.getValue()) {
               assert !observation.getScatterObjects().isEmpty();
            }
         });
         LsssQuery.fetch(ScatterObject.class, survey).executeAndGetValue(session).forEach(scatterObject -> {
            if (scatterObject.getObservationType() != ObservationTypeEnum.SCATTER_OBJECT_DUMMY.getValue()) {
               Scatter firstScatter = Collections.min(scatterObject.getScatters(), InterpretationSummary.SCATTER_COMPARATOR);
               Scatter lastScatter = Collections.max(scatterObject.getScatters(), InterpretationSummary.SCATTER_COMPARATOR);

               assert scatterObject.getObservationDate() == firstScatter.getCompId().getObservationDate()
                     && scatterObject.getObservationTime() == firstScatter.getCompId().getObservationTime()
                     : scatterObject + ", " + firstScatter;

               long firstMillis = DatabaseTime.toMillis(firstScatter);
               long lastMillis = DatabaseTime.toMillis(lastScatter);

               long expectedDuration = (lastMillis - firstMillis) / 10 + lastScatter.getDuration();
               assert scatterObject.getDuration() == expectedDuration :
                     scatterObject + ", expected duration: " + expectedDuration;
            }
         });
      });
   }

   private void assertCollectionSizes(Survey survey) {
      List<Observation> observations = getObservations(survey);
      List<ScatterObject> scatterObjectList = getScatterObjects(survey);
      List<Scatter> scatterList = getScatters(survey);

      List<Observation> scatterObservations = new ArrayList<>();
      List<Observation> scatterObjectObservations = new ArrayList<>();

      for (Observation observation : observations) {
         short type = observation.getCompId().getObservationType();
         if (type == ObservationTypeEnum.SCHOOL_OF_FISH_DATA.getValue() ||
               type == ObservationTypeEnum.SCATTERED_FISH_DATA.getValue()) {
            scatterObservations.add(observation);
         } else if (type == ObservationTypeEnum.SCATTER_OBJECT_PELAGIC.getValue() ||
               type == ObservationTypeEnum.SCATTER_OBJECT_SCHOOL.getValue()) {
            scatterObjectObservations.add(observation);
         }
      }
      assert scatterObservations.size() <= scatterList.size() :
            " scatterObservations " + scatterObservations.size() + " scatterList: " + scatterList.size();
      assert scatterObjectObservations.size() <= scatterObjectList.size() :
            " scatterObjectObservations " + scatterObjectObservations.size() + " scatterObjectList: " + scatterObjectList.size();
      assert scatterObjectList.size() <= scatterList.size() :
            " scatterObjectList " + scatterObjectList.size() + " scatterList: " + scatterList.size();
   }

   private List<Scatter> getScatters(Survey survey) {
      return databaseConnection.executeFetchQuery(LsssQuery.fetch(Scatter.class, survey));
   }

   private List<ScatterObject> getScatterObjects(Survey survey) {
      return databaseConnection.executeFetchQuery(LsssQuery.fetch(ScatterObject.class, survey));
   }

   private List<Observation> getObservations(Survey survey) {
      return databaseConnection.executeFetchQuery(LsssQuery.fetch(Observation.class, survey));
   }
}
