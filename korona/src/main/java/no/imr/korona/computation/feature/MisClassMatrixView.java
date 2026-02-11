package no.imr.korona.computation.feature;

import no.imr.korona.computation.categorization.Category;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.categorization.GaussUtils;
import no.imr.korona.resources.KoronaHelp;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class for displaying matrices with diagnostic values for
 * misclassification and overlap between categories.
 */
final class MisClassMatrixView {
   private final Configurator configurator;
   private final CategoryVisualizer categoryVisualizer;
   private Collection<String> misClassEnabledFeatures;
   private final JDialog misClassDialog;
   private static final String EXTRACTED = "Extracted points";
   private static final String UNKNOWN = "Unknown";
   private final String[] actions;
   private final ColorSchemeRenderer newCategoryMisClassRenderer;
   private final ColorSchemeRenderer newCategoryOverlapRenderer;
   private final TableCellRenderer addToCategoryMisClassRenderer;
   private final AddToOverlapRenderer addToCategoryOverlapRenderer;

   MisClassMatrixView(CategoryVisualizer categoryVisualizer, Configurator configurator, JComponent referenceComponent) {
      this.categoryVisualizer = categoryVisualizer;
      this.configurator = configurator;
      misClassEnabledFeatures = CategoryVisualizer.toFeatureNames(configurator.getEnabledFeatureExtractors());
      misClassDialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), "Misclassification matrix", Dialog.ModalityType.MODELESS);
      actions = new String[2];
      actions[0] = "Add to best category";
      actions[1] = "Create new category";
      newCategoryMisClassRenderer = new NewCategoryMisClassRenderer();
      newCategoryOverlapRenderer = new NewCategoryOverlapRenderer();
      addToCategoryMisClassRenderer = new AddToMisClassRender();
      addToCategoryOverlapRenderer = new AddToOverlapRenderer();
   }

   void showMisClassDialog() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(estimatedWrongCategorizationMatrix());
      panel.add(createButtonsPanel(), BorderLayout.SOUTH);
      misClassDialog.getContentPane().removeAll();
      misClassDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      misClassDialog.getContentPane().add(panel);
      misClassDialog.pack();
      misClassDialog.setLocationRelativeTo(misClassDialog.getParent());
      misClassDialog.setVisible(true);
   }

   private JComponent createButtonsPanel() {
      JButton closeButton = new JButton("Close");
      closeButton.addActionListener(_ -> misClassDialog.dispose());
      misClassDialog.getRootPane().setDefaultButton(closeButton);
      GuiUtils.setAccelerator(closeButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

      JButton helpButton = new JButton("Help");
      KoronaHelp.CATEGORIZATION_LIBRARY_VIEW_MISCLASSIFICATION.enableHelpKeyOnButton(helpButton);

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.add(closeButton);
      panel.add(helpButton);
      return panel;
   }

   /**
    * Redraws the matrices.
    */
   void redraw() {
      GuiUtils.replaceContent(misClassDialog.getContentPane(), estimatedWrongCategorizationMatrix());
   }

   private abstract static class ColorSchemeRenderer extends JLabel implements TableCellRenderer {
      final QualityComputer qualityComputer = linearDecreasingQuality(0.05f, 0.2f);
      private final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);
      private final DecimalFormat decimalFormat;

      private ColorSchemeRenderer() {
         setOpaque(true); //MUST do this for background to show up.
         decimalFormat = Utils.createDecimalFormat("##0.0");
      }

      void defaultCellRendererComponentBehaviour(JTable table, Object value,
                                                 boolean isSelected, boolean hasFocus,
                                                 int row, int column) {
         float floatValue = (Float) value * 100;
         setText(decimalFormat.format(floatValue) + '%');

         if (isSelected) {
            super.setForeground(table.getSelectionForeground());
            super.setBackground(table.getSelectionBackground());
         } else {
            super.setForeground(table.getForeground());
            super.setBackground(table.getBackground());
         }

         setFont(table.getFont());

         if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            if (table.isCellEditable(row, column)) {
               super.setForeground(UIManager.getColor("Table.focusCellForeground"));
               super.setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
         } else {
            setBorder(noFocusBorder);
         }
      }
   }

   private abstract class AddToRenderer extends ColorSchemeRenderer {
      String addToCategory = "";
      int categoryRow;

      private AddToRenderer() {
      }

      void setAddToCategory(String addToCategory) {
         this.addToCategory = addToCategory;
         Collection<Category> categories = categoryVisualizer.getCategoryConfigMap().keySet();
         int i = 0;
         categoryRow = 0;
         for (Category category : categories) {
            if (category.getName().equalsIgnoreCase(addToCategory)) {
               categoryRow = i;
               break;
            }
            i++;
         }
      }
   }

   private static final class NewCategoryMisClassRenderer extends ColorSchemeRenderer {
      private NewCategoryMisClassRenderer() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
         defaultCellRendererComponentBehaviour(table, value, isSelected, hasFocus, row, column);
         float floatValue = (Float) value;
         float quality = qualityComputer.quality(floatValue);
         if (quality < 0.9 && (row == 0 || table.getColumnName(column).equalsIgnoreCase(EXTRACTED))
               && !(row == 0 && table.getColumnName(column).equalsIgnoreCase(EXTRACTED))
               && !table.getColumnName(column).equalsIgnoreCase(UNKNOWN)) {
            if (quality < 0.5) {
               setForeground(Color.RED);
            } else {
               setForeground(Color.ORANGE);
            }
         }
         return this;
      }
   }

   private static final class NewCategoryOverlapRenderer extends ColorSchemeRenderer {
      private NewCategoryOverlapRenderer() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
         defaultCellRendererComponentBehaviour(table, value, isSelected, hasFocus, row, column);
         float floatValue = (Float) value;
         float quality = qualityComputer.quality(floatValue);
         if (quality < 0.9 && (row == 0 || table.getColumnName(column).equalsIgnoreCase(EXTRACTED))
               && !(row == 0 && table.getColumnName(column).equalsIgnoreCase(EXTRACTED))) {
            if (quality < 0.5) {
               setForeground(Color.RED);
            } else {
               setForeground(Color.ORANGE);
            }
         }
         return this;
      }
   }

   private final class AddToMisClassRender extends AddToRenderer {
      private AddToMisClassRender() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
         defaultCellRendererComponentBehaviour(table, value, isSelected, hasFocus, row, column);
         return this;
      }
   }

   private final class AddToOverlapRenderer extends AddToRenderer {
      private AddToOverlapRenderer() {
      }

      //todo implement validate, revalidate, repaint, and firePropertyChange methods for performance
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
         defaultCellRendererComponentBehaviour(table, value, isSelected, hasFocus, row, column);
         //set diagnostic colors
         float floatVal = (Float) value;
         float quality = qualityComputer.quality(floatVal);
         if (quality < 0.9 && row == 0 && !table.getColumnName(column).equalsIgnoreCase(EXTRACTED)
               && !table.getColumnName(column).equalsIgnoreCase(addToCategory)
               && !table.getColumnName(column).equalsIgnoreCase(UNKNOWN)) {
            if (quality < 0.5) {
               setForeground(Color.RED);
            } else {
               setForeground(Color.ORANGE);
            }
         } else if (quality < 0.9 && table.getColumnName(column).equalsIgnoreCase(EXTRACTED)
               && row != 0 && row != categoryRow) {
            if (quality < 0.5) {
               setForeground(Color.RED);
            } else {
               setForeground(Color.ORANGE);
            }
         } else if (row == 0 && table.getColumnName(column).equalsIgnoreCase(addToCategory)
               && floatVal < 0.5) {
            setForeground(Color.RED);
         }
         return this;
      }
   }

   @FunctionalInterface
   private interface QualityComputer {
      float quality(float value);
   }

   private static QualityComputer linearDecreasingQuality(float lowerLimit, float upperLimit) {
      return value -> {
         float qualityTerm = (upperLimit - value) / (upperLimit - lowerLimit);
         return Math.clamp(qualityTerm, 0, 1);
      };
   }

   private static void recommendedAddToCategory(Collection<Category> categories, List<List<Float>> overlapValues, RecommendationData recommendationData) {
      QualityComputer qualityReducer = linearDecreasingQuality(0.05f, 0.2f);
      //find how much extracted points overlaps with existing categories
      int j = 0;
      List<Category> candidateList = new ArrayList<>();
      float qualityMeasure = 0.0f;
      Category candidate = null;
      for (Category category : categories) {
         if (category.getName().equalsIgnoreCase(EXTRACTED)) {
            j++;
            continue;
         }
         float v = overlapValues.getFirst().get(j);
         if (v > qualityMeasure) {
            candidateList.add(category);
            candidate = category;
            qualityMeasure = Math.max(qualityMeasure, v);
         }
         j++;
      }
      //if candidate list does not contain exactly one candidate,
      //do not recommend adding to existing category
      if (candidateList.size() != 1) {
         recommendationData.addToCandidate = candidate;
         recommendationData.addToQuality = 0;
         return;
      }
      candidate = candidateList.getFirst();
      //check if extracted points overlaps sufficiently little with other categories,
      //and that other categories overlaps sufficiently little with extracted points
      j = 0;
      for (Category category : categories) {
         if (category.getName().equalsIgnoreCase(EXTRACTED)) {
            j++;
            continue;
         }
         float v = overlapValues.getFirst().get(j);
         if (category != candidate) {
            qualityMeasure *= qualityReducer.quality(v);
         }
         v = overlapValues.get(j).getFirst();
         if (category != candidate) {
            qualityMeasure *= qualityReducer.quality(v);
         }
         j++;
      }
      recommendationData.addToCandidate = candidate;
      recommendationData.addToQuality = qualityMeasure;
   }

   private static void recommendNewCategory(Collection<Category> categories, List<List<Float>> misClassValues, List<List<Float>> overlapValues, RecommendationData recommendationData) {
      int j = 0;
      float qualityMeasure = 1;
      QualityComputer qualityReducer = linearDecreasingQuality(0.05f, 0.2f);
      for (Category category : categories) {
         if (category.getName().equalsIgnoreCase(EXTRACTED)) {
            j++;
            continue;
         }
         //few points in new category must be categorized as an existing category
         float v = misClassValues.getFirst().get(j);
         qualityMeasure *= qualityReducer.quality(v);
         //few points in existing must be categorised as the new category
         v = misClassValues.get(j).getFirst();
         qualityMeasure *= qualityReducer.quality(v);
         //low overlap with all categories
         v = overlapValues.getFirst().get(j);
         qualityMeasure *= qualityReducer.quality(v);
         //existing categories must have low overlap with new category
         v = overlapValues.get(j).getFirst();
         qualityMeasure *= qualityReducer.quality(v);
         j++;
      }
      if (qualityMeasure > 0.5) {
         recommendationData.newCategory = true;
         recommendationData.newCategoryQuality = qualityMeasure;
         return;
      }
      recommendationData.newCategory = false;
      recommendationData.newCategoryQuality = qualityMeasure;
   }

   private void computeWrongCategorizationMatrix(List<List<Float>> values,
                                                 Collection<String> featureNames,
                                                 Collection<Category> categories) {
      values.clear();
      for (Category category : categories) {
         values.add(GaussUtils.fractionCategorizedAs(category, categories,
               configurator.outlierFraction.getFloatValue(), featureNames));
      }
   }

   private void computeOverlapMatrix(List<List<Float>> values, Collection<String> featureNames,
                                     Collection<Category> categories) {
      values.clear();
      for (Category sourceCategory : categories) {
         List<Float> listForCat = new ArrayList<>();
         for (Category targetCategory : categories) {
            listForCat.add(GaussUtils.computeOverlap(targetCategory.getPixelCategoryDistribution().getGaussDistribution(),
                  sourceCategory.getPixelCategoryDistribution().getNeighborhood(), configurator.outlierFraction.getFloatValue(), featureNames));
         }
         values.add(listForCat);
      }
   }

   private void computeValues(Collection<String> featureNames, Collection<Category> categories,
                              List<List<Float>> misClassValues, List<List<Float>> overlapValues,
                              RecommendationData recommendationData) {
      computeWrongCategorizationMatrix(misClassValues, featureNames, categories);
      computeOverlapMatrix(overlapValues, featureNames, categories);
      recommendedAddToCategory(categories, overlapValues, recommendationData);
      recommendNewCategory(categories, misClassValues, overlapValues, recommendationData);
   }

   private static final class RecommendationData {
      private @Nullable Category addToCandidate;
      private float addToQuality;
      private boolean newCategory;
      private float newCategoryQuality;

      private RecommendationData() {
      }
   }

   private static String recommendedAction(RecommendationData recommendationData) {
      if (recommendationData.addToCandidate == null) {
         return "";
      }
      if (recommendationData.addToQuality >= 0.5 && !recommendationData.newCategory) {
         return "Add to " + recommendationData.addToCandidate.getName() +
               " (Adding quality: " + recommendationData.addToQuality + ")" +
               " (New category quality: " + recommendationData.newCategoryQuality + ")";
      }
      if (recommendationData.addToQuality >= 0.5 && recommendationData.newCategory) {
         return "Add to " + recommendationData.addToCandidate.getName() +
               " (Adding quality: " + recommendationData.addToQuality + ")" + "\n" +
               "or create new (sub) category to " + recommendationData.addToCandidate.getName() +
               " (New category quality: " + recommendationData.newCategoryQuality + ")";
      }
      if (recommendationData.addToQuality < 0.5 && recommendationData.newCategory) {
         return "Create new category" +
               " (Adding quality: " + recommendationData.addToQuality + ")" +
               " (New category quality: " + recommendationData.newCategoryQuality + ")";
      }
      if (recommendationData.addToQuality < 0.5 && !recommendationData.newCategory) {
         return "Do not add pointset" + "\n" +
               " (Adding quality: " + recommendationData.addToQuality + ")" +
               " (New category quality: " + recommendationData.newCategoryQuality + ")";
      }
      return "";
   }

   private void setTableRenderers(String action, JTable misClassTable, JTable overlapTable) {
      if (action.equalsIgnoreCase(actions[0])) {
         misClassTable.setDefaultRenderer(Float.class, addToCategoryMisClassRenderer);
         overlapTable.setDefaultRenderer(Float.class, addToCategoryOverlapRenderer);
      } else {
         misClassTable.setDefaultRenderer(Float.class, newCategoryMisClassRenderer);
         overlapTable.setDefaultRenderer(Float.class, newCategoryOverlapRenderer);
      }
   }

   private JComponent estimatedWrongCategorizationMatrix() {
      List<List<Float>> misClassValues = new ArrayList<>();
      List<List<Float>> overlapValues = new ArrayList<>();
      Collection<String> allFeatures = CategoryVisualizer.toFeatureNames(configurator.getEnabledFeatureExtractors());
      //if there are features enabled that are not contained in allFeatures, these must be removed
      Collection<String> newEnabledFeatures = new ArrayList<>();
      for (String featureName : misClassEnabledFeatures) {
         if (allFeatures.contains(featureName)) {
            newEnabledFeatures.add(featureName);
         }
      }
      misClassEnabledFeatures = newEnabledFeatures;

      RecommendationData recommendationData = new RecommendationData();

      computeValues(misClassEnabledFeatures, categoryVisualizer.getCategoryConfigMap().keySet(), misClassValues, overlapValues,
            recommendationData);

      JTable misClassTable = new JTable();
      AbstractTableModel misClassTableModel = misClassTableModel(misClassValues);
      misClassTable.setModel(misClassTableModel);
      misClassTable.getColumnModel().getColumn(0).setPreferredWidth(160);

      JPanel misClassTablePanel = new JPanel(new BorderLayout());
      misClassTablePanel.add(misClassTable.getTableHeader(), BorderLayout.NORTH);
      misClassTable.setPreferredScrollableViewportSize(new Dimension(100, 100));
      misClassTablePanel.add(new JScrollPane(misClassTable), BorderLayout.CENTER);

      JTable overlapTable = new JTable();
      AbstractTableModel overlapTableModel = overlapTableModel(overlapValues);
      overlapTable.setModel(overlapTableModel);
      overlapTable.getColumnModel().getColumn(0).setPreferredWidth(160);

      addToCategoryOverlapRenderer.setAddToCategory(recommendationData.addToCandidate != null ?
            recommendationData.addToCandidate.getName() : "");

      JPanel overlapTablePanel = new JPanel(new BorderLayout());
      overlapTablePanel.add(overlapTable.getTableHeader(), BorderLayout.NORTH);
      overlapTable.setPreferredScrollableViewportSize(new Dimension(100, 100));
      overlapTablePanel.add(new JScrollPane(overlapTable), BorderLayout.CENTER);

      setTableRenderers(actions[0], misClassTable, overlapTable);

      JPanel vBox1 = new JPanel();
      vBox1.setLayout(new BoxLayout(vBox1, BoxLayout.PAGE_AXIS));
      vBox1.add(new JLabel("How points in the different point clouds will be categorized " +
            "if extracted points are added as a new category",
            JLabel.LEFT));
      vBox1.add(misClassTablePanel);
      JPanel vBox2 = new JPanel();
      vBox2.setLayout(new BoxLayout(vBox2, BoxLayout.PAGE_AXIS));
      vBox2.add(new JLabel("The percentage of the points in the point clouds " +
            "that lies within the different gauss curves.", JLabel.LEFT));
      vBox2.add(overlapTablePanel);

      JLabel recommendedAction = new JLabel("Recommendation: " + recommendedAction(recommendationData));

      JPanel intendedAction = new JPanel(new FlowLayout());
      JComboBox<String> pullDownActions = new JComboBox<>(actions);
      pullDownActions.addActionListener(_ -> {
         String action = (String) pullDownActions.getSelectedItem();
         if (action != null) {
            setTableRenderers(action, misClassTable, overlapTable);
            misClassTableModel.fireTableDataChanged();
            overlapTableModel.fireTableDataChanged();
         }
      });
      intendedAction.add(new JLabel("Color scheme: "));
      intendedAction.add(pullDownActions);
      JPanel featureToggles = new JPanel(new FlowLayout());
      for (String featureName : allFeatures) {
         Log.global.fine(featureName + " " + misClassEnabledFeatures.contains(featureName));
         JCheckBox check = new JCheckBox(featureName);
         check.setSelected(misClassEnabledFeatures.contains(featureName));
         check.addActionListener(e -> {
            if (((JCheckBox) e.getSource()).isSelected()) {
               misClassEnabledFeatures.add(featureName);
            } else {
               misClassEnabledFeatures.remove(featureName);
            }
            computeValues(misClassEnabledFeatures, categoryVisualizer.getCategoryConfigMap().keySet(), misClassValues, overlapValues,
                  recommendationData);
            addToCategoryOverlapRenderer.setAddToCategory(recommendationData.addToCandidate != null ?
                  recommendationData.addToCandidate.getName() : "");
            misClassTableModel.fireTableDataChanged();
            overlapTableModel.fireTableDataChanged();
            recommendedAction.setText("Recommendation: " + recommendedAction(recommendationData));
         });
         featureToggles.add(check);
      }
      JPanel panel = new JPanel();
      panel.setBorder(BorderFactory.createEtchedBorder());
      panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
      panel.add(vBox1);
      panel.add(vBox2);
      panel.add(intendedAction);
      panel.add(featureToggles);
      panel.add(recommendedAction, JLabel.LEFT);
      return panel;
   }

   private AbstractTableModel misClassTableModel(List<List<Float>> values) {
      List<String> columnNames = new ArrayList<>();
      for (Category category : categoryVisualizer.getCategoryConfigMap().keySet()) {
         columnNames.add(category.getName());
      }
      columnNames.add(UNKNOWN);

      return new AbstractTableModel() {
         @Override
         public String getColumnName(int column) {
            if (column == 0) {
               return "⮟ Point cloud  ╲  Category ⮞";
            } else {
               return columnNames.get(column - 1);
            }
         }

         @Override
         public int getRowCount() {
            return categoryVisualizer.getCategoryConfigMap().size();
         }

         @Override
         public int getColumnCount() {
            return columnNames.size() + 1;
         }

         @Override
         public Object getValueAt(int row, int column) {
            if (column == 0) {
               return columnNames.get(row);
            } else {
               return values.get(row).get(column - 1);
            }
         }

         @Override
         public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
         }
      };
   }

   private AbstractTableModel overlapTableModel(List<List<Float>> values) {
      List<String> columnNames = new ArrayList<>();
      for (Category category : categoryVisualizer.getCategoryConfigMap().keySet()) {
         columnNames.add(category.getName());
      }

      return new AbstractTableModel() {
         @Override
         public String getColumnName(int column) {
            if (column == 0) {
               return "⮟ Point cloud  ╲  Category ⮞";
            } else {
               return columnNames.get(column - 1);
            }
         }

         @Override
         public int getRowCount() {
            return categoryVisualizer.getCategoryConfigMap().size();
         }

         @Override
         public int getColumnCount() {
            return columnNames.size() + 1;
         }

         @Override
         public Object getValueAt(int row, int column) {
            if (column == 0) {
               return columnNames.get(row);
            } else {
               return values.get(row).get(column - 1);
            }
         }

         @Override
         public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
         }
      };
   }
}
