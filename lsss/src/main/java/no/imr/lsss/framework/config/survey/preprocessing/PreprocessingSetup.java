package no.imr.lsss.framework.config.survey.preprocessing;

import com.google.common.collect.ImmutableList;
import no.imr.korona.apps.relay.KoronaRelay;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsUtils;
import no.imr.korona.config.gui.ContextVisibility;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.util.CfsManager;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.SurveyFileParameter;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.range.Range;
import no.imr.tools.smoke.SmokeTestException;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.swing.icons.MiscIcons;
import no.marec.lsss.api.util.observing.Subscription;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Preprocessing setup of a {@link ConfigFileSettings}.
 */
public final class PreprocessingSetup implements ParameterContainer {
   public final StringParameter comment = new StringParameter(new Name("Comment"));
   public final CfsParameter cfsFile;
   public final FileParameter sourceDirectory;
   private final SyncParameter sourceDirectorySynchronization;
   public final FileParameter destinationDirectory;
   private final SyncParameter destinationDirectorySynchronization;

   private final PreprocessingConf preprocessingConf;
   private final LSSS lsss;
   private final CfsManager cfsManager;

   PreprocessingSetup(PreprocessingConf preprocessingConf) {
      this.preprocessingConf = preprocessingConf;
      lsss = preprocessingConf.getLSSS();

      comment.setProperty(BaseParameter.KEY_LEFT_ALIGNED, true);

      cfsFile = new CfsParameter(lsss);

      List<Optional<FileParameter>> allowedSyncSources = getAllowedSyncSources(preprocessingConf);

      sourceDirectory = new SurveyFileParameter(lsss,
            new Name("SourceDirectory", "Source directory"),
            FileParameter.Mode.DIRECTORY) {
         @Override
         public List<Component> makeExtraGuiComponents() {
            return List.of(makeSynchronizationComponent(preprocessingConf, sourceDirectorySynchronization, allowedSyncSources));
         }

         @Override
         public void customizeFileChooser(JFileChooser fileChooser) {
            fileChooser.setApproveButtonText("Select directory");
            ConfigFileSettingsUtils.installTooltip(fileChooser);
         }
      };

      sourceDirectorySynchronization = new SyncParameter(
            new Name("SourceDirectorySynchronization"),
            sourceDirectory, allowedSyncSources);

      destinationDirectory = new SurveyFileParameter(lsss,
            new Name("DestinationDirectory", "Destination directory"),
            FileParameter.Mode.DIRECTORY) {
         @Override
         public List<Component> makeExtraGuiComponents() {
            return List.of(makeSynchronizationComponent(preprocessingConf, destinationDirectorySynchronization, allowedSyncSources));
         }

         @Override
         public void customizeFileChooser(JFileChooser fileChooser) {
            fileChooser.setApproveButtonText("Select directory");
            ConfigFileSettingsUtils.installTooltip(fileChooser);
         }
      };

      destinationDirectorySynchronization = new SyncParameter(
            new Name("DestinationDirectorySynchronization"),
            destinationDirectory, allowedSyncSources);

      cfsManager = new CfsManager(lsss.getKorona(), cfsFile, preprocessingConf.getContext(), ContextVisibility.HIDE);
      cfsManager.setDefaultBrowseDirectorySupplier(lsss.getSurveyManager().getLsssReferenceDirectory()::getFile);
      cfsManager.setDataFileLabellingSupplier(preprocessingConf.getDataConf()::getDataFileLabelling);

      extendPopupMenu(preprocessingConf, sourceDirectory);
      extendPopupMenu(preprocessingConf, destinationDirectory);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            comment,
            cfsFile,
            sourceDirectory,
            sourceDirectorySynchronization,
            destinationDirectory,
            destinationDirectorySynchronization
      );
   }

   private static List<Optional<FileParameter>> getAllowedSyncSources(PreprocessingConf preprocessingConf) {
      ImmutableList.Builder<Optional<FileParameter>> builder = ImmutableList.<Optional<FileParameter>>builder()
            .add(Optional.empty())
            .add(Optional.of(preprocessingConf.getDataConf().getRawDir()));
      preprocessingConf.getDataConf().getAllPreprocessingTargetDirs().stream()
            .<Optional<FileParameter>>map(Optional::of)
            .forEach(builder::add);
      return builder.build();
   }

   private static JComponent makeSynchronizationComponent(PreprocessingConf preprocessingConf, SyncParameter syncParameter, List<Optional<FileParameter>> allowedSyncSources) {
      JMenu menu = new JMenu();
      WhenShowingListening.connect(menu, syncParameter, () -> {
         String text;
         Color color;
         String toolTipText;
         FileParameter syncedParameter = syncParameter.getValue().orElse(null);
         if (syncedParameter != null) {
            text = "🔒";
            color = ColorUtils.FORESTGREEN;
            toolTipText = "Synced from <b>" + syncedParameter.getDisplayName() + "</b>";
         } else {
            text = "🔓";
            color = Color.BLACK;
            toolTipText = "Not synced";
         }
         menu.setText(text);
         menu.setForeground(color);
         menu.setToolTipText("<html>" + toolTipText);
      });
      GuiUtils.autoCreateContentMenu(menu, () -> {
         allowedSyncSources.forEach(syncSource -> {
            JMenuItem item = menu.add(new JMenuItem());
            syncSource.ifPresentOrElse(fileParameter -> {
               item.setText(fileParameter.getDisplayName());
               Path file = fileParameter.getFile();
               item.setToolTipText(file != null ? file.toString() : "<Not specified>");
            }, () -> {
               item.setText("Not synced");
            });
            MiscIcons.check(syncParameter.getValue().equals(syncSource)).on(item);
            item.setEnabled(preprocessingConf.isParameterEnabled(syncParameter));
            item.addActionListener(_ -> syncParameter.setValue(syncSource));
         });
      });
      JMenuBar menuBar = new JMenuBar();
      menuBar.add(menu);
      return menuBar;
   }

   private static void extendPopupMenu(PreprocessingConf preprocessingConf, FileParameter parameter) {
      parameter.setPopupMenuExtender(menu -> {
         menu.addSeparator();
         addSetFromItem(menu, preprocessingConf, parameter, preprocessingConf.getDataConf().getRawDir());
         preprocessingConf.getDataConf().getAllPreprocessingTargetDirs().forEach(targetDir -> {
            addSetFromItem(menu, preprocessingConf, parameter, targetDir);
         });
      });
   }

   private static void addSetFromItem(JPopupMenu popupMenu, PreprocessingConf preprocessingConf, FileParameter parameter, FileParameter fromParameter) {
      Path file = fromParameter.getFile();
      JMenuItem item = popupMenu.add("Set to " + fromParameter.getDisplayName() + ": " + (file != null ? file.toString() : "<Not specified>"));
      item.setEnabled(preprocessingConf.isParameterEnabled(parameter));
      item.addActionListener(_ -> parameter.setFile(file));
   }

   /**
    * Starts {@link KoronaRelay}.
    * <p>
    * Arguments are set in environment since command line arguments containing spaces may not be passed correctly.
    *
    * @param selectedFiles files to select
    */
   public void start(Optional<Range<SegmentHandle>> selectedFiles) {
      if (Utils.IS_DIST_VERSION) {
         startExternalProcess(selectedFiles);
      } else {
         startInternalProcess(selectedFiles);
      }
   }

   private void startExternalProcess(Optional<Range<SegmentHandle>> selectedFiles) {
      Path startupScript = KoronaRelay.LOGGING_MANAGER.getStartupScript();

      ProcessBuilder processBuilder = new ProcessBuilder(startupScript.toString())
            .directory(startupScript.getParent().toFile());
      defineEnvironment(processBuilder.environment(), selectedFiles);

      Exec.CACHED_THREAD_POOL.execute(() -> runKoronaRelay(processBuilder));
   }

   private void startInternalProcess(Optional<Range<SegmentHandle>> selectedFiles) {
      Map<String, String> env = new HashMap<>();
      defineEnvironment(env, selectedFiles);
      KoronaRelay.start(env);
   }

   private void defineEnvironment(Map<String, String> env, Optional<Range<SegmentHandle>> selectedFiles) {
      env.put(KoronaRelay.ARG_COMMENT, comment.getValue());
      env.put(KoronaRelay.ARG_CONFIG_FILE_SETTINGS, cfsFile.getStringValue());
      env.put(KoronaRelay.ARG_SOURCE, sourceDirectory.getStringValue());
      env.put(KoronaRelay.ARG_DESTINATION, destinationDirectory.getStringValue());
      selectedFiles.ifPresent(range -> {
         env.put(KoronaRelay.ARG_FIRST_SELECTED_FILE, range.begin().getBaseName());
         env.put(KoronaRelay.ARG_LAST_SELECTED_FILE, range.end().getBaseName());
      });
      Path dataFileLabellingFile = preprocessingConf.getDataConf().getDataFileLabellingFile();
      if (dataFileLabellingFile != null) {
         env.put(KoronaRelay.ARG_DATA_FILE_LABELLING_FILE, dataFileLabellingFile.toString());
      }
   }

   public void smokeTest() {
      if (!Utils.IS_DIST_VERSION) {
         Log.global.info("KORONA relay: Skipped");
         return;
      }
      Path startupScript = KoronaRelay.LOGGING_MANAGER.getStartupScript();
      ProcessBuilder processBuilder = new ProcessBuilder(startupScript.toString(), "--smoke-test")
            .directory(startupScript.getParent().toFile());
      int exitCode = runKoronaRelay(processBuilder);
      if (exitCode != 0) {
         throw new SmokeTestException("KORONA relay exit code: " + exitCode);
      }
      Log.global.info("KORONA relay: ok");
   }

   Element toXml() {
      return new ParameterCollection(this).toXml();
   }

   void fromXml(Element parametersElement) {
      new ParameterCollection(this).fromXml(parametersElement);
   }

   private int runKoronaRelay(ProcessBuilder processBuilder) {
      processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
      processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

      Process process;
      try {
         process = processBuilder.start();

         // The startup script may pause on error, so make sure some new lines are available.
         process.getOutputStream().write("\n\n\n\n".getBytes(Utils.nativeCharset()));
         process.getOutputStream().flush();
      } catch (IOException e) {
         lsss.showError("Error running KoronaRelay", e);
         return -1;
      }

      try {
         process.waitFor();
      } catch (InterruptedException _) {
         Thread.currentThread().interrupt();
         return -2;
      }

      int exitCode = process.exitValue();
      if (exitCode != 0) {
         SwingUtilities.invokeLater(() -> lsss.showError("Preprocessing process returned with exit code " + exitCode));
      }
      return exitCode;
   }

   public ConfigFileSettings createConfigFileSettings() throws IOException {
      if (cfsFile.getFile() != null) {
         return cfsManager.loadConfigFileSettings();
      } else {
         return lsss.getKorona().createConfigFileSettings();
      }
   }

   public ModuleContainer createModuleContainer() throws IOException {
      return cfsManager.loadModuleContainer();
   }

   public final class CfsParameter extends ConfigFileSettingsParameter {
      private CfsParameter(LSSS lsss) {
         super(lsss, new Name("ConfigFileSettings", "Config file settings"));
      }

      @Override
      public void customizeFileChooser(JFileChooser fileChooser) {
         fileChooser.setDialogTitle("Select config file settings");
      }

      @Override
      public Editor getEditor() {
         return cfsManager.createCfsEditor();
      }

      @Override
      public Copier getCopier() {
         return cfsManager.createCfsCopier();
      }
   }

   private static final class SyncParameter extends ObjectParameter<Optional<FileParameter>> {
      private final FileParameter syncDestinationParameter;
      private @Nullable Subscription subscription;

      private SyncParameter(Name name, FileParameter syncDestinationParameter, List<Optional<FileParameter>> syncSources) {
         super(name, Optional.empty(), syncSources);

         this.syncDestinationParameter = syncDestinationParameter;
         syncDestinationParameter.subscribe(this::onSyncDestinationChange);
         subscribe(this::onSyncSourceChange);

         setVisible(false);
      }

      private synchronized void onSyncDestinationChange(Optional<Path> optSyncDestination) {
         FileParameter source = getValue().orElse(null);
         if (source != null && !source.getValue().equals(optSyncDestination)) {
            setValue(Optional.empty());
         }
      }

      private synchronized void onSyncSourceChange(Optional<FileParameter> optSource) {
         if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
         }
         FileParameter source = optSource.orElse(null);
         if (source != null) {
            subscription = source.subscribe(syncDestinationParameter);
            syncDestinationParameter.setValue(source.getValue());
         }
      }

      @Override
      public String toString(Optional<FileParameter> value) {
         return value.map(FileParameter::getPersistentName).orElse("");
      }
   }
}
