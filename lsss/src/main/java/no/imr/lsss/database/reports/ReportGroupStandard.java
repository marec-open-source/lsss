package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationComment;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterData;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ProgressView;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * DESCRIPTION: Generate standard reports by reading data from one frequency at a time at increasing time.
 */
final class ReportGroupStandard extends ReportGroup {
   private final PrintCompact printCompact;

   private final List<BaseSingleFrequencyReport> reports;

   private Observation gObservation = new Observation(new ObservationPK());
   private Observation stopObservation = new Observation(new ObservationPK());
   private ObservationComment mObservationComment = new ObservationComment();

   ReportGroupStandard(ReportEngine reportEngine) {
      super(reportEngine);

      printCompact = new PrintCompact(reportEngine);

      reports = List.of(
            printCompact,                  // Multi species report
            new PrintUser2(reportEngine),  // Multi species report
            new PrintUser3(reportEngine),  // Multi species report
            new PrintUser16(reportEngine), // Multi species report

            new PrintUser4(reportEngine),  // Single species report
            new PrintUser5(reportEngine),  // Single species report
            new PrintUser7(reportEngine),  // Single species report
            new PrintUser8(reportEngine),  // Single species report
            new PrintUser9(reportEngine),  // Single species report
            new PrintUser11(reportEngine)  // Single species report
      );
   }

