package no.imr.lsss.framework;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.track.SegmentInfoCache;
import no.imr.korona.region.WorkData;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformName;
import no.imr.lsss.database.tables.hibernate.PlatformNamePK;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.framework.config.application.OnSurveyOpenAction;
import no.imr.lsss.framework.config.survey.SurveyConfigurationXml;
import no.imr.lsss.framework.config.survey.data.SegmentHandlesAndAttributes;
import no.imr.lsss.framework.packages.LsssCallbackEvent;
import no.imr.lsss.framework.wizards.newsurvey.NewSurveyWizard;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.WorkFileManager;
import no.imr.lsss.util.LsssUtils;
import no.imr.lsss.viewer.MainDisplay;
import no.imr.lsss.viewer.SurveyFileDialog;
import no.imr.tools.NoCanDoException;
import no.imr.tools.ProgressHandler;
import no.imr.tools.UnionList;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.database.queries.FetchQuery;
import no.imr.tools.database.queries.SaveOrUpdateQuery;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.misc.ReferenceDirectory;
import no.imr.tools.parameter.misc.ReferenceDirectoryCollection;
import no.imr.tools.parameter.misc.ReferenceDirectoryManager;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.time.DateTimeMillis;
import no.imr.tools.upgrade.UpgradeEngine;
import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.xml.XmlUtils;
import no.imr.tools.xml.XslUpgraderFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.prefs.Preferences;

/**
 * Manages the current survey.
 * A survey consists of configuration and interpretation.
 */
public final class SurveyManager {
   private static final String XML_NEWEST_VERSION = "2";
   static final UpgradeEngine<Element> XML_SURVEY_FILE_UPGRADE_ENGINE = new UpgradeEngine<>(
         "Survey file", XML_NEWEST_VERSION, XmlUtils::getVersion,
         new XslUpgraderFactory("no/imr/lsss/resources/surveyFileUpgrade"));

   public static final String LSSS_DATA_DIR_NAME = "LSSS_DATA";

   private static final String CACHE_DIR_NAME = "cache";

   public static final String SURVEY_FILE_SUFFIX = ".lsss";

   public static final String SURVEY_REFERENCE_DIR_NAME = "SurveyDir";
   public static final String LSSS_REFERENCE_DIR_NAME = "LSSS";

   private static final String XML_SURVEY = "survey";
   private static final String XML_CONFIGURATION = "configuration";
   private static final String XML_VERSION = "version";
   private static final String XML_ECHOGRAM_ZOOM = "echogramZoom";

   private static final String PREFERENCE_LAST_FILE = "lastFile";
   private static final String PREFERENCE_LAST_FILE_OPEN = "lastFileOpen";

   private final LSSS lsss;

   private final ArgChangeManager<Optional<Path>> changeManager = new ArgChangeManager<>();

   private final ReferenceDirectory surveyReferenceDirectory = new ReferenceDirectory(new Name(SURVEY_REFERENCE_DIR_NAME));
   private final ReferenceDirectory lsssReferenceDirectory = new ReferenceDirectory(new Name(LSSS_REFERENCE_DIR_NAME));
   private final ReferenceDirectoryManager referenceDirectoryManager = new ReferenceDirectoryManager()
         .setRelativeEverywhereReferenceDirectory(lsssReferenceDirectory)
         .add(new ReferenceDirectoryCollection(new Name("SurveyManager"), surveyReferenceDirectory));

   private @Nullable Path surveyFile;

   private final SurveyFileDialog surveyFileDialog;

   public SurveyManager(LSSS lsss) {
      this.lsss = lsss;
      surveyFileDialog = new SurveyFileDialog(lsss, this);
   }

   public ArgChangeManager<Optional<Path>> getChangeManager() {
      return changeManager;
   }

   public ReferenceDirectoryManager getReferenceDirectoryManager() {
      return referenceDirectoryManager;
   }

   public ReferenceDirectory getSurveyReferenceDirectory() {
      return surveyReferenceDirectory;
   }

   public ReferenceDirectory getLsssReferenceDirectory() {
      return lsssReferenceDirectory;
   }

   public @Nullable Path getCacheDir() {
      return surveyFile != null ? surveyFile.resolveSibling(CACHE_DIR_NAME) : null;
   }

