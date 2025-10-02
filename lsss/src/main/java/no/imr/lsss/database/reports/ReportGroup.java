package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.swing.ProgressView;

import java.nio.file.Path;
import java.util.List;

abstract class ReportGroup {
   final ReportEngine reportEngine;

   ReportGroup(ReportEngine reportEngine) {
      this.reportEngine = reportEngine;
   }

   abstract void printReports(Survey aSelectedSurvey, Path aDirectory, ProgressView aProgressView, ReportEngine.Feedback aFeedback, AsyncHandle aAsyncHandle);

   <T extends BaseReport> List<T> getSelectedReports(List<T> reports) {
      return reports.stream()
            .filter(reportEngine::isReportSelected)
            .toList();
   }
}
