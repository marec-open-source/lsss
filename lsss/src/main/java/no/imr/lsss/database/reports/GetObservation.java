package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.tools.io.FileUtils;
import no.imr.tools.io.Print;
import no.imr.tools.logging.Log;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.SelectionQuery;

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
         SelectionQuery<Observation> query,
         Path observationFile,
         Path observationFileAll,
         Charset charset) {

      try (ScrollableResults<Observation> observationResults = query
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY);
           PrintWriter f = FileUtils.newPrintWriter(observationFile, charset);
           PrintWriter fAll = FileUtils.newPrintWriter(observationFileAll, charset)
      ) {
         while (observationResults.next()) {
            Observation obs = observationResults.get();

            ObservationPK obsPK = obs.getCompId();
            if (obsPK.getObservationType() == ObservationTypeEnum.SCATTERED_FISH_DATA.getValue() ||
                  obsPK.getObservationType() == ObservationTypeEnum.SCHOOL_OF_FISH_DATA.getValue()) {
               f.print(obsPK.getNation());
               Print.spaceAndValue(f, obsPK.getPlatform());
               Print.spaceAndValue(f, obsPK.getSurvey());
               Print.spaceAndValue(f, obsPK.getObservationDate());
               Print.spaceAndValue(f, obsPK.getObservationTime());
               Print.spaceAndValue(f, obsPK.getObservationType());
               Print.spaceAndValue(f, obs.getDistance());
               Print.spaceAndValue(f, obs.getLatitude());
               Print.spaceAndValue(f, obs.getLongitude());
               Print.spaceAndValue(f, obs.getBottomDepth());
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
            Print.spaceAndValue(fAll, obsPK.getPlatform());
            Print.spaceAndValue(fAll, obsPK.getSurvey());
            Print.spaceAndValue(fAll, obsPK.getObservationDate());
            Print.spaceAndValue(fAll, obsPK.getObservationTime());
            Print.spaceAndValue(fAll, obsPK.getObservationType());
            Print.spaceAndValue(fAll, obs.getDistance());
            Print.spaceAndValue(fAll, obs.getLatitude());
            Print.spaceAndValue(fAll, obs.getLongitude());
            Print.spaceAndValue(fAll, obs.getBottomDepth());
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
   static boolean nextObservation(BufferedReader reader, Observation observation) {
      try {
         String line = reader.readLine();
         if (line == null) {
            return false;
         }

         ObservationPK obsPK = observation.getCompId();

         int startIndex = 0;
         int stopIndex = line.indexOf(' ', startIndex);
         obsPK.setNation((short) Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setPlatform((short) Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setSurvey(Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setObservationDate(Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setObservationTime(Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         obsPK.setObservationType((short) Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observation.setDistance(Float.parseFloat(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observation.setLatitude(Float.parseFloat(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observation.setLongitude(Float.parseFloat(line.substring(startIndex, stopIndex)));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observation.setBottomDepth(Float.parseFloat(line.substring(startIndex, stopIndex)));

         return true; // One line read
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
         return false;
      }
   }
}
