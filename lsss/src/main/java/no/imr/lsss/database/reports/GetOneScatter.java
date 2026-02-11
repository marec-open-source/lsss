package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.logging.Log;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;

import java.util.logging.Level;

/**
 * Get one scatter.
 */
final class GetOneScatter {
   private GetOneScatter() {
   }

   static float getDistanceInterval(StatelessSession aSession, Survey aSurvey) {
      String query = " from Scatter a " +
            " where a.compId.nation   = " + aSurvey.getCompId().getNation() +
            " and   a.compId.platform = " + aSurvey.getCompId().getPlatform() +
            " and   a.compId.survey = " + aSurvey.getCompId().getSurvey();

      try (ScrollableResults<Scatter> scatterResults = aSession.createSelectionQuery(query, Scatter.class)
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY)) {

         if (scatterResults.next()) {
            Scatter scatter = scatterResults.get();
            return scatter.getDistanceInterval();
         }
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }

      return 0;
   }
}
