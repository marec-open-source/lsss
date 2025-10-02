package no.imr.lsss.database.reports;

import no.imr.lsss.database.util.LanguageUtils;

abstract class BaseReport {
   private final int type;
   private final ReportEngine reportEngine;

   BaseReport(int type, ReportEngine reportEngine) {
      this.type = type;
      this.reportEngine = reportEngine;
   }

   int getType() {
      return type;
   }

   ReportEngine getReportEngine() {
      return reportEngine;
   }

   LanguageUtils getLanguageUtils() {
      return reportEngine.getLanguageUtils();
   }

   String getFilePrefix() {
      return "ListUserFile" + String.format("%02d_", type);
   }

   String getFileSuffix() {
      return ".txt";
   }

   void finaliseReport(PrintData.Pelagic aPrintData) {
   }

   abstract void close();
}
