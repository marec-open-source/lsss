package no.imr.lsss;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import no.imr.korona.Korona;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.plugins.ModulePlugin;
import no.imr.korona.region.RegionManager;
import no.imr.lsss.database.DatabaseManager;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.framework.LsssConfig;
import no.imr.lsss.framework.LsssDataConfiguration;
import no.imr.lsss.framework.LsssRegionConfiguration;
import no.imr.lsss.framework.PluginManager;
import no.imr.lsss.framework.ServiceCollection;
import no.imr.lsss.framework.SurveyManager;
import no.imr.lsss.framework.config.ConfigurationManager;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.application.ApplicationConfiguration;
import no.imr.lsss.framework.config.application.ApplicationConfigurationXml;
import no.imr.lsss.framework.config.application.PluginConf;
import no.imr.lsss.framework.config.application.packages.PackagesConf;
import no.imr.lsss.framework.config.survey.data.DataSetManager;
import no.imr.lsss.framework.export.ExportManager;
import no.imr.lsss.framework.extensions.LsssAccessImpl;
import no.imr.lsss.framework.packages.Actions;
import no.imr.lsss.framework.packages.LsssCallbackEvent;
import no.imr.lsss.framework.packages.PackageManager;
import no.imr.lsss.framework.wizards.appsetup.ApplicationSetupWizard;
import no.imr.lsss.modules.ModuleManager;
import no.imr.lsss.modules.interpretation.InterpretationSummary;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.resources.LsssResource;
import no.imr.lsss.viewer.MainDisplay;
import no.imr.lsss.viewer.StartupDialog;
import no.imr.tools.Utils;
import no.imr.tools.adm.ApplicationInfo;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.help.HelpSystem;
import no.imr.tools.help.HelpSystemInfo;
import no.imr.tools.help.HelpSystemPort;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.LsssAccess;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jspecify.annotations.Nullable;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.prefs.Preferences;

/**
 * The main LSSS class.
 */
public final class LSSS {
   public static final String VERSION = Utils.IS_BUILT_VERSION
         ? "@LSSS_VERSION@" // Substituted during build.
         : Utils.readBuildVersion("lsss");
   public static final ApplicationInfo APPLICATION_INFO = new ApplicationInfo(
         "LSSS", "Large Scale Survey System", "LSSS", VERSION,
         LsssResource.LSSS_32, LsssResource.LSSS_64,
         "lsss", "lsss");
   public static final LoggingManager LOGGING_MANAGER = new LoggingManager(APPLICATION_INFO);

   private @Nullable StartupDialog startupDialog;
   private final LsssConfig lsssConfig;
   private final ConfigurationManager configurationManager;
   private final SurveyManager surveyManager;
   private @Nullable MainDisplay display;
   private final DatabaseManager databaseManager;
   private final RegionManager regionManager;
   private final DataSetManager dataSetManager;
   private final InterpretationSettings interpretationSettings;
   private final InterpretationSummary interpretationSummary;
   private final LsssAccessImpl lsssAccess;
   private final PluginManager pluginManager;
   private final ModuleManager moduleManager;
   private final ExportManager exportManager;
   private final PackageManager packageManager;
   private final Actions actions;
   private final Korona korona;
   private final HelpSystem helpSystem;

