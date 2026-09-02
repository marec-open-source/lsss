package no.imr.lsss.viewer;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.JavaDBMigration;
import no.imr.lsss.database.export.DatabaseImporter;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.database.HsqldbUtils;
import no.imr.tools.database.JavaDBUtils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.StatusView;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

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
import java.io.IOException;
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

      JLabel javaDbWarning = new JLabel(" ");
      topPanel.add(javaDbWarning);

      FileParameter directory = new FileParameter(new Name("Directory", "Source directory"),
            null, FileParameter.Mode.DIRECTORY);
      StringParameter databaseName = new StringParameter(new Name("DatabaseName", "Database name"));
      databaseName.setAllowedValuesAndValue(List.of(""), "");
      ParameterEditor parameterEditor = new ParameterEditor(List.of(directory, databaseName));
      parameterEditor.getEditorComponent().setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      topPanel.add(parameterEditor.getEditorComponent(), BorderLayout.SOUTH);

      JButton importDatabaseButton = new JButton("Import from DB");
      importDatabaseButton.setEnabled(false);
      importDatabaseButton.addActionListener(_ -> {
         dialog.dispose();
         startImport(lsss, referenceWindow, directory, databaseName, true);
      });

      JButton importTextFilesButton = new JButton("Import from text files");
      importTextFilesButton.setEnabled(false);
      importTextFilesButton.addActionListener(_ -> {
         dialog.dispose();
         startImport(lsss, referenceWindow, directory, databaseName, false);
      });

      directory.subscribe(_ -> {
         Path dir = directory.getFile();
         if (dir != null && JavaDBUtils.isJavaDBDirectory(dir)) {
            directory.setFile(dir.getParent());
            databaseName.setValue(dir.getFileName().toString());
            return;
         }
         List<String> dbNames = getDbNames(dir);
         if (dbNames.isEmpty()) {
            dbNames = List.of("");
         }
         databaseName.setAllowedValuesAndPossiblyValue(dbNames, dbNames.getFirst());
      });

      parameterEditor.getParameterChangeManager().subscribe(_ -> {
         Path dir = directory.getFile();
         String dbName = databaseName.getValue();
         if (dir != null && !dbName.isEmpty()) {
            boolean isJavaDB = JavaDBUtils.isJavaDBDatabase(dir, dbName);
            if (JavaDBMigration.interactivelyConvertJavaDBToHsqldb(lsss, dir, dbName, () -> dialog, "import")) {
               isJavaDB = false;
            }
            javaDbWarning.setText(isJavaDB
                  ? "<html><span style='color: red;'><b>Warning: JavaDB is deprecated. Support will be dropped in a future LSSS version."
                  : "");
            importDatabaseButton.setEnabled(HsqldbUtils.isHsqldbDatabase(dir, dbName) || isJavaDB);
            importTextFilesButton.setEnabled(Files.isRegularFile(dir.resolve("DBParameter.txt")));
         } else {
            javaDbWarning.setText("");
            importDatabaseButton.setEnabled(false);
            importTextFilesButton.setEnabled(false);
         }
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
      GuiUtils.expandSizeTo(dialog, 800, 0);
      dialog.setLocationRelativeTo(referenceWindow);
      dialog.setVisible(true);
   }

   private static List<String> getDbNames(@Nullable Path dir) {
      if (dir == null) {
         return List.of();
      }
      try {
         return FileUtils.listFilesWithAttributes(dir).stream()
               .<String>mapMulti((fileInfo, consumer) -> {
                  if (fileInfo.isDirectory() && JavaDBUtils.isJavaDBDirectory(fileInfo.file())) {
                     consumer.accept(fileInfo.getFileName());
                  } else if (fileInfo.getFileName().endsWith(HsqldbUtils.SCRIPT_FILE_SUFFIX)) {
                     String fileName = fileInfo.getFileName();
                     consumer.accept(fileName.substring(0, fileName.length() - HsqldbUtils.SCRIPT_FILE_SUFFIX.length()));
                  }
               })
               .sorted()
               .toList();
      } catch (IOException _) {
         return List.of();
      }
   }

   private static void startImport(LSSS lsss, Window referenceWindow, FileParameter directory, StringParameter databaseName, boolean fromDB) {
      Path dir = directory.getFile();
      if (dir == null) {
         return;
      }
      StatusView statusView = new StatusView(fromDB ? "Importing database..." : "Importing text files...");
      WorkerDialog.Result result = new WorkerDialog(referenceWindow, statusView.getComponent())
            .setMinimumSize(new Dimension(350, 0))
            .setOnError(e -> lsss.showError(referenceWindow, "Database import failed.", e))
            .start(asyncHandle -> {
               DatabaseImporter databaseImporter = new DatabaseImporter(lsss)
                     .setInteractiveMode(true)
                     .setAsyncHandle(asyncHandle)
                     .setStatusListener(statusView::setSecondaryText);
               if (fromDB) {
                  databaseImporter.importFromDatabase(referenceWindow, dir, databaseName.getValue());
               } else {
                  databaseImporter.importFromTextFiles(referenceWindow, dir);
               }
            });
      if (result.success()) {
         lsss.getDatabaseManager().getConnectionManager().resetDatabaseData();
      }
   }
}
