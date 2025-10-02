package no.imr.lsss.framework.config.survey.preprocessing;

import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsContext;
import no.imr.korona.config.ConfigFileSettingsUtils;
import no.imr.korona.config.gui.ConfigFileSettingsEditor;
import no.imr.korona.config.gui.ContextVisibility;
import no.imr.korona.resources.KoronaHelp;
import no.imr.lsss.LSSS;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class PreprocessingConfView implements ViewHolder.View {
   private final PreprocessingConf preprocessingConf;
   private final JScrollPane scrollPane;
   private final JPanel setupsPanel = new JPanel(new BorderLayout());

   PreprocessingConfView(PreprocessingConf preprocessingConf) {
      this.preprocessingConf = preprocessingConf;

      GridBag gridBag = new GridBag()
            .activateHorizontalFill();
      OnTheFlySetup onTheFlySetup = preprocessingConf.getOnTheFlySetup();
      if (onTheFlySetup != null) {
         ParameterEditor onTheFlyParameterEditor = new ParameterEditor(onTheFlySetup.getParameters());
         onTheFlyParameterEditor.getGUIConfig().setParameterEnabledDecider(preprocessingConf::isParameterEnabled);
         gridBag.addWithLineBreak(onTheFlyParameterEditor.getEditorComponent());
         addSeparator(gridBag);
      }
      gridBag.addWithLineBreak(setupsPanel);
      updateSetups();
      gridBag.addWithLineBreak(createButtonsPanel());
      gridBag.addWithLineBreak(Box.createVerticalStrut(10));
      gridBag.addWithLineBreak(createWizardButtonPanel());
      scrollPane = GuiUtils.createScrollPane(gridBag.getPanel());
   }

   @Override
   public JComponent getComponent() {
      return scrollPane;
   }

   void updateSetups() {
      GridBag gridBag = new GridBag()
            .activateHorizontalFill();
      for (PreprocessingSetup preprocessingSetup : preprocessingConf.getPreprocessingSetups()) {
         gridBag.addWithLineBreak(new PreprocessingSetupGUI(preprocessingSetup, preprocessingConf).getComponent());
         addSeparator(gridBag);
      }
      GuiUtils.replaceContent(setupsPanel, gridBag.getPanel());
   }

   private static void addSeparator(GridBag gridBag) {
      gridBag.addWithLineBreak(Box.createVerticalStrut(10));
      gridBag.addWithLineBreak(new JSeparator());
      gridBag.addWithLineBreak(Box.createVerticalStrut(10));
   }

   private JComponent createButtonsPanel() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(createHelpButton(), BorderLayout.WEST);
      panel.add(createAddButton(), BorderLayout.EAST);
      return panel;
   }

   private JComponent createWizardButtonPanel() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(createCopyButton(), BorderLayout.EAST);
      return panel;
   }

   private JButton createHelpButton() {
      JButton button = MiscIcons.HELP.on(new JButton("Show preprocessing help system"));
      button.addActionListener(e -> preprocessingConf.getKoronaHelpID().show());
      return button;
   }

   private JButton createAddButton() {
      JButton button = MiscIcons.ADD.on(new JButton("Add preprocessing setup"));
      button.addActionListener(e -> {
         PreprocessingSetup preprocessingSetup = preprocessingConf.createPreprocessingSetup();
         preprocessingSetup.fromXml(preprocessingConf.getPreprocessingSetups().getLast().toXml());
         preprocessingSetup.comment.setValue("");
         incrementDestinationDirectory(preprocessingSetup);
         preprocessingConf.getPreprocessingSetups().add(preprocessingSetup);
         updateSetups();
         SwingUtilities.invokeLater(() -> {
            JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
            verticalScrollBar.setValue(verticalScrollBar.getMaximum());
         });
      });
      return button;
   }

   private JButton createCopyButton() {
      JButton button = MiscIcons.COPY.on(new JButton("Copy more config files..."));
      Path surveyFile = preprocessingConf.getLSSS().getSurveyManager().getSurveyFile();
      if (surveyFile != null) {
         button.addActionListener(e -> {
            showCopyConfigFilesDialog(preprocessingConf.getLSSS(), preprocessingConf.getContext(), surveyFile.getParent());
         });
      } else {
         button.setEnabled(false);
      }
      return button;
   }

   private static void showCopyConfigFilesDialog(LSSS lsss, ConfigFileSettingsContext context, Path destinationDirectory) {
      ConfigFileSettings configFileSettings = lsss.getKorona().createConfigFileSettings(context);
      Path koronaConfigDir = lsss.getKorona().getKoronaSettings().getKoronaConfigDir().getFile();
      if (koronaConfigDir != null) {
         // For setting the default browse directory
         configFileSettings.setFile(koronaConfigDir.resolve("tmp-ConfigFileSettings.tmp~"));
      }
      List<Name> copyableFileServiceNames = configFileSettings.getFileServices().stream()
            .filter(ConfigFileService::isCopyable)
            .map(ConfigFileService::getName)
            .toList();
      Component referenceComponent = lsss.getReferenceComponent();
      boolean ok = new ConfigurableGUIDialog(referenceComponent, "Copy more config files", configFileSettings)
            .setTopText("<html><h1>Copy more config files</h1>"
                  + "<p>The selected files will be copied to " + destinationDirectory)
            .setHelpID(KoronaHelp.CONFIG_FILE_SETTINGS)
            .setGUI(new ConfigFileSettingsEditor(configFileSettings, copyableFileServiceNames, true, ContextVisibility.HIDE, false).getComponent())
            .setMinimumSize(900, 0)
            .accessOKButton(okButton -> MiscIcons.COPY.on(okButton).setText("Copy"))
            .show();
      if (ok) {
         copyConfigFiles(configFileSettings, destinationDirectory, referenceComponent);
      }
   }

   private static void copyConfigFiles(ConfigFileSettings configFileSettings, Path destinationDirectory, @Nullable Component referenceComponent) {
      List<ConfigFileService> applicableServices = configFileSettings.getFileServices().stream()
            .filter(ConfigFileService::isCopyable)
            .filter(configFileService -> configFileSettings.getFile(configFileService.getName()) != null)
            .toList();

      if (applicableServices.isEmpty()) {
         return;
      }

      Path referenceFilesDir = destinationDirectory.resolve(ConfigFileSettingsUtils.REFERENCE_FILES);
      if (!Files.exists(referenceFilesDir)) {
         referenceFilesDir = destinationDirectory;
      }
      Set<Path> existingDestinationFiles = new LinkedHashSet<>();
      for (ConfigFileService configFileService : applicableServices) {
         Path sourceFile = configFileSettings.getFile(configFileService.getName());
         if (sourceFile == null) {
            continue;
         }
         Path dir = configFileService.useReferenceFilesDir() ? referenceFilesDir : destinationDirectory;
         Path destinationFile = dir.resolve(sourceFile.getFileName());
         if (Files.exists(destinationFile)) {
            existingDestinationFiles.add(destinationFile);
         }
      }

      boolean skipExisting = false;
      if (!existingDestinationFiles.isEmpty()) {
         String existingFilesList = existingDestinationFiles.stream()
               .map(Path::toString)
               .collect(Collectors.joining("\n"));
         int answer = GuiUtils.showOptionDialog(referenceComponent, "Question",
               "Some files already exists:\n\n" + existingFilesList + "\n\nWhat should be done with those files?\n\n",
               new String[]{"Skip", "Overwrite", "Cancel"});
         if (answer < 0 || answer > 1) {
            return;
         }
         skipExisting = answer == 0;
      }

      Map<Path, Path> filesToCopy = new HashMap<>();
      for (ConfigFileService configFileService : applicableServices) {
         Path sourceFile = configFileSettings.getFile(configFileService.getName());
         if (sourceFile == null) {
            continue;
         }
         Path dir = configFileService.useReferenceFilesDir() ? referenceFilesDir : destinationDirectory;
         Path destinationFile = dir.resolve(sourceFile.getFileName());
         if (skipExisting && existingDestinationFiles.contains(destinationFile)) {
            continue;
         }
         filesToCopy.put(sourceFile, destinationFile);
         filesToCopy.putAll(configFileService.getAdditionalFilesToCopy(sourceFile, destinationFile));
      }

      ProgressView progressView = new ProgressView("Copying config files to survey directory", 1000)
            .mainProgressAsPercentage();
      new WorkerDialog(referenceComponent, progressView.getComponent())
            .start(asyncHandle -> {
               FileUtils.copyAllRecursively(filesToCopy, progressView.getMainProgressHandler(), asyncHandle);
            });
   }

   private static void incrementDestinationDirectory(PreprocessingSetup preprocessingSetup) {
      Path file = preprocessingSetup.destinationDirectory.getFile();
      if (file == null) {
         return;
      }

      String name = file.getFileName().toString();

      int i = name.length();
      while (i > 0 && Character.isDigit(name.charAt(i - 1))) {
         i--;
      }

      int n = i == name.length() ? 2 : 1 + Integer.parseInt(name.substring(i));
      String newName = name.substring(0, i) + n;

      preprocessingSetup.destinationDirectory.setFile(file.resolveSibling(newName));
   }
}
