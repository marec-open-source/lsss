package no.imr.lsss.framework.backup;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.backup.pojo.CopyRemoteInfo;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.framework.config.survey.data.remote.RemoteDataConf;
import no.imr.lsss.resources.LsssHelp;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;

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
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

public final class CopyRemoteFilesGui {
   private final LSSS lsss;
   private final List<SelectionItem<CopyRemoteItem>> copyRemoteItems;
   private final List<SelectionItem<BackupExclusionOption>> exclusionOptions;
   private final JDialog dialog;
   private final PreviousInfo previousInfo;

   public CopyRemoteFilesGui(LSSS lsss, Path surveyFile) {
      this.lsss = lsss;

      Path infoFile = surveyFile.resolveSibling("lsss_copy_info");
      previousInfo = readPreviousInfo(infoFile);

      RemoteDataConf remoteDataConf = lsss.getConfigurationManager().getDataConf().getRemoteDataConf();
      copyRemoteItems = remoteDataConf.remoteDirectoryParameters()
            .map(parameter -> {
               Path remoteDir = parameter.getFile();
               if (remoteDir == null) {
                  return null;
               }
               SurveyDirectoryParameter surveyDirectoryParameter = parameter.getSurveyDirectoryParameter();
               Path localDir = surveyDirectoryParameter.getFile();
               if (localDir == null) {
                  return null;
               }
               CopyRemoteInfo.DirInfo dirInfo = previousInfo.idToDirInfo.get(surveyDirectoryParameter.getName().persistentName());
               boolean selected = dirInfo != null && dirInfo.selected;
               return new SelectionItem<>(new CopyRemoteItem(surveyDirectoryParameter.getName(), remoteDir, localDir), selected);
            })
            .filter(Objects::nonNull)
            .toList();

      exclusionOptions = BackupFilesUtils.getExclusionOptions(lsss, previousInfo.copyRemoteInfo().options);

      JFrame lsssFrame = lsss.getFrame();
      dialog = new JDialog(lsssFrame, "Copy remote survey data", Dialog.ModalityType.DOCUMENT_MODAL);

      OptionalFloatParameter repeatInterval = new OptionalFloatParameter(
            new Name("RepeatInterval", "Repeat interval"),
            Optional.empty(), Unit.MINUTES);
      ParameterEditor parameterEditor = new ParameterEditor(List.of(repeatInterval));

      JButton helpButton = new JButton("Help");
      LsssHelp.COPY_REMOTE.enableHelpKeyOnButton(helpButton);

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);
      cancelButton.addActionListener(_ -> dialog.dispose());

      JButton copyButton = new JButton("Copy remote survey data");
      copyButton.addActionListener(_ -> {
         if (!parameterEditor.commitEdits()) {
            return;
         }

         CopyRemoteInfo currentInfo = new CopyRemoteInfo();
         currentInfo.directories = copyRemoteItems.stream()
               .map(item -> {
                  if (!item.isSelected()) {
                     CopyRemoteInfo.DirInfo previousDirInfo = previousInfo.idToDirInfo.get(item.get().name().persistentName());
                     if (previousDirInfo != null) {
                        previousDirInfo.selected = false;
                     }
                     return previousDirInfo;
                  }
                  return new CopyRemoteInfo.DirInfo(item.get().name().persistentName(), item.get().destinationDir(), true);
               })
               .filter(Objects::nonNull)
               .toList();
         currentInfo.options = BackupFilesUtils.toSaveOptions(exclusionOptions);
         writeCopyRemoteInfo(infoFile, currentInfo);

         List<CopyItem> copyItems = SelectionItem.selected(copyRemoteItems)
               .map(item -> new CopyItem(item.remoteDir(), item.destinationDir()))
               .toList();
         Set<Path> excludedSourceDirs = BackupFilesUtils.getExcludedSourceDirs(lsss);
         List<BackupExclusionOption> selectedExclusionOptions = SelectionItem.selected(exclusionOptions)
               .toList();

         JLabel info = new JLabel("Copying remote files to survey " + surveyFile.getFileName());
         info.setToolTipText(surveyFile.toString());
         Duration interval = repeatInterval.getValue().map(minutes -> Duration.ofSeconds(Math.round(minutes * 60))).orElse(null);
         new RepeatedCopyGui(lsssFrame, "Copy remote survey data", info, interval, () -> {
            return new CopyGui(copyItems, excludedSourceDirs, selectedExclusionOptions);
         });

         dialog.dispose();
      });

      JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonsPanel.add(copyButton);
      buttonsPanel.add(cancelButton);
      buttonsPanel.add(helpButton);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(GuiUtils.createScrollPane(createMainPanel(parameterEditor)));
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
      CopyRemoteInfo copyRemoteInfo = readCopyRemoteInfo(infoFile);

      Map<String, CopyRemoteInfo.DirInfo> idToDirInfo = new HashMap<>();
      for (CopyRemoteInfo.DirInfo dirInfo : copyRemoteInfo.directories) {
         idToDirInfo.put(dirInfo.id, dirInfo);
      }
      return new PreviousInfo(copyRemoteInfo, idToDirInfo);
   }

   private static CopyRemoteInfo readCopyRemoteInfo(Path infoFile) {
      try {
         return JsonUtils.JSON_MAPPER.readValue(infoFile, CopyRemoteInfo.class);
      } catch (Exception e) {
         // Prior to LSSS 2.16.0 the file contained a single line with a directory.
         if (BackupFilesUtils.getLastUsedDestinationDir(infoFile) == null && !FileUtils.notExists(e, infoFile)) {
            Log.global.log(Level.WARNING, "Error reading from " + infoFile, e);
         }
         return new CopyRemoteInfo();
      }
   }

   private static void writeCopyRemoteInfo(Path infoFile, CopyRemoteInfo copyRemoteInfo) {
      try {
         JsonUtils.writeValuePrettily(infoFile, copyRemoteInfo);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error writing to " + infoFile, e);
      }
   }

   private JPanel createMainPanel(ParameterEditor parameterEditor) {
      JLabel warningLabel = new JLabel("<html><br>NB: Some destination directories are different from last copy!");
      warningLabel.setForeground(Color.RED);
      warningLabel.setVisible(false);

      GridBag gridBag = new GridBag()
            .configureVerticalBox();
      gridBag.add(new JLabel("Ready to copy remote data files to current survey."));
      gridBag.add(new JLabel(" "));
      gridBag.add(GuiUtils.labelLikeHtmlTextPane("""
                  Remote directories (changeable from
                  <a href="remoteDirectories">Survey configuration - Data files - Remote directories</a>):""",
            href -> {
               switch (href) {
                  case "remoteDirectories" -> {
                     dialog.dispose();
                     lsss.getConfigurationManager().getDataConf().getRemoteDataConf().showInConfigurationDialog();
                  }
                  default -> {
                  }
               }
            }));
      GridBag checkBoxGridBag = new GridBag();
      checkBoxGridBag.getConstraints().anchor = GridBagConstraints.WEST;
      for (SelectionItem<CopyRemoteItem> item : copyRemoteItems) {
         JCheckBox checkBox = new JCheckBox(item.get().remoteDir().toString(), item.isSelected());
         checkBoxGridBag.deactivateFill();
         checkBoxGridBag.add(checkBox);
         checkBoxGridBag.add(new JLabel(" ➔ "));
         JLabel destinationLabel = new JLabel(item.get().destinationDir().toString());
         checkBoxGridBag.activateHorizontalFill();
         checkBoxGridBag.addWithLineBreak(destinationLabel);
         HtmlStringBuilder tooltipBuilder = new HtmlStringBuilder()
               .text(item.get().name().displayName());
         String id = item.get().name().persistentName();
         CopyRemoteInfo.DirInfo dirInfo = previousInfo.idToDirInfo().get(id);
         boolean differentDir = dirInfo != null && dirInfo.destinationDir != null && !dirInfo.destinationDir.equals(item.get().destinationDir());
         if (differentDir) {
            warningLabel.setVisible(true);
            destinationLabel.setForeground(Color.RED);
            tooltipBuilder.html("<br><br><div style='color: red;'>NB: Previously used destination directory:")
                  .text(dirInfo.destinationDir.toString()).html("</div>");
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
      gridBag.add(new JLabel(" "));
      gridBag.add(parameterEditor.getEditorComponent());
      return gridBag.getPanel();
   }

   private record CopyRemoteItem(Name name, Path remoteDir, Path destinationDir) {
   }

   private record PreviousInfo(CopyRemoteInfo copyRemoteInfo, Map<String, CopyRemoteInfo.DirInfo> idToDirInfo) {
   }
}
