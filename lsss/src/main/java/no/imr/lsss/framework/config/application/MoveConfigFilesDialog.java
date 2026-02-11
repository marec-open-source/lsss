package no.imr.lsss.framework.config.application;

import com.google.common.html.HtmlEscapers;
import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.VerticalScrollablePanel;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

final class MoveConfigFilesDialog {
   private boolean updateLsssConfigSelected;

   MoveConfigFilesDialog(@Nullable Component referenceComponent, ConfigFileSettings configFileSettings, Path koronaConfigDir, boolean isStartupCheck) {
      List<Task> tasks;
      try {
         tasks = findTasks(configFileSettings, koronaConfigDir);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error listing files in\n" + koronaConfigDir, e);
         return;
      }
      Map<Category, List<Task>> tasksByCategory = tasks.stream().collect(Collectors.groupingBy(Task::category));
      List<Task> moveTasks = tasksByCategory.getOrDefault(Category.MOVE, List.of());
      List<Task> duplicateTasks = tasksByCategory.getOrDefault(Category.DUPLICATE, List.of());
      List<Task> conflictTasks = tasksByCategory.getOrDefault(Category.CONFLICT, List.of());
      List<Task> unrecognizedTasks = tasksByCategory.getOrDefault(Category.UNRECOGNIZED, List.of());
      if (isStartupCheck && moveTasks.isEmpty() && duplicateTasks.isEmpty() && conflictTasks.isEmpty()) {
         return;
      }

      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), "KORONA config files", Dialog.ModalityType.DOCUMENT_MODAL);

      JPanel mainPanel = new JPanel(new BorderLayout());

      GridBag gridBag = new GridBag()
            .configureVerticalBox();
      gridBag.getPanel().setBorder(GuiUtils.DEFAULT_MARGIN);

      String dirAsText = HtmlEscapers.htmlEscaper().escape(koronaConfigDir.toString());

      JTextPane info = GuiUtils.labelLikeHtmlTextPane("<h1>KORONA config files</h1>"
            + "<p>"
            + "   Starting with LSSS 2.16.0, the KORONA config files are organized by type in different subdirectories."
            + "</p>"
            + "<p>"
            + "   This dialog assists in managing the files in <a href='dir'>" + dirAsText + "</a>."
            + "</p>"
      );
      GuiUtils.addHrefListener(info, href -> {
         switch (href) {
            case "dir" -> GuiUtils.desktopOpen(koronaConfigDir, dialog);
            default -> {
            }
         }
      });
      gridBag.add(info);

      JCheckBox updateLsssCheckBox = new JCheckBox("<html>"
            + "Update the configuration in Application configuration - Preprocessing.<br>"
            + "Example: If the transducer ranges parameter is set to <em>" + dirAsText + File.separator + "TransducerRanges.xml</em> "
            + "then change it to <em>" + dirAsText + File.separator + "TransducerRanges" + File.separator + "TransducerRanges.xml</em>.",
            true);
      JCheckBox moveFilesCheckBox = new JCheckBox("<html>"
            + "Move files to subdirectories. (" + toFileCount(moveTasks) + ")<br>"
            + "Example: Move <em>TransducerRanges.xml</em> to <em>TransducerRanges" + File.separator + "TransducerRanges.xml</em> "
            + "if the latter does not exist.",
            true);
      JCheckBox deleteDuplicatesCheckBox = new JCheckBox("<html>"
            + "Delete duplicates. (" + toFileCount(duplicateTasks) + ")<br>"
            + "Example: Delete <em>TransducerRanges.xml</em> if it has the same content as <em>TransducerRanges" + File.separator + "TransducerRanges.xml</em>.",
            true);
      JCheckBox overwriteConflictsCheckBox = new JCheckBox("<html>"
            + "Overwrite conflicting files keeping only the most recently modified file. (" + toFileCount(conflictTasks) + ")<br>"
            + "Example: If <em>TransducerRanges.xml</em> is newer than <em>TransducerRanges" + File.separator + "TransducerRanges.xml</em>, "
            + "then overwrite the second file with the first file, otherwise delete the first file.");

      updateLsssCheckBox.setVerticalTextPosition(JCheckBox.TOP);
      moveFilesCheckBox.setVerticalTextPosition(JCheckBox.TOP);
      deleteDuplicatesCheckBox.setVerticalTextPosition(JCheckBox.TOP);
      overwriteConflictsCheckBox.setVerticalTextPosition(JCheckBox.TOP);

      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(updateLsssCheckBox);
      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(moveFilesCheckBox);
      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(deleteDuplicatesCheckBox);
      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(overwriteConflictsCheckBox);
      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(new JLabel("Unrecognized files will not be changed. (" + toFileCount(unrecognizedTasks) + ")"));

      mainPanel.add(new JScrollPane(VerticalScrollablePanel.wrap(gridBag.getPanel())));

      JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      JButton executeButton = new JButton("Execute");
      bottomPanel.add(executeButton);
      executeButton.addActionListener(_ -> {
         if (updateLsssCheckBox.isSelected()) {
            updateLsssConfigSelected = true;
         }
         if (moveFilesCheckBox.isSelected()) {
            execute(koronaConfigDir, moveTasks);
         }
         if (deleteDuplicatesCheckBox.isSelected()) {
            execute(koronaConfigDir, duplicateTasks);
         }
         if (overwriteConflictsCheckBox.isSelected()) {
            execute(koronaConfigDir, conflictTasks);
         }
         dialog.dispose();
      });

      JButton cancelButton = new JButton("Cancel");
      bottomPanel.add(cancelButton);
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(_ -> dialog.dispose());

      mainPanel.add(bottomPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.getRootPane().setDefaultButton(executeButton);
      dialog.add(mainPanel);
      dialog.pack();
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   boolean isUpdateLsssConfigSelected() {
      return updateLsssConfigSelected;
   }

   private static String toFileCount(List<Task> tasks) {
      return tasks.size() == 1 ? "1 file" : tasks.size() + " files";
   }

   private static List<Task> findTasks(ConfigFileSettings configFileSettings, Path configDir) throws IOException {
      Map<String, ConfigFileService> dirNameToService = configFileSettings.getAllAvailableFileServices().stream()
            .filter(configFileService -> !configFileService.getInstallationSubDirName().equals("."))
            .collect(Collectors.toUnmodifiableMap(ConfigFileService::getInstallationSubDirName, Function.identity()));

      return FileUtils.listFilesWithAttributes(configDir, Predicate.not(FileInfo::isDirectory))
            .parallelStream()
            .map(FileInfo::file)
            .map(file -> {
               String fileName = file.getFileName().toString();
               Map.Entry<String, ConfigFileService> entry = dirNameToService.entrySet().stream()
                     .filter(e -> fileName.startsWith(e.getKey()))
                     .findFirst()
                     .orElse(null);
               if (entry != null) {
                  String subDirName = entry.getKey();
                  Path destination = configDir.resolve(subDirName).resolve(fileName);
                  Category category;
                  if (!Files.exists(destination)) {
                     category = Category.MOVE;
                  } else if (FileUtils.equals(file, destination)) {
                     category = Category.DUPLICATE;
                  } else {
                     category = Category.CONFLICT;
                  }
                  return new Task(fileName, subDirName, category);
               } else {
                  return new Task(fileName, "", Category.UNRECOGNIZED);
               }
            })
            .sorted(Comparator.comparing(Task::fileName))
            .toList();
   }

   private static void execute(Path koronaConfigDir, List<Task> tasks) {
      tasks.parallelStream()
            .forEach(task -> {
               Path sourceFile = koronaConfigDir.resolve(task.fileName);
               Path destinationFile = koronaConfigDir.resolve(task.subDirName).resolve(task.fileName);
               switch (task.category) {
                  case MOVE -> {
                     try {
                        Files.move(sourceFile, destinationFile);
                     } catch (IOException e) {
                        Log.global.log(Level.WARNING, "Error moving file " + sourceFile + " to subdirectory " + task.subDirName, e);
                     }
                  }
                  case DUPLICATE -> {
                     try {
                        Files.delete(sourceFile);
                     } catch (IOException e) {
                        Log.global.log(Level.WARNING, "Error deleting duplicate file " + sourceFile, e);
                     }
                  }
                  case CONFLICT -> {
                     try {
                        if (FileUtils.lastModified(destinationFile) >= FileUtils.lastModified(sourceFile)) {
                           Files.delete(sourceFile);
                        } else {
                           FileUtils.move(sourceFile, destinationFile);
                        }
                     } catch (IOException e) {
                        Log.global.log(Level.WARNING, "Error handling conflicting file " + sourceFile, e);
                     }
                  }
                  default -> {
                     // Do nothing.
                  }
               }
            });
   }

   private enum Category {
      MOVE, DUPLICATE, CONFLICT, UNRECOGNIZED
   }

   private record Task(String fileName, String subDirName, Category category) {
   }
}
