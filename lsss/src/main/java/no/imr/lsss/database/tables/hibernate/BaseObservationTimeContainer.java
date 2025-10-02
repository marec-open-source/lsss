package no.imr.lsss.database.tables.hibernate;

public interface BaseObservationTimeContainer {
   int getObservationDate();

   void setObservationDate(int observationDate);

   int getObservationTime();

   void setObservationTime(int observationTime);
}
