package no.imr.lsss.viewer;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.export.DatabaseExporter;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.Utils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.StatusView;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.time.TimeUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * GUI for controlling database export.
 */
final class DatabaseExportGUI {
   private final JCheckBox currentSurveyCheckBox = new JCheckBox("Current survey", true);
   private final JCheckBox referenceTablesCheckBox = new JCheckBox("Reference tables");
   private final JCheckBox entireDatabaseCheckBox = new JCheckBox("Entire database");

   private final JButton startExportButton = new JButton("Start export");

   DatabaseExportGUI(LSSS lsss, Window referenceWindow) {
      JDialog dialog = new JDialog(referenceWindow, "Export database", Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

      if (lsss.getConfigurationManager().getSurveyConf().getSurvey() == null) {
         currentSurveyCheckBox.setEnabled(false);
         currentSurveyCheckBox.setSelected(false);
      }

      Path exportDir = lsss.getConfigurationManager().getDataConf().getDir(DataConfLSSS.EXPORT_SUB_DIR).getFile();
      FileParameter currentSurveyDirectory = new FileParameter(
            new Name("Directory", "Destination directory"),
            exportDir != null ? exportDir.resolve("database") : Utils.getUserHome(),
            FileParameter.Mode.DIRECTORY);
      Path mainDir = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().getMainDir();
      FileParameter referenceTablesDirectory = new FileParameter(
            new Name("Directory", "Destination directory"),
            mainDir.resolve("DatabaseReferenceTables"),
            FileParameter.Mode.DIRECTORY);
      FileParameter entireDatabaseDirectory = new FileParameter(
            new Name("Directory", "Destination directory"),
            mainDir.resolve("DatabaseExport_" + TimeUtils.createLocalDateTimeFormatter("yyyyMMdd_HHmmss").format(Instant.now())),
            FileParameter.Mode.DIRECTORY);

      JCheckBox keepEmptyFiles = new JCheckBox("Keep empty text files");
      JComboBox<DatabaseExporter.Type> typeComboBox = new JComboBox<>(DatabaseExporter.Type.values());
      typeComboBox.setSelectedItem(DatabaseExporter.Type.HSQLDB);
      JLabel javaDbWarning = new JLabel("<html><span style='color: red;'><b>Warning: JavaDB is deprecated. Support will be dropped in a future LSSS version.");
      typeComboBox.addItemListener(_ -> {
         javaDbWarning.setVisible(typeComboBox.getSelectedItem() == DatabaseExporter.Type.JavaDB);
      });
      javaDbWarning.setVisible(typeComboBox.getSelectedItem() == DatabaseExporter.Type.JavaDB);

      ParameterEditor currentSurveyParameterEditor = createParameterEditor(currentSurveyDirectory);
      ParameterEditor referencesTablesParameterEditor = createParameterEditor(referenceTablesDirectory);
      ParameterEditor entireDatabaseParameterEditor = createParameterEditor(entireDatabaseDirectory);

      GridBag selectionPanel = new GridBag()
            .configureVerticalBox();
      selectionPanel.add(currentSurveyCheckBox);
      selectionPanel.add(currentSurveyParameterEditor.getEditorComponent());
      selectionPanel.add(Box.createVerticalStrut(10));
      selectionPanel.add(new JSeparator());
      selectionPanel.add(Box.createVerticalStrut(10));
      selectionPanel.add(referenceTablesCheckBox);
      selectionPanel.add(referencesTablesParameterEditor.getEditorComponent());
      selectionPanel.add(Box.createVerticalStrut(10));
      selectionPanel.add(new JSeparator());
      selectionPanel.add(Box.createVerticalStrut(10));
      selectionPanel.add(entireDatabaseCheckBox);
      selectionPanel.add(entireDatabaseParameterEditor.getEditorComponent());
      selectionPanel.add(Box.createVerticalStrut(10));
      selectionPanel.add(new JSeparator());
      selectionPanel.add(Box.createVerticalStrut(10));
      selectionPanel.deactivateFill();
      selectionPanel.add(keepEmptyFiles);
      selectionPanel.add(Box.createVerticalStrut(5));
      selectionPanel.add(GuiUtils.add(new JPanel(new FlowLayout(FlowLayout.LEFT)),
            new JLabel("Database type: "), typeComboBox, javaDbWarning));

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      topPanel.add(selectionPanel.getPanel());

      startExportButton.setEnabled(lsss.getConfigurationManager().getSurveyConf().getSurvey() != null);
      startExportButton.addActionListener(_ -> {
         if (!CurrentInputComponent.commitEdit()) {
            return;
         }
         dialog.dispose();
         StatusView statusView = new StatusView("Exporting database...");
         WorkerDialog.Result result = new WorkerDialog(referenceWindow, statusView.getComponent())
               .setMinimumSize(new Dimension(350, 0))
               .setOnError(e -> lsss.showError(referenceWindow, "Database export failed:\n\n" + e, e))
               .start(asyncHandle -> {
                  DatabaseExporter.Type databaseType = typeComboBox.getSelectedItem() instanceof DatabaseExporter.Type t ? t : DatabaseExporter.Type.JavaDB;
                  if (currentSurveyCheckBox.isSelected() && !asyncHandle.isCancelled()) {
                     statusView.setMainText("Exporting current survey...");
                     Path dir = currentSurveyDirectory.getFile();
                     if (dir != null) {
                        new DatabaseExporter(lsss, dir, DatabaseExporter.DATABASE_NAME, databaseType)
                              .setAsyncHandle(asyncHandle)
                              .setStatusListener(statusView::setSecondaryText)
                              .setDeleteEmptyTextFiles(!keepEmptyFiles.isSelected())
                              .exportSurveys(List.of(lsss.getConfigurationManager().getSurveyConf().getSurvey()));
                     }
                  }
                  if (referenceTablesCheckBox.isSelected() && !asyncHandle.isCancelled()) {
                     statusView.setMainText("Exporting reference tables...");
                     Path dir = referenceTablesDirectory.getFile();
                     if (dir != null) {
                        new DatabaseExporter(lsss, dir, DatabaseExporter.DATABASE_NAME, databaseType)
                              .setAsyncHandle(asyncHandle)
                              .setStatusListener(statusView::setSecondaryText)
                              .setDeleteEmptyTextFiles(!keepEmptyFiles.isSelected())
                              .exportReferenceTables();
                     }
                  }
                  if (entireDatabaseCheckBox.isSelected() && !asyncHandle.isCancelled()) {
                     statusView.setMainText("Exporting entire database...");
                     Path dir = entireDatabaseDirectory.getFile();
                     if (dir != null) {
                        new DatabaseExporter(lsss, dir, DatabaseExporter.DATABASE_NAME, databaseType)
                              .setAsyncHandle(asyncHandle)
                              .setStatusListener(statusView::setSecondaryText)
                              .setDeleteEmptyTextFiles(!keepEmptyFiles.isSelected())
                              .exportEntireDatabase();
                     }
                  }
               });
         if (result.success()) {
            JOptionPane.showMessageDialog(referenceWindow, "Database export completed.");
         }
      });

      currentSurveyCheckBox.addActionListener(_ -> updateStartExportButton());
      referenceTablesCheckBox.addActionListener(_ -> updateStartExportButton());
      entireDatabaseCheckBox.addActionListener(_ -> updateStartExportButton());

      currentSurveyDirectory.subscribe(value -> updateCheckBox(value, currentSurveyCheckBox));
      referenceTablesDirectory.subscribe(value -> updateCheckBox(value, referenceTablesCheckBox));
      entireDatabaseDirectory.subscribe(value -> updateCheckBox(value, entireDatabaseCheckBox));

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);
      cancelButton.addActionListener(_ -> dialog.dispose());

      JButton helpButton = new JButton("Help");
      LsssHelp.DATABASE_IMPORT_EXPORT_EXPORTING_SURVEYS.enableHelpKeyOnButton(helpButton);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(startExportButton);
      buttonPanel.add(cancelButton);
      buttonPanel.add(helpButton);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(new JScrollPane(topPanel));
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);

      dialog.add(mainPanel);
      dialog.getRootPane().setDefaultButton(startExportButton);
      dialog.pack();
      GuiUtils.expandSizeTo(dialog, 800, 0);
      dialog.setLocationRelativeTo(referenceWindow);
      dialog.setVisible(true);
   }

   private void updateCheckBox(Optional<Path> path, JCheckBox checkBox) {
      checkBox.setEnabled(path.isPresent());
      checkBox.setSelected(path.isPresent());
      updateStartExportButton();
   }

   private void updateStartExportButton() {
      startExportButton.setEnabled(currentSurveyCheckBox.isSelected() || referenceTablesCheckBox.isSelected() || entireDatabaseCheckBox.isSelected());
   }

   private static ParameterEditor createParameterEditor(FileParameter currentSurveyDirectory) {
      ParameterEditor parameterEditor = new ParameterEditor(List.of(currentSurveyDirectory));
      parameterEditor.getEditorComponent().setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      return parameterEditor;
   }
}