   public @Nullable SegmentInfoCache readSegmentInfoCache(@Nullable Path dir, SegmentHandlesAndAttributes segmentHandlesAndAttributes) {
      Path cacheDir = getCacheDir();
      if (dir == null || cacheDir == null) {
         return null;
      }
      String dirId = FileUtils.relativePath(dir, cacheDir);
      if (dirId == null) {
         dirId = dir.toString();
      }
      dirId = FileUtils.toSlashSeparatorChar(dirId);
      return new SegmentInfoCache(segmentHandlesAndAttributes.segmentHandles(), segmentHandlesAndAttributes.attributes(),
            cacheDir.resolve("dataDirs"), dirId);
   }

   public @Nullable Path getSurveyFile() {
      return surveyFile;
   }

   public boolean isOpen() {
      return surveyFile != null;
   }

   public boolean isWorkUnmodifiedOrUserApproved(FeaturePlugin plugin) {
      return isUnmodifiedOrUserApproved(false, List.of(plugin));
   }

   public boolean isUnmodifiedOrUserApproved() {
      return isUnmodifiedOrUserApproved(true, lsss.getPluginManager().getFeaturePlugins());
   }

   private boolean isUnmodifiedOrUserApproved(boolean checkConfig, List<FeaturePlugin> plugins) {
      if (!lsss.getLsssConfig().isPrimaryLSSS) {
         return true;
      }
      if (surveyFile == null) {
         return true;
      }

      List<Path> modifiedConfigFiles = new ArrayList<>();
      List<Path> modifiedWorkFiles = new ArrayList<>();

      Component referenceComponent = lsss.getReferenceComponent();
      ProgressView progressView = new ProgressView("Checking for modified work files", 100)
            .mainProgressAsPercentage();
      WorkerDialog.Result result = new WorkerDialog(referenceComponent, progressView.getComponent())
            .start(asyncHandle -> {
               if (checkConfig) {
                  if (!XmlUtils.equalContent(toSurveyXml(), surveyFile)) {
                     modifiedConfigFiles.add(surveyFile);
                  }
               }
               for (FeaturePlugin plugin : plugins) {
                  WorkFileManager workFileManager = plugin.getWorkFileManager();
                  if (workFileManager == null) {
                     continue;
                  }
                  progressView.setMainText(LsssUtils.infoText("Checking for modified", plugin, "work files"));
                  ProgressHandler progressHandler = progressView.getMainProgressHandler();
                  progressHandler.setProgress(0);
                  modifiedWorkFiles.addAll(workFileManager.getModifiedFiles(progressHandler, asyncHandle));
               }
            });
      if (!result.success()) {
         return false;
      }

      boolean configModified = !modifiedConfigFiles.isEmpty();
      boolean workModified = !modifiedWorkFiles.isEmpty();
      if (configModified || workModified) {
         int answer = JOptionPane.NO_OPTION;

         if (lsss.getInterpretationSettings().isInteractiveMode()) {
            StringBuilder message = new StringBuilder("<html>Survey ");
            if (configModified) {
               message.append("configuration ");
            }
            if (workModified) {
               if (configModified) {
                  message.append("and ");
               }
               message.append("interpretation ");
            }
            message.append(configModified && workModified ? "are" : "is")
                  .append(" modified.<br>Save changes?");

            StringBuilder toolTip = new StringBuilder("<html>Modified files:<br>");
            UnionList<Path> modifiedFiles = new UnionList<>(modifiedConfigFiles, modifiedWorkFiles);
            for (int i = 0; i < modifiedFiles.size(); i++) {
               if (i >= 50) {
                  toolTip.append("<br>...");
                  break;
               }
               toolTip.append("<br>").append(modifiedFiles.get(i));
            }

            JLabel messageLabel = new JLabel(message.toString());
            messageLabel.setToolTipText(toolTip.toString());
            if (!Utils.IS_DIST_VERSION) {
               messageLabel.addMouseListener(new PopupMenuMouseListener(mouseEvent -> {
                  JPopupMenu menu = new JPopupMenu();
                  menu.add("Debug: Save and show diff").addActionListener(actionEvent -> {
                     try {
                        Path dir = LoggingManager.getTopInstallationDir().resolve("tmp").resolve("debugDiff");
                        FileUtils.deleteContentsRecursively(dir);
                        Path oldDir = dir.resolve("old");
                        FileUtils.createDirectories(oldDir);
                        for (Path file : modifiedFiles) {
                           FileUtils.copy(file, oldDir.resolve(file.getFileName()));
                           FileUtils.copy(file, dir.resolve(file.getFileName() + "-old"));
                        }
                        save();
                        Path newDir = dir.resolve("new");
                        FileUtils.createDirectories(newDir);
                        for (Path file : modifiedFiles) {
                           FileUtils.copy(file, newDir.resolve(file.getFileName()));
                           FileUtils.copy(file, dir.resolve(file.getFileName() + "-new"));
                        }
                        // Copy old files back
                        for (Path file : modifiedFiles) {
                           FileUtils.copy(oldDir.resolve(file.getFileName()), file);
                        }
                        GuiUtils.desktopOpen(dir, messageLabel);
                     } catch (Exception e) {
                        lsss.showError(messageLabel, e.getMessage(), e);
                     }
                  });
                  return menu;
               }));
            }

            answer = JOptionPane.showConfirmDialog(referenceComponent, messageLabel, "Save survey?",
                  JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
         }

         return switch (answer) {
            case JOptionPane.YES_OPTION -> writeSurvey(plugins);
            case JOptionPane.NO_OPTION -> true;
            default -> false;
         };
      } else {
         return true;
      }
   }

   public void createNew() {
      close();
      lsss.getConfigurationManager().getSurveyConfiguration().loadDefault();
      if (lsss.getDatabaseManager().getDatabaseConnection().isConnected()) {
         new NewSurveyWizard(lsss).show();
      } else {
         JOptionPane.showMessageDialog(lsss.getReferenceComponent(), "Please connect to a database before creating a survey");
         lsss.getConfigurationManager().showDialog(lsss.getConfigurationManager().getApplicationConfiguration().getDatabaseConf());
      }
   }

   public boolean getLastSurveyFileOpen() {
      return getPreferences().getBoolean(PREFERENCE_LAST_FILE_OPEN, true);
   }

   public @Nullable Path getLastSurveyFile() {
      String pref = getPreferences().get(PREFERENCE_LAST_FILE, null);
      return pref != null ? Path.of(pref) : null;
   }

   private void setFile(@Nullable Path file) {
      surveyFile = file;
      Path lsssDir = FileUtils.getParent(file);
      lsssReferenceDirectory.setFile(lsssDir);
      surveyReferenceDirectory.setFile(lsssDirToSurveyDir(lsssDir));

      if (file != null) {
         Exec.CACHED_THREAD_POOL.execute(() -> {
            getPreferences().putBoolean(PREFERENCE_LAST_FILE_OPEN, true);
            getPreferences().put(PREFERENCE_LAST_FILE, file.toString());
            SurveyIndexFile.addSurveyFile(file);
         });
      }

      changeManager.notifyListeners(Optional.ofNullable(file));
   }

   public static @Nullable Path lsssDirToSurveyDir(@Nullable Path lsssDir) {
      // The current solution of #1465.
      return FileUtils.getParent(lsssDir);
   }

   public Preferences getPreferences() {
      return lsss.getPreferences("survey");
   }

   public void open() {
      Path file = surveyFileDialog.chooseFile(JFileChooser.OPEN_DIALOG);
      if (file != null) {
         open(file);
      }
   }

   public void open(Path file) {
      Document document;
      try {
         document = XmlUtils.readDocumentIfExists(file);
      } catch (IOException e) {
         lsss.showError("Error opening survey file:\n" + file, e);
         return;
      }
      if (document == null) {
         lsss.showError("Survey file does not exist:\n" + file);
         return;
      }

      close(); // Must close survey to be connected to the global database before checking if survey is in database

      if (!isDatabaseOK(new SurveyConfigurationXml(document))) {
         return;
      }

      Log.global.info("Opening survey " + file);
      setFile(file);
      fromSurveyXml(document.getRootElement());

      switch (lsss.getConfigurationManager().getAppMiscConf().onSurveyOpen.getValue()) {
         case SHOW_CONFIG_DIALOG -> {
            if (lsss.getInterpretationSettings().isInteractiveMode()) {
               lsss.getConfigurationManager().showDialog(lsss.getConfigurationManager().getDataConf());
            }
         }
         case OPEN_FILES -> {
            lsss.getConfigurationManager().ok();
         }
         case OPEN_FILES_AND_ZOOM -> {
            lsss.getConfigurationManager().ok();
            applyEchogramZoom(document.getRootElement());
         }
         case DO_NOTHING -> {
         }
      }

      if (lsss.getLsssConfig().isPrimaryLSSS) {
         lsss.getPackageManager().dispatchCallbackEvent(LsssCallbackEvent.surveyOpen);
      }
   }

   private boolean isDatabaseOK(SurveyConfigurationXml surveyConfigurationXml) {
      if (Boolean.parseBoolean(surveyConfigurationXml.getUseLocalDatabase())) {
         return true;
      }

      if (!lsss.getDatabaseManager().getDatabaseConnection().isConnected()) {
         String message = "Please connect to a database before opening this survey";
         if (lsss.getInterpretationSettings().isInteractiveMode()) {
            JOptionPane.showMessageDialog(lsss.getReferenceComponent(), message);
            lsss.getConfigurationManager().showDialog(lsss.getConfigurationManager().getApplicationConfiguration().getDatabaseConf());
            return false;
         } else {
            throw new NoCanDoException(message);
         }
      }

      String nationName = surveyConfigurationXml.getNation();
      if (nationName == null) {
         return true;
      }

      Nation nation = lsss.getDatabaseManager().getDatabaseData().getNation(nationName);
      if (nation == null) {
         String message = "Unknown nation: " + nationName;
         if (lsss.getInterpretationSettings().isInteractiveMode()) {
            lsss.showError(message);
            return false;
         } else {
            throw new NoCanDoException(message);
         }
      }

      String platformIdString = surveyConfigurationXml.getPlatformId();
      if (platformIdString == null) {
         return true;
      }
      short platformId = Short.parseShort(platformIdString);
      List<Platform> platforms = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(Platform.class,
            DatabaseData.NATION, nation.getNation(),
            DatabaseData.PLATFORM, platformId));
      Platform platform;
      if (platforms.isEmpty()) {
         String platformString = surveyConfigurationXml.getPlatform();
         if (platformString == null) {
            platformString = "Unspecified";
         }
         if (lsss.getInterpretationSettings().isInteractiveMode()) {
            int answer = JOptionPane.showConfirmDialog(lsss.getFrame(),
                  "Platform not found in this database.\nCreate \"" + platformString + "\"?",
                  "Create platform?", JOptionPane.OK_CANCEL_OPTION);
            if (answer != JOptionPane.OK_OPTION) {
               return false;
            }
         }
         platform = new Platform(new PlatformPK(nation.getNation(), platformId), (short) 0, (short) 0, 0, 0);
         String name = platformString.replaceAll(" \\(\\d+\\)$", "");
         PlatformName platformName = new PlatformName(new PlatformNamePK(nation.getNation(), platform.getCompId().getPlatform(), 0), 0, name);
         Log.global.info("Creating platform: " + platform);
         lsss.getDatabaseManager().getDatabaseConnection().executeQuery(new SaveOrUpdateQuery(List.of(platform, platformName)));
         lsss.getConfigurationManager().getSurveyConf().updateAllowedPlatforms();
      } else {
         platform = platforms.getFirst();
      }

      String surveyIdString = surveyConfigurationXml.getSurveyId();
      if (surveyIdString == null) {
         return true;
      }
      int surveyId = Integer.parseInt(surveyIdString);
      List<Survey> surveys = lsss.getDatabaseManager().getDatabaseConnection().executeFetchQuery(new FetchQuery<>(Survey.class,
            DatabaseData.NATION, nation.getNation(),
            DatabaseData.PLATFORM, platform.getCompId().getPlatform(),
            DatabaseData.SURVEY, surveyId));
      if (surveys.isEmpty()) {
         String surveyTitle = surveyConfigurationXml.getSurveyTitle();
         if (surveyTitle == null) {
            surveyTitle = "Unspecified";
         }
         if (lsss.getInterpretationSettings().isInteractiveMode()) {
            int answer = JOptionPane.showConfirmDialog(lsss.getFrame(),
                  "Survey not found in this database.\nCreate \"" + surveyTitle + "\"?",
                  "Create survey?", JOptionPane.OK_CANCEL_OPTION);
            if (answer != JOptionPane.OK_OPTION) {
               return false;
            }
         }
         String name = surveyTitle.replaceAll(" \\(\\d+\\)$", "");
         Survey survey = new Survey(new SurveyPK(nation.getNation(), platform.getCompId().getPlatform(), surveyId),
               name,
               Optional.ofNullable(surveyConfigurationXml.getStartDate()).map(DateTimeMillis::stringDateToInt).orElse(0),
               Optional.ofNullable(surveyConfigurationXml.getStartTime()).map(DateTimeMillis::centisTimeToInt).orElse(0),
               Optional.ofNullable(surveyConfigurationXml.getStopDate()).map(DateTimeMillis::stringDateToInt).orElse(0),
               Optional.ofNullable(surveyConfigurationXml.getStopTime()).map(DateTimeMillis::centisTimeToInt).orElse(0),
               Optional.ofNullable(surveyConfigurationXml.getSurveyDescription()).orElse(""),
               Optional.ofNullable(surveyConfigurationXml.getBoundaryNorth()).map(Float::parseFloat).orElse(0f),
               Optional.ofNullable(surveyConfigurationXml.getBoundarySouth()).map(Float::parseFloat).orElse(0f),
               Optional.ofNullable(surveyConfigurationXml.getBoundaryWest()).map(Float::parseFloat).orElse(0f),
               Optional.ofNullable(surveyConfigurationXml.getBoundaryEast()).map(Float::parseFloat).orElse(0f));
         Log.global.info("Creating survey: " + survey);
         lsss.getDatabaseManager().getDatabaseConnection().executeQuery(new SaveOrUpdateQuery(List.of(platform, survey)));
         lsss.getConfigurationManager().getSurveyConf().updateAllowedSurveys();
      }

      return true;
   }

