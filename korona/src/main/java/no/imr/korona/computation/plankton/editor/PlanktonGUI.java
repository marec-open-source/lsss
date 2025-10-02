package no.imr.korona.computation.plankton.editor;

import no.imr.korona.computation.plankton.PlanktonFile;
import no.imr.korona.computation.plankton.PlanktonFileException;
import no.imr.korona.computation.plankton.PlanktonRectangle;
import no.imr.korona.resources.KoronaHelp;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.table.MultiLineHeaderRenderer;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.DocumentHelper;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * A GUI for editing initial plankton size
 * distribution xml file.
 */
public final class PlanktonGUI {
   private final JDialog dialog;

   private PlanktonFile planktonFile;

   private PlanktonRectangleTableModel tableModel;
   private final JCheckBox showAllCheckBox = new JCheckBox("Show all", false);
   private final JTable table;
   private final JComboBox<String> sizeFactorComboBox = new JComboBox<>();

   private final Map<String, Double> factorMap = new TreeMap<>();

   public final String[] scatterNames =
         {"Hard", "Gas", "FluidS", "FBCyl", "FBCyl2", "SDWBA", "SDWBA2", "SDWBA3", "SDWBA4", "SDWBA5", "SDWBAS"};

   private final JComboBox<String> modelCombo;

   private boolean ok;

   public PlanktonGUI(@Nullable Component referenceComponent, PlanktonFile planktonFile, boolean editable) {
      this.planktonFile = planktonFile;

      Window window = GuiUtils.windowForComponent(referenceComponent);
      dialog = new JDialog(window, "Initial size distribution editor", Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

      tableModel = new PlanktonRectangleTableModel(planktonFile.getSizeFactor(), dialog);

      Map<String, List<PlanktonRectangle>> planktonRectangles = planktonFile.getPlanktonRectangles();

      for (Map.Entry<String, List<PlanktonRectangle>> entry : planktonRectangles.entrySet()) {
         String name = entry.getKey();
         List<PlanktonRectangle> planktonRectangleList = entry.getValue();

         for (PlanktonRectangle iter : planktonRectangleList) {
            tableModel.addRow(iter, name);
         }
      }

      modelCombo = new JComboBox<>(scatterNames);
      modelCombo.setEnabled(editable);

      table = new JTable(tableModel) {
         @Override
         public void tableChanged(TableModelEvent e) {
            super.tableChanged(e);
            if (getTableHeader() != null) {
               getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(modelCombo));
               getColumnModel().getColumn(0).setPreferredWidth(100);
               getColumnModel().getColumn(1).setPreferredWidth(150);
               if (showAllCheckBox.isSelected()) {
                  getColumnModel().getColumn(2).setPreferredWidth(150);
                  getColumnModel().getColumn(3).setPreferredWidth(300);
                  getColumnModel().getColumn(4).setPreferredWidth(200);
                  getColumnModel().getColumn(5).setPreferredWidth(300);
                  getColumnModel().getColumn(6).setPreferredWidth(200);
                  getColumnModel().getColumn(7).setPreferredWidth(150);
                  getColumnModel().getColumn(8).setPreferredWidth(150);
               }
               getTableHeader().setReorderingAllowed(false);
               MultiLineHeaderRenderer multiRenderer = new MultiLineHeaderRenderer();
               for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
                  getColumnModel().getColumn(i).setHeaderRenderer(multiRenderer);
               }
            }
         }
      };
      table.setEnabled(editable);

      //Here we toggle if we should see everything in the table or just a little bit.
      showAllCheckBox.addActionListener(e -> tableModel.setShowAll(showAllCheckBox.isSelected()));
      showAllCheckBox.setEnabled(editable);

      JPanel tablePanel = new JPanel(new BorderLayout());
      table.setPreferredScrollableViewportSize(new Dimension(1000, 200));
      tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

      factorMap.put("0.1 mm", 1e-4);
      factorMap.put("0.01 mm", 1e-5);
      factorMap.put("0.001 mm", 1e-6);

      for (String s : factorMap.keySet()) {
         sizeFactorComboBox.addItem(s);
      }

      sizeFactorComboBox.addActionListener(e -> {
         String key = (String) sizeFactorComboBox.getSelectedItem();
         double selected = factorMap.get(key);
         tableModel.setSizeFactor(selected);
         planktonFile.setSizeFactor(selected);
      });
      sizeFactorComboBox.setEnabled(editable);

