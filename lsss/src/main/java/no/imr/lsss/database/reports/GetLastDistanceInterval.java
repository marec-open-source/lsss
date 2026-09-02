package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.tools.logging.Log;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.SelectionQuery;

import java.util.logging.Level;

final class GetLastDistanceInterval {
   private GetLastDistanceInterval() {
   }

   static float getLastDistanceInterval(SelectionQuery<Scatter> query) {
      try (ScrollableResults<Scatter> scatterResults = query
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY)) {

         float distanceInterval = 0;
         while (scatterResults.next()) {
            Scatter scatter = scatterResults.get();
            distanceInterval = scatter.getDistanceInterval();
         }
         return distanceInterval;
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }

      return 0;
   }
}