   @Override
   void printReports(Survey aSelectedSurvey, Path aDirectory, ProgressView aProgressView, ReportEngine.Feedback aFeedback, AsyncHandle aAsyncHandle) {
      ReportMode aMode = reportEngine.getMode();
      reportEngine.getLSSS().getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {
         int printFrequency = -1;       // Frequency of currently open file
         short printTransceiver = -1;   // Transceiver of currently open file
         int currentFr;                 // Synchronizing frequency (ruling data: scatterData rules)
         short currentTr;               // Synchronizing transceiver
         int currentDa;                 // Synchronizing date
         int currentTi;                 // Synchronizing time
         boolean doInitializeAccumulate = true;    // Indicates if accumulate data-structure should be initialized
         short scatterTypePelagic;
         short scatterTypeBottom;
         if (reportEngine.isSchoolReport()) {
            scatterTypePelagic = ScatterTypeEnum.PELAGIC_SCHOOL.getValue();
            scatterTypeBottom = ScatterTypeEnum.BOTTOM_SCHOOL.getValue();
         } else {
            scatterTypePelagic = ScatterTypeEnum.PELAGIC.getValue();
            scatterTypeBottom = ScatterTypeEnum.BOTTOM.getValue();
         }
         PrintData.Pelagic aPrintData = new PrintData.Pelagic(reportEngine, aFeedback, scatterTypePelagic, aSelectedSurvey);
         PrintData.Bottom aPrintDataBottom = new PrintData.Bottom(reportEngine, aFeedback, scatterTypeBottom, aSelectedSurvey);

         int[] regSchoolObject = new int[ReportEngine.MAX_REMEMBERED_SCHOOLS];
         int noRegSchoolObject = 0;
         int actualNoFrequencies = 1; //Start count

         List<BaseSingleFrequencyReport> selectedReports = getSelectedReports(reports);
         if (selectedReports.isEmpty()) {
            return;
         }

         String progressText = "Increasing time (reports 1, 2, ...) - ";
         aProgressView.setSecondaryText(progressText + "Starting");

         aPrintData.clearData(ReportMode.NATIVE);

         aPrintDataBottom.clearData(ReportMode.NATIVE);

         String callsign = GetCallSign.getCallsign(session, aSelectedSurvey);
         aPrintData.setCallsign(callsign);

         String platformIocCode = GetIocCode.getIocCode(session, aSelectedSurvey);
         aPrintData.setPlatformIocCode(platformIocCode);

         //Queries
         String queryGeneral =
               " where a.compId.nation   = " + aSelectedSurvey.getCompId().getNation() +
                     " and   a.compId.platform = " + aSelectedSurvey.getCompId().getPlatform() +
                     " and   a.compId.survey   = " + aSelectedSurvey.getCompId().getSurvey() +
                     " and ( a.compId.observationDate > " + reportEngine.getStartDate() +
                     "  or  (a.compId.observationDate = " + reportEngine.getStartDate() + " and a.compId.observationTime >= " + reportEngine.getStartTime() + ") )" +
                     " and ( a.compId.observationDate < " + reportEngine.getStopDate() +
                     "  or  (a.compId.observationDate = " + reportEngine.getStopDate() + " and a.compId.observationTime <= " + reportEngine.getStopTime() + ") )";

         String queryObservation = "from Observation a " + queryGeneral +
               " order by a.compId.observationDate, a.compId.observationTime, " +
               "          a.compId.observationType";

         String queryScatter = "from Scatter a " + queryGeneral +
               " order by a.compId.frequency,       a.compId.transceiver, " +
               "          a.compId.observationDate, a.compId.observationTime, " +
               "          a.compId.scatterType";

         String queryScatterData = "from ScatterData a " + queryGeneral +
               " order by a.compId.frequency,       a.compId.transceiver, " +
               "          a.compId.observationDate, a.compId.observationTime, " +
               "          a.compId.scatterType, " +
               "          a.compId.channelNumber,   a.compId.acousticCategory";

         String queryComment = "from ObservationComment a " + queryGeneral +
               " order by a.compId.observationDate, a.compId.observationTime, " +
               "          a.compId.observationType";   // "Scattered Fish Data"=3000 (=EchogramData) or "School Of Fish Data"=4000
         // BEI: SCATTER=3000                             SCHOOL=4000

         // File to write observations since scroll back is not allowed in hibernate yet (2007)
         Path observationFile = aDirectory.resolve(ReportGenerator.SCRATCH_FILE_PREFIX + "standard_acoustic_observations");
         Path observationFileAll = aDirectory.resolve(ReportGenerator.SCRATCH_FILE_PREFIX + "standard_acoustic_observations_all");
         GetObservation getObservation = new GetObservation();
         getObservation.generateObservationFile(session, queryObservation, observationFile, observationFileAll, reportEngine.getCharset());

         float lastDistanceInterval = GetLastDistanceInterval.getLastDistanceInterval(session, queryScatter);

         // File to write comments since scroll back is not allowed in hibernate yet (2007)
         Path commentFile = aDirectory.resolve(ReportGenerator.SCRATCH_FILE_PREFIX + "standard_comments");
         GetComment.generateCommentFile(session, queryComment, commentFile, reportEngine.getCharset());

         try (BufferedReader fObsStop = Files.newBufferedReader(observationFileAll, reportEngine.getCharset());
              RewindableBufferedReader fObs = new RewindableBufferedReader(observationFile, reportEngine.getCharset());
              RewindableBufferedReader fComment = new RewindableBufferedReader(commentFile, reportEngine.getCharset());
              ScrollableResults scatterResults = session.createQuery(queryScatter)
                    .setReadOnly(true)
                    .scroll(ScrollMode.FORWARD_ONLY);
              ScrollableResults scatterDataResults = session.createQuery(queryScatterData)
                    .setReadOnly(true)
                    .scroll(ScrollMode.FORWARD_ONLY)) {

            // Make ready for first fetch
            boolean scatExist = scatterResults.next();
            boolean sDataExist = scatterDataResults.next();
            boolean obsRead = GetObservation.nextObservation(fObs.getReader(), gObservation);
            boolean obsReadStop = GetObservation.nextObservation(fObsStop, stopObservation);
            boolean commentRead = GetComment.nextComment(fComment.getReader(), mObservationComment);                               // Read from file

            if (scatExist) {
               // Frequency and transceiver of open files
               Scatter scat = (Scatter) scatterResults.get(0);

               aPrintData.setScatter(scat);     //todo: is this needed ?
               if (scat.getBottomActive() == 1) {
                  aPrintDataBottom.setScatter((Scatter) scatterResults.get(0));
               }

               if (!selectedReports.isEmpty()) {
                  if (reportEngine.getPrintScrutinizedSpCheck()) {
                     List<Integer> categoryCodeList = reportEngine.getScrutinizedSpeciesList(session, aSelectedSurvey);
                     for (Integer categoryCode : categoryCodeList) {
                        String query = String.format("from AcousticCategory a " +
                                    "where a.compId.nation=%1d " +
                                    "and   a.compId.platform=%1d " +
                                    "and   a.compId.acousticCategory=%1d ",
                              aSelectedSurvey.getCompId().getNation(),
                              aSelectedSurvey.getCompId().getPlatform(),
                              categoryCode);

                        try (ScrollableResults acousticCategoryResults = session.createQuery(query)
                              .setReadOnly(true)
                              .scroll(ScrollMode.FORWARD_ONLY)) {
                           if (acousticCategoryResults.next()) {
                              AcousticCategory acousticCategory = (AcousticCategory) acousticCategoryResults.get(0);
                              aPrintData.setAcousticCategoryPrint(acousticCategory);
                              aPrintDataBottom.setAcousticCategoryPrint(acousticCategory);
                           }
                        }
                     }
                  } else { // !printScrutinizedSpCheck.isSelected()
                     List<Purpose> sortedSpeciesPurposeList = ReportEngine.getSortedSpeciesPurposeList(session, aSelectedSurvey);
                     for (Purpose aPurpose : sortedSpeciesPurposeList) {
                        String query = String.format("from AcousticCategory a " +
                                    "where a.compId.nation=%1d " +
                                    "and   a.compId.platform=%1d " +
                                    "and   a.compId.acousticCategory=%1d ",
                              aPurpose.getCompId().getNation(),
                              aPurpose.getCompId().getPlatform(),
                              aPurpose.getCompId().getAcousticCategory());

                        try (ScrollableResults acousticCategoryResults = session.createQuery(query)
                              .setReadOnly(true)
                              .scroll(ScrollMode.FORWARD_ONLY)) {
                           if (acousticCategoryResults.next()) {
                              AcousticCategory acousticCategory = (AcousticCategory) acousticCategoryResults.get(0);
                              aPrintData.setAcousticCategoryPrint(acousticCategory);
                              aPrintDataBottom.setAcousticCategoryPrint(acousticCategory);
                           }
                        }
                     } // for
                  } //if
               } // if
            } //if (scat_exist)

            int lastSchoolObject = -1;

            // Initialize sData and clear data of print-structure
            if (!sDataExist) {
               return;  //No need to continue if first fetch does not contain any scatterData
            }
            ScatterData sData = (ScatterData) scatterDataResults.get(0);

            // Clear data
            aPrintData.clearData(ReportMode.NATIVE);
            aPrintDataBottom.clearData(ReportMode.NATIVE);
            aPrintData.clearData(ReportMode.ACCUMULATE);
            aPrintDataBottom.clearData(ReportMode.ACCUMULATE);

            mainLoop:
            while (scatExist) {   //Enter if data exist
               // Values read from database (really from hibernate object)
               Scatter scat = (Scatter) scatterResults.get(0);        // Read what scatterResults.next() "points" to

               // Get Scatter of desired type
               while (scatterTypePelagic != scat.getCompId().getScatterType() &&
                     scatterTypeBottom != scat.getCompId().getScatterType()) {
                  scatExist = scatterResults.next();
                  if (!scatExist) break;
                  scat = (Scatter) scatterResults.get(0);
               }

               // Frequency, transceiver, date and time from table SCATTER
               int scatFr = scat.getCompId().getFrequency();
               short scatTr = scat.getCompId().getTransceiver();
               int scatDa = scat.getCompId().getObservationDate();
               int scatTi = scat.getCompId().getObservationTime();

               // Ruling data: Frequency, transceiver, date and time from table SCATTER. Scatter rules!
               currentFr = scatFr;
               currentTr = scatTr;
               currentDa = scatDa;
               currentTi = scatTi;

               // Frequency, transceiver, date and time from table SCATTERDATA
               int sDataFr = sData.getCompId().getFrequency();
               short sDataTr = sData.getCompId().getTransceiver();
               int sDataDa = sData.getCompId().getObservationDate();
               int sDataTi = sData.getCompId().getObservationTime();

               if (sDataExist) {
                  sData = (ScatterData) scatterDataResults.get(0);    // Read what scatterDataResults.next() "points" to

                  // Only desired scatter type: works since SCHOOL_PELAGIC > SCATTER_PELAGIC
                  while (scatterTypePelagic != sData.getCompId().getScatterType() &&
                        scatterTypeBottom != sData.getCompId().getScatterType()) {
                     sDataExist = scatterDataResults.next();
                     if (!sDataExist) break;
                     sData = (ScatterData) scatterDataResults.get(0);
                  }

                  // Frequency, transceiver, date and time from table ScatterData.
                  sDataFr = sData.getCompId().getFrequency();
                  sDataTr = sData.getCompId().getTransceiver();
                  sDataDa = sData.getCompId().getObservationDate();
                  sDataTi = sData.getCompId().getObservationTime();

                  // Synchronize: get new scatterData if necessary.
                  while (sDataFr < currentFr ||
                        (sDataFr == currentFr && sDataTr < currentTr) ||
                        (sDataFr == currentFr && sDataTr == currentTr && sDataDa < currentDa) ||
                        (sDataFr == currentFr && sDataTr == currentTr && sDataDa == currentDa && sDataTi < currentTi)) {
                     sDataExist = scatterDataResults.next();
                     if (!sDataExist) break;
                     sData = (ScatterData) scatterDataResults.get(0);
                     sDataFr = sData.getCompId().getFrequency();
                     sDataTr = sData.getCompId().getTransceiver();
                     sDataDa = sData.getCompId().getObservationDate();
                     sDataTi = sData.getCompId().getObservationTime();
                  }
               }  //if - scatterData

               // If scatterData exists, it is now synchronized to current_ (frequency, transceiver, date, time),
               // but it may not be of the type requested for printing
               while ((sDataFr == currentFr &&
                     sDataTr == currentTr &&
                     sDataDa == currentDa &&
                     sDataTi == currentTi) &&
                     (scatterTypePelagic != sData.getCompId().getScatterType() &&
                           scatterTypeBottom != sData.getCompId().getScatterType())) {
                  sDataExist = scatterDataResults.next();
                  if (!sDataExist) break;
                  sData = (ScatterData) scatterDataResults.get(0);
                  sDataFr = sData.getCompId().getFrequency();
                  sDataTr = sData.getCompId().getTransceiver();
                  sDataDa = sData.getCompId().getObservationDate();
                  sDataTi = sData.getCompId().getObservationTime();
               }  //while - scatterData

               // Synchronize: set observation if necessary
               while (gObservation.getCompId().getObservationDate() < currentDa ||
                     (gObservation.getCompId().getObservationDate() == currentDa &&
                           gObservation.getCompId().getObservationTime() < currentTi)) {
                  if (!obsRead) {
                     fObs.rewind();
                  }
                  obsRead = GetObservation.nextObservation(fObs.getReader(), gObservation);
               } //while - observation

               // Synchronize: set comment if necessary (and exist). NB! time usually different from current_time
               while (commentRead &&
                     (mObservationComment.getCompId().getObservationDate() < currentDa ||
                           (mObservationComment.getCompId().getObservationDate() == currentDa &&
                                 mObservationComment.getCompId().getObservationTime() < currentTi))) {
                  commentRead = GetComment.nextComment(fComment.getReader(), mObservationComment);
               } // while - comment

               // ============================ HERE: DATA SYNCHRONISING FINISHED =======================================

               // Here: observation, scatter and scatterData should be synchronized to "current_da" and "current_ti"
               // FILL: OBSERVATION
               aPrintData.setObservation(gObservation);
               aPrintDataBottom.setObservation(gObservation);

               // FILL: SCATTER
               aPrintData.setScatter(scat);
               if (scat.getBottomActive() == 1) {
                  aPrintDataBottom.setScatter(scat);  //Will fill only if type is correct
               }

               // Bottom_scatter_type is not fetched yet
               if (scat.getBottomActive() == 1) {  //According to DB: bottom data exist
                  if ((scat.getCompId().getScatterType() == ScatterTypeEnum.PELAGIC.getValue() &&
                        ScatterTypeEnum.BOTTOM.getValue() > ScatterTypeEnum.PELAGIC.getValue()) ||
                        (scat.getCompId().getScatterType() > ScatterTypeEnum.PELAGIC_SCHOOL.getValue() &&
                              ScatterTypeEnum.BOTTOM_SCHOOL.getValue() > ScatterTypeEnum.PELAGIC_SCHOOL.getValue())) {
                     scatExist = scatterResults.next();
                     if (scatExist) {
                        scat = (Scatter) scatterResults.get(0);
                        if (scatFr == scat.getCompId().getFrequency() &&
                              scatTr == scat.getCompId().getTransceiver() &&
                              scatDa == scat.getCompId().getObservationDate() &&
                              scatTi == scat.getCompId().getObservationTime()) {
                           // FILL: SCATTER (bottom)
                           aPrintData.setScatter(scat);
                           if (scat.getBottomActive() == 1) {
                              aPrintDataBottom.setScatter(scat);
                           }
                        }
                     }
                  } //if
               } //if

               // Get stop time of interval
               DatabaseTime currentStop = new DatabaseTime(DatabaseTime.toMillis(currentDa, currentTi)
                     + aPrintData.getScatter(ReportMode.NATIVE).getDuration() * 10L);
               int currentStopTime = currentStop.getTime();
               int currentStopDate = currentStop.getDate();

               // Set stop of observation
               if (obsReadStop) {
                  while (obsReadStop &&
                        (stopObservation.getCompId().getObservationDate() < currentStopDate ||
                              (stopObservation.getCompId().getObservationDate() == currentStopDate &&
                                    stopObservation.getCompId().getObservationTime() < currentStopTime))) {
                     obsReadStop = GetObservation.nextObservation(fObsStop, stopObservation);
                  }
                  if (obsReadStop &&
                        stopObservation.getCompId().getObservationDate() == currentStopDate &&
                        stopObservation.getCompId().getObservationTime() == currentStopTime) {
                     aPrintData.setObservationStop(stopObservation, ReportMode.NATIVE);
                     aPrintDataBottom.setObservationStop(stopObservation, ReportMode.NATIVE);
                  } else {
                     aPrintData.clearObservationStop(ReportMode.NATIVE);
                     aPrintDataBottom.clearObservationStop(ReportMode.NATIVE);
                  }
               }

               // Comment handling
               while (commentRead &&
                     (mObservationComment.getCompId().getObservationDate() < currentStopDate ||
                           (mObservationComment.getCompId().getObservationDate() == currentStopDate &&
                                 mObservationComment.getCompId().getObservationTime() < currentStopTime))) {
                  if (!commentRead) {
                     fComment.rewind();
                  }
                  // FILL: COMMENT
                  aPrintData.setCommentNative(mObservationComment);
                  mObservationComment = new ObservationComment();

                  if (aPrintData.getCommentCount(ReportMode.NATIVE) > 0) {
                     commentRead = GetComment.nextComment(fComment.getReader(), mObservationComment);
                  }
               } //while - comment

               // FILL: SCATTERDATA
               // ScatterData will in general have many (or none) entries for the same frequency, transceiver, date and
               // time: i.e. get all those and fill into aPrintData and aPrintDataBottom. Current (... date, time) is
               // already inserted into print-structure, so testing against that assures that the data in aPrintData
               // has the same frequency, transceiver, date, time (which is also the case for aPrintDataBottom)
               while (sDataFr == aPrintData.getScatter(ReportMode.NATIVE).getCompId().getFrequency() &&
                     sDataTr == aPrintData.getScatter(ReportMode.NATIVE).getCompId().getTransceiver() &&
                     sDataDa == aPrintData.getScatter(ReportMode.NATIVE).getCompId().getObservationDate() &&
                     sDataTi == aPrintData.getScatter(ReportMode.NATIVE).getCompId().getObservationTime()) {
                  if (aAsyncHandle.isCancelled()) {
                     break mainLoop;
                  }

                  // FILL: SCATTER_DATA. NB! Only filled for type scat.getCompId().getScatterType()
                  aPrintData.setSa(sData);       //Allocate pelagic data of type SCATTER_PELAGIC
                  aPrintDataBottom.setSa(sData); //Allocate bottom data of type SCATTER_BOTTOM

                  sDataExist = scatterDataResults.next();
                  if (!sDataExist) break;
                  sData = (ScatterData) scatterDataResults.get(0);

                  // Frequency, transceiver, date and time from table ScatterData.
                  sDataFr = sData.getCompId().getFrequency();
                  sDataTr = sData.getCompId().getTransceiver();
                  sDataDa = sData.getCompId().getObservationDate();
                  sDataTi = sData.getCompId().getObservationTime();

                  // Test for school
                  while (sData.getCompId().getScatterType() == ScatterTypeEnum.PELAGIC_SCHOOL.getValue() &&
                        (sDataDa < currentStopDate ||
                              (sDataDa == currentStopDate &&
                                    sDataTi < currentStopTime))) {
                     if (sDataFr != currentFr) break;
                     if (sDataTr != currentTr) break;

                     // Need to do this here since "school-data" is treated here, and sData is read again later in while-loop
                     if (printFrequency != sDataFr || printTransceiver != sDataTr) {
                        noRegSchoolObject = 0;
                     }

                     if (sData.getCompId().getObject() != lastSchoolObject &&  //No need to check if recently checked
                           !ReportEngine.schoolObjectRegistered(sData.getCompId().getObject(), regSchoolObject, noRegSchoolObject)) {
                        aPrintData.addSchoolCountNative();
                        lastSchoolObject = sData.getCompId().getObject();
                        regSchoolObject[noRegSchoolObject] = sData.getCompId().getObject();
                        noRegSchoolObject++;

                        // Free space in array:
                        if (noRegSchoolObject >= ReportEngine.MAX_REMEMBERED_SCHOOLS - 1) {
                           System.arraycopy(regSchoolObject, 1, regSchoolObject, 0, noRegSchoolObject - 1);
                           noRegSchoolObject--;
                        }
                     }

                     // Continue to read ScatterData until new date,time or until no longer of ScatterType=SCHOOL
                     sDataExist = scatterDataResults.next();
                     if (!sDataExist) break;
                     sData = (ScatterData) scatterDataResults.get(0);

                     // Frequency, transceiver, date and time from table ScatterData.
                     sDataFr = sData.getCompId().getFrequency();
                     sDataTr = sData.getCompId().getTransceiver();
                     sDataDa = sData.getCompId().getObservationDate();
                     sDataTi = sData.getCompId().getObservationTime();

                     if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC_SCHOOL.getValue()) break;
                  } //while - Test for school

                  // Break loop when all ScatterData for current frequency, transceiver, date and time are read
                  if (!sDataExist) break;
                  if (sDataFr > currentFr) break;
                  if (sDataFr == currentFr && sDataTr > currentTr) break;
                  if (sDataDa > currentDa) break;
                  if (sDataDa == currentDa && sDataTi > currentTi) break;

                  {
                     float d = aPrintData.getObservation(ReportMode.NATIVE).getDistance();
                     float delta = getObservation.getStopObservationDistance() - getObservation.getStartObservationDistance();
                     float frequencyProgress = (d - getObservation.getStartObservationDistance()) / delta;
                     float totalProgress = (actualNoFrequencies - 1 + frequencyProgress) / reportEngine.getExpectedFrequencyCount();
                     aProgressView.getSecondaryProgressHandler().setProgress(totalProgress);
                  }
               } //while

               // =========== HERE: ALL DATA FILLED INTO PrintData. We are ready to print at NATIVE resolution ============

               // At this point, aPrintData should be filled with all data at "current_da" and "current_ti"
               if (aMode == ReportMode.ACCUMULATE) { //1
                  if (doInitializeAccumulate) { //1
                     aPrintData.initializeDataAccumulate(reportEngine.getAccumulateDistance());
                     aPrintDataBottom.initializeDataAccumulate(reportEngine.getAccumulateDistance());
                     doInitializeAccumulate = false;
                  }
                  aPrintData.addToAccumulate();
                  aPrintDataBottom.addToAccumulate();
               }

               boolean firstTime = printFrequency < 0;

               // If necessary: open first print files
               if (firstTime) {
                  for (BaseSingleFrequencyReport report : selectedReports) {
                     printFrequency = aPrintData.getScatter(ReportMode.NATIVE).getCompId().getFrequency();
                     printTransceiver = aPrintData.getScatter(ReportMode.NATIVE).getCompId().getTransceiver();
                     report.open(aPrintData, aDirectory, aMode,
                           printFrequency,
                           printTransceiver,
                           getObservation.getStartObservationDistance(),
                           getObservation.getFinalObservationDistance(lastDistanceInterval));
                  }
                  aProgressView.setSecondaryText(progressText + Utils.hzToKHz(printFrequency) + " kHz");
               } else { // New frequency: necessary to change output files
                  if (printFrequency != aPrintData.getScatter(ReportMode.NATIVE).getCompId().getFrequency() ||
                        printTransceiver != aPrintData.getScatter(ReportMode.NATIVE).getCompId().getTransceiver()) {
                     printFrequency = aPrintData.getScatter(ReportMode.NATIVE).getCompId().getFrequency();
                     printTransceiver = aPrintData.getScatter(ReportMode.NATIVE).getCompId().getTransceiver();
                     actualNoFrequencies++;
                     aFeedback.setActualNoFrequencies(actualNoFrequencies);
                     printCompact.resetRow();
                     noRegSchoolObject = 0;    //Reset
                     if (actualNoFrequencies > reportEngine.getExpectedFrequencyCount()) {
                        reportEngine.setExpectedFrequencyCount(actualNoFrequencies);
                     }
                     aProgressView.setSecondaryText(progressText + Utils.hzToKHz(printFrequency) + " kHz");

                     // Reset observation file (and start reading from the beginning)
                     fObs.rewind();
                     obsRead = GetObservation.nextObservation(fObs.getReader(), gObservation);

                     // Reset comment file (and start reading from the beginning)
                     fComment.rewind();
                     commentRead = GetComment.nextComment(fComment.getReader(), mObservationComment);

                     // Get stop time of interval
                     currentStop = new DatabaseTime(DatabaseTime.toMillis(currentDa, currentTi)
                           + aPrintData.getScatter(ReportMode.NATIVE).getDuration() * 10L);
                     currentStopTime = currentStop.getTime();
                     currentStopDate = currentStop.getDate();

                     while (commentRead &&
                           (mObservationComment.getCompId().getObservationDate() < currentStopDate ||
                                 (mObservationComment.getCompId().getObservationDate() == currentStopDate &&
                                       mObservationComment.getCompId().getObservationTime() < currentStopTime))) {
                        if (!commentRead) {
                           fComment.rewind();
                        }
                        // FILL: COMMENT
                        aPrintData.setCommentNative(mObservationComment);
                        aPrintDataBottom.setCommentNative(mObservationComment);
                        mObservationComment = new ObservationComment();

                        if (aPrintData.getCommentCount(ReportMode.NATIVE) > 0) {
                           commentRead = GetComment.nextComment(fComment.getReader(), mObservationComment);
                        }
                     } //while - comment

                     // Fill accumulate data
                     if (aMode == ReportMode.ACCUMULATE) { //2
                        aPrintData.initializeDataAccumulate(reportEngine.getAccumulateDistance());
                        aPrintDataBottom.initializeDataAccumulate(reportEngine.getAccumulateDistance());
                        doInitializeAccumulate = false;
                        aPrintData.addToAccumulate();
                        aPrintDataBottom.addToAccumulate();
                     }

                     for (BaseSingleFrequencyReport report : selectedReports) {
                        report.open(aPrintData, aDirectory, aMode,
                              printFrequency,
                              printTransceiver,
                              getObservation.getStartObservationDistance(),
                              getObservation.getStopObservationDistance());
                     }
                  }
               } //if

               if (!scatExist) {
                  break;
               }

               //if ( wAccumulateCheck.isSelected() && !aPrintData.accumulateDistanceReached() ) {
               if (aMode == ReportMode.ACCUMULATE) {
                  if (aPrintData.accumulateDistanceReached()) { // Make ready to print
                     aPrintData.finalizeAccumulate();
                     aPrintDataBottom.finalizeAccumulate();
                     doInitializeAccumulate = true;
                  }
               }

               if (aMode == ReportMode.NATIVE ||
                   aMode == ReportMode.ACCUMULATE && aPrintData.accumulateDistanceReached() && aPrintData.accumulateDistanceIntervalFull()) {
                  for (BaseSingleFrequencyReport report : selectedReports) {
                     report.print(aPrintData, printFrequency, printTransceiver, aMode, aPrintDataBottom);
                  }
               } //If_native_resolution or accumulate_ready_to_print

               scatExist = scatterResults.next();

               // Clear data at native resolution and then read data from ScatterData. Note that "current_..." =  "sData"
               aPrintData.clearData(ReportMode.NATIVE);
               aPrintDataBottom.clearData(ReportMode.NATIVE);
            }  //while_main

            for (BaseSingleFrequencyReport report : selectedReports) {
               report.finaliseReport(aPrintData);
            }
         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Error", e);
         } finally {
            for (BaseSingleFrequencyReport report : selectedReports) {
               report.close();
            }
         }
         aProgressView.incrementMainProgress("");
      });
   }
}
