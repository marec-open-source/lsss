package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.ObservationComment;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.StandardCommentPK;
import no.imr.tools.io.FileUtils;
import no.imr.tools.io.Print;
import no.imr.tools.logging.Log;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;

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
         StatelessSession session,
         SelectionQuery<ObservationComment> query,
         Path commentFile,
         Charset aCharset) {

      Map<StandardCommentPK, StandardComment> pkToStandardComment = new HashMap<>();

      try (ScrollableResults<ObservationComment> observationCommentResults = query
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY);
           PrintWriter f = FileUtils.newPrintWriter(commentFile, aCharset)) {

         while (observationCommentResults.next()) {
            ObservationComment observationComment = observationCommentResults.get();

            String text;
            if (observationComment.getStandardComment() != StandardComment.FREE_TEXT_STANDARD_COMMENT) {
               StandardCommentPK standardCommentPK = new StandardCommentPK(
                     observationComment.getCompId().getNation(),
                     observationComment.getCompId().getPlatform(),
                     observationComment.getStandardComment()
               );
               StandardComment standardComment = pkToStandardComment.computeIfAbsent(standardCommentPK, _ -> {
                  session.fetch(observationComment.getReferencedStandardComment());
                  return observationComment.getReferencedStandardComment();
               });
               text = standardComment.getText();
            } else {
               text = observationComment.getText();
            }

            f.print(observationComment.getCompId().getNation());
            Print.spaceAndValue(f, observationComment.getCompId().getPlatform());
            Print.spaceAndValue(f, observationComment.getCompId().getSurvey());
            Print.spaceAndValue(f, observationComment.getCompId().getObservationDate());
            Print.spaceAndValue(f, observationComment.getCompId().getObservationTime());
            Print.spaceAndValue(f, observationComment.getCompId().getObservationType());
            Print.spaceAndValue(f, observationComment.getMantissa());
            Print.spaceAndValue(f, observationComment.getExp());
            Print.spaceAndValue(f, text.replace('\n', ' ')); //Line feed generates problems
            f.println();
         }
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
      }
   }

   // Get next comment from file. Comments are repeated for frequencies
   static boolean nextComment(BufferedReader f, ObservationComment observationComment) {
      try {
         String line = f.readLine();
         if (line == null) {
            f.close();
            return false;
         }

         int startIndex = 0;
         int stopIndex = line.indexOf(' ', startIndex);
         observationComment.getCompId().setNation((short) Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observationComment.getCompId().setPlatform((short) Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observationComment.getCompId().setSurvey(Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observationComment.getCompId().setObservationDate(Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observationComment.getCompId().setObservationTime(Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observationComment.getCompId().setObservationType((short) Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observationComment.setMantissa(Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.indexOf(' ', startIndex);
         observationComment.setExp(Integer.parseInt(line, startIndex, stopIndex, 10));

         startIndex = stopIndex + 1;
         stopIndex = line.length();
         observationComment.setText(line.substring(startIndex, stopIndex));

         return true; // One line read
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error", e);
         return false;
      }
   } // nextComment())
}
