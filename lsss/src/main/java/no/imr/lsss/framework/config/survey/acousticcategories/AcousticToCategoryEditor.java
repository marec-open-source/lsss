package no.imr.lsss.framework.config.survey.acousticcategories;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingSetup;
import no.imr.lsss.resources.LsssHelp;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.Utils;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.table.TableUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class AcousticToCategoryEditor {
   private final LSSS lsss;
   private final AcousticToCategory acousticToCategory;
   private final JDialog dialog;
   private final Element backupXml;
   private final List<Cac0Datagram.Category> categories;
   private final List<Pic0Datagram.PlanktonCategory> planktonCategories;

   AcousticToCategoryEditor(LSSS lsss, AcousticToCategory acousticToCategory, JComponent parent) {
      this.lsss = lsss;
      this.acousticToCategory = acousticToCategory;
      backupXml = acousticToCategory.localToXml();

      Window parentWindow = GuiUtils.windowForComponent(parent);

      List<PingItem> configurationItems = getAllConfigurationItems(parentWindow);
      categories = getCategories(configurationItems);
      planktonCategories = getPlanktonCategories(configurationItems);
      removeRedundantMappings();

      dialog = new JDialog(parentWindow, "Mapping from acoustic category to preprocessed categories", Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.getContentPane().add(getComponent());
      Dimension dialogSize = acousticToCategory.getDialogSize();
      if (dialogSize == null) {
         dialog.pack();
         GuiUtils.expandSizeTo(dialog, 800, 0);
      } else {
         dialog.setSize(dialogSize);
      }
      dialog.setLocationRelativeTo(parentWindow);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            cancelAndClose();
         }
      });
      dialog.setVisible(true);
   }

   private static List<Cac0Datagram.Category> getCategories(List<PingItem> configurationItems) {
      return Utils.getAllOfType(configurationItems, Cac0Datagram.class)
            .flatMap(cac0Datagram -> cac0Datagram.getCategories().stream())
            .filter(Utils.distinctBy(Cac0Datagram.Category::getName))
            .sorted(Utils.comparingIgnoringCase(Cac0Datagram.Category::getName))
            .toList();
   }

   private static List<Pic0Datagram.PlanktonCategory> getPlanktonCategories(List<PingItem> configurationItems) {
      return Utils.getAllOfType(configurationItems, Pic0Datagram.class)
            .flatMap(pic0Datagram -> pic0Datagram.getPlanktonCategories().stream())
            .filter(Utils.distinctBy(Pic0Datagram.PlanktonCategory::getLegend))
            .sorted(Utils.comparingIgnoringCase(Pic0Datagram.PlanktonCategory::getLegend))
            .toList();
   }

   private List<PingItem> getAllConfigurationItems(@Nullable Component referenceComponent) {
      List<PingItem> pingItems = new ArrayList<>();

      lsss.getConfigurationManager().getDataConf().addAllConfigurationItems(pingItems);

      List<SegmentHandle> segmentHandles = lsss.getConfigurationManager().getDataConf().getSelectedOriginalSegmentHandles();
      if (!segmentHandles.isEmpty()) {
         SegmentHandle segmentHandle = segmentHandles.getFirst();
         List<PingItem> result = new WorkerDialog(referenceComponent, "Examining module processing setup")
               .setOnError(e -> lsss.showError(referenceComponent, "Error accessing module setup", e))
               .startMakeValue(asyncHandle -> {
                  PreprocessingSetup preprocessingSetup = lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getMainSetup();
                  ModuleContainer moduleContainer = preprocessingSetup.createModuleContainer();
                  try (PingReader pingReader = segmentHandle.createPingReader();
                       ModuleContainerComputation computation = new ModuleContainerComputation(new ComputationContext(moduleContainer, pingReader, asyncHandle)
                             .setUsingKoronaPlaybox(true))) {
                     PingConfiguration pingConfiguration = computation.getPingConfiguration();
                     return pingConfiguration.getConfigurationItems();
                  }
               });
         if (result != null) {
            pingItems.addAll(result);
         }
      }

      return pingItems;
   }

   private JComponent getComponent() {
      List<AcousticToCategory.KoronaMapping> koronaMappings = acousticToCategory.getAcousticCategories().stream()
            .map(acousticToCategory.getKoronaMappings()::get)
            .toList();
      TableModel tableModel = new AcousticToCategoryTableModel(lsss, koronaMappings, categories, planktonCategories);
      JTable table = new JTable(tableModel) {
         @Override
         protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(getColumnModel()) {
               @Override
               public @Nullable String getToolTipText(MouseEvent event) {
                  int column = TableUtils.pointToModelColumn(getTable(), event.getPoint());
                  if (column > 0) {
                     AcousticToCategory.KoronaMapping koronaMapping = koronaMappings.get(column - 1);
                     AcousticCategory acousticCategory = koronaMapping.acousticCategory();
                     return lsss.getConfigurationManager().getLanguageUtils().getAcCatName(acousticCategory)
                           + " (" + acousticCategory.getCompId().getAcousticCategory() + ")";
                  }
                  return null;
               }
            };
         }
      };
      table.setRowSelectionAllowed(false);
      table.getTableHeader().setReorderingAllowed(false);
      table.getColumnModel().getColumn(0).setPreferredWidth(120);
      table.setPreferredScrollableViewportSize(new Dimension(800, 400));
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.setBorder(GuiUtils.DEFAULT_MARGIN);
      mainPanel.add(new JScrollPane(table));
      mainPanel.add(buttonsPanel(), BorderLayout.SOUTH);
      return mainPanel;
   }

   private void removeRedundantMappings() {
      Set<String> categoryNames = categories.stream()
            .map(Cac0Datagram.Category::getName)
            .collect(Collectors.toUnmodifiableSet());

      Set<String> planktonNames = planktonCategories.stream()
            .map(Pic0Datagram.PlanktonCategory::getLegend)
            .collect(Collectors.toUnmodifiableSet());

      for (AcousticToCategory.KoronaMapping koronaMapping : acousticToCategory.getKoronaMappings().values()) {
         koronaMapping.categoryNames().retainAll(categoryNames);
         koronaMapping.planktonNames().retainAll(planktonNames);
      }

      lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryChangeManager().notifyListeners();
   }

   private JPanel buttonsPanel() {
      JButton okButton = new JButton("OK");
      okButton.addActionListener(_ -> closeDialog());
      dialog.getRootPane().setDefaultButton(okButton);

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(_ -> cancelAndClose());
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);

      JButton helpButton = new JButton("Help");
      LsssHelp.ACOUSTIC_CATEGORY_CONF_CATEGORY_MAPPING.enableHelpKeyOnButton(helpButton);

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.add(okButton);
      panel.add(cancelButton);
      panel.add(helpButton);
      return panel;
   }

   private void cancelAndClose() {
      acousticToCategory.localFromXml(backupXml);
      lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryChangeManager().notifyListeners();
      closeDialog();
   }

   private void closeDialog() {
      acousticToCategory.setDialogSize(dialog.getSize());
      dialog.dispose();
   }

   /**
    * KORONA mapping table.
    * <pre>
    *             column 0             column 1     column 2     column 3
    * header: | Info                | LSSS_CAT_0 | LSSS_CAT_1 | LSSS_CAT_2 |
    * row 0:  | KORONA_CATEGORY_0   |            |            |            |
    * row 1:  | KORONA_CATEGORY_1   |            |            |            |
    * row 2:  | KORONA_CATEGORY_2   |            |            |            |
    * row 3:  | KORONA_PLANKTON_0   |            |            |            |
    * row 4:  | KORONA_PLANKTON_1   |            |            |            |
    * row 5:  | KORONA_PLANKTON_2   |            |            |            |
    * </pre>
    */
   private static final class AcousticToCategoryTableModel extends AbstractTableModel {
      private final LSSS lsss;
      private final List<AcousticToCategory.KoronaMapping> koronaMappings;
      private final List<Cac0Datagram.Category> possibleCategories;
      private final List<Pic0Datagram.PlanktonCategory> possiblePlanktonCategories;

      private AcousticToCategoryTableModel(LSSS lsss, List<AcousticToCategory.KoronaMapping> koronaMappings,
                                           List<Cac0Datagram.Category> possibleCategories,
                                           List<Pic0Datagram.PlanktonCategory> possiblePlanktonCategories) {
         this.lsss = lsss;
         this.koronaMappings = koronaMappings;
         this.possibleCategories = possibleCategories;
         this.possiblePlanktonCategories = possiblePlanktonCategories;
      }

      @Override
      public int getRowCount() {
         return possibleCategories.size() + possiblePlanktonCategories.size();
      }

      @Override
      public int getColumnCount() {
         return 1 + koronaMappings.size();
      }

      @Override
      public String getColumnName(int columnIndex) {
         if (columnIndex == 0) {
            return "⮟ KORONA  ╲  LSSS ⮞";
         } else {
            AcousticToCategory.KoronaMapping koronaMapping = koronaMappings.get(columnIndex - 1);
            return lsss.getConfigurationManager().getLanguageUtils().getAcCatInitials(koronaMapping.acousticCategory());
         }
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
         return columnIndex == 0 ? String.class : Boolean.class;
      }

      @Override
      public boolean isCellEditable(int rowIndex, int columnIndex) {
         return columnIndex != 0;
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         if (rowIndex < possibleCategories.size()) {
            // Category
            Cac0Datagram.Category category = possibleCategories.get(rowIndex);
            String categoryName = category.getName();
            return columnIndex == 0
                  ? new HtmlStringBuilder().html("<span style=\"color: " + ColorUtils.colorToHex(category.getColor()) + ";\">■</span> ").text(categoryName)
                  : koronaMappings.get(columnIndex - 1).categoryNames().contains(categoryName);
         } else {
            // Plankton
            int planktonIndex = rowIndex - possibleCategories.size();
            Pic0Datagram.PlanktonCategory plankton = possiblePlanktonCategories.get(planktonIndex);
            String planktonName = plankton.getLegend();
            return columnIndex == 0
                  ? new HtmlStringBuilder().html("<span style=\"color: " + ColorUtils.colorToHex(plankton.getColor()) + ";\">■</span> ").text(planktonName)
                  : koronaMappings.get(columnIndex - 1).planktonNames().contains(planktonName);
         }
      }

      @Override
      public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
         if (!(aValue instanceof Boolean selected)) {
            return;
         }
         AcousticToCategory.KoronaMapping koronaMapping = koronaMappings.get(columnIndex - 1);
         if (rowIndex < possibleCategories.size()) {
            // Category
            String categoryName = possibleCategories.get(rowIndex).getName();
            if (selected) {
               koronaMapping.categoryNames().add(categoryName);
            } else {
               koronaMapping.categoryNames().remove(categoryName);
            }
         } else {
            // Plankton
            int planktonIndex = rowIndex - possibleCategories.size();
            String planktonName = possiblePlanktonCategories.get(planktonIndex).getLegend();
            if (selected) {
               koronaMapping.planktonNames().add(planktonName);
            } else {
               koronaMapping.planktonNames().remove(planktonName);
            }
         }
         // notify listeners
         lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryChangeManager().notifyListeners();
      }
   }
}
