package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.AcousticCategory;

final class PrintUser26 extends PrintUser25 {
   PrintUser26(ReportEngine reportEngine, ReportEngine.Feedback feedback) {
      super(26, reportEngine, feedback);
   }

   @Override
   String getCategory(AcousticCategory acousticCategory) {
      return Integer.toString(acousticCategory.getCompId().getAcousticCategory());
   }
}
