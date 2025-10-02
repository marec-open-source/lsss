package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterData;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ProgressView;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * DESCRIPTION: Generate reports by reading data from one frequency at a time at increasing object and time.
 * There are two types of objects: school-objects and scatter-objects. The center of school-objects are
 * marked by a "x". The scatter-objects are the echograms visualised on the screen (i.e., they are not
 * connected to the data-files).
 */
final class ReportGroupObject extends ReportGroup {
   private final List<BaseSingleFrequencyReport> reports;

   private Observation gObservation = new Observation(new ObservationPK());
   private int current_fr;                 // Synchronizing frequency (ruling data: scatterData rules)
   private short current_tr;               // Synchronizing transceiver
   private int current_ob;                 // Synchronizing object number
   private int current_da;                 // Synchronizing date
   private int current_ti;                 // Synchronizing time

   ReportGroupObject(ReportEngine reportEngine) {
      super(reportEngine);

      reports = List.of(
            new PrintUser30(reportEngine), // Multi species report
            new PrintUser31(reportEngine), // Single species report
            new PrintUser32(reportEngine)  // Single species report
      );
   }

   @Override
   void printReports(Survey aSelectedSurvey, Path aDirectory, ProgressView aProgressView, ReportEngine.Feedback aFeedback, AsyncHandle aAsyncHandle) {
      ReportMode aMode = ReportMode.NATIVE; // ReportMode.NATIVE only
      reportEngine.getLSSS().getDatabaseManager().getDatabaseConnection().executeStatelessQuery(session -> {
         int printFrequency = -1;       // Frequency of currently open file
         short printTransceiver = -1;   // Transceiver of currently open file
         short observationType;
         short scatterTypePelagic;
         short scatterTypeBottom;
         if (reportEngine.isSchoolReport()) {
            observationType = ObservationTypeEnum.SCHOOL_OF_FISH_DATA.getValue();
            scatterTypePelagic = ScatterTypeEnum.PELAGIC_SCHOOL.getValue();
            scatterTypeBottom = ScatterTypeEnum.BOTTOM_SCHOOL.getValue();
         } else {
            observationType = ObservationTypeEnum.SCATTERED_FISH_DATA.getValue();
            scatterTypePelagic = ScatterTypeEnum.PELAGIC.getValue();
            scatterTypeBottom = ScatterTypeEnum.BOTTOM.getValue();
         }
         PrintData.Pelagic aPrintData = new PrintData.Pelagic(reportEngine, aFeedback, scatterTypePelagic, aSelectedSurvey);
         PrintData.Bottom aPrintDataBottom = new PrintData.Bottom(reportEngine, aFeedback, scatterTypeBottom, aSelectedSurvey);

         int actualNoFrequencies = 1; //Start count

         List<BaseSingleFrequencyReport> selectedReports = getSelectedReports(reports);
         if (selectedReports.isEmpty()) {
            return;
         }

         String progressText = "Increasing time (object reports: 30, 31, ...) - ";
         aProgressView.setSecondaryText(progressText + "Starting");

         aPrintData.clearData(ReportMode.NATIVE);

         aPrintDataBottom.clearData(ReportMode.NATIVE);

         //Queries
         String queryGeneral =
               " where a.compId.nation   = " + aSelectedSurvey.getCompId().getNation() +
                     " and   a.compId.platform = " + aSelectedSurvey.getCompId().getPlatform() +
                     " and   a.compId.survey   = " + aSelectedSurvey.getCompId().getSurvey() +
                     " and ( a.compId.observationDate > " + reportEngine.getStartDate() +
                     "  or  (a.compId.observationDate = " + reportEngine.getStartDate() + " and a.compId.observationTime >= " + reportEngine.getStartTime() + ") )" +
                     " and ( a.compId.observationDate < " + reportEngine.getStopDate() +
                     "  or  (a.compId.observationDate = " + reportEngine.getStopDate() + " and a.compId.observationTime <= " + reportEngine.getStopTime() + ") )";

         String queryObservation = "from Observation a , Scatter b" + queryGeneral +
               " and   a.compId.platform        = b.compId.platform " +
               " and   a.compId.survey          = b.compId.survey " +
               " and   a.compId.observationDate = b.compId.observationDate " +
               " and   a.compId.observationTime = b.compId.observationTime " +
               " and   a.compId.observationType = " + observationType +
               " and   (   b.compId.scatterType = " + scatterTypePelagic +
               "        or b.compId.scatterType = " + scatterTypeBottom + " ) " +
               " order by b.compId.object, " +
               "          a.compId.observationDate, " +
               "          a.compId.observationTime";

         String queryScatter = "from Scatter a " + queryGeneral +
               " and  (   a.compId.scatterType = " + scatterTypePelagic +
               "       or a.compId.scatterType = " + scatterTypeBottom + " ) " +
               " order by a.compId.frequency,       a.compId.transceiver, " +
               "          a.compId.object, " +
               "          a.compId.observationDate, a.compId.observationTime, " +
               "          a.compId.scatterType";

         String queryScatterData = "from ScatterData a " + queryGeneral +
               " and  (   a.compId.scatterType = " + scatterTypePelagic +
               "       or a.compId.scatterType = " + scatterTypeBottom + " ) " +
               " order by a.compId.frequency,       a.compId.transceiver, " +
               "          a.compId.object, " +
               "          a.compId.observationDate, a.compId.observationTime, " +
               "          a.compId.scatterType, " +
               "          a.compId.channelNumber,   a.compId.acousticCategory";

         String queryComment = "from ObservationComment a " + queryGeneral +
               " order by a.compId.observationDate, a.compId.observationTime, " +
               "          a.compId.observationType";   // "Scattered Fish Data"=3000 (=EchogramData) or "School Of Fish Data"=4000
         // BEI: SCATTER=3000                             SCHOOL=4000

         // File to write observations since scroll back is not allowed in hibernate yet (2007)
         Path observationFile = aDirectory.resolve(ReportGenerator.SCRATCH_FILE_PREFIX + "object_acoustic_observations");
         Path observationFileAll = aDirectory.resolve(ReportGenerator.SCRATCH_FILE_PREFIX + "object_acoustic_observations_all");
         GetObservation getObservation = new GetObservation();
         getObservation.generateObservationFile(session, queryObservation, observationFile, observationFileAll, reportEngine.getCharset());

         float lastDistanceInterval = GetLastDistanceInterval.getLastDistanceInterval(session, queryScatter);
         String callsign = GetCallSign.getCallsign(session, aSelectedSurvey);
         aPrintData.setCallsign(callsign);

         String platformIocCode = GetIocCode.getIocCode(session, aSelectedSurvey);
         aPrintData.setPlatformIocCode(platformIocCode);

         // File to write comments since scroll back is not allowed in hibernate yet (2007)
         Path commentFile = aDirectory.resolve(ReportGenerator.SCRATCH_FILE_PREFIX + "object_comments");
         GetComment.generateCommentFile(session, queryComment, commentFile, reportEngine.getCharset());

         try (RewindableBufferedReader fObs = new RewindableBufferedReader(observationFile, reportEngine.getCharset());
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

            if (scatExist) {
               // Frequency and transceiver of open files
               Scatter scat = (Scatter) scatterResults.get(0);
               setCurrent(scat);  //??

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

               setCurrent(scat);

               // This should not have been necessary, but experience show that it is, e.g. scat_object > sData_object
               // ScatterData is ahead of Scatter
               if (currentCompare(sData) > 0) {
                  scatExist = scatterResults.next();
                  continue;
               }

               if (sDataExist) {
                  sData = (ScatterData) scatterDataResults.get(0);    // Read what scatterDataResults.next() "points" to

                  // Only desired scatter type: works since SCHOOL_PELAGIC > SCATTER_PELAGIC
                  while (scatterTypePelagic != sData.getCompId().getScatterType() &&
                        scatterTypeBottom != sData.getCompId().getScatterType()) {
                     sDataExist = scatterDataResults.next();
                     if (!sDataExist) break;
                     sData = (ScatterData) scatterDataResults.get(0);
                  }

                  // Synchronize: get new scatterData if necessary.
                  while (currentCompare(sData) < 0) {
                     sDataExist = scatterDataResults.next();
                     if (!sDataExist) break;
                  }
               }  //if - scatterData

               // If scatterData exists, it is now synchronized to current_ (frequency, transceiver, object, date, time),
               // but it may not be of the type requested for printing
               while (currentCompare(sData) == 0 &&
                     scatterTypePelagic != sData.getCompId().getScatterType() &&
                     scatterTypeBottom != sData.getCompId().getScatterType()) {
                  sDataExist = scatterDataResults.next();
                  if (!sDataExist) break;
                  sData = (ScatterData) scatterDataResults.get(0);
               }  //while - scatterData

               // Synchronize: set observation if necessary
               while (currentCompare(gObservation.getCompId().getObservationDate(), gObservation.getCompId().getObservationTime()) != 0) {
                  if (!obsRead) {
                     fObs.rewind();
                  }
                  obsRead = GetObservation.nextObservation(fObs.getReader(), gObservation);
                  if (!obsRead) {
                     break;   // exit while-loop if end-of-file is reached
                  }
               } //while - observation

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
                        if (currentCompare(scat) == 0) {
                           // FILL: SCATTER (bottom)
                           aPrintData.setScatter(scat);
                           if (scat.getBottomActive() == 1) {
                              aPrintDataBottom.setScatter(scat);
                           }
                        }
                        setCurrent(scat); //??
                     }
                  } //if
               } //if

               // FILL: SCATTERDATA
               // ScatterData will in general have many (or none) entries for the same frequency, transceiver, date and
               // time: i.e. get all those and fill into aPrintData and aPrintDataBottom. Current (... date, time) is
               // already inserted into print-structure, so testing against that assures that the data in aPrintData
               // has the same frequency, transceiver, date, time (which is also the case for aPrintDataBottom)
               while (sData.getCompId().getFrequency() == aPrintData.getScatter(ReportMode.NATIVE).getCompId().getFrequency() &&
                     sData.getCompId().getTransceiver() == aPrintData.getScatter(ReportMode.NATIVE).getCompId().getTransceiver() &&
                     sData.getCompId().getObject() == aPrintData.getScatter(ReportMode.NATIVE).getCompId().getObject() &&
                     sData.getCompId().getObservationDate() == aPrintData.getScatter(ReportMode.NATIVE).getCompId().getObservationDate() &&
                     sData.getCompId().getObservationTime() == aPrintData.getScatter(ReportMode.NATIVE).getCompId().getObservationTime()) {
                  if (aAsyncHandle.isCancelled()) {
                     break mainLoop;
                  }

                  // FILL: SCATTER_DATA. NB! Only filled for type scat.getCompId().getScatterType()
                  aPrintData.setSa(sData);       //Allocate pelagic data of type SCATTER_PELAGIC
                  aPrintDataBottom.setSa(sData); //Allocate bottom data of type SCATTER_BOTTOM

                  sDataExist = scatterDataResults.next();
                  if (!sDataExist) break;
                  sData = (ScatterData) scatterDataResults.get(0);

                  // Break loop when all ScatterData for current frequency, transceiver, date and time are read
                  if (currentCompare(sData) > 0) break;

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
                     if (actualNoFrequencies > reportEngine.getExpectedFrequencyCount()) {
                        reportEngine.setExpectedFrequencyCount(actualNoFrequencies);
                     }
                     aProgressView.setSecondaryText(progressText + Utils.hzToKHz(printFrequency) + " kHz");

                     // Reset observation file (and start reading from the beginning)
                     fObs.rewind();
                     obsRead = GetObservation.nextObservation(fObs.getReader(), gObservation);

                     for (BaseSingleFrequencyReport report : selectedReports) {
                        report.open(aPrintData, aDirectory, aMode,
                              printFrequency,
                              printTransceiver,
                              getObservation.getStartObservationDistance(),
                              getObservation.getFinalObservationDistance(lastDistanceInterval));
                     }
                  }
               } //if

               if (!scatExist) {
                  break;
               }

               for (BaseSingleFrequencyReport report : selectedReports) {
                  report.print(aPrintData, printFrequency, printTransceiver, aMode, aPrintDataBottom);
               }

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

   private void setCurrent(Scatter aScatter) {
      current_fr = aScatter.getCompId().getFrequency();
      current_tr = aScatter.getCompId().getTransceiver();
      current_ob = aScatter.getCompId().getObject();
      current_da = aScatter.getCompId().getObservationDate();
      current_ti = aScatter.getCompId().getObservationTime();
   }

   private int currentCompare(Scatter aScat) {
      int frCmp = Integer.compare(aScat.getCompId().getFrequency(), current_fr);
      if (frCmp != 0) {
         return frCmp;
      }
      int trCmp = Integer.compare(aScat.getCompId().getTransceiver(), current_tr);
      if (trCmp != 0) {
         return trCmp;
      }
      int obCmp = Integer.compare(aScat.getCompId().getObject(), current_ob);
      if (obCmp != 0) {
         return obCmp;
      }
      int tiCmp = currentCompare(aScat.getCompId().getObservationDate(), aScat.getCompId().getObservationTime());
      return tiCmp;
   }

   private int currentCompare(ScatterData aScatData) {
      int frCmp = Integer.compare(aScatData.getCompId().getFrequency(), current_fr);
      if (frCmp != 0) {
         return frCmp;
      }
      int trCmp = Integer.compare(aScatData.getCompId().getTransceiver(), current_tr);
      if (trCmp != 0) {
         return trCmp;
      }
      int obCmp = Integer.compare(aScatData.getCompId().getObject(), current_ob);
      if (obCmp != 0) {
         return obCmp;
      }
      int tiCmp = currentCompare(aScatData.getCompId().getObservationDate(), aScatData.getCompId().getObservationTime());
      return tiCmp;
   }

   private int currentCompare(int aDa, int aTi) {
      if (aDa > current_da ||
            (aDa == current_da && aTi > current_ti)) {
         return 1;
      } else if (aDa == current_da && aTi == current_ti) {
         return 0;
      } else {
         return -1;
      }
   }
}
