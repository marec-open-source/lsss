package no.imr.lsss.viewer;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.export.DatabaseExporter;
import no.imr.lsss.database.export.DatabaseImporter;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.StatusView;
import no.imr.tools.swing.WorkerDialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * GUI for controlling database import.
 */
final class DatabaseImportGUI {
   DatabaseImportGUI(LSSS lsss, Window referenceWindow) {
      JDialog dialog = new JDialog(referenceWindow, "Import database", Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

      topPanel.add(new JLabel("""
            <html>
            You are about to import data into the LSSS database.<br>
            Specify either the directory in which the import database is located,<br>
            or a directory containing text files with table data.
            """), BorderLayout.NORTH);

      FileParameter directory = new FileParameter(new Name("Directory", "Source directory"),
            null, FileParameter.Mode.DIRECTORY);
      ParameterEditor parameterEditor = new ParameterEditor(List.of(directory));
      parameterEditor.getEditorComponent().setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      topPanel.add(parameterEditor.getEditorComponent(), BorderLayout.SOUTH);

      JButton importDatabaseButton = new JButton("Import from DB");
      importDatabaseButton.setEnabled(false);
      importDatabaseButton.addActionListener(_ -> {
         dialog.dispose();
         startImport(lsss, referenceWindow, directory, true);
      });

      JButton importTextFilesButton = new JButton("Import from text files");
      importTextFilesButton.setEnabled(false);
      importTextFilesButton.addActionListener(_ -> {
         dialog.dispose();
         startImport(lsss, referenceWindow, directory, false);
      });

      directory.subscribe(value -> {
         value.ifPresentOrElse(dir -> {
            Path javaDbDir = toJavaDbDir(dir);
            importDatabaseButton.setEnabled(JavaDBUtils.isJavaDBDirectory(javaDbDir));
            importTextFilesButton.setEnabled(Files.isRegularFile(javaDbDir.resolveSibling("DBParameter.txt")));
         }, () -> {
            importDatabaseButton.setEnabled(false);
            importTextFilesButton.setEnabled(false);
         });
      });

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);
      cancelButton.addActionListener(_ -> dialog.dispose());

      JButton helpButton = new JButton("Help");
      LsssHelp.DATABASE_IMPORT_EXPORT_IMPORTING_SURVEYS.enableHelpKeyOnButton(helpButton);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(importDatabaseButton);
      buttonPanel.add(importTextFilesButton);
      buttonPanel.add(cancelButton);
      buttonPanel.add(helpButton);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(new JScrollPane(topPanel));
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);

      dialog.add(mainPanel);
      dialog.pack();
      GuiUtils.expandSizeTo(dialog, 600, 0);
      dialog.setLocationRelativeTo(referenceWindow);
      dialog.setVisible(true);
   }

   private static Path toJavaDbDir(Path dir) {
      if (JavaDBUtils.isJavaDBDirectory(dir)) {
         return dir;
      }
      return dir.resolve(DatabaseExporter.DATABASE_NAME);
   }

   private static void startImport(LSSS lsss, Window referenceWindow, FileParameter directory, boolean fromDB) {
      Path dir = directory.getFile();
      if (dir == null) {
         return;
      }
      StatusView statusView = new StatusView(fromDB ? "Importing database..." : "Importing text files...");
      WorkerDialog.Result result = new WorkerDialog(referenceWindow, statusView.getComponent())
            .setMinimumSize(new Dimension(350, 0))
            .setOnError(e -> lsss.showError(referenceWindow, "Database import failed.", e))
            .start(asyncHandle -> {
               Path javaDbDir = toJavaDbDir(dir);
               DatabaseImporter databaseImporter = new DatabaseImporter(lsss, javaDbDir.getParent(), javaDbDir.getFileName().toString())
                     .setInteractiveMode(true)
                     .setAsyncHandle(asyncHandle)
                     .setStatusListener(statusView::setSecondaryText);
               if (fromDB) {
                  databaseImporter.importFromDatabase(referenceWindow);
               } else {
                  databaseImporter.importFromTextFiles(referenceWindow);
               }
            });
      if (result.success()) {
         lsss.getConfigurationManager().getSurveyConf().updateAllowedSurveys();
      }
   }
}