   public void save() {
      if (isOpen()) {
         writeSurvey(lsss.getPluginManager().getFeaturePlugins());
      } else {
         saveAs();
      }
   }

   public void saveAs() {
      Path file = surveyFileDialog.chooseFile(JFileChooser.SAVE_DIALOG);
      if (file != null) {
         saveAs(file);
      }
   }

   public void saveAs(Path file) {
      setFile(file);
      save();
   }

   public void closeByUser() {
      getPreferences().putBoolean(PREFERENCE_LAST_FILE_OPEN, false);
      close();
   }

   public void close() {
      if (!isOpen()) {
         return;
      }
      setFile(null);

      lsss.getInterpretationSettings().cancelAll();
      lsss.getConfigurationManager().installBlankSurveyXml();
      if (lsss.getLsssConfig().isPrimaryLSSS) {
         lsss.getPackageManager().dispatchCallbackEvent(LsssCallbackEvent.surveyClose);
      }
   }

   private static Element upgrade(Element element) throws UpgradeException {
      return XML_SURVEY_FILE_UPGRADE_ENGINE.upgrade(element);
   }

   public void fromSurveyXml(Element surveyElement) {
      try {
         surveyElement = upgrade(surveyElement);
      } catch (Exception e) {
         lsss.showError("Error upgrading survey file", e);
      }

      Element configurationElement = surveyElement.element(XML_CONFIGURATION);
      if (configurationElement != null) {
         lsss.getConfigurationManager().getSurveyConfiguration().fromXml(configurationElement.elements().getFirst());
      }

      Element displayElement = surveyElement.element(MainDisplay.XML_DISPLAY);
      if (displayElement != null) {
         MainDisplay display = lsss.getDisplay();
         if (display != null) {
            SwingUtilities.invokeLater(() -> display.fromXml(displayElement));
         } else {
            MainDisplay.noDisplayFromXml(displayElement, lsss.getModuleManager());
         }
      }
   }

