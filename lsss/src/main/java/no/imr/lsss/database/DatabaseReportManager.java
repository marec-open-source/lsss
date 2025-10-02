package no.imr.lsss.database;

import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.svg.SvgIcon;

import java.io.IOException;
import java.nio.file.Path;

public interface DatabaseReportManager {
   String getTitle();

   SvgIcon getIcon();

   ViewHolder<?> getViewHolder();

   int getActiveReportGroupCount();

   void writeReports(Survey survey, Path directory, ProgressView progressView, AsyncHandle asyncHandle) throws IOException;

   void deleteReports(Path directory) throws IOException;
}
