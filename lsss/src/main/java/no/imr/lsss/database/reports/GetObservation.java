package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Read observations from file. Need to do this since Hibernate does not allow cursor reset to start of Hibernate object.
 */
final class GetObservation {
   private float startObservationDistance = -1;
   private float stopObservationDistance = Float.NEGATIVE_INFINITY;
   private float deltaDistance = 0;

   GetObservation() {
   }

   void generateObservationFile(
         StatelessSession aSession,
         String aQuery,
         Path aObservationFile,
         Path aObservationFileAll,
         Charset aCharset) {

      try (ScrollableResults<Observation> observationResults = aSession.createSelectionQuery(aQuery, Observation.class)
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY);
           PrintWriter f = FileUtils.newPrintWriter(aObservationFile, aCharset);
           PrintWriter fAll = FileUtils.newPrintWriter(aObservationFileAll, aCharset)
      ) {
         while (observationResults.next()) {
            Observation obs = observationResults.get();

            ObservationPK obsPK = obs.getCompId();
            if (obsPK.getObservationType() == ObservationTypeEnum.SCATTERED_FISH_DATA.getValue() ||
                  obsPK.getObservationType() == ObservationTypeEnum.SCHOOL_OF_FISH_DATA.getValue()) {
               f.print(obsPK.getNation());
               f.print(" " + obsPK.getPlatform());
               f.print(" " + obsPK.getSurvey());
               f.print(" " + obsPK.getObservationDate());
               f.print(" " + obsPK.getObservationTime());
               f.print(" " + obsPK.getObservationType());
               f.print(" " + obs.getDistance());
               f.print(" " + obs.getLatitude());
               f.print(" " + obs.getLongitude());
               f.print(" " + obs.getBottomDepth());
               if (obsPK.getObservationType() == ObservationTypeEnum.SCATTERED_FISH_DATA.getValue()) {
                  f.print(" SCATTER");
               } else if (obsPK.getObservationType() == ObservationTypeEnum.SCHOOL_OF_FISH_DATA.getValue()) {
                  f.print(" SCHOOL");
               }
               f.println();
               float previous_stop = stopObservationDistance;
               if (obsPK.getObservationType() == ObservationTypeEnum.SCATTERED_FISH_DATA.getValue() ||
                   obsPK.getObservationType() == ObservationTypeEnum.SCHOOL_OF_FISH_DATA.getValue()) {
                  if (startObservationDistance == -1) {
                     //startObservationDistance = Math.min(startObservationDistance, obs.getDistance());
                     startObservationDistance = obs.getDistance();
                  }
                  //stopObservationDistance = Math.max(stopObservationDistance, obs.getDistance());
                  stopObservationDistance = obs.getDistance();
               }
               deltaDistance = stopObservationDistance - previous_stop;
            }

            fAll.print(obsPK.getNation());
            fAll.print(" " + obsPK.getPlatform());
            fAll.print(" " + obsPK.getSurvey());
            fAll.print(" " + obsPK.getObservationDate());
            fAll.print(" " + obsPK.getObservationTime());
            fAll.print(" " + obsPK.getObservationType());
            fAll.print(" " + obs.getDistance());
            fAll.print(" " + obs.getLatitude());
            fAll.print(" " + obs.getLongitude());
            fAll.print(" " + obs.getBottomDepth());
            if (obsPK.getObservationType() == ObservationTypeEnum.SCATTERED_FISH_DATA.getValue()) {
               fAll.print(" SCATTER");
            } else if (obsPK.getObservationType() == ObservationTypeEnum.SCHOOL_OF_FISH_DATA.getValue()) {
               fAll.print(" SCHOOL");
            } else {
               fAll.print(" POSITION");
            }
            fAll.println();
         } //while
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }
   }

   float getStopObservationDistance() {
      return stopObservationDistance + deltaDistance;
   }

   float getFinalObservationDistance(float finalDistanceInterval) {
      return stopObservationDistance + finalDistanceInterval;
   }

   float getStartObservationDistance() {
      return startObservationDistance;
   }

   // Get next observation from file. Observations are reused for all frequencies
   static boolean nextObservation(BufferedReader aReader, Observation aObservation) {
      try {
         String line = aReader.readLine();
         if (line == null) {
            return false;
         }

         int startIndex;
         int stopIndex;
         ObservationPK obsPK = aObservation.getCompId();

         startIndex = 0;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setNation(Short.parseShort(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setPlatform(Short.parseShort(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setSurvey(Integer.parseInt(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setObservationDate(Integer.parseInt(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setObservationTime(Integer.parseInt(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setObservationType(Short.parseShort(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         aObservation.setDistance(Float.parseFloat(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         aObservation.setLatitude(Float.parseFloat(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         aObservation.setLongitude(Float.parseFloat(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         aObservation.setBottomDepth(Float.parseFloat(line.substring(startIndex, stopIndex)));

         return true; // One line read
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
         return false;
      }
   }
}
