package no.imr.lsss.database.reports;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseReportManager;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.database.util.LanguageUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ProgressView;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StatelessSession;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Level;

public final class ReportEngine {
   static final int DEFAULT_MAX_PRINT_PELAGIC = 100;
   static final int DEFAULT_MAX_PRINT_BOTTOM = 100;
   static final float DEFAULT_ACCUMULATE_DISTANCE = 5.0f;
   static final int MAX_REMEMBERED_SCHOOLS = 1000;

   private final LSSS lsss;
   private final LanguageUtils languageUtils;
   private final List<DatabaseReportManager> pluginReportManagers;

   private Predicate<Integer> reports = type -> false;
   private int startDate;
   private int startTime;
   private int stopDate;
   private int stopTime;
   private int maxSpecialReportSpecies = Integer.MAX_VALUE;
   private int maxPrintPelagicCh = DEFAULT_MAX_PRINT_PELAGIC;
   private int maxPrintBottomCh = DEFAULT_MAX_PRINT_BOTTOM;
   private float accumulateDistance = DEFAULT_ACCUMULATE_DISTANCE;
   private boolean extinctionCheck;
   private boolean printScrutinizedSpCheck;
   private ReportMode mode = ReportMode.NATIVE;
   private boolean schoolReport;
   private boolean distanceFileExtension;

   private int expectedFrequencyCount = 1;

   private Charset charset = Utils.nativeCharset();
   private OutputStreamFactory outputStreamFactory = Files::newOutputStream;

   public ReportEngine(LSSS lsss) {
      this.lsss = lsss;
      languageUtils = lsss.getConfigurationManager().getLanguageUtils();
      pluginReportManagers = lsss.getPluginManager().getFeaturePlugins().stream()
            .map(featurePlugin -> featurePlugin.createDatabaseReportManager(this))
            .filter(Objects::nonNull)
            .toList();
   }

   public LSSS getLSSS() {
      return lsss;
   }

   public LanguageUtils getLanguageUtils() {
      return languageUtils;
   }

   public List<DatabaseReportManager> getPluginReportManagers() {
      return pluginReportManagers;
   }

   public void setReports(Predicate<Integer> reports) {
      this.reports = reports;
   }

   boolean isReportSelected(BaseReport report) {
      return reports.test(report.getType());
   }

   public int getStartDate() {
      return startDate;
   }

   public void setStartDate(int startDate) {
      this.startDate = startDate;
   }

   public int getStartTime() {
      return startTime;
   }

   public void setStartTime(int startTime) {
      this.startTime = startTime;
   }

   public int getStopDate() {
      return stopDate;
   }

   public void setStopDate(int stopDate) {
      this.stopDate = stopDate;
   }

   public int getStopTime() {
      return stopTime;
   }

   public void setStopTime(int stopTime) {
      this.stopTime = stopTime;
   }

   public int getMaxSpecialReportSpecies() {
      return maxSpecialReportSpecies;
   }

   public void setMaxSpecialReportSpecies(int maxSpecialReportSpecies) {
      this.maxSpecialReportSpecies = maxSpecialReportSpecies;
   }

   public int getMaxPrintPelagicCh() {
      return maxPrintPelagicCh;
   }

   public void setMaxPrintPelagicCh(int maxPrintPelagicCh) {
      this.maxPrintPelagicCh = maxPrintPelagicCh;
   }

   public int getMaxPrintBottomCh() {
      return maxPrintBottomCh;
   }

   public void setMaxPrintBottomCh(int maxPrintBottomCh) {
      this.maxPrintBottomCh = maxPrintBottomCh;
   }

   public float getAccumulateDistance() {
      return accumulateDistance;
   }

   public void setAccumulateDistance(float accumulateDistance) {
      this.accumulateDistance = accumulateDistance;
   }

   public boolean getExtinctionCheck() {
      return extinctionCheck;
   }

   public void setExtinctionCheck(boolean extinctionCheck) {
      this.extinctionCheck = extinctionCheck;
   }

   public boolean getPrintScrutinizedSpCheck() {
      return printScrutinizedSpCheck;
   }

   public void setPrintScrutinizedSpCheck(boolean printScrutinizedSpCheck) {
      this.printScrutinizedSpCheck = printScrutinizedSpCheck;
   }

   public ReportMode getMode() {
      return mode;
   }

   public void setMode(ReportMode mode) {
      this.mode = mode;
   }

   public boolean isSchoolReport() {
      return schoolReport;
   }

   public void setSchoolReport(boolean schoolReport) {
      this.schoolReport = schoolReport;
   }

   public boolean getDistanceFileExtension() {
      return distanceFileExtension;
   }

   public void setDistanceFileExtension(boolean distanceFileExtension) {
      this.distanceFileExtension = distanceFileExtension;
   }

   public int getExpectedFrequencyCount() {
      return expectedFrequencyCount;
   }

   public void setExpectedFrequencyCount(int expectedFrequencyCount) {
      this.expectedFrequencyCount = expectedFrequencyCount;
   }

   public Charset getCharset() {
      return charset;
   }

   public void setCharset(Charset charset) {
      this.charset = charset;
   }

   public OutputStream newOutputStream(Path file) throws IOException {
      return outputStreamFactory.newOutputStream(file);
   }

   public void setOutputStreamFactory(OutputStreamFactory outputStreamFactory) {
      this.outputStreamFactory = outputStreamFactory;
   }

