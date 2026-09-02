package no.imr.lsss.framework.config.survey.misc.ices;

import com.google.common.html.HtmlEscapers;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.lsss.database.ices.IcesAcousticMetadata;
import no.imr.lsss.database.ices.IcesCalibration;
import no.imr.lsss.database.ices.IcesDataAcquisition;
import no.imr.lsss.database.ices.IcesDataProcessing;
import no.imr.lsss.database.ices.IcesGroup;
import no.imr.lsss.database.ices.IcesInstrument;
import no.imr.lsss.database.ices.IdRefParameter;
import no.imr.lsss.database.reports.GetIocCode;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.framework.config.survey.data.DataSetManager;
import no.imr.lsss.framework.config.survey.data.DataType;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.gui.ParameterTableGUI;
import no.imr.tools.parameter.gui.ParameterTableModel;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.PopupMenuAdapter;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.SimpleDocumentListener;
import no.imr.tools.swing.ToolTipManagerState;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.table.MultiLineHeaderRenderer;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

final class IcesConfView implements ViewHolder.View {
   private final IcesConf icesConf;
   private Map<String, SchemaWrapper> schemaNameToIcesCodes = Map.of();
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JScrollPane scrollPane = GuiUtils.createScrollPane(mainPanel);

   IcesConfView(IcesConf icesConf) {
      this.icesConf = icesConf;
      loadSchemas();
   }

   private void loadSchemas() {
      schemaNameToIcesCodes = IcesConf.findSchemaNames().collect(Collectors.toMap(
            Function.identity(),
            schemaName -> new SchemaWrapper(icesConf.readIcesCodes(schemaName))));
      updateContent();
   }