   public Element toSurveyXml() {
      Element surveyElement = DocumentHelper.createElement(XML_SURVEY)
            .addAttribute(XML_VERSION, XML_NEWEST_VERSION);

      Element configurationElement = surveyElement.addElement(XML_CONFIGURATION);
      configurationElement.add(lsss.getConfigurationManager().getSurveyConfiguration().toXml());

      MainDisplay display = lsss.getDisplay();
      if (display != null) {
         surveyElement.add(display.toXml());
      }

      if (lsss.getConfigurationManager().getAppMiscConf().onSurveyOpen.getValue() == OnSurveyOpenAction.OPEN_FILES_AND_ZOOM) {
         addEchogramZoom(surveyElement);
      }

      return surveyElement;
   }

   private void addEchogramZoom(Element surveyElement) {
      InterpretationSettings interpretationSettings = lsss.getInterpretationSettings();
      PingRange pingRange = interpretationSettings.getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }
      Element zoomElement = surveyElement.addElement(XML_ECHOGRAM_ZOOM);

      zoomElement.addElement("time")
            .addAttribute("min", Double.toString(PingMapping.ntDateToTimeValue(pingRange.begin().getNTDate())))
            .addAttribute("max", Double.toString(PingMapping.ntDateToTimeValue(pingRange.end().getNTDate())));

