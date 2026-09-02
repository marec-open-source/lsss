package no.imr.lsss.region.ek500;

import no.imr.korona.data.formats.ek500.EK500SegmentHandle;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.resources.LsssHelp;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EK500WorkConversionGUI {
   private final FileParameter workIn;
   private final FileParameter workOut;

   private final FloatParameter mainFrequency = new FloatParameter(
         new Name("MainFrequency", "Main frequency in conversion"),
         38000, Unit.HZ,
         "Only the layers for the main frequency are converted");

   private final ObjectParameter<String> defaultSpecies = new ObjectParameter<>(
         new Name("DefaultSpecies", "The default species"),
         "",
         "The default species is used if no mapping between BEI species and LSSS species exist");

   private final List<AcousticCategory> allAcousticCategories;

   private final ConversionTableModel tableModel;
   private final JDialog mainDialog;

   public EK500WorkConversionGUI(@Nullable Component referenceComponent, List<EK500SegmentHandle> ek500SegmentHandles, Path ek500WorkDir, Path ek60WorkDir, List<AcousticCategory> acousticCategories) {
      workIn = new FileParameter(new Name("InputDir", "Input dir"),
            ek500WorkDir, FileParameter.Mode.DIRECTORY);

      workOut = new FileParameter(new Name("OutputDir", "Output dir"),
            ek60WorkDir, FileParameter.Mode.DIRECTORY);

      allAcousticCategories = acousticCategories;
      List<String> acousticCategoryNames = new ArrayList<>();
      String initialDefaultSpecies = null;
      for (AcousticCategory acousticCategory : allAcousticCategories) {
         acousticCategoryNames.add(acousticCategory.getInitials());
         if (acousticCategory.getCompId().getAcousticCategory() == 1) {
            initialDefaultSpecies = acousticCategory.getInitials();
         }
      }

      if (!acousticCategoryNames.isEmpty()) {
         if (initialDefaultSpecies == null) {
            initialDefaultSpecies = acousticCategoryNames.getFirst();
         }
         defaultSpecies.setAllowedValuesAndValue(acousticCategoryNames, initialDefaultSpecies);
      }

      mainDialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), "EK500 work file conversion", Dialog.ModalityType.DOCUMENT_MODAL);

      tableModel = new ConversionTableModel(ek500SegmentHandles);

      JTable table = new JTable(tableModel);
      table.getTableHeader().setReorderingAllowed(false);

      TableColumn selectedColumn = table.getColumnModel().getColumn(ConversionTableModel.Column.Selected.ordinal());
      selectedColumn.setMinWidth(60);
      selectedColumn.setMaxWidth(60);

      ParameterEditor parameterEditor = new ParameterEditor(List.of(workIn, workOut, mainFrequency, defaultSpecies), new GUIConfig()
            .setHorizontalFill(true)
      );
      parameterEditor.getEditorComponent().setBorder(GuiUtils.DEFAULT_MARGIN);

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(parameterEditor.getEditorComponent(), BorderLayout.NORTH);
      panel.add(new JScrollPane(table));
      panel.add(createBottomPanel(), BorderLayout.SOUTH);

      mainDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      mainDialog.getContentPane().add(panel);
      mainDialog.pack();
      GuiUtils.expandSizeWith(mainDialog, 200, 0);
      mainDialog.setLocationRelativeTo(referenceComponent);
      mainDialog.setVisible(true);
   }

   private JPanel createBottomPanel() {
      JButton okButton = new JButton("Convert");
      okButton.setToolTipText("Convert selected work files");
      okButton.addActionListener(_ -> {
         if (!CurrentInputComponent.commitEdit()) {
            return;
         }
         if (convert()) {
            close();
         }
      });
      mainDialog.getRootPane().setDefaultButton(okButton);

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);
      cancelButton.addActionListener(_ -> close());

      JButton helpButton = new JButton("Help");
      LsssHelp.EK500.enableHelpKeyOnButton(helpButton);

      JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      bottomPanel.setBackground(Color.WHITE);
      bottomPanel.setBorder(BorderFactory.createEtchedBorder());
      bottomPanel.add(okButton);
      bottomPanel.add(cancelButton);
      bottomPanel.add(helpButton);
      return bottomPanel;
   }

   private boolean convert() {
      int defaultAcousticCategoryNumber = getDefaultAcousticCategoryNumber(defaultSpecies.getValue());

      Path inDir = workIn.getFile();
      if (inDir == null) {
         JOptionPane.showMessageDialog(mainDialog, workIn.getDisplayName() + " must be specified.", "Error", JOptionPane.ERROR_MESSAGE);
         return false;
      }
      Path outDir = workOut.getFile();
      if (outDir == null) {
         JOptionPane.showMessageDialog(mainDialog, workOut.getDisplayName() + " must be specified.", "Error", JOptionPane.ERROR_MESSAGE);
         return false;
      }
      new ConversionSwingWorker(mainDialog, tableModel.getSelectedEK500SegmentHandles(), inDir, outDir,
            mainFrequency.getFloatValue(), defaultAcousticCategoryNumber);
      return true;
   }

   private int getDefaultAcousticCategoryNumber(String defaultCategoryName) {
      for (AcousticCategory acousticCategory : allAcousticCategories) {
         if (acousticCategory.getInitials().equals(defaultCategoryName)) {
            return acousticCategory.getCompId().getAcousticCategory();
         }
      }
      return -1;
   }

   private void close() {
      mainDialog.dispose();
   }
}
