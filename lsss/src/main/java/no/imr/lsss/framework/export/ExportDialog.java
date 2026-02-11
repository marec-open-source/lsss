package no.imr.lsss.framework.export;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.resources.LsssHelp;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.ConfigurableContainer;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ConfigurableGUI;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditorData;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.parameter.gui.input.ParameterGUI;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.VerticalScrollablePanel;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Shows export dialog.
 */
public final class ExportDialog {
   private ExportDialog() {
   }

   public static void show(LSSS lsss, @Nullable Component referenceComponent, List<Exporter> exporters, @Nullable Path defaultDirectory, ExportSettings settings) {
      Preferences preferences = lsss.getPreferences("exportDialog");
      String preferencesWidthKey = "width";

      if (defaultDirectory == null) {
         defaultDirectory = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().getMainDir()
               .resolve(DataConfLSSS.EXPORT_SUB_DIR.defaultRelativePath());
      }
      settings.exportReferenceDirectory.setFile(defaultDirectory);
      for (Exporter exporter : exporters) {
         exporter.outputDirectory.setReferenceDirectoryManager(settings.referenceDirectoryManager);
      }

      Configurable mainConfigurable = new ConfigurableContainer(new Name("Export"), exporters);
      if (settings.xml != null) {
         mainConfigurable.fromXml(settings.xml);
      }

      for (Exporter exporter : exporters) {
         if (exporter.outputDirectory.getFile() == null) {
            exporter.outputDirectory.setFile(defaultDirectory.resolve(exporter.name.persistentName()));
         }
      }

      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), "Export", Dialog.ModalityType.DOCUMENT_MODAL);
      JTextField fileNamePrefix = new JTextField(settings.fileNamePrefix, 20);

      JButton settingsButton = new JButton("Settings");
      settingsButton.addActionListener(_ -> showSettingsDialog(dialog, exporters));
      JButton exportButton = new JButton("Export");
      exportButton.setMnemonic(KeyEvent.VK_P);
      exportButton.addActionListener(_ -> {
         export(dialog, getEnabledExporters(exporters), fileNamePrefix.getText());
         dialog.dispose();
      });
      JButton deleteButton = new JButton("Delete");
      deleteButton.setToolTipText("Delete previously exported files");
      deleteButton.setMnemonic(KeyEvent.VK_D);
      deleteButton.addActionListener(_ -> delete(dialog, getEnabledExporters(exporters)));
      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);
      cancelButton.addActionListener(_ -> dialog.dispose());
      JButton helpButton = new JButton("Help");
      LsssHelp.EXPORT.enableHelpKeyOnButton(helpButton);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(settingsButton);
      buttonPanel.add(exportButton);
      buttonPanel.add(deleteButton);
      buttonPanel.add(cancelButton);
      buttonPanel.add(helpButton);

      List<BooleanParameter> enabledParameters = exporters.stream()
            .map(exporter -> exporter.enabled)
            .toList();
      WhenShowingListening.connect(dialog, enabledParameters, () -> {
         boolean enabled = exporters.stream().anyMatch(exporter -> exporter.enabled.getBooleanValue());
         exportButton.setEnabled(enabled);
         deleteButton.setEnabled(enabled);
      });

      Map<String, List<Exporter>> pluginToExporters = exporters.stream()
            .collect(Collectors.groupingBy(exporter -> exporter.getPlugin().getMainPluginId()));

      JPanel parameterPanel = new VerticalScrollablePanel(new GridBagLayout());
      parameterPanel.setBorder(GuiUtils.DEFAULT_MARGIN);
      GridBag gridBag = new GridBag(parameterPanel);
      boolean needSeparator = false;
      for (FeaturePlugin plugin : lsss.getPluginManager().getFeaturePlugins()) {
         List<Exporter> exportersForPlugin = pluginToExporters.get(plugin.getPersistentName());
         if (exportersForPlugin == null) {
            continue;
         }
         if (needSeparator) {
            gridBag.addWithLineBreak(Box.createVerticalStrut(5));
            gridBag.activateHorizontalFill();
            gridBag.addWithLineBreak(new JSeparator());
            gridBag.deactivateFill();
            gridBag.addWithLineBreak(Box.createVerticalStrut(2));
            Box box = Box.createHorizontalBox();
            JLabel iconLabel = plugin.getIconOrEmpty().on(new JLabel());
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 8));
            box.add(iconLabel);
            JLabel textLabel = new JLabel(plugin.getName().displayName());
            textLabel.setFont(textLabel.getFont().deriveFont(Font.BOLD));
            box.add(textLabel);
            gridBag.addWithLineBreak(box);
         }
         needSeparator = true;
         for (Exporter exporter : exportersForPlugin) {
            GridBagConstraints constraints = gridBag.getConstraints();
            constraints.gridwidth = 1;
            constraints.insets = new Insets(1, 3, 1, 3);
            constraints.anchor = GridBagConstraints.WEST;
            gridBag.deactivateFill();

            addToGridBag(gridBag, exporter);
         }
      }

      JButton allOn = new JButton("All on");
      allOn.addActionListener(_ -> setExportersEnabled(exporters, true));
      JButton allOff = new JButton("All off");
      allOff.addActionListener(_ -> setExportersEnabled(exporters, false));

      JPanel bottomWestPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      bottomWestPanel.add(allOn);
      bottomWestPanel.add(allOff);
      bottomWestPanel.add(new JLabel("Optional file name prefix: "));
      bottomWestPanel.add(fileNamePrefix);

      JPanel bottomPanel = new JPanel(new BorderLayout());
      bottomPanel.setBorder(BorderFactory.createEtchedBorder());
      bottomPanel.add(bottomWestPanel, BorderLayout.WEST);
      bottomPanel.add(buttonPanel, BorderLayout.EAST);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(new JScrollPane(parameterPanel), BorderLayout.CENTER);
      mainPanel.add(bottomPanel, BorderLayout.SOUTH);

      dialog.getRootPane().setDefaultButton(exportButton);
      dialog.add(mainPanel);
      dialog.pack();
      GuiUtils.expandSizeTo(dialog, preferences.getInt(preferencesWidthKey, 0), 0);
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
      dialog.dispose();

      preferences.putInt(preferencesWidthKey, dialog.getWidth());

      settings.fileNamePrefix = fileNamePrefix.getText();
      settings.xml = mainConfigurable.toXml();
   }

   private static void showSettingsDialog(JDialog referenceComponent, List<? extends Exporter> exporters) {
      List<Configurable> settingsConfigurables = exporters.stream()
            .map(Exporter::getSettingsConfigurable)
            .filter(settingsConfigurable -> !settingsConfigurable.getSubConfigurables().isEmpty())
            .toList();
      Configurable mainConfigurable = new ConfigurableContainer(new Name("ExportSettings", "Export settings"), settingsConfigurables);
      new ConfigurableGUIDialog(referenceComponent, "Export settings", mainConfigurable)
            .setGUI(new ConfigurableGUI().createComponent(settingsConfigurables))
            .show();
   }

   private static List<Exporter> getEnabledExporters(List<Exporter> exporters) {
      return exporters.stream()
            .filter(exporter -> exporter.enabled.getBooleanValue() && exporter.outputDirectory.getFile() != null)
            .toList();
   }

   private static void setExportersEnabled(List<Exporter> exporters, boolean enabled) {
      for (Exporter exporter : exporters) {
         exporter.enabled.setBooleanValue(enabled);
      }
   }

   private static void delete(JDialog dialog, List<? extends Exporter> exporters) {
      List<Path> dirs = exporters.stream()
            .map(exporter -> exporter.outputDirectory.getFile())
            .filter(Objects::nonNull)
            .toList();
      if (dirs.isEmpty()) {
         return;
      }
      StringBuilder message = new StringBuilder("Delete all contents in the selected export directories?\n");
      for (Path dir : dirs) {
         message.append('\n').append(dir);
      }
      message.append("\n\n");

      int answer = JOptionPane.showConfirmDialog(dialog, message.toString(), "Message", JOptionPane.YES_NO_OPTION);
      if (answer != JOptionPane.YES_OPTION) {
         return;
      }

      for (Path dir : dirs) {
         try {
            FileUtils.deleteContentsRecursively(dir);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error clearing " + dir, e);
         }
      }
   }

   private static void export(JDialog dialog, List<Exporter> exporters, String fileNamePrefix) {
      ProgressView progressView = new ProgressView("Exporting", exporters.size())
            .useSecondaryProgress();
      List<IOException> ioExceptions = new ArrayList<>();
      List<String> failed = new ArrayList<>();
      new WorkerDialog(dialog, progressView.getComponent())
            .start(asyncHandle -> {
               for (int i = 0; i < exporters.size(); i++) {
                  Exporter exporter = exporters.get(i);
                  progressView.setMainProgress(i, exporter.name.displayName());
                  try {
                     exporter.export(fileNamePrefix, asyncHandle, progressView.getSecondaryProgressHandler());
                  } catch (IOException e) {
                     ioExceptions.add(e);
                     failed.add(exporter.name.displayName());
                  }
               }
            });
      if (!ioExceptions.isEmpty()) {
         GuiUtils.showErrorDialog(dialog, "Export failed for " + failed, ioExceptions.getFirst());
      }
   }

   private static void addToGridBag(GridBag gridBag, Exporter exporter) {
      ParameterEditorData parameterEditorData = new ParameterEditorData(gridBag.getPanel(), new GUIConfig(), List.of(exporter.enabled, exporter.outputDirectory));
      gridBag.add(parameterEditorData.getInputComponent(exporter.enabled));
      ParameterGUI<?> fileParameterGUI = parameterEditorData.getParameterGUIs().get(exporter.outputDirectory);
      fileParameterGUI.addMouseClickListener(exporter.enabled::toggle);
      fileParameterGUI.installGUI(gridBag);
   }
}