      JPanel nonTablePanel = new JPanel(new BorderLayout());

      JPanel sizeFactorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      sizeFactorPanel.add(new JLabel("Histogram sizeFactor"));

      double factor = planktonFile.getSizeFactor();
      factorMap.forEach((i, value) -> {
         if (factor == value) {
            sizeFactorComboBox.setSelectedItem(i);
         }
      });

      sizeFactorPanel.add(sizeFactorComboBox);
      sizeFactorPanel.add(showAllCheckBox);

      JPanel buttonPanel = createButtonPanel(editable);

      nonTablePanel.add(sizeFactorPanel, BorderLayout.PAGE_START);
      nonTablePanel.add(buttonPanel, BorderLayout.PAGE_END);

      tablePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      nonTablePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      dialog.add(tablePanel, BorderLayout.CENTER);
      dialog.add(nonTablePanel, BorderLayout.PAGE_END);
      dialog.pack();
      dialog.setLocationRelativeTo(window);
      dialog.setVisible(true);
   }

   public PlanktonFile getPlanktonFile() {
      return planktonFile;
   }

   public static boolean showDialog(@Nullable Component referenceComponent, Path file, boolean editable) {
      PlanktonFile planktonFile;
      try {
         planktonFile = new PlanktonFile(file);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error reading file " + file, e);
         return false;
      }
      PlanktonGUI gui = new PlanktonGUI(referenceComponent, planktonFile, editable);
      if (gui.isOK()) {
         try {
            XmlUtils.writeDocument(gui.getPlanktonFile().toXml(), file);
         } catch (IOException e) {
            GuiUtils.showErrorDialog(referenceComponent, "Error saving " + file, e);
         }
      }
      return gui.isOK();
   }

   public boolean isOK() {
      return ok;
   }

   private JPanel createButtonPanel(boolean editable) {
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      JButton addButton = MiscIcons.ADD.on(new JButton("Add row"));
      addButton.addActionListener(ae -> {
         tableModel = (PlanktonRectangleTableModel) table.getModel();
         try {
            String sizeFactor = (String) sizeFactorComboBox.getSelectedItem();
            PlanktonRectangle planktonRectangle = new PlanktonRectangle(DocumentHelper.createElement(PlanktonFile.XML_MODEL), factorMap.get(sizeFactor));
            tableModel.addRow(planktonRectangle, "");
            table.repaint();
         } catch (PlanktonFileException e) {
            Log.global.log(Level.WARNING, e.toString(), e);
         }
      });
      addButton.setEnabled(editable);

      JButton removeButton = MiscIcons.DELETE.on(new JButton("Delete row"));
      removeButton.addActionListener(e -> {
         int selected = table.getSelectedRow();
         if (selected >= 0 && tableModel.getRowCount() > 0) {
            tableModel.deleteRow(selected);
            dialog.repaint();
         }
      });
      removeButton.setEnabled(editable);

      JButton okButton = new JButton("OK");
      okButton.addActionListener(e -> {
         PlanktonFile planktonFileOut = new PlanktonFile();

         for (int i = 0; i < tableModel.getRowCount(); i++) {
            planktonFileOut.setSizeFactor(tableModel.getSizeFactor());
            PlanktonRectangle planktonRectangle = tableModel.getRow(i).getPlanktonRectangle();
            if (planktonRectangle.getSizeHistogram().checkConsistencyOfHistogram()) {
               planktonFileOut.addEntry(tableModel.getRow(i).getAlgClass(), tableModel.getRow(i).getPlanktonRectangle());
            } else {
               JOptionPane.showMessageDialog(dialog,
                     "Inconsistent histogram, check histogram dividers.", "LSSS", JOptionPane.INFORMATION_MESSAGE);
               return;
            }
         }

         planktonFile = planktonFileOut;
         ok = true;
         dialog.dispose();
      });
      dialog.getRootPane().setDefaultButton(okButton);

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(e -> dialog.dispose());

      JButton helpButton = new JButton("Help");
      KoronaHelp.PLANKTON_CONFIGURATION.enableHelpKeyOnButton(helpButton);

      buttonPanel.add(addButton);
      buttonPanel.add(removeButton);
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);
      buttonPanel.add(helpButton);
      return buttonPanel;
   }
}