   public Feedback printReports(Survey survey, Path directory, ProgressView progressView, AsyncHandle asyncHandle) {

      Feedback feedback = new Feedback();

      List<ReportGroup> reportGroups = List.of(
            new ReportGroupStandard(this),
            new ReportGroupTime(this, feedback),
            new ReportGroupObject(this),
            new ReportGroupTotal(this)
      );
      for (ReportGroup reportGroup : reportGroups) {
         progressView.setSecondaryIndeterminate();
         reportGroup.printReports(survey, directory, progressView, feedback, asyncHandle);
         if (asyncHandle.isCancelled()) {
            return feedback;
         }
      }

      for (DatabaseReportManager reportManager : pluginReportManagers) {
         if (asyncHandle.isCancelled()) {
            break;
         }
         try {
            progressView.setSecondaryIndeterminate();
            reportManager.writeReports(survey, directory, progressView, asyncHandle);
         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Error writing reports: \"" + reportManager.getTitle() + "\"", e);
         }
      }

      return feedback;
   }

   List<Integer> getAllFrequencies(StatelessSession session, Survey survey) {
      List<Integer> frequencies = new ArrayList<>();

      String query =
            " select distinct " +
                  "    a.compId.frequency, " +
                  "    a.compId.transceiver " +
                  " from Scatter a" +
                  " where a.compId.nation   = " + survey.getCompId().getNation() +
                  " and   a.compId.platform = " + survey.getCompId().getPlatform() +
                  " and   a.compId.survey   = " + survey.getCompId().getSurvey() +
                  " and ( a.compId.observationDate > " + startDate +
                  "  or  (a.compId.observationDate = " + startDate + " and a.compId.observationTime >= " + startTime + ") )" +
                  " and ( a.compId.observationDate < " + stopDate +
                  "  or  (a.compId.observationDate = " + stopDate + " and a.compId.observationTime <= " + stopTime + ") )" +
                  " order by a.compId.frequency, a.compId.transceiver ";

      try (ScrollableResults frequencyResults = session.createQuery(query)
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY)) {

         while (frequencyResults.next()) {
            frequencies.add((Integer) frequencyResults.get(0));
         }
      }

      return frequencies;
   }

   static boolean schoolObjectRegistered(int object, int[] registeredObjects, int noRegisteredObjects) {
      for (int i = 0; i < noRegisteredObjects; i++) {
         if (object == registeredObjects[i]) {
            return true;
         }
      }
      return false;
   }

   static List<Purpose> getSortedSpeciesPurposeList(StatelessSession session, Survey survey) {
      List<Purpose> species = LsssQuery.fetch(Purpose.class, survey).executeAndGetValue(session);
      species.sort(Comparator.comparingInt(Purpose::getPurpose)
            .thenComparingInt(purpose -> purpose.getCompId().getAcousticCategory()));
      return species;
   }

   List<Integer> getScrutinizedSpeciesList(StatelessSession session, Survey survey) {
      List<Integer> acousticCategory = new ArrayList<>();

      String query =
            " select distinct " +
                  "    a.compId.acousticCategory " +
                  " from ScatterData a" +
                  " where a.compId.nation   = " + survey.getCompId().getNation() +
                  " and   a.compId.platform = " + survey.getCompId().getPlatform() +
                  " and   a.compId.survey   = " + survey.getCompId().getSurvey() +
                  " and   a.compId.channelNumber = 0 " +
                  " and   a.compId.acousticCategory > 0 " +
                  " and ( a.compId.observationDate > " + startDate +
                  "  or  (a.compId.observationDate = " + startDate + " and a.compId.observationTime >= " + startTime + ") )" +
                  " and ( a.compId.observationDate < " + stopDate +
                  "  or  (a.compId.observationDate = " + stopDate + " and a.compId.observationTime <= " + stopTime + ") )" +
                  " order by a.compId.acousticCategory ";

      try (ScrollableResults acousticCategoryResults = session.createQuery(query)
            .setReadOnly(true)
            .scroll(ScrollMode.FORWARD_ONLY)) {

         while (acousticCategoryResults.next()) {
            acousticCategory.add((Integer) acousticCategoryResults.get(0));
         }
      }

      return acousticCategory;
   }

   static final class Feedback {
      private int actualNoFrequencies = 1;  // At least one frequency
      private int maxChannel = 0;
      private final Map<Integer, AcousticCategory> missingIcesCategories = new HashMap<>();
      private final Map<SurveyPK, Survey> missingIcesValues = new HashMap<>();

      private Feedback() {
      }

      int getActualNoFrequencies() {
         return actualNoFrequencies;
      }

      void setActualNoFrequencies(Integer actualNoFrequencies) {
         this.actualNoFrequencies = actualNoFrequencies;
      }

      int getMaxChannel() {
         return maxChannel;
      }

      void registerMaxChannel(int channel) {
         maxChannel = Math.max(maxChannel, channel);
      }

      Map<Integer, AcousticCategory> getMissingIcesCategories() {
         return missingIcesCategories;
      }

      void addMissingIcesCategory(AcousticCategory acousticCategory) {
         missingIcesCategories.put(acousticCategory.getCompId().getAcousticCategory(), acousticCategory);
      }

      Map<SurveyPK, Survey> getMissingIcesValues() {
         return missingIcesValues;
      }

      void addMissingIcesValue(Survey survey) {
         missingIcesValues.put(survey.getCompId(), survey);
      }
   }

   @FunctionalInterface
   public interface OutputStreamFactory {
      OutputStream newOutputStream(Path file) throws IOException;
   }
}
