package no.imr.lsss.database.reports;

import no.imr.lsss.database.ices.IcesAcousticMetadata;
import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.PurposePK;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterData;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.framework.config.survey.misc.ices.IcesUtils;
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
 * DESCRIPTION: sort data on (survey), date, time. One file for all data.
 */
final class ReportGroupTime extends ReportGroup {
   private final List<BaseMultiFrequencyReport> reports;

   private Observation gObservation = new Observation(new ObservationPK());
   private Observation stopObservation = new Observation(new ObservationPK());

   ReportGroupTime(ReportEngine reportEngine, ReportEngine.Feedback feedback) {
      super(reportEngine);

      reports = List.of(
            new PrintUser20(reportEngine), // Multi species report
            new PrintUser21(reportEngine),
            new PrintUser22(reportEngine),
            new PrintUser23(reportEngine),
            new PrintUser24(reportEngine),
            new PrintUser25(reportEngine, feedback),
            new PrintUser26(reportEngine, feedback)
      );
   }

   @Override
   void printReports(Survey aSelectedSurvey, Path aDirectory, ProgressView aProgressView, ReportEngine.Feedback aFeedback, AsyncHandle aAsyncHandle) {
      ReportMode aMode = ReportMode.NATIVE; // ReportMode.NATIVE only
      reportEngine.getLSSS().getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {
         int currentFr;        // Synchronizing frequency (ruling data: scatterData rules)
         short currentTr;      // Synchronizing transceiver
         int currentDa;        // Synchronizing date
         int currentTi;        // Synchronizing time
         int currentOb;        // Synchronizing object number (e.g. "echogram number" and "school number")
         boolean skipNextScat = false;
         float startObservationDistance = 999888777;
         float stopObservationDistance = 0;
         float lastDistanceInterval;
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

         List<Integer> freqList = reportEngine.getAllFrequencies(session, aSelectedSurvey);   //List of frequencies available
         aPrintData.setFrequencyList(freqList);
         aPrintDataBottom.setFrequencyList(freqList);

         List<BaseMultiFrequencyReport> selectedReports = getSelectedReports(reports);
         if (selectedReports.isEmpty()) {
            return;
         }

         IcesAcousticMetadata icesAcousticMetadata = IcesUtils.acousticMetadataFromDatabase(reportEngine.getLSSS().getDatabaseManager().getDatabaseConnection(), aSelectedSurvey);
         aPrintData.setIcesAcousticMetadata(icesAcousticMetadata);
         aPrintDataBottom.setIcesAcousticMetadata(icesAcousticMetadata);

         String platformIocCode = GetIocCode.getIocCode(session, aSelectedSurvey);
         aPrintData.setPlatformIocCode(platformIocCode);
         aPrintDataBottom.setPlatformIocCode(platformIocCode);

         float distanceInterval = GetOneScatter.getDistanceInterval(session, aSelectedSurvey);
         aPrintData.setOneDistanceInterval(distanceInterval);
         aPrintDataBottom.setOneDistanceInterval(distanceInterval);

         aProgressView.setSecondaryText("Increasing time - all frequencies (reports 20, 21, ...)");

         aPrintData.clearData(ReportMode.NATIVE);

         aPrintDataBottom.clearData(ReportMode.NATIVE);

         //Queries
         String criterionGeneral =
               "a.compId.nation   = " + aSelectedSurvey.getCompId().getNation() +
                     " and   a.compId.platform = " + aSelectedSurvey.getCompId().getPlatform() +
                     " and   a.compId.survey   = " + aSelectedSurvey.getCompId().getSurvey() +
                     " and ( a.compId.observationDate > " + reportEngine.getStartDate() +
                     "  or  (a.compId.observationDate = " + reportEngine.getStartDate() + " and a.compId.observationTime >= " + reportEngine.getStartTime() + ") )" +
                     " and ( a.compId.observationDate < " + reportEngine.getStopDate() +
                     "  or  (a.compId.observationDate = " + reportEngine.getStopDate() + " and a.compId.observationTime <= " + reportEngine.getStopTime() + ") )";

         String queryObservation = "from Observation a where " + criterionGeneral +
               " order by a.compId.observationDate, a.compId.observationTime";

         String queryScatter = "from Scatter a where " + criterionGeneral +
               " and a.compId.scatterType in (" + scatterTypePelagic + "," + scatterTypeBottom + ")" +
               " order by a.compId.observationDate, a.compId.observationTime, " +
               "          a.compId.object, " +
               "          a.compId.frequency,       a.compId.transceiver, " +
               "          a.compId.scatterType";

         String queryScatterData = "from ScatterData a where " + criterionGeneral +
               " and a.compId.scatterType in (" + scatterTypePelagic + "," + scatterTypeBottom + ")" +
               " order by a.compId.observationDate, a.compId.observationTime, " +
               "          a.compId.object, " +
               "          a.compId.frequency,       a.compId.transceiver, " +
               "          a.compId.scatterType, " +
               "          a.compId.channelNumber,   a.compId.acousticCategory";


         // File to write observations since scroll back is not allowed in hibernate yet (2007)
         Path observationFile = aDirectory.resolve(ReportGenerator.SCRATCH_FILE_PREFIX + "time_xml_acoustic_observations");
         Path observationFileAll = aDirectory.resolve(ReportGenerator.SCRATCH_FILE_PREFIX + "time_xml_acoustic_observations_all");
         GetObservation getObservation = new GetObservation();
         getObservation.generateObservationFile(session.createSelectionQuery(queryObservation, Observation.class), observationFile, observationFileAll, reportEngine.getCharset());
         lastDistanceInterval = GetLastDistanceInterval.getLastDistanceInterval(session.createSelectionQuery(queryScatter, Scatter.class));

         try (BufferedReader fObsStop = Files.newBufferedReader(observationFileAll, reportEngine.getCharset());
              RewindableBufferedReader fObs = new RewindableBufferedReader(observationFile, reportEngine.getCharset());
              ScrollableResults<Scatter> scatterResults = session.createSelectionQuery(queryScatter, Scatter.class)
                    .setReadOnly(true)
                    .scroll(ScrollMode.FORWARD_ONLY);
              ScrollableResults<ScatterData> scatterDataResults = session.createSelectionQuery(queryScatterData, ScatterData.class)
                    .setReadOnly(true)
                    .scroll(ScrollMode.FORWARD_ONLY)) {

            // Make ready for first fetch
            boolean scatExist = scatterResults.next();
            boolean sDataExist = scatterDataResults.next();
            boolean obsRead = GetObservation.nextObservation(fObs.getReader(), gObservation);
            boolean obsReadStop = GetObservation.nextObservation(fObsStop, stopObservation);

            // Open print file (one for all frequencies)
            for (BaseMultiFrequencyReport report : selectedReports) {
               startObservationDistance = getObservation.getStartObservationDistance();
               stopObservationDistance = getObservation.getFinalObservationDistance(lastDistanceInterval);
               report.open(aPrintData, aDirectory, aMode, startObservationDistance, stopObservationDistance);
            }

            // No need to continue if there is not Scatter and ScatterData even for first fetch
            if (!scatExist || !sDataExist) {
               for (BaseMultiFrequencyReport report : selectedReports) {
                  report.finaliseReport(aPrintData);
               }
               return;
            }

            // Frequency and transceiver of open files
            Scatter scat = scatterResults.get();

            aPrintData.setScatter(scat);
            if (scat.getBottomActive() == 1) {
               aPrintDataBottom.setScatter(scatterResults.get());
            }

            if (reportEngine.getPrintScrutinizedSpCheck()) {
               List<Integer> categoryCodeList = reportEngine.getScrutinizedSpeciesList(session, aSelectedSurvey);
               for (Integer categoryCode : categoryCodeList) {
                  AcousticCategory acousticCategory = session.get(AcousticCategory.class, new AcousticCategoryPK(
                        aSelectedSurvey.getCompId().getNation(),
                        aSelectedSurvey.getCompId().getPlatform(),
                        categoryCode));

                  Purpose purpose = session.get(Purpose.class, new PurposePK(
                        aSelectedSurvey.getCompId().getNation(),
                        aSelectedSurvey.getCompId().getPlatform(),
                        aSelectedSurvey.getCompId().getSurvey(),
                        categoryCode));

                  if (acousticCategory != null) {
                     aPrintData.setAcousticCategoryPrint(acousticCategory);
                     aPrintDataBottom.setAcousticCategoryPrint(acousticCategory);
                     if (purpose != null) {
                        aPrintData.addPurposePrint(purpose);
                        if (aPrintData.getScatter(aMode).getBottomActive() == 1) {
                           aPrintDataBottom.addPurposePrint(purpose);
                        }
                     } else { // purpose_exist == false
                        aPrintData.addUnknownPurposePrint(aSelectedSurvey, acousticCategory);
                        if (aPrintData.getScatter(aMode).getBottomActive() == 1) {
                           aPrintDataBottom.addUnknownPurposePrint(aSelectedSurvey, acousticCategory);
                        }
                     }
                  }
               }
            } else { // !printScrutinizedSpCheck.isSelected()
               List<Purpose> sortedSpeciesPurposeList = ReportEngine.getSortedSpeciesPurposeList(session, aSelectedSurvey);
               for (Purpose aPurpose : sortedSpeciesPurposeList) {
                  AcousticCategory acousticCategory = session.get(AcousticCategory.class, new AcousticCategoryPK(
                        aPurpose.getCompId().getNation(),
                        aPurpose.getCompId().getPlatform(),
                        aPurpose.getCompId().getAcousticCategory()));

                  if (acousticCategory != null) {
                     aPrintData.setAcousticCategoryPrint(acousticCategory);
                     aPrintDataBottom.setAcousticCategoryPrint(acousticCategory);
                     aPrintData.addPurposePrint(aPurpose);
                     aPrintDataBottom.addPurposePrint(aPurpose);
                  }
               } // for
            } //if

            // If reached, ScatterData exists.
            ScatterData sData = scatterDataResults.get();

            //Print metadata
            for (BaseMultiFrequencyReport report : selectedReports) {
               report.printMetadata(aPrintData);
            }

            //remove: while (scat_exist && sData_exist)
            while (scatExist) {   //Enter if data exist
               // Values read from database (really from hibernate object)
               scat = scatterResults.get();        // Read what scatterResults.next() "points" to
               if (sDataExist)  //RK
                  sData = scatterDataResults.get();    // Read what scatterDataResults.next() "points" to

               if (aAsyncHandle.isCancelled()) {
                  break;
               }

               // Ruling data: Date, time, frequency and transceiver from table SCATTER. Scatter rules!
               currentFr = scat.getCompId().getFrequency();
               currentTr = scat.getCompId().getTransceiver();
               currentDa = scat.getCompId().getObservationDate();
               currentTi = scat.getCompId().getObservationTime();
               currentOb = scat.getCompId().getObject();   //RK 2014.07.15

               // Date, time, frequency and transceiver from table SCATTERDATA
               int sDataFr = sData.getCompId().getFrequency();
               int sDataTr = sData.getCompId().getTransceiver();
               int sDataDa = sData.getCompId().getObservationDate();
               int sDataTi = sData.getCompId().getObservationTime();
               int sDataOb = sData.getCompId().getObject();  //RK 2014.07.15

               // Synchronize: get new scatterData if necessary.
               while ((sDataDa < currentDa ||
                     (sDataDa == currentDa && sDataTi < currentTi)) ||
                     (sDataDa == currentDa && sDataTi == currentTi && sDataOb < currentOb) ||
                     (sDataDa == currentDa && sDataTi == currentTi && sDataOb == currentOb && sDataFr < currentFr) ||
                     (sDataDa == currentDa && sDataTi == currentTi && sDataOb == currentOb && sDataFr == currentFr && sDataTr < currentTr)) { //RK 2014.07.15
                  sDataExist = scatterDataResults.next();
                  if (!sDataExist) break;
                  sData = scatterDataResults.get();   //RK 2014.07.15  fix:  "scat = (Scatter) scatter.get(0);"
                  sDataFr = sData.getCompId().getFrequency();
                  sDataTr = sData.getCompId().getTransceiver();
                  sDataDa = sData.getCompId().getObservationDate();
                  sDataTi = sData.getCompId().getObservationTime();
                  sDataOb = sData.getCompId().getObject();    //RK 2014.07.15
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

               // ============================ HERE: DATA SYNCHRONISING FINISHED =======================================

               // Here: observation, scatter and scatterData should be synchronized to "current_date" and "current_time"
               // ALERT: ScatterData will have read ahead of current_time if there is no data scrutinized (i.e. sa=0) for current_time
               // FILL: OBSERVATION
               aPrintData.setObservation(gObservation);
               aPrintDataBottom.setObservation(gObservation);

               // FILL: SCATTER - both SCATTER_PELAGIC and SCATTER_BOTTOM types
               aPrintData.setScatter(scat);        //SCATTER_PELAGIC
               if (scat.getBottomActive() == 1) {  //Bottom data exist, and DB sort order give next entry: SCATTER_BOTTOM
                  if (skipNextScat) {
                     skipNextScat = false;
                  } else {
                     scatExist = scatterResults.next();
                  }
                  if (!scatExist) break;         //Should never happen
                  scat = scatterResults.get();

                  // BottomActive=1, so this "scat = (Scatter) scatter.get(0)" could be a bottom channel.
                  // For schools, however, the school may not extend to the bottom region (usually 10 m above bottom)
                  // even if the bottom window is active. Therefore: signal that next "scatter.next" should be
                  // skipped, and use the one that is already read.
                  if (scat.getCompId().getScatterType() != ScatterTypeEnum.BOTTOM.getValue() &&
                        scat.getCompId().getScatterType() != ScatterTypeEnum.BOTTOM_SCHOOL.getValue()) {
                     skipNextScat = true;
                  }

                  if (currentFr == scat.getCompId().getFrequency() &&
                        currentTr == scat.getCompId().getTransceiver() &&
                        currentDa == scat.getCompId().getObservationDate() &&
                        currentTi == scat.getCompId().getObservationTime() &&
                        currentOb == scat.getCompId().getObject()) { // RK 2014.07.15
                     aPrintDataBottom.setScatter(scat); //SCATTER_BOTTOM
                  }
               }

               // Fill aPrintData when date and time of sData is same as in aPrintData
               // I.e. get all scatterData for given frequency, transceiver, date and time
               while (currentDa == aPrintData.getScatter(aMode).getCompId().getObservationDate() &&
                     currentTi == aPrintData.getScatter(aMode).getCompId().getObservationTime() &&
                     currentOb == aPrintData.getScatter(aMode).getCompId().getObject()) { //RK 2014.07.15
                  // FILL: SCATTER_DATA. NB! Only filled for type scat.getCompId().getScatterType()
                  if (currentDa == sData.getCompId().getObservationDate() &&
                        currentTi == sData.getCompId().getObservationTime() &&
                        currentOb == aPrintData.getScatter(aMode).getCompId().getObject()) { // RK 2014.07.15
                     aPrintData.setSa(sData);       //Allocate pelagic data of type SCATTER_PELAGIC
                     aPrintDataBottom.setSa(sData); //Allocate bottom data of type SCATTER_BOTTOM
                     sDataExist = scatterDataResults.next();
                  }

                  // Get stop time of interval
                  DatabaseTime currentStop = new DatabaseTime(DatabaseTime.toInstant(currentDa, currentTi)
                        .plusMillis(aPrintData.getScatter(ReportMode.NATIVE).getDuration() * 10L));
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

                        //Cheating: last read position, hopefully only slightly before stopObservation
                        if (gObservation.getCompId().getObservationDate() == currentStopDate &&
                              Math.abs(gObservation.getCompId().getObservationTime() - currentStopTime) < 200) { // closer than 2 seconds: hhmmssxx => ss=2 xx=0
                           aPrintData.setObservationStop(gObservation, ReportMode.NATIVE);
                           aPrintDataBottom.setObservationStop(gObservation, ReportMode.NATIVE);
                        }
                     }
                  }

                  // Get stop time of interval
                  if (!sDataExist) {  //No new data, but maybe the last already read data should be printed
                     for (BaseMultiFrequencyReport report : selectedReports) {
                        report.print(aPrintData, aMode, aPrintDataBottom);
                     }
                     aPrintData.clearData(ReportMode.NATIVE);
                     aPrintDataBottom.clearData(ReportMode.NATIVE);

                     if (aMode == ReportMode.ACCUMULATE) {
                        aPrintData.initializeDataAccumulate(reportEngine.getAccumulateDistance());
                        aPrintDataBottom.initializeDataAccumulate(reportEngine.getAccumulateDistance());
                     }
                     break;
                  }
                  sData = scatterDataResults.get();

                  // Print if new time or frequency
                  if (currentFr != sData.getCompId().getFrequency() ||
                        currentTr != sData.getCompId().getTransceiver() ||
                        currentDa != sData.getCompId().getObservationDate() ||
                        currentTi != sData.getCompId().getObservationTime() ||
                        currentOb != sData.getCompId().getObject()) { //RK 2014.07.15
                     for (BaseMultiFrequencyReport report : selectedReports) {
                        report.print(aPrintData, aMode, aPrintDataBottom);   // Trig by new frequency, transceiver, date or time
                     }
                     aPrintData.clearData(ReportMode.NATIVE);
                     aPrintDataBottom.clearData(ReportMode.NATIVE);

                     if (aMode == ReportMode.ACCUMULATE) { //New frequency: drop rest of accumulated data and re-initialize
                        aPrintData.initializeDataAccumulate(reportEngine.getAccumulateDistance()); // mAccumulateDistance);
                        aPrintDataBottom.initializeDataAccumulate(reportEngine.getAccumulateDistance()); //mAccumulateDistance);
                     }
                     break;
                  }

                  {
                     float d = aPrintData.getObservation(aMode).getDistance();
                     float delta = stopObservationDistance - startObservationDistance;
                     aProgressView.getSecondaryProgressHandler().setProgress((d - startObservationDistance) / delta);
                     //Warning: If a session.flush(); is placed here, the printout stops to work (or becomes very, very slow)
                  }
               } //while

               // ==================== HERE: ALL DATA FILLED INTO PrintData. We are ready to print =====================

               if (!scatExist) {
                  break;
               }

               //Move cursor to get next scatter:
               if (skipNextScat) {
                  skipNextScat = false;
               } else {
                  scatExist = scatterResults.next();
               }
            }  //while_main

            for (BaseMultiFrequencyReport report : selectedReports) {
               report.finaliseReport(aPrintData);
            }
         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Error", e);
         } finally {
            for (BaseMultiFrequencyReport report : selectedReports) {
               report.close();
            }
         }
         aProgressView.incrementMainProgress("");
      });
   }
}
