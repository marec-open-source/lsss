package no.imr.lsss.framework.config.application;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.SurveyManager;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.Utils;
import no.imr.tools.io.FilePredicates;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * Directory templates.
 */
public final class DirectoryConf extends ConfigurationUnit {
   public static final String DEFAULT_DIR_STRUCTURE = "default";

   public final FileParameter mainDir = new FileParameter(
         new Name("MainDir"),
         null, FileParameter.Mode.DIRECTORY);

   public final ObjectParameter<String> surveyDirStructure = new ObjectParameter<>(
         new Name("SurveyDirStructure"),
         DEFAULT_DIR_STRUCTURE);

   public final ObjectParameter<String> backupDirStructure = new ObjectParameter<>(
         new Name("BackupDirStructure"),
         DEFAULT_DIR_STRUCTURE);

   public final FileParameter backupDestinationDir = new FileParameter(
         new Name("BackupDestinationDir"),
         null, FileParameter.Mode.DIRECTORY);

   public final BooleanParameter advanced = new BooleanParameter(
         new Name("Advanced", ""),
         false,
         "Optional advanced configuration (specify individual directories)");

   private final Map<SubDir, MainDirectory> subDirs = new LinkedHashMap<>();

   private @Nullable SurveyDirStructure selectedSurveyDirStructure;
   private @Nullable SurveyDirStructure selectedBackupDirStructure;

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   DirectoryConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("DirectoryConf", "Directories"),
            "Configuration of default location for new surveys");

      mainDir.setConstraintAndValue(optional -> {
         return optional.isEmpty() ? "Must be specified" : null;
      }, Optional.of(getDefaultMainDir()));

      updateAllowedSurveyDirStructures();

      surveyDirStructure.subscribe(__ -> {
         selectedSurveyDirStructure = null;
         viewHolder.ifView(View::update);
      });
      backupDirStructure.subscribe(__ -> {
         selectedBackupDirStructure = null;
         viewHolder.ifView(View::update);
      });
      advanced.subscribe(viewHolder.coalescingListener(View::update));

      for (FeaturePlugin featurePlugin : getLSSS().getPluginManager().getFeaturePlugins()) {
         for (SubDir subDir : featurePlugin.getSubDirs()) {
            MainDirectory mainDirectory = new MainDirectory(subDir.mainDirParameterName());
            subDirs.put(subDir, mainDirectory);
         }
      }
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            mainDir,
            surveyDirStructure,
            backupDirStructure,
            backupDestinationDir,
            advanced
      );
   }

   public FileParameter getMainDirectoryParameter(SubDir subDir) {
      return subDirs.get(subDir);
   }

   @Override
   public JComponent getComponent() {
      return viewHolder.getComponent();
   }

   @Override
   public boolean stopEditing() {
      return !viewHolder.hasView() || viewHolder.getView().stopEditing();
   }

   @Override
   public boolean prepareApply() {
      return stopEditing();
   }

   @Override
   public void removeView() {
      viewHolder.removeView();
   }

   public static Path getSurveyDirStructureDir() {
      return LSSS.getInstallationDir().resolve("data").resolve("surveyDirStructure");
   }

   private static List<String> findAllowedDirStructures() {
      NavigableSet<String> allowedValues = new TreeSet<>();
      allowedValues.add(DEFAULT_DIR_STRUCTURE);
      try {
         for (Path file : FileUtils.listFiles(getSurveyDirStructureDir(), FilePredicates.endsWith(".xml"))) {
            String name = FileUtils.baseName(file);
            allowedValues.add(name);
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
      return List.copyOf(allowedValues);
   }

   private void updateAllowedSurveyDirStructures() {
      List<String> allowedValues = findAllowedDirStructures();
      surveyDirStructure.setAllowedValuesAndPossiblyValue(allowedValues, DEFAULT_DIR_STRUCTURE);
      backupDirStructure.setAllowedValuesAndPossiblyValue(allowedValues, DEFAULT_DIR_STRUCTURE);
   }

   private SurveyDirStructure loadSelectedSurveyDirStructure() {
      try {
         return loadSurveyDirStructure(surveyDirStructure.getValue());
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error loading survey directory structure " + surveyDirStructure.getValue(), e);
         surveyDirStructure.setValue(DEFAULT_DIR_STRUCTURE);
         return defaultSurveyDirStructure();
      }
   }

   private SurveyDirStructure loadSelectedBackupDirStructure() {
      try {
         return loadSurveyDirStructure(backupDirStructure.getValue());
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error loading survey directory structure " + backupDirStructure.getValue(), e);
         backupDirStructure.setValue(DEFAULT_DIR_STRUCTURE);
         return defaultSurveyDirStructure();
      }
   }

   private SurveyDirStructure defaultSurveyDirStructure() {
      return new SurveyDirStructure(subDirs.keySet());
   }

   private SurveyDirStructure loadSurveyDirStructure(String fileName) throws IOException {
      SurveyDirStructure structure = defaultSurveyDirStructure();
      if (!fileName.equals(DEFAULT_DIR_STRUCTURE)) {
         structure.read(getSurveyDirStructureDir().resolve(fileName + ".xml"));
      }
      return structure;
   }

   public SurveyDirStructure getSelectedBackupDirStructure() {
      SurveyDirStructure dirStructure = selectedBackupDirStructure;
      if (dirStructure == null) {
         dirStructure = loadSelectedBackupDirStructure();
         selectedBackupDirStructure = dirStructure;
      }
      return dirStructure;
   }

   public SurveyDirStructure getSelectedSurveyDirStructure() {
      SurveyDirStructure dirStructure = selectedSurveyDirStructure;
      if (dirStructure == null) {
         dirStructure = loadSelectedSurveyDirStructure();
         selectedSurveyDirStructure = dirStructure;
      }
      return dirStructure;
   }

   public List<SurveyDirStructure> getAllSurveyDirStructures() {
      List<SurveyDirStructure> surveyDirStructures = new ArrayList<>();
      for (String fileName : surveyDirStructure.getAllowedValues()) {
         try {
            surveyDirStructures.add(loadSurveyDirStructure(fileName));
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error loading survey directory structure " + fileName, e);
         }
      }
      return surveyDirStructures;
   }

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      return UserProfile.ADMINISTRATOR_MODE;
   }

   public Path getMainDir() {
      Path dir = mainDir.getFile();
      return dir != null ? dir : getDefaultMainDir();
   }

   private static Path getDefaultMainDir() {
      Path dir = File.separatorChar == '/'
            ? Utils.getUserHome()
            : Path.of("C:\\");
      return dir.resolve(SurveyManager.LSSS_DATA_DIR_NAME);
   }

   public @Nullable Path getSurveySubDir(Path surveyDir, SubDir subDir) {
      Survey survey = getConfigurationManager().getSurveyConf().getSurvey();
      if (survey == null) {
         return null;
      }
      FileParameter mainDirectoryParameter = getMainDirectoryParameter(subDir);
      Path mainDirectoryParameterFile = advanced.getBooleanValue() ? mainDirectoryParameter.getFile() : null;
      if (mainDirectoryParameterFile != null) {
         return mainDirectoryParameterFile.resolve(getSelectedSurveyDirStructure().getSurveyDirName(survey));
      } else {
         return surveyDir.resolve(getSelectedSurveyDirStructure().getRelativePath(subDir));
      }
   }

   private final class MainDirectory extends FileParameter {
      private MainDirectory(Name name) {
         super(name, null, FileParameter.Mode.DIRECTORY);
      }

      @Override
      public void customizeFileChooser(JFileChooser fileChooser) {
         if (getFile() == null) {
            fileChooser.setCurrentDirectory(getMainDir().toFile());
         }
      }
   }

   private static final class View implements ViewHolder.View {
      private final DirectoryConf directoryConf;
      private final JPanel panel = new JPanel(new BorderLayout());
      private List<ParameterEditor> parameterEditors = List.of();

      private View(DirectoryConf directoryConf) {
         this.directoryConf = directoryConf;
      }

      @Override
      public JComponent getComponent() {
         update();
         return panel;
      }

      private boolean stopEditing() {
         return parameterEditors.stream().allMatch(ParameterEditor::commitEdits);
      }

      private void update() {
         List<Component> components = new ArrayList<>();

         components.add(createInfoComponent("<h2>Default directories</h2>"
               + "<p>When a new survey is configured the actual directories used are subdirectories to <b>" + directoryConf.mainDir.getDisplayName() + "</b>."
               + "</p>"

               + "<p>Example: If the survey number is 2006123 and the platform has name TestPlatform and number 1234"
               + " then the following directories will be used:"
               + "</p>"

               + "<br><table border=1 align=center><tr>"
               + "<td>" + directoryConf.getConfigurationManager().getDataConf().getRawDir().getDisplayName() + "</td>"
               + "<td><code>&lt;" + directoryConf.mainDir.getDisplayName() + ">" + File.separator + "S2006123PTestPlatform[1234]" + File.separator + directoryConf.getSelectedSurveyDirStructure().getRelativePath(DataConfLSSS.RAW_SUB_DIR) + "</code></td>"
               + "</tr><tr>"
               + "<td>" + directoryConf.getConfigurationManager().getDataConf().getProcessedDir().getDisplayName() + "</td>"
               + "<td><code>&lt;" + directoryConf.mainDir.getDisplayName() + ">" + File.separator + "S2006123PTestPlatform[1234]" + File.separator + directoryConf.getSelectedSurveyDirStructure().getRelativePath(DataConfLSSS.KORONA_SUB_DIR) + "</code></td>"
               + "</tr><tr>"
               + "<td>" + directoryConf.getConfigurationManager().getDataConf().getDir(DataConfLSSS.TRAWL_SUB_DIR).getDisplayName() + "</td>"
               + "<td><code>&lt;" + directoryConf.mainDir.getDisplayName() + ">" + File.separator + "S2006123PTestPlatform[1234]" + File.separator + directoryConf.getSelectedSurveyDirStructure().getRelativePath(DataConfLSSS.TRAWL_SUB_DIR) + "</code></td>"
               + "</tr><tr>"
               + "<td>...</td>"
               + "<td>...</td>"
               + "</tr></table>"

               + "<p>Notes:"
               + "<br>&nbsp; - If <b>" + directoryConf.mainDir.getDisplayName() + "</b> is left blank it defaults to the user home directory: " + Utils.getUserHome()
               + "<br>&nbsp; - In MS-Windows a directory must include the drive, e.g. \"C:\\\"."
               + "</p>"));

         directoryConf.updateAllowedSurveyDirStructures();

         ParameterEditor parameterEditor = directoryConf.createParameterEditor(List.of(
               directoryConf.mainDir,
               directoryConf.surveyDirStructure,
               directoryConf.backupDirStructure,
               directoryConf.backupDirStructure,
               directoryConf.backupDestinationDir
         ));
         parameterEditors = List.of(parameterEditor);
         JComponent editorComponent = parameterEditor.getEditorComponent();
         editorComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
         components.add(editorComponent);

         JSeparator separator = new JSeparator();
         components.add(separator);

         ParameterEditor advancedParameterEditor = directoryConf.createParameterEditor(List.of(directoryConf.advanced));
         components.add(advancedParameterEditor.getEditorComponent());

         if (directoryConf.advanced.getBooleanValue()) {
            FileParameter mainTrawlDir = directoryConf.getMainDirectoryParameter(DataConfLSSS.TRAWL_SUB_DIR);
            FileParameter mainCtdDir = directoryConf.getMainDirectoryParameter(DataConfLSSS.CTD_SUB_DIR);

            components.add(createInfoComponent("<br><h2>Optional advanced configuration</h2>"
                  + "<p>Each of the directories below can optionally be specified."
                  + " For directories not specified the rule above is applied."
                  + "</p>"

                  + "<p>Example (cont.): If <b>" + mainTrawlDir.getDisplayName() + "</b> is specified and"
                  + " <b>" + mainCtdDir.getDisplayName() + "</b> is not specified"
                  + " then the following directories will be used:"
                  + "</p>"

                  + "<br><table border=1 align=center><tr>"
                  + "<td>" + directoryConf.getConfigurationManager().getDataConf().getDir(DataConfLSSS.TRAWL_SUB_DIR).getDisplayName() + "</td>"
                  + "<td><code>&lt;" + mainTrawlDir.getDisplayName() + ">" + File.separator + "S2006123PTestPlatform[1234]" + "</code></td>"
                  + "</tr><tr>"
                  + "<td>" + directoryConf.getConfigurationManager().getDataConf().getDir(DataConfLSSS.CTD_SUB_DIR).getDisplayName() + "</td>"
                  + "<td><code>&lt;" + directoryConf.mainDir.getDisplayName() + ">" + File.separator + "S2006123PTestPlatform[1234]" + File.separator + directoryConf.getSelectedSurveyDirStructure().getRelativePath(DataConfLSSS.CTD_SUB_DIR) + "</code></td>"
                  + "</tr></table>"));

            List<BaseParameter<?>> parameters = new ArrayList<>();
            for (FeaturePlugin plugin : directoryConf.getLSSS().getPluginManager().getFeaturePlugins()) {
               List<MainDirectory> mainDirectories = plugin.getSubDirs().stream()
                     .map(directoryConf.subDirs::get)
                     .filter(Objects::nonNull)
                     .toList();
               if (mainDirectories.isEmpty()) {
                  continue;
               }
               if (!(plugin instanceof BaseSystemFeaturePlugin)) {
                  parameters.add(SeparatorParameter.line());
                  parameters.add(new HeaderParameter(plugin.getName().displayName(), plugin.getIcon()));
               }
               parameters.addAll(mainDirectories);
            }
            ParameterEditor advancedEditor = directoryConf.createParameterEditor(parameters);
            parameterEditors = List.of(parameterEditor, advancedEditor);
            JComponent advancedComponent = advancedEditor.getEditorComponent();
            advancedComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
            components.add(advancedComponent);
         }

         GuiUtils.replaceContent(panel, GuiUtils.createScrollPane(components));
      }
   }
}
