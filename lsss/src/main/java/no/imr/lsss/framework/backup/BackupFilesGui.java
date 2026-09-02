package no.imr.lsss.framework.backup;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.LsssDatabaseUtils;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.types.DatabasePlugin;
import no.imr.lsss.framework.SurveyManager;
import no.imr.lsss.framework.backup.pojo.BackupInfo;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.application.DirectoryConf;
import no.imr.lsss.framework.config.application.SubDir;
import no.imr.lsss.framework.config.application.SurveyDirStructure;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.framework.config.survey.data.SurveyDirectoryConf;
import no.imr.lsss.framework.config.survey.data.extra.ExtraDataDir;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingConf;
import no.imr.lsss.resources.LsssHelp;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.Utils;
import no.imr.tools.database.ConnectionType;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.misc.ReferenceDirectory;
import no.imr.tools.parameter.misc.ReferenceDirectoryCollection;
import no.imr.tools.parameter.misc.ReferenceDirectoryManager;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.hibernate.cfg.Configuration;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class BackupFilesGui {
   private final LSSS lsss;
   private final List<BackupItem> allBackupItems;
   private final List<SelectionItem<BackupItem>> backupItems;
   private final List<SelectionItem<BackupExclusionOption>> exclusionOptions;
   private final JDialog dialog;
   private final PreviousInfo previousInfo;

   public BackupFilesGui(LSSS lsss, Path surveyFile) {
      this.lsss = lsss;

      Path infoFile = surveyFile.resolveSibling("lsss_backup_info");
      previousInfo = readPreviousInfo(infoFile);

      SurveyDirStructure dirStructure = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().getSelectedBackupDirStructure();
      allBackupItems = findAllBackupItems(lsss, dirStructure);
      List<BackupItem> existingBackupItems = allBackupItems.parallelStream()
            .filter(BackupItem::exists)
            .toList();
      backupItems = existingBackupItems.stream()
            .filter(Utils.distinctBy(BackupItem::sourceDir))
            .map(backupItem -> {
               BackupInfo.DirInfo dirInfo = previousInfo.dirInfo(backupItem);
               boolean selected = dirInfo != null && dirInfo.selected;
               return new SelectionItem<>(backupItem, selected);
            })
            .toList();

      exclusionOptions = BackupFilesUtils.getExclusionOptions(lsss, previousInfo.backupInfo().options);

      JFrame lsssFrame = lsss.getFrame();
      dialog = new JDialog(lsssFrame, "Backup survey data", Dialog.ModalityType.DOCUMENT_MODAL);

      Path dir = previousInfo.backupInfo().outputDirectory;
      if (dir == null) {
         dir = getDefaultDestinationDir();
      }
      FileParameter outputDirectory = new FileParameter(
            new Name("OutputDirectory", "Output directory"),
            dir, FileParameter.Mode.DIRECTORY);
      ParameterEditor parameterEditor = new ParameterEditor(List.of(outputDirectory));

      JButton helpButton = new JButton("Help");
      LsssHelp.BACKUP_SURVEY.enableHelpKeyOnButton(helpButton);

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);
      cancelButton.addActionListener(_ -> dialog.dispose());

      JButton copyButton = new JButton("Copy current survey");
      copyButton.addActionListener(_ -> {
         if (!CurrentInputComponent.commitEdit()) {
            return;
         }

         Path destinationDir = outputDirectory.getFile();
         assert destinationDir != null;

         Set<BackupItem> selectedBackupItems = SelectionItem.selected(backupItems).collect(Collectors.toUnmodifiableSet());
         BackupInfo currentInfo = new BackupInfo();
         currentInfo.outputDirectory = destinationDir;
         currentInfo.directories = allBackupItems.stream()
               .<BackupInfo.DirInfo>mapMulti((item, consumer) -> {
                  if (selectedBackupItems.contains(item)) {
                     consumer.accept(new BackupInfo.DirInfo(item.name().persistentName(), item.sourceDir(), true));
                  } else {
                     BackupInfo.DirInfo previousDirInfo = previousInfo.dirInfo(item);
                     if (previousDirInfo != null) {
                        previousDirInfo.selected = false;
                        consumer.accept(previousDirInfo);
                     }
                  }
               })
               .toList();
         currentInfo.options = BackupFilesUtils.toSaveOptions(exclusionOptions);
         writeBackupInfo(infoFile, currentInfo);

         // Disconnect database to avoid copying an active (e.g. survey-local) database. First get info to be able to re-connect:
         DatabasePlugin databasePlugin = lsss.getDatabaseManager().getConnectionManager().getDatabasePlugin();
         DatabaseConnection sourceConnection = lsss.getDatabaseManager().getDatabaseConnection();
         Runnable databaseReconnect;
         if (databasePlugin != null && sourceConnection.isConnected() && lsss.getDatabaseManager().isUseSurveyLocalDatabase()) {
            Configuration configuration = databasePlugin.getConfiguration(ConnectionType.CONNECT);
            sourceConnection.waitUntilFinished();
            sourceConnection.disconnect();
            databaseReconnect = () -> sourceConnection.connect(ConnectionType.CONNECT, configuration, LsssDatabaseUtils.getAllDatabaseClasses(lsss));
         } else {
            databaseReconnect = null;
         }

         List<CopyItem> copyItems = SelectionItem.selected(backupItems)
               .map(backupItem -> backupItem.toCopyItem(destinationDir))
               .toList();
         Set<Path> excludedSourceDirs = BackupFilesUtils.getExcludedSourceDirs(lsss);
         List<BackupExclusionOption> selectedExclusionOptions = SelectionItem.selected(exclusionOptions)
               .toList();

         CopyGui copyGui = new CopyGui(copyItems, excludedSourceDirs, selectedExclusionOptions);
         new WorkerDialog(dialog, copyGui.getComponent())
               .start(copyGui::run);

         // Re-connect database
         if (databaseReconnect != null) {
            databaseReconnect.run();
         }

         if (SelectionItem.selected(backupItems).anyMatch(item -> item.subDir() == DataConfLSSS.LSSS_SUB_DIR)) {
            Path lsssFile = destinationDir.resolve(dirStructure.getRelativePath(DataConfLSSS.LSSS_SUB_DIR)).resolve(surveyFile.getFileName());
            updatePaths(destinationDir, lsssFile);
         }

         dialog.dispose();
      });

      outputDirectory.subscribe(optionalDir -> {
         copyButton.setEnabled(optionalDir.isPresent());
      });

      JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonsPanel.add(copyButton);
      buttonsPanel.add(cancelButton);
      buttonsPanel.add(helpButton);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(GuiUtils.createScrollPane(createMainPanel(parameterEditor, outputDirectory)));
      mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

      SwingUtilities.invokeLater(copyButton::requestFocusInWindow);

      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.add(mainPanel);
      dialog.pack();
      dialog.setLocationRelativeTo(lsssFrame);
      dialog.getRootPane().setDefaultButton(copyButton);
      dialog.setVisible(true);
   }

   private static PreviousInfo readPreviousInfo(Path infoFile) {
      BackupInfo backupInfo = readBackupInfo(infoFile);

      Map<String, BackupInfo.DirInfo> idToDirInfo = new HashMap<>();
      Map<Path, BackupInfo.DirInfo> extraDirectories = new HashMap<>();
      for (BackupInfo.DirInfo dirInfo : backupInfo.directories) {
         if (dirInfo.id.isEmpty()) {
            Path sourceDir = dirInfo.sourceDir;
            if (sourceDir != null) {
               extraDirectories.put(sourceDir, dirInfo);
            }
         } else {
            idToDirInfo.put(dirInfo.id, dirInfo);
         }
      }
      return new PreviousInfo(backupInfo, idToDirInfo, extraDirectories);
   }

   private static BackupInfo readBackupInfo(Path infoFile) {
      try {
         return JsonUtils.JSON_MAPPER.readValue(infoFile, BackupInfo.class);
      } catch (Exception e) {
         // Prior to LSSS 2.16.0 the file contained a single line with a directory.
         BackupInfo backupInfo = new BackupInfo();
         backupInfo.outputDirectory = BackupFilesUtils.getLastUsedDestinationDir(infoFile);
         if (backupInfo.outputDirectory == null && !FileUtils.notExists(e, infoFile)) {
            Log.global.log(Level.WARNING, "Error reading from " + infoFile, e);
         }
         return backupInfo;
      }
   }

   private static void writeBackupInfo(Path infoFile, BackupInfo backupInfo) {
      try {
         JsonUtils.writeValuePrettily(infoFile, backupInfo);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error writing to " + infoFile, e);
      }
   }

   private JPanel createMainPanel(ParameterEditor parameterEditor, FileParameter outputDirectory) {
      JLabel warningLabel = new JLabel("<html><br>NB: Some source directories are different from last copy!");
      warningLabel.setForeground(Color.RED);
      warningLabel.setVisible(false);

      GridBag gridBag = new GridBag()
            .configureVerticalBox();
      gridBag.add(new JLabel("Ready to copy data files from current survey."));
      gridBag.add(warningLabel);
      gridBag.add(new JLabel(" "));
      gridBag.add(GuiUtils.labelLikeHtmlTextPane("""
                  Source directories (changeable from
                  <a href="dataFiles">Survey configuration - Data files</a>):""",
            href -> {
               switch (href) {
                  case "dataFiles" -> {
                     dialog.dispose();
                     lsss.getConfigurationManager().getDataConf().showInConfigurationDialog();
                  }
                  default -> {
                  }
               }
            }));
      GridBag checkBoxGridBag = new GridBag();
      checkBoxGridBag.getConstraints().anchor = GridBagConstraints.WEST;
      for (SelectionItem<BackupItem> item : backupItems) {
         JCheckBox checkBox = new JCheckBox(item.get().sourceDir().toString(), item.isSelected());
         checkBoxGridBag.deactivateFill();
         checkBoxGridBag.add(checkBox);
         checkBoxGridBag.add(new JLabel(" ➔ "));
         JLabel destinationLabel = new JLabel();
         outputDirectory.addListenerAndNotify(optDir -> {
            destinationLabel.setText(optDir.map(dir -> dir.resolve(item.get().relativePath()).toString()).orElse(""));
         });
         checkBoxGridBag.activateHorizontalFill();
         checkBoxGridBag.addWithLineBreak(destinationLabel);
         HtmlStringBuilder tooltipBuilder = new HtmlStringBuilder()
               .text(item.get().name().displayName())
               .html("<br>Destination relative path: ").text(item.get().relativePath());
         BackupInfo.DirInfo dirInfo = previousInfo.dirInfo(item.get());
         boolean differentDir = dirInfo != null && dirInfo.sourceDir != null && !dirInfo.sourceDir.equals(item.get().sourceDir());
         if (differentDir) {
            warningLabel.setVisible(true);
            checkBox.setForeground(Color.RED);
            tooltipBuilder.html("<br><br><div style='color: red;'>NB: Previously used source directory:")
                  .text(dirInfo.sourceDir.toString()).html("</div>");
         }
         String tooltip = tooltipBuilder.build();
         checkBox.setToolTipText(tooltip);
         destinationLabel.setToolTipText(tooltip);
         checkBox.addItemListener(e -> {
            item.setSelected(e.getStateChange() == ItemEvent.SELECTED);
         });
      }
      gridBag.add(checkBoxGridBag.getPanel());
      if (!exclusionOptions.isEmpty()) {
         gridBag.add(new JLabel(" "));
         gridBag.add(new JLabel("Options:"));
         for (SelectionItem<BackupExclusionOption> item : exclusionOptions) {
            JCheckBox checkBox = new JCheckBox(item.get().name().displayName(), item.isSelected());
            gridBag.add(checkBox);
            checkBox.addItemListener(e -> {
               item.setSelected(e.getStateChange() == ItemEvent.SELECTED);
            });
         }
      }
      String s = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().backupDirStructure.getValue();
      if (s.equals(DirectoryConf.DEFAULT_DIR_STRUCTURE)) {
         s = "default LSSS";
      } else {
         s = s + " (not default LSSS)";
      }
      gridBag.add(new JLabel(" "));
      gridBag.add(GuiUtils.labelLikeHtmlTextPane("""
                  Output directory structure (changeable from
                  <a href="directories">Application configuration - Directories</a>):""",
            href -> {
               switch (href) {
                  case "directories" -> {
                     dialog.dispose();
                     lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().showInConfigurationDialog();
                  }
                  default -> {
                  }
               }
            }));
      gridBag.add(new JLabel("  - " + s));
      gridBag.add(parameterEditor.getEditorComponent());
      return gridBag.getPanel();
   }

   private void updatePaths(Path destinationSurveyDir, Path destinationLsssFile) {
      Path lsssDir = destinationLsssFile.getParent();
      ReferenceDirectoryManager referenceDirectoryManager = new ReferenceDirectoryManager()
            .setRelativeEverywhereReferenceDirectory(new ReferenceDirectory(new Name(SurveyManager.LSSS_REFERENCE_DIR_NAME), lsssDir))
            .add(new ReferenceDirectoryCollection(new Name("SurveyManager"),
                  new ReferenceDirectory(new Name(SurveyManager.SURVEY_REFERENCE_DIR_NAME), SurveyManager.lsssDirToSurveyDir(lsssDir))));

      FileParameter sourceFileParameter = new FileParameter(new Name("source"),
            null, FileParameter.Mode.FILE_OR_DIRECTORY);
      sourceFileParameter.setReferenceDirectoryManager(lsss.getSurveyManager().getReferenceDirectoryManager());

      FileParameter destinationFileParameter = new FileParameter(new Name("destination"),
            null, FileParameter.Mode.FILE_OR_DIRECTORY);
      destinationFileParameter.setReferenceDirectoryManager(referenceDirectoryManager);

      Map<Path, String> sourceDirToRelativePath = new HashMap<>();
      for (BackupItem backupItem : allBackupItems) {
         sourceDirToRelativePath.putIfAbsent(backupItem.sourceDir(), backupItem.relativePath());
      }

      record UpdateTask(ConfigurationUnit configurationUnit, List<String> parameterNames) {
      }

      List<UpdateTask> updateTasks = new ArrayList<>();

      for (SurveyDirectoryConf surveyDirectoryConf : lsss.getConfigurationManager().getDataConf().getAllSurveyDirectoryConfs()) {
         List<String> parameterNames = surveyDirectoryConf.getAllDirectoryParameters().stream()
               .map(BaseParameter::getPersistentName)
               .toList();
         updateTasks.add(new UpdateTask(surveyDirectoryConf, parameterNames));
      }

      updateTasks.add(new UpdateTask(lsss.getConfigurationManager().getSurveyConfiguration().getDataConf().getExtraDataConf(), List.of("Dir")));

      for (DataConf dataConf : lsss.getConfigurationManager().getDataConf().getAllDataConfs()) {
         PreprocessingConf preprocessingConf = dataConf.getPreprocessingConf();
         updateTasks.add(new UpdateTask(preprocessingConf, List.of("OnTheFlyDataDir", "SourceDirectory", "DestinationDirectory")));
      }

      Document surveyXml = XmlUtils.toDocument(lsss.getSurveyManager().toSurveyXml());

      for (UpdateTask updateTask : updateTasks) {
         if (surveyXml.selectSingleNode("//unit[@name='" + updateTask.configurationUnit.getPersistentName() + "']") instanceof Element configurationUnitElement) {
            for (String parameterName : updateTask.parameterNames) {
               Utils.getAllOfType(configurationUnitElement.selectNodes("configuration//parameter[@name='" + parameterName + "']"), Element.class)
                     .forEach(parameterElement -> {
                        sourceFileParameter.setFile(null);
                        sourceFileParameter.fromXml(parameterElement);
                        Path sourceDir = sourceFileParameter.getFile();
                        if (sourceDir == null) {
                           return;
                        }
                        String relativePath = sourceDirToRelativePath.get(sourceDir);
                        if (relativePath == null) {
                           return;
                        }
                        destinationFileParameter.setFile(destinationSurveyDir.resolve(relativePath));
                        Element destinationXml = destinationFileParameter.toXml();
                        parameterElement
                              .addAttribute(FileParameter.XML_REF, destinationXml.attributeValue(FileParameter.XML_REF))
                              .setText(destinationXml.getText());
                     });
            }
         }
      }

      try {
         XmlUtils.writeDocument(surveyXml, destinationLsssFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error writing " + destinationLsssFile, e);
      }
   }

   // Find one destination root different from the original: suggest last on list
   private Path oneDestinationRoot() {
      Path rawDir = lsss.getConfigurationManager().getDataConf().getRawDir().getFile();

      List<Path> roots = FileUtils.listExistingRoots();
      for (int i = roots.size() - 1; i >= 0; i--) { // Reverse order.
         Path root = roots.get(i);
         if (rawDir == null || !FileUtils.isInDir(rawDir, root)) { // To get source different from destination.
            return root;
         }
      }
      return roots.getLast();
   }

   private @Nullable String getDefaultDestinationDirName() {
      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      if (survey != null) {
         SurveyDirStructure dirStructure = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().getSelectedBackupDirStructure();
         return dirStructure.getSurveyDirName(survey);
      }
      Path referenceDir = lsss.getSurveyManager().getSurveyReferenceDirectory().getFile();
      if (referenceDir != null) {
         return referenceDir.getFileName().toString();
      }
      return null;
   }

   private @Nullable Path getDefaultDestinationDir() {
      String destinationDirName = getDefaultDestinationDirName();
      if (destinationDirName == null) {
         return null;
      }
      Path destinationDir = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().backupDestinationDir.getFile();
      if (destinationDir == null) {
         destinationDir = oneDestinationRoot().resolve(SurveyManager.LSSS_DATA_DIR_NAME);
      }
      return destinationDir.resolve(destinationDirName);
   }

   private static List<BackupItem> findAllBackupItems(LSSS lsss, SurveyDirStructure dirStructure) {
      Path referenceDir = lsss.getSurveyManager().getSurveyReferenceDirectory().getFile();

      List<BackupItem> backupItems = new ArrayList<>();

      // Configuration directory:
      Path surveyFile = lsss.getSurveyManager().getSurveyFile();
      if (surveyFile != null) {
         Path lsssDir = surveyFile.getParent();
         if (lsssDir != null) {
            backupItems.add(new BackupItem(DataConfLSSS.LSSS_SUB_DIR, lsssDir, dirStructure));
         }
      }

      // Data directories:
      for (SurveyDirectoryConf surveyDirectoryConf : lsss.getConfigurationManager().getDataConf().getAllSurveyDirectoryConfs()) {
         for (SurveyDirectoryParameter parameter : surveyDirectoryConf.getAllDirectoryParameters()) {
            Path dir = parameter.getFile();
            if (dir == null) {
               continue;
            }
            backupItems.add(new BackupItem(parameter.getSubDir(), dir, dirStructure));
         }
      }

      // Extra data directories:
      for (ExtraDataDir extraDataDir : lsss.getConfigurationManager().getDataConf().getExtraDataConf().getExtraDataDirs()) {
         Path dir = extraDataDir.dataDir.getFile();
         if (dir == null) {
            continue;
         }
         String relativePath = extraDataDir.relativePath.getValue();
         if (relativePath.isEmpty() && referenceDir != null && FileUtils.isInDir(dir, referenceDir)) {
            relativePath = FileUtils.relativePath(dir, referenceDir);
         }
         if (relativePath == null || relativePath.isEmpty()) {
            relativePath = dir.getFileName().toString();
         }
         backupItems.add(new BackupItem(new Name("", "Extra directory"), dir, relativePath, null));
      }

      return backupItems;
   }

   private record BackupItem(
         Name name,
         Path sourceDir,
         String relativePath,
         @Nullable SubDir subDir
   ) {
      private BackupItem {
         relativePath = FileUtils.toNativeSeparatorChar(relativePath);
      }

      private BackupItem(SubDir subDir, Path dir, SurveyDirStructure dirStructure) {
         this(subDir.parameterName(), dir, dirStructure.getRelativePath(subDir), subDir);
      }

      private boolean exists() {
         return Files.exists(sourceDir);
      }

      private CopyItem toCopyItem(Path destinationDir) {
         return new CopyItem(sourceDir, destinationDir.resolve(relativePath));
      }
   }

   private record PreviousInfo(BackupInfo backupInfo, Map<String, BackupInfo.DirInfo> idToDirInfo, Map<Path, BackupInfo.DirInfo> extraDirectories) {

      private BackupInfo.@Nullable DirInfo dirInfo(BackupItem item) {
         String id = item.name().persistentName();
         return id.isEmpty()
               ? extraDirectories.get(item.sourceDir())
               : idToDirInfo.get(id);
      }
   }
}