   void updateContent() {
      IcesAcousticMetadata icesAcousticMetadata = icesConf.getIcesAcousticMetadata();
      icesAcousticMetadata.getListConfigurables().stream()
            .flatMap(c -> c.getValues().stream())
            .forEach(this::updateItem);

      ParameterTableGUI<IcesInstrument> instrumentGui = new ParameterTableGUI<>(new ParameterTableModel<>(itemFactory(IcesInstrument::new), icesAcousticMetadata.instruments)
            .setParameterToColumnName(IcesConfView::parameterToColumnName));
      ParameterTableGUI<IcesCalibration> calibrationGui = new ParameterTableGUI<>(new ParameterTableModel<>(itemFactory(IcesCalibration::new), icesAcousticMetadata.calibrations));
      ParameterTableGUI<IcesDataAcquisition> dataAcquisitionGui = new ParameterTableGUI<>(new ParameterTableModel<>(itemFactory(IcesDataAcquisition::new), icesAcousticMetadata.dataAcquisitions));
      ParameterTableGUI<IcesDataProcessing> dataProcessingGui = new ParameterTableGUI<>(new ParameterTableModel<>(itemFactory(IcesDataProcessing::new), icesAcousticMetadata.dataProcessings)
            .setParameterToColumnName(IcesConfView::parameterToColumnName));

      MultiLineHeaderRenderer optionalHeaderRenderer = new MultiLineHeaderRenderer();
      optionalHeaderRenderer.setFont(optionalHeaderRenderer.getFont().deriveFont(Font.ITALIC));
      MultiLineHeaderRenderer mandatoryHeaderRenderer = new MultiLineHeaderRenderer();
      mandatoryHeaderRenderer.setFont(mandatoryHeaderRenderer.getFont().deriveFont(Font.BOLD));

      List.of(instrumentGui, calibrationGui, dataAcquisitionGui, dataProcessingGui).forEach(gui -> {
         gui.getModel()
               .setParameterToInputToolTip(IcesConfView::parameterToInputToolTip)
               .setParameterToHeaderToolTip(this::parameterToHeaderToolTip);

         Set<String> mandatoryParameterNames = gui.getModel().getNewRowSupplier().get().getMandatoryParameters().stream()
               .map(BaseParameter::getPersistentName)
               .collect(Collectors.toSet());
         for (int i = 0; i < gui.getTable().getColumnCount(); i++) {
            TableColumn tableColumn = gui.getTable().getColumnModel().getColumn(i);
            ValueParameter<?> parameter = gui.getModel().getHeaderParameter(i);
            if (mandatoryParameterNames.contains(parameter.getPersistentName())) {
               tableColumn.setHeaderRenderer(mandatoryHeaderRenderer);
            } else {
               tableColumn.setHeaderRenderer(optionalHeaderRenderer);
            }
         }
         gui.getTable().getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
               ToolTipManagerState.ALWAYS_ON.apply();
            }

            @Override
            public void mouseExited(MouseEvent e) {
               ToolTipManagerState.DEFAULT.apply();
            }
         });
      });

      Survey survey = icesConf.getConfigurationManager().getSurveyConf().getSurvey();

      GridBag gridBag = new GridBag()
            .configureVerticalBox();
      gridBag.activateHorizontalFill();

      gridBag.add(new JLabel("<html><h2>ICES Survey series</h2>"));
      gridBag.add(makeSelectionGui(icesConf.getIcesAcousticMetadata().survey, IcesUtils.SURVEY_SCHEMA));
      WarningLabel surveyWarningLabel = new WarningLabel(gridBag, () -> {
         String value = icesConf.getIcesAcousticMetadata().survey.getValue();
         List<IcesCode> codes = schemaNameToIcesCodes.get(IcesUtils.SURVEY_SCHEMA).icesCodes;
         HtmlStringBuilder warning = new HtmlStringBuilder()
               .html("<span style='color: red;'>");
         if (value.isEmpty()) {
            warning.text("Missing ICES survey code.");
         } else if (codes.stream().noneMatch(code -> code.key().equals(value))) {
            warning.text("\"" + value + "\" is not an ICES survey code.");
         } else if (survey != null && !survey.getSurveyTitle().contains(value)) {
            warning.text("Survey title \"" + survey.getSurveyTitle() + "\" does not contain ICES survey code \"" + value + "\".");
         } else {
            return null;
         }
         warning.html("</span>");
         if (survey != null) {
            List<IcesCode> suggestedCodes = codes.stream()
                  .filter(code -> survey.getSurveyTitle().contains(code.key()))
                  .toList();
            if (!suggestedCodes.isEmpty()) {
               warning.html(" Use survey code: ");
               for (int i = 0; i < suggestedCodes.size(); i++) {
                  IcesCode suggestedCode = suggestedCodes.get(i);
                  if (i > 0) {
                     warning.text(", ");
                  }
                  warning.html("<a href='" + suggestedCode.key() + "'>" + suggestedCode.key() + "</a>");
               }
            }
         }
         return warning.build();
      }, icesConf.getIcesAcousticMetadata().survey::setValue);
      WhenShowingListening.connect(mainPanel, icesConf.getIcesAcousticMetadata().survey, surveyWarningLabel::update);

      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(new JLabel("<html><h2>ICES Platform code</h2>"));
      gridBag.add(makeSelectionGui(icesConf.getIcesAcousticMetadata().platform, IcesUtils.PLATFORM_SCHEMA));
      String platformIocCode = survey == null ? null : icesConf.getLSSS().getDatabaseManager().getDatabaseConnection().executeStatelessValuedQuery(session -> {
         return GetIocCode.getIocCode(session, survey);
      });
      WarningLabel platformWarningLabel = new WarningLabel(gridBag, () -> {
         String value = icesConf.getIcesAcousticMetadata().platform.getValue();
         List<IcesCode> icesCodes = schemaNameToIcesCodes.get(IcesUtils.PLATFORM_SCHEMA).icesCodes;
         HtmlStringBuilder warning = new HtmlStringBuilder()
               .html("<span style='color: red;'>");
         if (value.isEmpty()) {
            warning.text("Missing ICES platform code.");
         } else if (icesCodes.stream().noneMatch(code -> code.key().equals(value))) {
            warning.text("\"" + value + "\" is not an ICES platform code.");
         } else if (!value.equals(platformIocCode)) {
            warning.text("Platform " + GetIocCode.CODE_SYS_NAME + " code \"" + platformIocCode + "\" differs from ICES platform code \"" + value + "\".");
         } else {
            return null;
         }
         warning.html("</span>");
         if (platformIocCode != null && icesCodes.stream().map(IcesCode::key).anyMatch(platformIocCode::equals)) {
            warning.html(" Use platform code: <a href='" + platformIocCode + "'>" + platformIocCode + "</a>");
         }
         return warning.build();
      }, icesConf.getIcesAcousticMetadata().platform::setValue);
      WhenShowingListening.connect(mainPanel, icesConf.getIcesAcousticMetadata().platform, platformWarningLabel::update);

      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(new JLabel("<html><h2>ICES Organisation number</h2>"));
      gridBag.add(makeSelectionGui(icesConf.getIcesAcousticMetadata().organisation, IcesUtils.ORGANIZATION_SCHEMA));
      WarningLabel organisationWarningLabel = new WarningLabel(gridBag, () -> {
         String value = icesConf.getIcesAcousticMetadata().organisation.getValue();
         List<IcesCode> icesCodes = schemaNameToIcesCodes.get(IcesUtils.ORGANIZATION_SCHEMA).icesCodes;
         if (value.isEmpty()) {
            return "Missing ICES organisation code";
         } else if (icesCodes.stream().noneMatch(code -> code.key().equals(value))) {
            return "\"" + value + "\" is not an ICES organisation code";
         } else {
            return null;
         }
      });
      WhenShowingListening.connect(mainPanel, icesConf.getIcesAcousticMetadata().organisation, organisationWarningLabel::update);

      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(new JLabel("<html><h2>Instrument</h2>"));
      gridBag.add(instrumentGui.createPanel());
      addTableWarningLabel(gridBag, instrumentGui);

      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(new JLabel("<html><h2>Calibration</h2>"));
      gridBag.add(calibrationGui.createPanel());
      addTableWarningLabel(gridBag, calibrationGui);

      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(new JLabel("<html><h2>DataAcquisition</h2>"));
      gridBag.add(dataAcquisitionGui.createPanel());
      addTableWarningLabel(gridBag, dataAcquisitionGui);

      gridBag.add(Box.createVerticalStrut(10));
      gridBag.add(new JLabel("<html><h2>DataProcessing</h2>"));
      gridBag.add(dataProcessingGui.createPanel());
      addTableWarningLabel(gridBag, dataProcessingGui);

      gridBag.deactivateFill();
      gridBag.add(Box.createVerticalStrut(30));

      DataSetManager dataSetManager = icesConf.getConfigurationManager().getDataConf().getDataSetManager();
      DataFileSet currentDataFileSet = dataSetManager.getDataFileSet();
      DataFileSet originalDataFileSet = dataSetManager.getDataFileSet(DataType.RAW);
      if (originalDataFileSet.isEmpty()) {
         originalDataFileSet = currentDataFileSet;
      }
      JButton resetValuesFromDataButton = new JButton("Reset values from data");
      if (currentDataFileSet.isEmpty()) {
         resetValuesFromDataButton.setEnabled(false);
         resetValuesFromDataButton.setToolTipText("No data currently loaded");
      } else {
         HtmlStringBuilder toolTipBuilder = new HtmlStringBuilder()
               .html("Resets configuration based values from<br>")
               .text(originalDataFileSet.getDataFiles().getFirst().getSegmentHandle().getMainFile().getFileName().toString());
         if (currentDataFileSet != originalDataFileSet) {
            toolTipBuilder
                  .html("<br>and<br>")
                  .text(currentDataFileSet.getDataFiles().getFirst().getSegmentHandle().getMainFile().getFileName().toString());
         }
         resetValuesFromDataButton.setToolTipText(toolTipBuilder.build());
      }
      resetValuesFromDataButton.addActionListener(_ -> icesConf.resetValuesFromData());

      JButton downloadFromIcesButton = new JButton("Download schemas");
      downloadFromIcesButton.setToolTipText("<html>Downloads schema files from acoustics.ices.dk to<br>"
            + HtmlEscapers.htmlEscaper().escape(icesConf.icesSchemaDir().toString()));
      downloadFromIcesButton.addActionListener(_ -> downloadFromIces());

      JButton resetValuesIoImrButton = new JButton("Use IMR default values");
      resetValuesIoImrButton.setToolTipText("Use default values for IMR, Norway");
      resetValuesIoImrButton.addActionListener(_ -> useImrDefaults());

      Box buttonBox = Box.createHorizontalBox();
      buttonBox.add(resetValuesFromDataButton);
      buttonBox.add(Box.createHorizontalStrut(10));
      buttonBox.add(downloadFromIcesButton);
      gridBag.add(buttonBox);
      buttonBox.add(Box.createHorizontalStrut(10));
      buttonBox.add(resetValuesIoImrButton);
      gridBag.add(buttonBox);

      GuiUtils.replaceContent(mainPanel, gridBag.getPanel());
   }

   private static void addTableWarningLabel(GridBag gridBag, ParameterTableGUI<? extends IcesGroup> tableGUI) {
      WarningLabel warningLabel = new WarningLabel(gridBag, () -> {
         List<? extends IcesGroup> rows = tableGUI.getModel().getRows();
         if (rows.isEmpty()) {
            return "Missing values";
         }
         String emptyParameters = rows.stream()
               .flatMap(icesGroup -> icesGroup.getMandatoryParameters().stream())
               .filter(parameter -> parameter.getStringValue().isEmpty())
               .map(BaseParameter::getDisplayName)
               .distinct()
               .sorted()
               .collect(Collectors.joining(", "));
         return emptyParameters.isEmpty() ? null : "Missing value for: " + emptyParameters;
      });
      warningLabel.update();
      tableGUI.getTable().getModel().addTableModelListener(_ -> warningLabel.update());
   }

   private Box makeSelectionGui(StringParameter parameter, String schema) {
      List<IcesCode> codes = schemaNameToIcesCodes.get(schema).getSortedIcesCodes();

      Function<String, @Nullable IcesCode> keyToCode = key -> {
         return codes.stream()
               .filter(code -> code.key().equals(key))
               .findFirst()
               .orElse(null);
      };

      JTextField keyTextField = new JTextField(parameter.getValue());
      keyTextField.setMinimumSize(new Dimension(60, 0));
      keyTextField.setMaximumSize(new Dimension(60, Short.MAX_VALUE));
      keyTextField.setPreferredSize(new Dimension(60, keyTextField.getPreferredSize().height));

      int maxKeyLength = codes.stream()
            .mapToInt(code -> code.key().length())
            .max()
            .orElse(0);

      JComboBox<IcesCode> comboBox = new JComboBox<>();
      Runnable updateMinimalCombobox = () -> {
         IcesCode code = keyToCode.apply(parameter.getValue());
         comboBox.setModel(new DefaultComboBoxModel<>());
         if (code != null) {
            comboBox.addItem(code);
         }
         comboBox.setSelectedItem(code);
      };
      comboBox.addPopupMenuListener(new PopupMenuAdapter() {
         @Override
         public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            comboBox.setModel(new DefaultComboBoxModel<>(codes.toArray(IcesCode[]::new)));
            comboBox.setSelectedItem(keyToCode.apply(parameter.getValue()));
         }

         @Override
         public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            updateMinimalCombobox.run();
         }
      });
      comboBox.setRenderer(new ListCellRenderer<>() {
         private final JPanel panel = new JPanel(new BorderLayout());
         private final DefaultListCellRenderer key = new DefaultListCellRenderer();
         private final DefaultListCellRenderer description = new DefaultListCellRenderer();

         {
            panel.add(key, BorderLayout.WEST);
            panel.add(description);
         }

         @Override
         public Component getListCellRendererComponent(JList<? extends IcesCode> list, @Nullable IcesCode value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
               String keyPadding = " ".repeat(maxKeyLength - value.key().length() + 1);
               key.getListCellRendererComponent(list, value.key() + keyPadding, index, isSelected, cellHasFocus);
               description.getListCellRendererComponent(list, value.description(), index, isSelected, cellHasFocus);
            } else {
               key.getListCellRendererComponent(list, " ", index, isSelected, cellHasFocus);
               description.getListCellRendererComponent(list, " ", index, isSelected, cellHasFocus);
            }
            key.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            return panel;
         }
      });
      updateMinimalCombobox.run();
      comboBox.addActionListener(_ -> {
         if (comboBox.hasFocus()) {
            IcesCode code = (IcesCode) comboBox.getSelectedItem();
            String key = code != null ? code.key() : "";
            if (!key.isEmpty()) {
               parameter.setValue(key);
               keyTextField.setText(key);
            }
         }
      });

      keyTextField.getDocument().addDocumentListener(new SimpleDocumentListener(_ -> {
         if (keyTextField.hasFocus()) {
            String key = keyTextField.getText();
            parameter.setValue(key);
            updateMinimalCombobox.run();
         }
      }));

      WhenShowingListening.connect(keyTextField, parameter, () -> {
         String value = parameter.getValue();
         if (!keyTextField.getText().equals(value)) {
            keyTextField.setText(value);
         }
         if (!comboBox.hasFocus()) {
            updateMinimalCombobox.run();
         }
      });

      Box box = Box.createHorizontalBox();
      box.add(new JLabel("Key: "));
      box.add(keyTextField);
      box.add(Box.createHorizontalStrut(10));
      box.add(new JLabel("Name: "));
      box.add(comboBox);
      return box;
   }

   @Override
   public JComponent getComponent() {
      return scrollPane;
   }

   private <T extends IcesGroup> Supplier<T> itemFactory(Supplier<T> factory) {
      return () -> {
         T item = factory.get();
         updateItem(item);
         return item;
      };
   }

   private void updateItem(IcesGroup item) {
      Utils.getAllOfType(item.getParameters(), IdRefParameter.class).forEach(parameter -> {
         List<IcesCode> icesCodes = schemaNameToIcesCodes.get(parameter.getSchemaName()).icesCodes;
         parameter.setIcesCodes(icesCodes);
      });
   }

   private static String parameterToColumnName(BaseParameter<?> parameter) {
      String name = parameter.getDisplayName();
      int i = 1;
      while (i < name.length() && (Character.isLowerCase(name.charAt(i)) || i < 4)) {
         i++;
      }
      return name.substring(0, i) + "\n" + name.substring(i);
   }

   private String parameterToHeaderToolTip(ValueParameter<?> parameter) {
      HtmlStringBuilder toolTip = new HtmlStringBuilder()
            .text(Utils.nameAndUnit(parameter.getDisplayName(), parameter.getUnit()));
      if (parameter instanceof IdRefParameter idRefParameter) {
         List<IcesCode> icesCodes = schemaNameToIcesCodes.get(idRefParameter.getSchemaName()).icesCodes;
         if (!icesCodes.isEmpty()) {
            toolTip.html("<br><br><table cellpadding=0 cellspacing=0>");
            icesCodes.forEach(code -> {
               toolTip.html("<tr><td>").text(code.key())
                     .html("</td><td style='padding-left: 5px; padding-right: 5px'>-</td><td>").text(code.description()).html("</td></tr>");
            });
            toolTip.html("</table>");
         }
      }
      return toolTip.build();
   }

   private static @Nullable String parameterToInputToolTip(ValueParameter<?> parameter) {
      String value = parameter.getStringValue();
      return value.length() < 10 ? null : value;
   }

   private void downloadFromIces() {
      Path dir = icesConf.icesSchemaDir();
      List<String> schemaNames = IcesConf.findSchemaNames().toList();
      ProgressView progressView = new ProgressView("Downloading from ices.dk...", schemaNames.size());
      new WorkerDialog(getComponent(), progressView.getComponent())
            .setWaitUntilFinishedIfCancelled(false)
            .start(asyncHandle -> {
               FileUtils.createDirectories(dir);
               for (String schemaName : schemaNames) {
                  if (asyncHandle.isCancelled()) {
                     return;
                  }
                  String fileName = IcesConf.schemaNameToFileName(schemaName);
                  URL url = Utils.toURL("https://acoustic.ices.dk/Services/Schema/XML/" + fileName);
                  progressView.setSecondaryText(url.toString());
                  Path file = dir.resolve(fileName);
                  Path tmpFile = dir.resolve(fileName + ".download");
                  try {
                     FileUtils.copy(url, tmpFile);   // Download to tmp file
                     XmlUtils.readDocument(tmpFile); // Test if tmp file is valid XML
                     FileUtils.move(tmpFile, file);  // Move tmp file to actual file
                  } catch (IOException e) {
                     if (!asyncHandle.isCancelled()) {
                        Log.global.log(Level.WARNING, "Error downloading " + url, e);
                     }
                  }
                  progressView.incrementMainProgress("");
               }
            });

      loadSchemas();
   }

   private void useImrDefaults() {
      icesConf.resetValuesFromSurvey();

      // Default organisation: Institute of Marine Research, Norway
      icesConf.getIcesAcousticMetadata().organisation.setStringValue("1351");

      // Some default IMR calibration parameters
      icesConf.getIcesAcousticMetadata().calibrations.clear();
      IcesCalibration calibration = new IcesCalibration();
      //calibration.date.setStringValue("2011-11-11");
      calibration.acquisitionMethod.setStringValue("SS");
      calibration.processingMethod.setStringValue("calibration.exe");
      calibration.accuracyEstimate.setStringValue("");
      calibration.report.setStringValue("No report");
      calibration.comments.setStringValue("IMR defaults");
      icesConf.getIcesAcousticMetadata().calibrations.add(calibration);

      for (IcesDataProcessing dataProcessing : icesConf.getIcesAcousticMetadata().dataProcessings) {
         dataProcessing.absorptionDescription.setStringValue("Typical for centre frequency");
         dataProcessing.soundSpeedDescription.setStringValue("Typical for surveyed waters");
      }

      updateContent();
   }

   private static final class SchemaWrapper {
      private final List<IcesCode> icesCodes;
      private @Nullable List<IcesCode> sortedIcesCodes;

      private SchemaWrapper(List<IcesCode> icesCodes) {
         this.icesCodes = icesCodes;
      }

      private List<IcesCode> getSortedIcesCodes() {
         if (sortedIcesCodes == null) {
            sortedIcesCodes = icesCodes.stream()
                  .sorted(Comparator.comparing(IcesCode::description, Collator.getInstance(Locale.ENGLISH)))
                  .toList();
         }
         return sortedIcesCodes;
      }
   }

   private static final class WarningLabel {
      private final Supplier<@Nullable String> warningSupplier;
      private final JTextPane label;

      private WarningLabel(GridBag gridBag, Supplier<@Nullable String> warningSupplier) {
         this(gridBag, warningSupplier, _ -> {
         });
      }

      private WarningLabel(GridBag gridBag, Supplier<@Nullable String> warningSupplier, Consumer<String> hrefListener) {
         this.warningSupplier = warningSupplier;
         label = GuiUtils.labelLikeHtmlTextPane("", hrefListener);
         gridBag.add(label);
         label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      }

      private void update() {
         String warning = warningSupplier.get();
         label.setForeground(warning != null && warning.startsWith("<html>") ? null : Color.RED);
         label.setVisible(warning != null);
         label.setText(warning);
      }
   }
}