   public LSSS(LsssConfig lsssConfig) {
      this.lsssConfig = lsssConfig;

      if (!lsssConfig.loadSettings) {
         LoggingManager.initTmpApplicationDataDir();
      }

      if (lsssConfig.visible) {
         SwingUtilities.invokeLater(() -> {
            display = new MainDisplay(this);
            startupDialog = new StartupDialog(display.getFrame());
            startupDialog.show();
         });
      }

      try {
         korona = new Korona();
         dataSetManager = new DataSetManager(new LsssDataConfiguration(this));
         packageManager = new PackageManager();
         interpretationSettings = new InterpretationSettings(this);
         actions = new Actions(this);
         interpretationSummary = new InterpretationSummary(this);
         surveyManager = new SurveyManager(this);
         lsssAccess = new LsssAccessImpl(this);
         pluginManager = new PluginManager(this, lsssConfig.serviceCollection);
         configurationManager = new ConfigurationManager(this);
         databaseManager = new DatabaseManager(this);
         regionManager = new RegionManager(new LsssRegionConfiguration(this));
         exportManager = new ExportManager(pluginManager);
         helpSystem = new HelpSystem(new HelpSystemInfo(HelpSystemPort.LSSS, "lsss", VERSION));

         setStartupText("Creating modules...");
         moduleManager = new ModuleManager(pluginManager);

         SwingUtilities.invokeLater(() -> {
            if (display != null) {
               display.setup();
            }
         });

         Future<?>[] setupFutures = {
               Exec.FORK_JOIN_POOL.submit(lsssAccess::setup),
               Exec.FORK_JOIN_POOL.submit(pluginManager::setup),
               Exec.FORK_JOIN_POOL.submit(moduleManager::setup),
               Exec.FORK_JOIN_POOL.submit(configurationManager::setup),
               Exec.FORK_JOIN_POOL.submit(interpretationSettings::setup),
               Exec.FORK_JOIN_POOL.submit(interpretationSummary::setup),
               Exec.FORK_JOIN_POOL.submit(actions::setup)
         };

         for (FeaturePlugin plugin : pluginManager.getFeaturePlugins()) {
            helpSystem.addHelpSet(plugin.getHelpSet());
         }
         for (ModulePlugin plugin : korona.getModuleManager().getModulePlugins()) {
            helpSystem.addHelpSet(plugin.getHelpSet());
         }

         for (Future<?> future : setupFutures) {
            Utils.awaitFuture(future);
         }
         interpretationSettings.waitUntilFinished();

         configurationManager.defineBlankSurveyXml();

         if (lsssConfig.loadSettings) {
            setStartupText("Loading settings...");
            if (lsssConfig.applicationXml != null) {
               configurationManager.getApplicationConfiguration().fromXml(lsssConfig.applicationXml.getRootElement());
            }
            configurationManager.getSurveyConfiguration().loadDefault();
         }

         closeStartupDialog();

         if (lsssConfig.loadSettings && lsssConfig.applicationXml == null) {
            GuiUtils.invokeNowOrWait(() -> new ApplicationSetupWizard(this).show());
            configurationManager.getApplicationConfiguration().saveDefault();
         }

         if (lsssConfig.isPrimaryLSSS) {
            packageManager.dispatchCallbackEvent(LsssCallbackEvent.applicationStart);
         }
      } finally {
         closeStartupDialog();
      }
   }

   private void closeStartupDialog() {
      SwingUtilities.invokeLater(() -> {
         if (startupDialog != null) {
            startupDialog.dispose();
            startupDialog = null;
         }
      });
   }

   public boolean hasStartupDialog() {
      return startupDialog != null;
   }

   public void setStartupText(String text) {
      SwingUtilities.invokeLater(() -> {
         if (startupDialog != null) {
            startupDialog.setText(text);
         }
      });
   }

   private void onApplicationStart(String[] args) {
      List<Path> files = new ArrayList<>();
      for (String arg : args) {
         if (arg.startsWith("--")) {
            continue;
         }
         Path file;
         try {
            file = Path.of(arg);
         } catch (Exception e) {
            showError("Cannot parse command line argument \"" + arg + "\" as a file:\n" + e.getMessage());
            continue;
         }
         if (arg.endsWith(SurveyManager.SURVEY_FILE_SUFFIX)) {
            surveyManager.open(file);
            return;
         }
         files.add(file);
      }
      if (!files.isEmpty()) {
         configurationManager.getDataConf().selectFiles(files);
         configurationManager.showDialog(configurationManager.getDataConf());
         return;
      }

      switch (configurationManager.getAppMiscConf().onApplicationStart.getValue()) {
         case DO_NOTHING -> {
         }
         case OPEN_LAST_SURVEY -> {
            Path lastSurveyFile = surveyManager.getLastSurveyFile();
            if (lastSurveyFile != null && surveyManager.getLastSurveyFileOpen()) {
               surveyManager.open(lastSurveyFile);
            }
         }
         case SHOW_OPEN_SURVEY_DIALOG -> {
            surveyManager.open();
         }
      }
   }

