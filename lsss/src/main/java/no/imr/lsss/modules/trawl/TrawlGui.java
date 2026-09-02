package no.imr.lsss.modules.trawl;

import no.imr.lsss.modules.trawl.biotic.BioticUtils;
import no.imr.lsss.modules.trawl.spd.SpdParser;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.swing.WrappingFlowLayout;
import no.imr.tools.swing.icons.MiscIcons;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class TrawlGui {
   private Language language = Language.ENGLISH;

   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JButton previousButton = MiscIcons.NAVIGATE_PREVIOUS.on(new JButton());
   private final JButton previous10Button = MiscIcons.FAST_REWIND.on(new JButton());
   private final JButton nextButton = MiscIcons.NAVIGATE_NEXT.on(new JButton());
   private final JButton next10Button = MiscIcons.FAST_FORWARD.on(new JButton());
   private final JTextField stationTextField = new JTextField();
   private final JLabel stationCountLabel = new JLabel();
   private final JCheckBox percentCheckBox = new JCheckBox("%", true);
   private final JCheckBox allCheckBox = new JCheckBox("A", false);
   private final JCheckBox zeroGroupCheckBox = new JCheckBox("0gr", true);
   private final JCheckBox zooplanktonCheckBox = new JCheckBox("Z", true);
   private final JSlider thresholdSlider = new JSlider(JSlider.VERTICAL, -100, -20, -100);
   private final JTabbedPane tabbedPane = new JTabbedPane();

   private @Nullable Path directory;
   private @Nullable Path file;
   private List<FishStation> stations = List.of();
   private int stationIndex;

   private final ChangeManager fileChangeManager = new ChangeManager();

   TrawlGui() {
      Insets margin = new Insets(0, 0, 0, 0);

      JButton openButton = MiscIcons.OPEN.on(new JButton());
      openButton.setMargin(margin);
      openButton.addActionListener(_ -> openButtonHandler());

      previousButton.setMargin(margin);
      previousButton.addActionListener(_ -> setStationIndex(stationIndex - 1));

      nextButton.setMargin(margin);
      nextButton.addActionListener(_ -> setStationIndex(stationIndex + 1));

      next10Button.setMargin(margin);
      next10Button.addActionListener(_ -> setStationIndex(stationIndex + 10));

      previous10Button.setMargin(margin);
      previous10Button.addActionListener(_ -> setStationIndex(stationIndex - 10));

      stationTextField.setHorizontalAlignment(JTextField.RIGHT);
      stationTextField.addActionListener(_ -> stationTextFieldChanged());
      stationTextField.addFocusListener(new FocusAdapter() {
         @Override
         public void focusLost(FocusEvent e) {
            stationTextFieldChanged();
         }
      });

      percentCheckBox.addActionListener(_ -> updateGraphics());
      allCheckBox.addActionListener(_ -> updateGraphics());
      zeroGroupCheckBox.addActionListener(_ -> updateGraphics());
      zooplanktonCheckBox.addActionListener(_ -> updateGraphics());

      thresholdSlider.setBackground(Color.WHITE);
      thresholdSlider.setBorder(new TitledBorder("TS [dB]"));
      thresholdSlider.addChangeListener(_ -> updateGraphics());
      thresholdSlider.setMajorTickSpacing(10);
      thresholdSlider.setMinorTickSpacing(2);
      thresholdSlider.setPaintTicks(true);
      thresholdSlider.setPaintLabels(true);

      stationTextField.setToolTipText("Current station number in file");
      openButton.setToolTipText("Trawl-file chooser");
      previous10Button.setToolTipText("Select previous -10 station");
      previousButton.setToolTipText("Select previous station");
      nextButton.setToolTipText("Select next station");
      next10Button.setToolTipText("Select next +10 station");
      percentCheckBox.setToolTipText("Toggle between %/kg view of weight data");
      allCheckBox.setToolTipText("Show all species (weight and length-panel only)");
      zeroGroupCheckBox.setToolTipText("Show 0-group part of samples separately or not (only Sa-panel)");
      zooplanktonCheckBox.setToolTipText("Toggle on/off zooplankton (only Sa-panel)");
      thresholdSlider.setToolTipText("Set TS threshold for Sa calculations (only Sa-panel)");

      JPanel buttonPanel = new JPanel(new WrappingFlowLayout());
      buttonPanel.add(stationTextField);
      buttonPanel.add(stationCountLabel);
      buttonPanel.add(previous10Button);
      buttonPanel.add(previousButton);
      buttonPanel.add(openButton);
      buttonPanel.add(nextButton);
      buttonPanel.add(next10Button);
      buttonPanel.add(percentCheckBox);
      buttonPanel.add(allCheckBox);
      buttonPanel.add(zeroGroupCheckBox);
      buttonPanel.add(zooplanktonCheckBox);

      mainPanel.add(buttonPanel, BorderLayout.NORTH);
      mainPanel.add(tabbedPane);

      setFile(null);
   }

   ChangeManager getFileChangeManager() {
      return fileChangeManager;
   }

   JComponent getComponent() {
      return mainPanel;
   }

   List<FishStation> getStations() {
      return stations;
   }

   void setDirectory(@Nullable Path directory) {
      this.directory = directory;
      if (directory == null || file != null && !directory.equals(file.getParent())) {
         setFile(null);
      }
   }

   void setUseEnglish(boolean useEnglish) {
      language = useEnglish ? Language.ENGLISH : Language.NORWEGIAN;
      updateGraphics();
   }

   private void updateGraphics() {
      int selectedIndex = tabbedPane.getSelectedIndex();
      tabbedPane.removeAll();

      if (stations.isEmpty()) {
         return;
      }
      FishStation station = stations.get(stationIndex);
      showWeightBarChart(station);
      showLengthBarChart(station);
      showSaBarChart(station);
      showLengthDistribution(station);
      tabbedPane.setSelectedIndex(Math.clamp(selectedIndex, 0, tabbedPane.getTabCount() - 1));
   }

   private void openButtonHandler() {
      JFileChooser fc = new JFileChooser(FileUtils.toFile(directory));
      fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fc.setSelectedFile(FileUtils.toFile(file));
      fc.addChoosableFileFilter(new SuffixFileFilter("SPD", ".spd"));
      fc.addChoosableFileFilter(new SuffixFileFilter("BIOTIC", ".xml"));

      int returnVal = fc.showOpenDialog(mainPanel);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         setFile(fc.getSelectedFile().toPath());
      }
   }

   void reload() {
      if (file != null && Files.exists(file)) {
         setFile(file);
      } else {
         setFile(null);
      }
   }

   @Nullable Path getFile() {
      return file;
   }

   private static List<FishStation> loadFishStations(Path file) throws IOException {
      if (Utils.endsWithIgnoringCase(file.toString(), ".spd")) {
         try (BufferedReader reader = Files.newBufferedReader(file, Utils.ISO_8859_1)) {
            return FishStation.fromSpd(SpdParser.parse(reader));
         }
      } else if (Utils.endsWithIgnoringCase(file.toString(), ".xml")) {
         try (InputStream in = FileUtils.newBufferedInputStream(file)) {
            return FishStation.fromBiotic(BioticUtils.loadBioticFile(in));
         }
      } else {
         Log.global.warning("Cannot read trawl file " + file);
         return List.of();
      }
   }

   void setFile(@Nullable Path file) {
      this.file = file;
      try {
         stations = file != null ? loadFishStations(file) : List.of();
      } catch (Exception e) {
         stations = List.of();
         GuiUtils.showErrorDialog(mainPanel, "Error opening file: " + file, e);
      }
      stationTextField.setEnabled(!stations.isEmpty());
      stationCountLabel.setText("/ " + stations.size());
      setStationIndex(0);
      fileChangeManager.notifyListeners();
   }

   private void stationTextFieldChanged() {
      try {
         setStationIndex(Integer.parseInt(stationTextField.getText()) - 1);
      } catch (NumberFormatException _) {
         setStationIndex(stationIndex);
      }
   }

   void setStationIndex(int index) {
      if (stations.isEmpty()) {
         stationIndex = 0;
         stationTextField.setText("0");
         stationTextField.setColumns(0);
      } else {
         stationIndex = Math.clamp(index, 0, stations.size() - 1);
         stationTextField.setText(Integer.toString(stationIndex + 1));
         stationTextField.setColumns((int) Math.ceil(Math.log10(stations.size())));
      }
      previousButton.setEnabled(stationIndex > 0);
      previous10Button.setEnabled(stationIndex > 0);
      nextButton.setEnabled(stationIndex < stations.size() - 1);
      next10Button.setEnabled(stationIndex < stations.size() - 1);
      updateGraphics();
   }

   private void showWeightBarChart(FishStation station) {
      CategoryDataset dataset = station.getWeightDataset(language, percentCheckBox.isSelected(), allCheckBox.isSelected());
      tabbedPane.add("Weight", PlotBarChart.plot(station.getStationInfo(), null, station.getDepthRange(), dataset, PlotChart.GREEN));
   }

   private void showLengthBarChart(FishStation station) {
      CategoryDataset dataset = station.getLengthDataset(language, allCheckBox.isSelected());
      tabbedPane.add("Length", PlotStatisticalBarChart.plot(station.getStationInfo(), null, station.getDepthRange(), dataset, PlotChart.YELLOW));
   }

   private void showSaBarChart(FishStation station) {
      CategoryDataset dataset = station.getSaDataset(language, percentCheckBox.isSelected(), zeroGroupCheckBox.isSelected(), thresholdSlider.getValue(), zooplanktonCheckBox.isSelected());
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(PlotBarChart.plot(station.getStationInfo(), null, station.getDepthRange(), dataset, null));
      panel.add(thresholdSlider, BorderLayout.EAST);
      tabbedPane.add("Sa", panel);
   }

   private void showLengthDistribution(FishStation station) {
      Set<String> catchNames = new HashSet<>();
      for (FishTarget target : station.getTargets()) {
         if (target.getIndividuals().size() < 5) { // Remove species with few individuals
            continue;
         }
         String catchName = target.getCatchName(language);
         if (catchNames.add(catchName)) {
            IntervalXYDataset dataset = target.getFishLengthDataset(language);
            tabbedPane.add(catchName, PlotHistogramChart.plot(station.getStationInfo(), dataset));
         }
      }
   }
}
