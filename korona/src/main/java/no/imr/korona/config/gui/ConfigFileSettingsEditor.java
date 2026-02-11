package no.imr.korona.config.gui;

import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsContext;
import no.imr.korona.data.datamanager.labelling.DataFileLabel;
import no.imr.korona.data.datamanager.labelling.DataFileLabelling;
import no.imr.tools.Utils;
import no.imr.tools.help.HelpID;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.ComboBoxListModel;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Editor for {@link ConfigFileSettings}.
 */
public final class ConfigFileSettingsEditor {
   private final ConfigFileSettings configFileSettings;
   private final @Nullable Collection<Name> activeConfigFileServiceNames;
   private final boolean editable;
   private final JComponent mainPanel = new JPanel(new BorderLayout());
   private final JComponent editorPanel = new JPanel(new BorderLayout());

   public ConfigFileSettingsEditor(ConfigFileSettings configFileSettings, @Nullable Collection<Name> activeConfigFileServiceNames,
                                   boolean editable, ContextVisibility contextVisibility, boolean showDataFileLabel) {
      this.configFileSettings = configFileSettings;
      this.activeConfigFileServiceNames = activeConfigFileServiceNames;
      this.editable = editable;

      mainPanel.add(editorPanel);
      editorPanel.add(createEditorPanel());

      JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      northPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
      if (contextVisibility == ContextVisibility.SHOW && configFileSettings.getContexts().size() > 2) { // ALL, KORONA, and one more
         addContextSelector(northPanel);
      }
      DataFileLabelling dataFileLabelling = configFileSettings.getDataFileLabellingSupplier().get();
      if (showDataFileLabel && dataFileLabelling != null && !dataFileLabelling.getAllLabels().isEmpty()) {
         if (northPanel.getComponentCount() > 0) {
            northPanel.add(Box.createHorizontalStrut(20));
         }
         addLabelSelector(northPanel, dataFileLabelling);
      }
      if (northPanel.getComponentCount() > 0) {
         mainPanel.add(northPanel, BorderLayout.NORTH);
      }
   }