   public LsssConfig getLsssConfig() {
      return lsssConfig;
   }

   public ConfigurationManager getConfigurationManager() {
      return configurationManager;
   }

   public LsssAccess getLsssAccess() {
      return lsssAccess;
   }

   public PluginManager getPluginManager() {
      return pluginManager;
   }

   public DatabaseManager getDatabaseManager() {
      return databaseManager;
   }

   public DataSetManager getDataSetManager() {
      return dataSetManager;
   }

   public DataManager getDataManager() {
      return dataSetManager.getDataManager();
   }

   public SurveyManager getSurveyManager() {
      return surveyManager;
   }

   public @Nullable MainDisplay getDisplay() {
      MainDisplay theDisplay = display;
      if (theDisplay == null && lsssConfig.visible) {
         theDisplay = GuiUtils.getNowOrWait(() -> display);
      }
      return theDisplay;
   }

   public @Nullable JFrame getFrame() {
      MainDisplay theDisplay = getDisplay();
      return theDisplay != null ? theDisplay.getFrame() : null;
   }

   public @Nullable Component getReferenceComponent() {
      return configurationManager.isShowing() ? configurationManager.getDialog() : getFrame();
   }

   public RegionManager getRegionManager() {
      return regionManager;
   }

   public InterpretationSettings getInterpretationSettings() {
      return interpretationSettings;
   }

   public InterpretationSummary getInterpretationSummary() {
      return interpretationSummary;
   }

   public ModuleManager getModuleManager() {
      return moduleManager;
   }

   public ExportManager getExportManager() {
      return exportManager;
   }

   public PackageManager getPackageManager() {
      return packageManager;
   }

   public Actions getActions() {
      return actions;
   }

   public Korona getKorona() {
      return korona;
   }

   public HelpSystem getHelpSystem() {
      return helpSystem;
   }

   /**
    * Returns the directory where application data for LSSS should be stored.
    * Application data should not be stored to the installation directory, {@link #getInstallationDir()}
    * since the user might not have write permissions there.
    *
    * @return the LSSS application data directory
    */
   public static Path getApplicationDataDir() {
      return LOGGING_MANAGER.getApplicationDataDir();
   }

   /**
    * Returns the installation directory of LSSS.
    * Application data should not be stored to this directory since the user might not have write permissions.
    *
    * @return the installation directory of LSSS
    */
   public static Path getInstallationDir() {
      return LOGGING_MANAGER.getInstallationDir();
   }

   public Preferences getPreferences(String nodeName) {
      return Preferences.userRoot().node(lsssConfig.preferencesNode).node(nodeName);
   }

   public void showError(String message) {
      showError(message, null);
   }

   public void showError(String message, @Nullable Throwable throwable) {
      showError(getReferenceComponent(), message, throwable);
   }

   public void showError(@Nullable Component referenceComponent, String message) {
      showError(referenceComponent, message, null);
   }

   public void showError(@Nullable Component referenceComponent, String message, @Nullable Throwable throwable) {
      if (interpretationSettings.isInteractiveMode()) {
         GuiUtils.showErrorDialog(referenceComponent, message, throwable);
      } else {
         Log.global.log(Level.WARNING, message, throwable);
      }
   }

