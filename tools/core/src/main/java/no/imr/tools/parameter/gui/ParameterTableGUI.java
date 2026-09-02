package no.imr.tools.parameter.gui;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.ParameterException;
import no.imr.tools.parameter.PasswordParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.gui.input.ParameterGuiUtils;
import no.imr.tools.parameter.gui.input.ParameterListCellRenderer;
import no.imr.tools.swing.ComboBoxListModel;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.SimpleDocumentListener;
import no.imr.tools.swing.UiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.table.MultiLineHeaderRenderer;
import no.imr.tools.swing.table.TableUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * A table for editing parameters.
 *
 * @see ParameterTableModel
 * @see BaseParameter
 */
public final class ParameterTableGUI<T extends ParameterContainer> {
   private final ParameterTableModel<T> model;
   private final JTable table;
   private @Nullable JDialog errorDialog;

   public ParameterTableGUI(ParameterTableModel<T> model) {
      this.model = model;
      table = new ParameterTable(model);
      table.getTableHeader().setReorderingAllowed(false);
      table.setColumnSelectionAllowed(true);
      addPopupMenuMouseListener(table);
      table.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_BACK_SPACE, KeyEvent.VK_DELETE -> {
                  if (e.getModifiersEx() == 0) {
                     TableUtils.setValueAtSelection(table, "");
                  }
               }
               case KeyEvent.VK_V -> {
                  if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
                     TableUtils.pasteFromClipboard(table);
                  }
               }
               default -> {
               }
            }
         }
      });
      table.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !table.isShowing()) {
            closeErrorDialog();
         }
      });
   }

   public JTable getTable() {
      return table;
   }

   public ParameterTableModel<T> getModel() {
      return model;
   }

   public JPanel createPanel() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(BorderFactory.createEtchedBorder());
      panel.add(table.getTableHeader(), BorderLayout.NORTH);
      panel.add(table);
      return panel;
   }

   public JScrollPane createScrollPane() {
      JScrollPane scrollPane = new JScrollPane(table);
      addPopupMenuMouseListener(scrollPane);
      return scrollPane;
   }

   public void addPopupMenuMouseListener(JComponent component) {
      component.addMouseListener(new PopupMenuMouseListener(this::makePopupMenu));
   }

   private @Nullable JPopupMenu makePopupMenu(MouseEvent mouseEvent) {
      if (!CurrentInputComponent.commitEdit()) {
         return null;
      }

      JPopupMenu menu = new JPopupMenu();

      int viewRow = TableUtils.rowAtPoint(table, mouseEvent.getPoint());
      if (viewRow >= 0 && !table.isRowSelected(viewRow)) {
         table.setRowSelectionInterval(viewRow, viewRow);
         int viewColumn = table.columnAtPoint(mouseEvent.getPoint());
         if (viewColumn >= 0) {
            table.setColumnSelectionInterval(viewColumn, viewColumn);
         } else {
            table.setColumnSelectionInterval(0, table.getColumnCount() - 1);
         }
      }

      int[] selectedModelRows = TableUtils.getSelectedModelRows(table);

      if (selectedModelRows.length == 0) {
         JMenuItem addItem = MiscIcons.ADD.on(menu.add("Add row"));
         addItem.setEnabled(model.isEditable());
         addItem.addActionListener(_ -> addRowAndSelect(model.getRowCount()));
      } else {
         JMenuItem addBeforeItem = MiscIcons.ADD.on(menu.add("Insert row before"));
         addBeforeItem.setMnemonic(KeyEvent.VK_B);
         addBeforeItem.setEnabled(model.isEditable());
         addBeforeItem.addActionListener(_ -> addRowAndSelect(selectedModelRows[0]));

         JMenuItem addAfterItem = menu.add("Insert row after");
         addAfterItem.setMnemonic(KeyEvent.VK_A);
         addAfterItem.setEnabled(model.isEditable());
         addAfterItem.addActionListener(_ -> addRowAndSelect(selectedModelRows[selectedModelRows.length - 1] + 1));

         JMenuItem duplicateItem = menu.add("Duplicate row" + (selectedModelRows.length == 1 ? "" : "s"));
         duplicateItem.setMnemonic(KeyEvent.VK_U);
         duplicateItem.setEnabled(model.isEditable());
         duplicateItem.addActionListener(_ -> {
            int insertionIndex = selectedModelRows[selectedModelRows.length - 1] + 1;
            for (int i = 0; i < selectedModelRows.length; i++) {
               model.copyRow(selectedModelRows[i], insertionIndex + i);
            }
            selectRowInterval(insertionIndex, insertionIndex + selectedModelRows.length - 1);
         });

         JMenuItem removeItem = MiscIcons.DELETE.on(menu.add("Delete row" + (selectedModelRows.length == 1 ? "" : "s")));
         removeItem.setMnemonic(KeyEvent.VK_D);
         removeItem.setEnabled(model.isEditable());
         removeItem.addActionListener(_ -> {
            for (int i = selectedModelRows.length - 1; i >= 0; i--) {
               model.removeRow(selectedModelRows[i]);
            }
         });
      }

      menu.addSeparator();

      JMenuItem copyItem = MiscIcons.COPY.on(menu.add("Copy selection to clipboard"));
      copyItem.setMnemonic(KeyEvent.VK_C);
      copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
      copyItem.addActionListener(_ -> TableUtils.copyToClipboard(table));

      JMenuItem pasteItem = MiscIcons.PASTE.on(menu.add("Paste from clipboard"));
      pasteItem.setMnemonic(KeyEvent.VK_P);
      pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
      pasteItem.setEnabled(model.isEditable());
      pasteItem.addActionListener(_ -> TableUtils.pasteFromClipboard(table));

      return menu;
   }

   private void addRowAndSelect(int row) {
      model.addRow(row);
      selectRowInterval(row, row);
   }

   private void selectRowInterval(int index0, int index1) {
      int viewRow0 = table.convertRowIndexToView(index0);
      int viewRow1 = table.convertRowIndexToView(index1);
      table.setRowSelectionInterval(viewRow0, viewRow1);
      table.setColumnSelectionInterval(0, table.getColumnCount() - 1);
      table.scrollRectToVisible(table.getCellRect(viewRow0, 0, true));
   }

   private void closeErrorDialog() {
      if (errorDialog != null) {
         errorDialog.dispose();
         errorDialog = null;
      }
   }

   private final class ParameterTable extends JTable {
      private boolean lastEditOK;

      private ParameterTable(ParameterTableModel<T> parameterTableModel) {
         super(parameterTableModel);

         getTableHeader().addMouseListener(new PopupMenuMouseListener(mouseEvent -> {
            return makePopupMenu(SwingUtilities.convertMouseEvent(getTableHeader(), mouseEvent, this));
         }));

         FocusListener focusListener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
               CurrentInputComponent.set(ParameterTable.this, () -> {
                  TableUtils.stopCellEditing(ParameterTable.this);
                  return lastEditOK;
               });
            }

            @Override
            public void focusLost(FocusEvent e) {
               TableUtils.stopCellEditing(ParameterTable.this);
            }
         };

         MultiLineHeaderRenderer multiLineHeaderRenderer = new MultiLineHeaderRenderer();

         int columnCount = getColumnModel().getColumnCount();
         for (int i = 0; i < columnCount; i++) {
            TableColumn tableColumn = getColumnModel().getColumn(i);
            tableColumn.setHeaderRenderer(multiLineHeaderRenderer);
            ValueParameter<?> parameter = parameterTableModel.getHeaderParameter(i);

            if (parameter instanceof BooleanParameter) {
               continue;
            }

            DefaultCellEditor cellEditor = createCellEditor(parameter);
            tableColumn.setCellEditor(cellEditor);
            cellEditor.getComponent().addFocusListener(focusListener);

            if (parameter instanceof PasswordParameter) {
               tableColumn.setCellRenderer(new DefaultTableCellRenderer() {
                  @Override
                  protected void setValue(@Nullable Object value) {
                     char echoChar = UiUtils.passwordFieldEchoChar();
                     int n = value != null ? value.toString().length() : 0;
                     super.setValue(Character.toString(echoChar).repeat(n));
                  }
               });
            }
         }
      }

      private static <V> DefaultCellEditor createCellEditor(ValueParameter<V> parameter) {
         List<V> allowedValues = parameter.getAllowedValues();
         List<V> values = allowedValues != null ? allowedValues : parameter.getSuggestedValues();
         if (!values.isEmpty()) {
            List<String> stringValues = values.stream()
                  .map(parameter::toValueString)
                  .toList();
            JComboBox<String> comboBox = new JComboBox<>(new ComboBoxListModel<>(parameter.getStringValue(), stringValues, false));
            comboBox.setRenderer(new ParameterListCellRenderer<>(parameter, values, stringValues));
            comboBox.setEditable(allowedValues == null);
            return new DefaultCellEditor(comboBox);
         }

         JTextField textField = parameter instanceof PasswordParameter ? new JPasswordField() : new JTextField();
         textField.getDocument().addDocumentListener(new SimpleDocumentListener(_ -> {
            if (!textField.hasFocus()) {
               textField.requestFocusInWindow();
            }
         }));
         return new DefaultCellEditor(textField);
      }

      @Override
      protected JTableHeader createDefaultTableHeader() {
         return new JTableHeader(getColumnModel()) {
            @Override
            public @Nullable String getToolTipText(MouseEvent event) {
               int columnIndex = TableUtils.pointToModelColumn(ParameterTable.this, event.getPoint());
               if (columnIndex < 0) {
                  return null;
               }
               ValueParameter<?> parameter = model.getHeaderParameter(columnIndex);
               return model.getParameterToHeaderToolTip().apply(parameter);
            }
         };
      }

      @Override
      public @Nullable String getToolTipText(MouseEvent event) {
         Point point = event.getPoint();
         int columnIndex = TableUtils.pointToModelColumn(this, point);
         if (columnIndex < 0) {
            return null;
         }
         int rowIndex = TableUtils.pointToModelRow(this, point);
         if (rowIndex < 0) {
            return null;
         }
         ValueParameter<?> parameter = model.getParameter(rowIndex, columnIndex);
         return model.getParameterToInputToolTip().apply(parameter);
      }

      @Override
      public void setValueAt(Object aValue, int row, int column) {
         lastEditOK = false;
         try {
            super.setValueAt(aValue, row, column);
            lastEditOK = true;
         } catch (ParameterException e) {
            if (errorDialog == null) {
               try {
                  errorDialog = ParameterGuiUtils.createErrorDialog(e, this);
                  errorDialog.setVisible(true);
               } finally {
                  closeErrorDialog();
                  SwingUtilities.invokeLater(() -> {
                     editCellAt(row, column);
                     Component editorComponent = getEditorComponent();
                     if (editorComponent != null) {
                        if (editorComponent instanceof JComboBox<?> comboBox && comboBox.isEditable()) {
                           editorComponent = comboBox.getEditor().getEditorComponent();
                        }
                        editorComponent.requestFocusInWindow();
                     }
                  });
               }
            }
         }
      }
   }
}
