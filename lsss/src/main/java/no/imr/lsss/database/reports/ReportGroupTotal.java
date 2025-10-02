package no.imr.lsss.database.reports;

import no.imr.lsss.database.reports.hibernate.AcCat;
import no.imr.lsss.database.reports.hibernate.DistCount;
import no.imr.lsss.database.reports.hibernate.ReportTotalData;
import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ProgressView;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * DESCRIPTION: Generate total reports with vertical resolution.
 */
final class ReportGroupTotal extends ReportGroup {
   private final List<BaseTotalReport> reports;

   ReportGroupTotal(ReportEngine reportEngine) {
      super(reportEngine);

      reports = List.of(
            new PrintUser40(reportEngine) // Multi species report
      );
   }

   @Override
   void printReports(Survey aSelectedSurvey, Path aDirectory, ProgressView aProgressView, ReportEngine.Feedback feedback, AsyncHandle aAsyncHandle) {
      List<BaseTotalReport> selectedReports = getSelectedReports(reports);
      if (selectedReports.isEmpty()) {
         return;
      }

      String progressText = "Total reports";
      aProgressView.setSecondaryText(progressText);

      PrintTotalData printTotalData = new PrintTotalData(aSelectedSurvey);
      printTotalData.clearData();

      //Queries
      int pelagicEchogramChannels = ScatterTypeEnum.PELAGIC.getValue();
      int bottomEchogramChannels = ScatterTypeEnum.BOTTOM.getValue();

      String queryGeneral =
            " select " +
                  "new no.imr.lsss.database.reports.hibernate.ReportTotalData(" +
                  " a.compId.frequency, " +
                  " a.compId.transceiver, " +
                  " a.compId.scatterType, " +
                  " a.compId.acousticCategory, " +
                  " a.compId.channelNumber, " +
                  " sum(a.sa)" +
                  ")" +
                  " from ScatterData a " +
                  " where a.compId.nation   = " + aSelectedSurvey.getCompId().getNation() +
                  " and   a.compId.platform = " + aSelectedSurvey.getCompId().getPlatform() +
                  " and   a.compId.survey   = " + aSelectedSurvey.getCompId().getSurvey() +
                  " and ( a.compId.observationDate > " + reportEngine.getStartDate() +
                  "  or  (a.compId.observationDate = " + reportEngine.getStartDate() + " and a.compId.observationTime >= " + reportEngine.getStartTime() + ") )" +
                  " and ( a.compId.observationDate < " + reportEngine.getStopDate() +
                  "  or  (a.compId.observationDate = " + reportEngine.getStopDate() + " and a.compId.observationTime <= " + reportEngine.getStopTime() + ") )" +
                  " and ( a.compId.scatterType = " + pelagicEchogramChannels +
                  "  or   a.compId.scatterType = " + bottomEchogramChannels + ")" +
                  " group by a.compId.frequency," +
                  " a.compId.transceiver," +
                  " a.compId.scatterType," +
                  " a.compId.acousticCategory," +
                  " a.compId.channelNumber" +
                  " order by a.compId.frequency," +
                  " a.compId.transceiver," +
                  " a.compId.scatterType," +
                  " a.compId.acousticCategory," +
                  " a.compId.channelNumber";

      String queryAcousticCategory =
            " select distinct " +
                  "new no.imr.lsss.database.reports.hibernate.AcCat(" +
                  " b.compId.acousticCategory, " +
                  " b.composite, " +
                  " b.initials, " +
                  " b.englishInitials, " +
                  " b.commonName, " +
                  " b.englishName " +
                  ")" +
                  " from ScatterData a, AcousticCategory b " +
                  " where a.compId.nation   = " + aSelectedSurvey.getCompId().getNation() +
                  " and   a.compId.nation   = b.compId.nation " +
                  " and   a.compId.platform = " + aSelectedSurvey.getCompId().getPlatform() +
                  " and   a.compId.platform = b.compId.platform " +
                  " and   a.compId.acousticCategory = b.compId.acousticCategory " +
                  " and   a.compId.survey   = " + aSelectedSurvey.getCompId().getSurvey() +
                  " and ( a.compId.observationDate > " + reportEngine.getStartDate() +
                  "  or  (a.compId.observationDate = " + reportEngine.getStartDate() + " and a.compId.observationTime >= " + reportEngine.getStartTime() + ") )" +
                  " and ( a.compId.observationDate < " + reportEngine.getStopDate() +
                  "  or  (a.compId.observationDate = " + reportEngine.getStopDate() + " and a.compId.observationTime <= " + reportEngine.getStopTime() + ") )" +
                  " and ( a.compId.scatterType = " + pelagicEchogramChannels +
                  "  or   a.compId.scatterType = " + bottomEchogramChannels + ")" +
                  " order by b.compId.acousticCategory," +
                  " b.composite, " +
                  " b.initials, " +
                  " b.englishInitials, " +
                  " b.commonName, " +
                  " b.englishName ";

      String queryDistCount =
            " select " +
                  "new no.imr.lsss.database.reports.hibernate.DistCount(" +
                  " a.compId.frequency, " +
                  " a.compId.transceiver, " +
                  " a.compId.scatterType, " +
                  " a.distanceInterval, " +
                  " count(a.sa)" +
                  ")" +
                  " from Scatter a " +
                  " where a.compId.nation   = " + aSelectedSurvey.getCompId().getNation() +
                  " and   a.compId.platform = " + aSelectedSurvey.getCompId().getPlatform() +
                  " and   a.compId.survey   = " + aSelectedSurvey.getCompId().getSurvey() +
                  " and ( a.compId.observationDate > " + reportEngine.getStartDate() +
                  "  or  (a.compId.observationDate = " + reportEngine.getStartDate() + " and a.compId.observationTime >= " + reportEngine.getStartTime() + ") )" +
                  " and ( a.compId.observationDate < " + reportEngine.getStopDate() +
                  "  or  (a.compId.observationDate = " + reportEngine.getStopDate() + " and a.compId.observationTime <= " + reportEngine.getStopTime() + ") )" +
                  " and ( a.compId.scatterType = " + pelagicEchogramChannels +
                  "  or   a.compId.scatterType = " + bottomEchogramChannels + ")" +
                  " group by a.compId.frequency," +
                  " a.compId.transceiver," +
                  " a.compId.scatterType," +
                  " a.distanceInterval" +
                  " order by a.compId.frequency," +
                  " a.compId.transceiver," +
                  " a.compId.scatterType," +
                  " a.distanceInterval";

      reportEngine.getLSSS().getDatabaseManager().getDatabaseConnection().executeStatelessQuery(aSession -> {
         List<ReportTotalData> dataList = aSession.createQuery(queryGeneral, ReportTotalData.class).list();

         List<AcCat> acCatList = aSession.createQuery(queryAcousticCategory, AcCat.class).list();
         acCatList.forEach(printTotalData::setAcCat);

         List<DistCount> distCountList = aSession.createQuery(queryDistCount, DistCount.class).list();
         distCountList.forEach(printTotalData::setDistCount);

         for (int i = 0; i < dataList.size(); i++) {
            if (aAsyncHandle.isCancelled()) {
               break;
            }
            aProgressView.getSecondaryProgressHandler().setProgress((double) i / dataList.size());
            ReportTotalData data = dataList.get(i);
            if (!printTotalData.setData(data)) { // Returns false if not set: data is now full, and
                                                  // next fetch contains data from new frequency.
               // New frequency/transceiver: print and open new output file
               for (BaseTotalReport report : selectedReports) {
                  try {
                     report.open(printTotalData, aDirectory);
                  } catch (IOException e) {
                     Log.global.log(Level.WARNING, e.getMessage(), e);
                     return;
                  }

                  report.print(printTotalData);
                  report.close();                 // Close report for this frequency
                  printTotalData.clearData();    // New frequency: clear data before filling array again
                  printTotalData.setData(data);  // Data was not set by start of "if"
               }
            }
         }
         // New frequency/transceiver: print and open new output file - last fetch
         for (BaseTotalReport report : selectedReports) {
            try {
               report.open(printTotalData, aDirectory);
            } catch (IOException e) {
               Log.global.log(Level.WARNING, e.getMessage(), e);
               return;
            }

            report.print(printTotalData);
            report.close();                 // Close report for this frequency
            printTotalData.clearData();    // New frequency: clear data before filling array again
         }
      });

      selectedReports.forEach(BaseTotalReport::close);

      aProgressView.incrementMainProgress("");
   }
}