   public void close() {
      if (lsssConfig.isPrimaryLSSS) {
         packageManager.dispatchCallbackEvent(LsssCallbackEvent.applicationExit);
         PackagesConf packagesConf = configurationManager.getAppMiscConf().getPackagesConf();
         if (!packagesConf.getAsyncHandle().isFinished()) {
            new WorkerDialog(getFrame(), "Waiting for callbacks to " + LsssCallbackEvent.applicationExit)
                  .setDelay(0) // Launch dialog immediately to avoid blocking api calls using LsssServerUtils.doInGuiThread
                  .start(asyncHandle -> {
                     while (!asyncHandle.isCancelled() && !packagesConf.getAsyncHandle().isFinished()) {
                        asyncHandle.sleep(10);
                     }
                  });
         }
         lsssConfig.serviceCollection.close();
      }
      pluginManager.close();
      surveyManager.close();
      regionManager.close();
      interpretationSettings.close();
      databaseManager.close();
      moduleManager.close();
      SwingUtilities.invokeLater(() -> {
         if (display != null) {
            display.close();
         }
      });
      helpSystem.close();
      lsssConfig.onClose.run();
   }

   private static @Nullable Document loadApplicationXml() {
      Path file = ApplicationConfiguration.getConfigFile(ApplicationConfiguration.CONFIG_FILE_NAME);
      try {
         return XmlUtils.readDocumentIfExists(file);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error loading " + file, e);
      }
      return null;
   }

   private static Set<String> getDeactivatedPlugins(ServiceCollection serviceCollection, Element applicationXml) {
      return XmlUtils.getFirstWithAttribute(applicationXml.elements(ConfigurationUnit.XML_UNIT), ConfigurationUnit.XML_NAME, PluginConf.NAME.persistentName())
            .map(pluginElement -> PluginConf.getDeactivatedPlugins(serviceCollection, pluginElement))
            .orElse(Set.of());
   }

   /**
    * Starts LSSS.
    *
    * @param args the files or directories to open
    */
   public static void main(String[] args) {
      long t0 = System.currentTimeMillis();

      Utils.init(args, LsssResource.LSSS_64);
      if (Utils.isTestRun()) {
         LsssSmoke.main(args);
         return;
      }

      LOGGING_MANAGER.startLogging();

      ServiceCollection serviceCollection = new ServiceCollection();

      Integer serverPort = null;
      if (args.length > 0) {
         OptionParser parser = new OptionParser();
         parser.allowsUnrecognizedOptions();
         ArgumentAcceptingOptionSpec<Integer> serverPortOption = parser.accepts("server-port")
               .withRequiredArg()
               .ofType(Integer.class);
         try {
            OptionSet optionSet = parser.parse(args);
            serverPort = optionSet.valueOf(serverPortOption);
         } catch (Exception e) {
            Log.global.warning("Error parsing command line options: " + e.getMessage());
         }
      }

      Document applicationXml = loadApplicationXml();
      if (applicationXml != null) {
         Set<String> deactivatedPlugins = getDeactivatedPlugins(serviceCollection, applicationXml.getRootElement());
         serviceCollection.removeFeatures(deactivatedPlugins);

         if (serverPort != null) {
            Node serverPortNode = new ApplicationConfigurationXml(applicationXml).lsssServerPortNode();
            if (serverPortNode != null) {
               serverPortNode.setText(serverPort.toString());
            }
         }
      }

      LsssConfig lsssConfig = new LsssConfig(serviceCollection);
      lsssConfig.applicationXml = applicationXml;
      lsssConfig.onClose = LOGGING_MANAGER::shutDown;
      if (serverPort != null) {
         lsssConfig.serverPort = serverPort;
      }
      LSSS lsss = new LSSS(lsssConfig);

      SwingUtilities.invokeLater(() -> {
         lsss.getConfigurationManager().getApplicationConfiguration().getAppPreprocessingConf().doStartupChecks();

         long t1 = System.currentTimeMillis();
         Log.global.info("Startup time: " + (t1 - t0) / 1000f + " sec");

         lsss.onApplicationStart(args);
      });
   }
}