      FloatRange pelagicZ = interpretationSettings.getPelagicZSettings().getZoomedZRange();
      zoomElement.addElement("pelagic")
            .addAttribute("min", Utils.toString(pelagicZ.min()))
            .addAttribute("max", Utils.toString(pelagicZ.max()));

      if (lsss.getConfigurationManager().getSurveyMiscConf().pelagicMode.getBooleanValue()) {
         FloatRange bottomZ = interpretationSettings.getBottomZSettings().getZoomedZRange();
         zoomElement.addElement("bottom")
               .addAttribute("min", Utils.toString(bottomZ.min()))
               .addAttribute("max", Utils.toString(bottomZ.max()));
      }
   }

   private void applyEchogramZoom(Element surveyElement) {
      Element zoomElement = surveyElement.element(XML_ECHOGRAM_ZOOM);
      if (zoomElement == null) {
         return;
      }

      InterpretationSettings interpretationSettings = lsss.getInterpretationSettings();

      Element time = zoomElement.element("time");
      if (time != null) {
         double min = Double.parseDouble(time.attributeValue("min"));
         double max = Double.parseDouble(time.attributeValue("max"));

         PingIndex begin = interpretationSettings.getDataFileSet().getContainingPingIndex(min, PingMapping.TIME);
         PingIndex end = interpretationSettings.getDataFileSet().getContainingPingIndex(max, PingMapping.TIME);
         if (begin != null && end != null) {
            interpretationSettings.setPingRange(PingRange.of(begin, end));
         }
      }

      Element pelagic = zoomElement.element("pelagic");
      if (pelagic != null) {
         float min = Float.parseFloat(pelagic.attributeValue("min"));
         float max = Float.parseFloat(pelagic.attributeValue("max"));
         interpretationSettings.getPelagicZSettings().setZ(min, max);
      }

      Element bottom = zoomElement.element("bottom");
      if (bottom != null) {
         float min = Float.parseFloat(bottom.attributeValue("min"));
         float max = Float.parseFloat(bottom.attributeValue("max"));
         interpretationSettings.getBottomZSettings().setZ(min, max);
      }
   }

   private boolean writeSurvey(List<FeaturePlugin> plugins) {
      ProgressView progressView = new ProgressView("Saving work files", 100)
            .mainProgressAsPercentage();
      return new WorkerDialog(lsss::getReferenceComponent, progressView::getComponent)
            .start(asyncHandle -> doWriteSurvey(plugins, progressView, asyncHandle))
            .success();
   }

   private void doWriteSurvey(List<FeaturePlugin> plugins, ProgressView progressView, AsyncHandle asyncHandle) {
      writeConfiguration();
      writeInterpretation(plugins, progressView, asyncHandle);
   }

   private void writeConfiguration() {
      if (surveyFile == null) {
         return;
      }
      lsss.getConfigurationManager().getApplicationConfiguration().saveDefault();
      try {
         XmlUtils.writeDocument(toSurveyXml(), surveyFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error writing survey file " + surveyFile, e);
      }
   }

   private static void writeInterpretation(List<FeaturePlugin> plugins, ProgressView progressView, AsyncHandle asyncHandle) {
      plugins.forEach(plugin -> {
         if (asyncHandle.isCancelled()) {
            return;
         }
         WorkFileManager workFileManager = plugin.getWorkFileManager();
         if (workFileManager == null) {
            return;
         }
         progressView.setMainText(LsssUtils.infoText("Saving", plugin, "work files"));
         ProgressHandler progressHandler = progressView.getMainProgressHandler();
         progressHandler.setProgress(0);
         workFileManager.save(progressHandler, asyncHandle);
      });
   }

   public WorkData getWorkData() {
      BaseSystemWorkFileManager workFileManager = lsss.getPluginManager().getFeaturePlugin(BaseSystemFeaturePlugin.class).getWorkFileManager();
      return workFileManager.getWorkData();
   }
}
