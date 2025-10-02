package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.PlatformCodes;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.logging.Log;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;

import java.util.logging.Level;

/**
 * Get ship callsign.
 */
final class GetCallSign {
   private GetCallSign() {
   }

   static String getCallsign(StatelessSession aSession, Survey aSurvey) {
      String query = " from PlatformCodes a " +
            " where a.compId.nation   = " + aSurvey.getCompId().getNation() +
            " and   a.compId.platform = " + aSurvey.getCompId().getPlatform() +
            " and   a.compId.platformCodeSysName = 'Call signal'";

      try (ScrollableResults platformCodesResults = aSession.createQuery(query)
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY)) {

         if (platformCodesResults.next()) {
            PlatformCodes ship = (PlatformCodes) platformCodesResults.get(0);
            return ship.getPlatformCode();
         }
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }

      return "????";
   }
}
