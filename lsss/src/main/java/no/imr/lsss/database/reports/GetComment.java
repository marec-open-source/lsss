package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.ObservationComment;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.StandardCommentPK;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Read comments from file. Need to do this since Hibernate does not allow cursor reset to start of Hibernate object.
 */
final class GetComment {
   private GetComment() {
   }

   static void generateCommentFile(
         StatelessSession aSession,
         String aQuery,
         Path aCommentFile,
         Charset aCharset) {

      Map<StandardCommentPK, StandardComment> pkToStandardComment = new HashMap<>();

      try (ScrollableResults<ObservationComment> observationCommentResults = aSession.createSelectionQuery(aQuery, ObservationComment.class)
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY);
           PrintWriter f = FileUtils.newPrintWriter(aCommentFile, aCharset)) {

         while (observationCommentResults.next()) {
            ObservationComment observationComment = observationCommentResults.get();

            f.printf("%d ", observationComment.getCompId().getNation());
            f.printf("%d ", observationComment.getCompId().getPlatform());
            f.printf("%d ", observationComment.getCompId().getSurvey());
            f.printf("%d ", observationComment.getCompId().getObservationDate());
            f.printf("%d ", observationComment.getCompId().getObservationTime());
            f.printf("%d ", observationComment.getCompId().getObservationType());
            f.printf("%d ", observationComment.getMantissa());
            f.printf("%d ", observationComment.getExp());
            if (observationComment.getText().isEmpty()) {
               StandardCommentPK standardCommentPK = new StandardCommentPK(
                     observationComment.getCompId().getNation(),
                     observationComment.getCompId().getPlatform(),
                     observationComment.getStandardComment()
               );
               StandardComment standardComment = pkToStandardComment.computeIfAbsent(standardCommentPK, _ -> {
                  aSession.fetch(observationComment.getReferencedStandardComment());
                  return observationComment.getReferencedStandardComment();
               });
               f.printf("%s ", standardComment.getText());
            } else {
               f.printf("%s ", observationComment.getText().replace('\n', ' '));   //Line feed generates problems
            }
            f.printf("%n");
         }
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }
   }

   // Get next comment from file. Comments are repeated for frequencies
   static boolean nextComment(BufferedReader f, ObservationComment aObservationComment) {
      String line;
      int startIndex;
      int stopIndex;
      int mantissa;
      int exponent;

      try {
         if ((line = f.readLine()) != null) {
            startIndex = 0;
            stopIndex = line.indexOf(' ', startIndex);
            aObservationComment.getCompId().setNation(Short.parseShort(line.substring(startIndex, stopIndex)));

            startIndex = stopIndex + 1;
            stopIndex = line.indexOf(' ', startIndex);
            aObservationComment.getCompId().setPlatform(Short.parseShort(line.substring(startIndex, stopIndex)));

            startIndex = stopIndex + 1;
            stopIndex = line.indexOf(' ', startIndex);
            aObservationComment.getCompId().setSurvey(Integer.parseInt(line.substring(startIndex, stopIndex)));

            startIndex = stopIndex + 1;
            stopIndex = line.indexOf(' ', startIndex);
            aObservationComment.getCompId().setObservationDate(Integer.parseInt(line.substring(startIndex, stopIndex)));

            startIndex = stopIndex + 1;
            stopIndex = line.indexOf(' ', startIndex);
            aObservationComment.getCompId().setObservationTime(Integer.parseInt(line.substring(startIndex, stopIndex)));

            startIndex = stopIndex + 1;
            stopIndex = line.indexOf(' ', startIndex);
            aObservationComment.getCompId().setObservationType(Short.parseShort(line.substring(startIndex, stopIndex)));

            startIndex = stopIndex + 1;
            stopIndex = line.indexOf(' ', startIndex);
            mantissa = Integer.parseInt(line.substring(startIndex, stopIndex));
            aObservationComment.setMantissa(mantissa);

            startIndex = stopIndex + 1;
            stopIndex = line.indexOf(' ', startIndex);
            exponent = Integer.parseInt(line.substring(startIndex, stopIndex));
            aObservationComment.setExp(exponent);

            startIndex = stopIndex + 1;
            stopIndex = line.length() - 1;
            aObservationComment.setText(line.substring(startIndex, stopIndex));

            return true; // One line read
         } else {
            f.close();
            return false; // Nothing read
         }
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }

      return false;  // Nothing read
   } // nextComment())
}
