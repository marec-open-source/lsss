package no.imr.korona.computation.categorization;

import com.google.common.base.Strings;
import no.imr.korona.computation.categorization.apriori.GeoAPriori;
import no.imr.korona.computation.categorization.apriori.gui.GeoAPrioriEditor;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.korona.computation.feature.FeatureRequirementFactory;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.korona.resources.KoronaHelp;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.compile.CompileException;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.table.MultiLineHeaderRenderer;
import no.imr.tools.swing.table.TableCellCallbackEditor;
import no.imr.tools.swing.table.TableCellColorRenderer;
import no.imr.tools.swing.table.TableColumnInfo;
import no.imr.tools.swing.table.TableUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class ConfiguratorEditor {
   private static final class FeatureTableModel extends AbstractTableModel {
      private static final class Row {
         private final FeatureExtractor featureExtractor;
         private boolean enabled;

         private Row(FeatureExtractor featureExtractor) {
            this.featureExtractor = featureExtractor;
            enabled = featureExtractor.isEnabled();
         }
      }

      private static final int ENABLED_COLUMN = 0;
      private static final int NAME_COLUMN = 1;

      private final TableColumnInfo[] columnInfos = {
            new TableColumnInfo("Enabled", Boolean.class),
            new TableColumnInfo("Name", String.class),
      };

      private final List<Row> rows = new ArrayList<>();

      private FeatureTableModel(Configurator configurator) {
         for (FeatureExtractor featureExtractor : configurator.getFeatureExtractors()) {
            rows.add(new Row(featureExtractor));
         }
      }

      @Override
      public int getColumnCount() {
         return columnInfos.length;
      }

      @Override
      public int getRowCount() {
         return rows.size();
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         Row row = rows.get(rowIndex);
         return switch (columnIndex) {
            case ENABLED_COLUMN -> row.enabled;
            case NAME_COLUMN -> row.featureExtractor.getFeatureName();
            default -> throw new IllegalArgumentException(Integer.toString(columnIndex));
         };
      }

      @Override
      public String getColumnName(int column) {
         return columnInfos[column].name();
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
         return columnInfos[columnIndex].columnClass();
      }

      @Override
      public boolean isCellEditable(int rowIndex, int columnIndex) {
         return columnIndex == ENABLED_COLUMN;
      }

      @Override
      public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
         Row row = rows.get(rowIndex);
         switch (columnIndex) {
            case ENABLED_COLUMN -> row.enabled = (Boolean) aValue;
            default -> throw new IllegalArgumentException(rowIndex + ", " + columnIndex + ": " + aValue);
         }
      }

      private void acceptEdits() {
         for (Row row : rows) {
            row.featureExtractor.setEnabled(row.enabled);
         }
      }
   }

   private static final class CategoryTableModel extends AbstractTableModel {
      private final class Row {
         private final Category category;

         private boolean enabled;
         private String name;
         private String legend;
         private @Nullable String comment;
         private Color color;
         private Category.Type type;
         private float aPriori;
         private @Nullable Float schoolAPriori;
         private @Nullable GeoAPriori geoAPriori;
         private @Nullable Float minSv38;
         private @Nullable Float maxSv38;
         private String featureRequirementExpression;

         private Row(Category category) {
            this.category = category;
            enabled = category.isEnabled();
            name = category.getName();
            legend = category.getLegend();
            comment = category.getComment();
            color = category.getColor();
            type = category.getType();
            aPriori = category.getApriori();
            schoolAPriori = category.getSchoolApriori().orElse(null);
            geoAPriori = category.getCategoryAPriori().getGeoAPriori();
            minSv38 = category.getMinLogSv38().orElse(null);
            maxSv38 = category.getMaxLogSv38().orElse(null);
            featureRequirementExpression = category.getFeatureRequirementExpression();
         }

         private void acceptEdits() {
            category.setEnabled(enabled);
            category.setName(name);
            category.setLegend(legend);
            category.setComment(comment);
            category.setColor(color);
            if (!category.isSpecial()) {
               category.setType(type);
               category.setApriori(aPriori);
               category.setSchoolApriori(Optional.ofNullable(schoolAPriori));
               category.getCategoryAPriori().setGeoAPriori(geoAPriori == null || geoAPriori.getPolygons().isEmpty() ? null : geoAPriori.copy());
               category.setMinLogSv38(Optional.ofNullable(minSv38));
               category.setMaxLogSv38(Optional.ofNullable(maxSv38));
               try {
                  category.setFeatureRequirementExpression(featureRequirementExpression);
               } catch (CompileException e) {
                  // Compilability is ensured in setValueAt
                  throw new ShouldNotHappenException(e);
               }
            }
         }

         private boolean isCellEditable(int columnIndex) {
            if (category.isSpecial()) {
               return columnIndex == LEGEND_COLUMN
                     || columnIndex == COMMENT_COLUMN
                     || columnIndex == COLOR_COLUMN;
            }
            return true;
         }

         private @Nullable Object getValueAt(int columnIndex) {
            if (category.isSpecial() && columnIndex > COLOR_COLUMN) {
               return null;
            }
            return switch (columnIndex) {
               case ENABLED_COLUMN -> enabled;
               case NAME_COLUMN -> name;
               case LEGEND_COLUMN -> legend;
               case COMMENT_COLUMN -> comment;
               case COLOR_COLUMN -> color;
               case TYPE_COLUMN -> type;
               case A_PRIORI_COLUMN -> aPriori;
               case SCHOOL_A_PRIORI_COLUMN -> schoolAPriori;
               case GEO_A_PRIORI_COLUMN -> geoAPriori != null ? "Edit..." : "New...";
               case MIN_SV_38_COLUMN -> minSv38;
               case MAX_SV_38_COLUMN -> maxSv38;
               case REQUIREMENT_COLUMN -> featureRequirementExpression;
               default -> throw new IllegalArgumentException(Integer.toString(columnIndex));
            };
         }

         private void setValueAt(Object value, int columnIndex) {
            if (category.isSpecial() && !isCellEditable(columnIndex)) {
               throw new IllegalArgumentException(columnIndex + ": " + value);
            }

            lastSetValueOK = true;

            switch (columnIndex) {
               case ENABLED_COLUMN -> {
                  enabled = (Boolean) value;
               }
               case NAME_COLUMN -> {
                  String newName = ((String) value).trim();
                  Category existingCategory = configurator.getCategory(newName);
                  Row row = getRow(newName);
                  if (!newName.matches(Configurator.CATEGORY_NAME_REG_EXP)) {
                     showErrorMessage(rows.indexOf(this), NAME_COLUMN,
                           "Illegal category name: \"" + newName + "\""
                                 + "\nA category name must match the regular expression \""
                                 + Configurator.CATEGORY_NAME_REG_EXP + "\"");
                  } else if (row != null && row != this) {
                     showErrorMessage(rows.indexOf(this), NAME_COLUMN,
                           "Another category is named \"" + newName + "\""
                                 + "\nCategory names must be unique.");
                  } else if (existingCategory != null && existingCategory != category) {
                     showErrorMessage(rows.indexOf(this), NAME_COLUMN,
                           "A saved category is named \"" + newName + "\""
                                 + "\nSave current changes before renaming \""
                                 + name + "\" to \"" + newName + "\"");
                  } else {
                     // new name is acceptable
                     name = newName;
                  }
               }
               case LEGEND_COLUMN -> {
                  String newLegend = ((String) value).trim();
                  if (newLegend.length() > Configurator.CATEGORY_LEGEND_MAX_LENGTH) {
                     showErrorMessage(rows.indexOf(this), LEGEND_COLUMN,
                           "Too long legend: " + newLegend
                                 + "\nUse maximum " + Configurator.CATEGORY_LEGEND_MAX_LENGTH + " characters");
                  } else {
                     legend = newLegend;
                  }
               }
               case COMMENT_COLUMN -> {
                  comment = (String) value;
               }
               case COLOR_COLUMN -> {
                  color = (Color) value;
               }
               case TYPE_COLUMN -> {
                  type = (Category.Type) value;
               }
               case A_PRIORI_COLUMN -> {
                  if (value instanceof Float f) {
                     aPriori = f;
                  }
               }
               case SCHOOL_A_PRIORI_COLUMN -> {
                  schoolAPriori = (Float) value;
               }
               case GEO_A_PRIORI_COLUMN -> {
                  geoAPriori = (GeoAPriori) value;
               }
               case MIN_SV_38_COLUMN -> {
                  minSv38 = (Float) value;
               }
               case MAX_SV_38_COLUMN -> {
                  maxSv38 = (Float) value;
               }
               case REQUIREMENT_COLUMN -> {
                  String requirement = (String) value;
                  try {
                     FeatureRequirementFactory.create(requirement);
                     featureRequirementExpression = requirement;
                  } catch (CompileException e) {
                     showErrorMessage(rows.indexOf(this), LEGEND_COLUMN,
                           "Invalid expression: " + requirement + "\n" + e.getMessage());
                  }
               }
               default -> {
                  throw new IllegalArgumentException(columnIndex + ": " + value);
               }
            }
         }

         private void showErrorMessage(int rowIndex, int columnIndex, String errorMessage) {
            lastSetValueOK = false;
            table.setRowSelectionInterval(rowIndex, rowIndex);
            JOptionPane.showMessageDialog(table,
                  errorMessage,
                  null, JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(() -> {
               table.setRowSelectionInterval(rowIndex, rowIndex);
               table.editCellAt(rowIndex, columnIndex);
               table.getEditorComponent().requestFocus();
            });
         }
      }

      private static final int ENABLED_COLUMN = 0;
      private static final int NAME_COLUMN = 1;
      private static final int LEGEND_COLUMN = 2;
      private static final int COMMENT_COLUMN = 3;
      private static final int COLOR_COLUMN = 4;
      private static final int TYPE_COLUMN = 5;
      private static final int A_PRIORI_COLUMN = 6;
      private static final int SCHOOL_A_PRIORI_COLUMN = 7;
      private static final int GEO_A_PRIORI_COLUMN = 8;
      private static final int MIN_SV_38_COLUMN = 9;
      private static final int MAX_SV_38_COLUMN = 10;
      private static final int REQUIREMENT_COLUMN = 11;

      private final Configurator configurator;
      private final JTable table;

      private final TableColumnInfo[] columnInfos = {
            new TableColumnInfo("Enabled", Boolean.class),
            new TableColumnInfo("Name", String.class),
            new TableColumnInfo("Legend", String.class),
            new TableColumnInfo("Com-\nment", String.class),
            new TableColumnInfo("Color", Color.class),
            new TableColumnInfo("Type", Category.Type.class),
            new TableColumnInfo("Overall\na priori", Float.class),
            new TableColumnInfo("School\na priori", Float.class),
            new TableColumnInfo("Geo\na priori", String.class),
            new TableColumnInfo("Min\nSv 38", Float.class),
            new TableColumnInfo("Max\nSv 38", Float.class),
            new TableColumnInfo("Requirement", String.class),
      };
      private final List<Row> rows = new ArrayList<>();
      private boolean lastSetValueOK = true;

      private CategoryTableModel(JTable table, Configurator configurator) {
         this.table = table;
         this.configurator = configurator;
         for (Category category : configurator.getAllCategories()) {
            rows.add(new Row(category));
         }
      }

      @Override
      public int getRowCount() {
         return rows.size();
      }

      @Override
      public int getColumnCount() {
         return columnInfos.length;
      }

      @Override
      public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
         Row row = rows.get(rowIndex);
         return row.getValueAt(columnIndex);
      }

      @Override
      public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
         Row row = rows.get(rowIndex);
         row.setValueAt(aValue, columnIndex);
      }

      @Override
      public boolean isCellEditable(int rowIndex, int columnIndex) {
         Row row = rows.get(rowIndex);
         return row.isCellEditable(columnIndex);
      }

      @Override
      public String getColumnName(int column) {
         return columnInfos[column].name();
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
         return columnInfos[columnIndex].columnClass();
      }

      private @Nullable Row getRow(String name) {
         for (Row row : rows) {
            if (row.name.equals(name)) {
               return row;
            }
         }
         return null;
      }

      private boolean acceptEdits() {
         if (table.isEditing()) {
            int row = table.getEditingRow();
            int column = table.getEditingColumn();
            table.getCellEditor(row, column).stopCellEditing();
         }
         if (!lastSetValueOK) {
            return false;
         }
         List<Category> categories = new ArrayList<>();
         for (Row row : rows) {
            categories.add(row.category);
            row.acceptEdits();
         }
         configurator.setCategories(categories);
         return true;
      }

      private void addRow() {
         int lastRowIndex = rows.size();
         rows.add(new Row(new Category(configurator, "", Color.BLACK)));
         fireTableRowsInserted(lastRowIndex, lastRowIndex);
      }

      private void removeSelectedRows(int[] rowsToDelete) {
         Arrays.sort(rowsToDelete);
         for (int i = rowsToDelete.length - 1; i >= 0; i--) {
            int rowIndex = rowsToDelete[i];
            Row row = rows.get(rowIndex);
            if (!row.category.isSpecial()) { // don't remove special categories
               rows.remove(rowIndex);
               fireTableRowsDeleted(rowIndex, rowIndex);
            }
         }
      }
   }

   private ConfiguratorEditor() {
   }

   /**
    * Displays a modal dialog for editing a categorization configuration.
    *
    * @param referenceComponent the reference component for this dialog
    * @param configurator       the configurator to edit
    */
   public static void editCategorizationConfiguration(@Nullable Component referenceComponent, Configurator configurator) {
      Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);

      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent),
            "Categorization configuration", Dialog.ModalityType.DOCUMENT_MODAL);
      JPanel categoryPanel = new JPanel(new BorderLayout());
      categoryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(emptyBorder,
                  BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Categories")),
            emptyBorder));

      JTable categoryTable = new JTable() {
         @Override
         public @Nullable String getToolTipText(MouseEvent event) {
            int rowIndex = TableUtils.pointToModelRow(this, event.getPoint());
            if (rowIndex >= 0) {
               int columnIndex = TableUtils.pointToModelColumn(this, event.getPoint());
               if (columnIndex == CategoryTableModel.COMMENT_COLUMN) {
                  CategoryTableModel categoryTableModel = (CategoryTableModel) getModel();
                  String comment = categoryTableModel.rows.get(rowIndex).comment;
                  return comment != null
                        ? new HtmlStringBuilder().multilineText(comment).build()
                        : null;
               }
            }
            return super.getToolTipText(event);
         }
      };
      CategoryTableModel categoryTableModel = new CategoryTableModel(categoryTable, configurator);
      categoryTable.setModel(categoryTableModel);
      categoryTable.getTableHeader().setReorderingAllowed(false);
      categoryTable.setPreferredScrollableViewportSize(new Dimension(600, 200));
      categoryTable.addMouseListener(new PopupMenuMouseListener(mouseEvent -> {
         JPopupMenu menu = new JPopupMenu();

         JMenuItem removeGeoAPrioriItem = MiscIcons.DELETE.on(menu.add("Remove geo a priori from selected categories"));
         removeGeoAPrioriItem.addActionListener(e -> {
            for (int row : categoryTable.getSelectedRows()) {
               categoryTableModel.rows.get(row).geoAPriori = null;
               categoryTableModel.fireTableRowsUpdated(row, row);
            }
         });

         return menu;
      }));

      MultiLineHeaderRenderer multiLineHeaderRenderer = new MultiLineHeaderRenderer();
      for (int i = 0; i < categoryTable.getColumnCount(); i++) {
         categoryTable.getColumnModel().getColumn(i).setHeaderRenderer(multiLineHeaderRenderer);
      }

      TableColumn enabledColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.ENABLED_COLUMN);
      enabledColumn.setMinWidth(55);
      enabledColumn.setMaxWidth(55);

      TableColumn nameColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.NAME_COLUMN);
      nameColumn.setMinWidth(80);
      nameColumn.setMaxWidth(150);
      nameColumn.setPreferredWidth(100);

      TableColumn legendColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.LEGEND_COLUMN);
      legendColumn.setMinWidth(55);
      legendColumn.setMaxWidth(55);

      TableColumn commentColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.COMMENT_COLUMN);
      commentColumn.setCellRenderer(TableUtils.defaultTableCellRenderer(DefaultTableCellRenderer.CENTER));
      commentColumn.setCellEditor(new TableCellCallbackEditor((table, row, column) -> editComment(dialog, categoryTableModel, row)));
      commentColumn.setMinWidth(45);
      commentColumn.setMaxWidth(45);

      TableColumn colorColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.COLOR_COLUMN);
      colorColumn.setMinWidth(40);
      colorColumn.setMaxWidth(40);
      colorColumn.setCellRenderer(new TableCellColorRenderer());
      colorColumn.setCellEditor(new TableCellCallbackEditor((table, row, column) -> editColor(dialog, categoryTableModel, row)));

      TableColumn typeColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.TYPE_COLUMN);
      typeColumn.setMinWidth(80);
      typeColumn.setMaxWidth(80);
      typeColumn.setCellEditor(new DefaultCellEditor(new JComboBox<>(Category.Type.values())));
      if (!KoronaIncubatorFeatureToggles.USE_TRACK_CATEGORIZATION) {
         typeColumn.setMinWidth(0);
         typeColumn.setMaxWidth(0);
      }

      TableColumn aPrioriColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.A_PRIORI_COLUMN);
      aPrioriColumn.setMinWidth(50);
      aPrioriColumn.setMaxWidth(50);

      TableColumn schoolAPrioriColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.SCHOOL_A_PRIORI_COLUMN);
      schoolAPrioriColumn.setMinWidth(50);
      schoolAPrioriColumn.setMaxWidth(50);

      TableColumn geoAPrioriColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.GEO_A_PRIORI_COLUMN);
      geoAPrioriColumn.setMinWidth(50);
      geoAPrioriColumn.setMaxWidth(50);
      geoAPrioriColumn.setCellRenderer(TableUtils.defaultTableCellRenderer(DefaultTableCellRenderer.CENTER));
      geoAPrioriColumn.setCellEditor(new TableCellCallbackEditor((table, row, column) -> editGeoAPriori(dialog, categoryTableModel, row)));

      TableColumn minSvColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.MIN_SV_38_COLUMN);
      minSvColumn.setMinWidth(45);
      minSvColumn.setMaxWidth(45);

      TableColumn maxSvColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.MAX_SV_38_COLUMN);
      maxSvColumn.setMinWidth(45);
      maxSvColumn.setMaxWidth(45);

      TableColumn requirementColumn = categoryTable.getColumnModel().getColumn(CategoryTableModel.REQUIREMENT_COLUMN);
      requirementColumn.setMinWidth(130);
      requirementColumn.setPreferredWidth(130);

      JButton addButton = MiscIcons.ADD.on(new JButton("Add"));
      addButton.setToolTipText("Add a new category");
      addButton.addActionListener(e -> {
         if (categoryTable.isEditing()) {
            categoryTable.getCellEditor().stopCellEditing();
            if (!categoryTableModel.lastSetValueOK) {
               return;
            }
         }
         categoryTableModel.addRow();
         int lastRowIndex = categoryTable.getRowCount() - 1;
         categoryTable.setRowSelectionInterval(lastRowIndex, lastRowIndex);
         categoryTable.editCellAt(lastRowIndex, CategoryTableModel.NAME_COLUMN);
         categoryTable.getEditorComponent().requestFocus();
      });

      JButton removeButton = MiscIcons.DELETE.on(new JButton("Remove"));
      removeButton.setToolTipText("Remove selected categories");
      removeButton.addActionListener(e -> {
         if (categoryTable.isEditing()) {
            categoryTable.getCellEditor().cancelCellEditing();
         }
         categoryTableModel.removeSelectedRows(categoryTable.getSelectedRows());
      });

      GridBag buttonGridBag = new GridBag();
      buttonGridBag.activateHorizontalFill();
      buttonGridBag.addWithLineBreak(addButton);
      buttonGridBag.addWithLineBreak(Box.createVerticalStrut(5));
      buttonGridBag.addWithLineBreak(removeButton);

      JPanel buttonPanelWithoutResizing = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
      buttonPanelWithoutResizing.add(buttonGridBag.getPanel());

      Box buttons = Box.createVerticalBox();
      buttons.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
      buttons.add(Box.createVerticalGlue());
      buttons.add(buttonPanelWithoutResizing);
      buttons.add(Box.createVerticalGlue());

      categoryPanel.add(new JScrollPane(categoryTable));
      categoryPanel.add(buttons, BorderLayout.EAST);

      //---

      JPanel featurePanel = new JPanel(new BorderLayout());
      featurePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(emptyBorder,
                  BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Features")),
            emptyBorder));

      JTable featureTable = new JTable();
      FeatureTableModel featureTableModel = new FeatureTableModel(configurator);
      featureTable.setModel(featureTableModel);
      featureTable.getTableHeader().setReorderingAllowed(false);
      featureTable.setPreferredScrollableViewportSize(new Dimension(130, 200));

      TableColumn featureEnabledColumn = featureTable.getColumnModel().getColumn(FeatureTableModel.ENABLED_COLUMN);
      featureEnabledColumn.setMinWidth(55);
      featureEnabledColumn.setMaxWidth(55);

      featurePanel.add(new JScrollPane(featureTable));

      //---

      JButton okButton = new JButton("OK");
      okButton.addActionListener(e -> {
         if (categoryTableModel.acceptEdits()) {
            featureTableModel.acceptEdits();
            dialog.dispose();
         }
      });

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(e -> dialog.dispose());

      JButton helpButton = new JButton("Help");
      KoronaHelp.CATEGORIZATION_LIBRARY.enableHelpKeyOnButton(helpButton);

      JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonsPanel.add(okButton);
      buttonsPanel.add(cancelButton);
      buttonsPanel.add(helpButton);

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(categoryPanel);
      panel.add(featurePanel, BorderLayout.EAST);
      panel.add(buttonsPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.getContentPane().add(panel);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.pack();
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   private static @Nullable String editComment(Component referenceComponent, CategoryTableModel categoryTableModel, int rowIndex) {
      CategoryTableModel.Row row = categoryTableModel.rows.get(rowIndex);
      TextParameter commentParameter = new TextParameter(new Name("Comment"), Strings.nullToEmpty(row.comment));
      List<TextParameter> parameters = List.of(commentParameter);
      ParameterEditor parameterEditor = new ParameterEditor(parameters);
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      boolean ok = new ConfigurableGUIDialog(referenceComponent, "Comment for " + row.name, new ParameterCollection(parameters))
            .setCloseOnOk(parameterEditor::commitEdits)
            .setGUI(parameterEditor.getEditorComponent())
            .setMinimumSize(800, 0)
            .show();
      return ok ? Strings.emptyToNull(commentParameter.getValue().trim()) : row.comment;
   }

   private static Color editColor(Component referenceComponent, CategoryTableModel categoryTableModel, int rowIndex) {
      CategoryTableModel.Row row = categoryTableModel.rows.get(rowIndex);
      Color color = JColorChooser.showDialog(referenceComponent, "Color for " + row.name, row.color);
      return color != null ? color : row.color;
   }

   private static @Nullable GeoAPriori editGeoAPriori(Component referenceComponent, CategoryTableModel categoryTableModel, int rowIndex) {
      CategoryTableModel.Row row = categoryTableModel.rows.get(rowIndex);
      GeoAPriori geoAPriori = row.geoAPriori;
      AtomicReference<@Nullable GeoAPriori> result = new AtomicReference<>(geoAPriori);
      GeoAPriori editingCopy = geoAPriori != null ? geoAPriori.copy() : new GeoAPriori();

      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent),
            "Geographical a priori for " + row.name, Dialog.ModalityType.DOCUMENT_MODAL);

      JButton okButton = new JButton("OK");
      okButton.addActionListener(e -> {
         result.set(editingCopy);
         dialog.dispose();
      });

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(e -> dialog.dispose());

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);

      JComponent editorComponent = new GeoAPrioriEditor(editingCopy).getComponent();
      editorComponent.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY));

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(editorComponent);
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.add(mainPanel);
      dialog.setSize(800, 800);
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);

      return result.get();
   }
}