   private void addContextSelector(JPanel panel) {
      JComboBox<ConfigFileSettingsContext> comboBox = new JComboBox<>(new ComboBoxListModel<>(configFileSettings.getContext(), new ArrayList<>(configFileSettings.getContexts())));
      comboBox.setEnabled(editable);
      comboBox.setRenderer(new DefaultListCellRenderer() {
         @Override
         public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            ConfigFileSettingsContext context = (ConfigFileSettingsContext) value;
            String text = context.name().displayName();
            super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            return context.icon().on(this);
         }
      });
      Map<String, @Nullable Path> nameToFile = new HashMap<>();
      comboBox.addItemListener(e -> {
         if (e.getStateChange() == ItemEvent.SELECTED) {
            configFileSettings.getFileParameters().forEach(p -> nameToFile.put(p.getPersistentName(), p.getFile()));
            configFileSettings.setContext((ConfigFileSettingsContext) e.getItem());
            configFileSettings.getFileParameters().forEach(p -> p.setFile(nameToFile.get(p.getPersistentName())));
            GuiUtils.replaceContent(editorPanel, createEditorPanel());
         }
      });
      JLabel label = new JLabel("Context: ");
      label.setEnabled(editable);
      panel.add(label);
      panel.add(comboBox);
   }

   private void addLabelSelector(JPanel panel, DataFileLabelling dataFileLabelling) {
      List<@Nullable DataFileLabel> labels = new ArrayList<>();
      labels.add(null);
      labels.addAll(Utils.sorted(dataFileLabelling.getAllLabels()));
      DataFileLabel currentLabel = dataFileLabelling.getLabelByTitle(configFileSettings.getDataFileLabelTitle());
      JComboBox<DataFileLabel> comboBox = new JComboBox<>(new ComboBoxListModel<>(currentLabel, labels));
      comboBox.setEnabled(editable);
      comboBox.setToolTipText("""
            <html>Data file label
            <br>Used for initial selection when starting Korona relay.
            """);
      comboBox.setRenderer(new DefaultListCellRenderer() {
         @Override
         public Component getListCellRendererComponent(JList<?> list, @Nullable Object value, int index, boolean isSelected, boolean cellHasFocus) {
            DataFileLabel label = (DataFileLabel) value;
            String text = label != null ? "<html>" + label.toHtml() : "";
            super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            setToolTipText(label != null ? label.description : "No label selected");
            return this;
         }
      });
      comboBox.addItemListener(e -> {
         if (e.getStateChange() == ItemEvent.SELECTED) {
            DataFileLabel label = (DataFileLabel) e.getItem();
            configFileSettings.setDataFileLabelTitle(label != null ? label.title : "");
         }
      });
      JLabel label = new JLabel("Label: ");
      label.setEnabled(editable);
      panel.add(label);
      panel.add(comboBox);
   }

   public JComponent getComponent() {
      return mainPanel;
   }

   public static boolean showDialog(ConfigFileSettings configFileSettings, @Nullable Component referenceComponent, boolean editable,
                                    ContextVisibility contextVisibility, HelpID helpID) {
      return new ConfigurableGUIDialog(referenceComponent, configFileSettings.getName().displayName(), configFileSettings)
            .setHelpID(helpID)
            .setGUI(new ConfigFileSettingsEditor(configFileSettings, null, editable, contextVisibility, true).getComponent())
            .setMinimumSize(800, 0)
            .extraButton(createSetEmptyToDefaultButton(configFileSettings, editable))
            .show();
   }

   private static JButton createSetEmptyToDefaultButton(ConfigFileSettings configFileSettings, boolean editable) {
      JButton setEmptyToDefaultButton = new JButton("Set empty to default values");
      setEmptyToDefaultButton.setEnabled(editable);
      Path cfsFile = configFileSettings.getFile();
      setEmptyToDefaultButton.setToolTipText("Set default values relative to " + (cfsFile != null ? cfsFile.getParent() : "selected directory"));
      setEmptyToDefaultButton.addActionListener(_ -> {
         Path referenceDir;
         if (cfsFile == null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select reference directory for default values");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int returnValue = fileChooser.showDialog(GuiUtils.windowForComponent(setEmptyToDefaultButton), "Select reference directory");
            if (returnValue != JFileChooser.APPROVE_OPTION) {
               return;
            }
            Path selectedFile = fileChooser.getSelectedFile().toPath();
            referenceDir = Files.isDirectory(selectedFile) ? selectedFile : selectedFile.getParent();
         } else {
            referenceDir = cfsFile.getParent();
         }
         for (ConfigFileService fileService : configFileSettings.getFileServices()) {
            FileParameter fileParameter = configFileSettings.getFileParameter(fileService.getName());
            if (fileParameter.getFile() == null) {
               fileParameter.setFile(fileService.getDefaultInConfigDirectory(referenceDir));
            }
         }
      });
      return setEmptyToDefaultButton;
   }

   private JComponent createEditorPanel() {
      List<BaseParameter<?>> parameters = new ArrayList<>();
      Set<BaseParameter<?>> notEditableParameters = new HashSet<>();

      boolean useHeaders = activeConfigFileServiceNames == null;
      boolean useIcons = useHeaders
            && configFileSettings.getContext() == ConfigFileSettings.ALL
            && configFileSettings.getContextsExcludingAll().size() > 1;
      ConfigFileService previousConfigFileService = null;

      for (Name name : activeConfigFileServiceNames != null ? activeConfigFileServiceNames : configFileSettings.getFileServiceNames()) {
         FileParameter fileParameter = configFileSettings.getOptionalFileParameter(name);
         if (fileParameter == null) {
            fileParameter = new FileParameter(new Name(name.persistentName(), name.displayName() + " (Not available)"),
                  null, FileParameter.Mode.FILE);
            notEditableParameters.add(fileParameter);
         }
         if (!fileParameter.isVisible()) {
            continue;
         }
         if (useHeaders) {
            ConfigFileService configFileService = configFileSettings.getFileService(name);
            if (previousConfigFileService == null
                  || configFileService.getContext() != previousConfigFileService.getContext()
                  || configFileService.isModuleConfiguration() != previousConfigFileService.isModuleConfiguration()
            ) {
               HeaderParameter headerParameter = new HeaderParameter(
                     configFileService.isModuleConfiguration() ? "Computation setup" : "Reference files",
                     useIcons ? configFileService.getContext().icon() : null);
               parameters.add(headerParameter);
            }
            previousConfigFileService = configFileService;
         }
         parameters.add(fileParameter);
      }

      ParameterEditor parameterEditor = new ParameterEditor(parameters);
      parameterEditor.getGUIConfig().setParameterEnabledDecider(parameter -> editable && !notEditableParameters.contains(parameter));

      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
      panel.add(parameterEditor.getEditorComponent());

      return panel;
   }
}
