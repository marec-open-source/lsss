package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.PlatformCodes;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.logging.Log;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;

import java.util.logging.Level;

/**
 * Get ship IOC/NODC code (that is a string).
 */
public final class GetIocCode {
   public static final String CODE_SYS_NAME = "IOC/NODC";

   private GetIocCode() {
   }

   public static String getIocCode(StatelessSession aSession, Survey aSurvey) {
      String query = " from PlatformCodes a " +
            " where a.compId.nation   = " + aSurvey.getCompId().getNation() +
            " and   a.compId.platform = " + aSurvey.getCompId().getPlatform() +
            " and   a.compId.platformCodeSysName = '" + CODE_SYS_NAME + "'";

      try (ScrollableResults<PlatformCodes> platformCodesResults = aSession.createSelectionQuery(query, PlatformCodes.class)
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY)) {

         if (platformCodesResults.next()) {
            PlatformCodes ship = platformCodesResults.get();
            return ship.getPlatformCode();
         }
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }

      // A few hard-coded platform codes for Norwegian ships:
      short nation = aSurvey.getCompId().getNation();
      short platform = aSurvey.getCompId().getPlatform();
      if (nation == 578) {
         if (platform == 9387) {
            return "58KC";      // FF Dr. Fridtjof Nansen
         }
      }

      return "????";
   }
}
