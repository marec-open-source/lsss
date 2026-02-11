package no.imr.lsss.database.util;

import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.misc.TextFilter;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.PreferredSizeLayout;
import no.imr.tools.swing.SimpleDocumentListener;
import no.imr.tools.swing.table.TableUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class SurveySelectionDialog {
   private final List<Survey> allSurveys;
   private List<Survey> filteredSurveys;
   private final Set<Survey> initiallySelectedSurveys;
   private final Set<Survey> currentlySelectedSurveys;
   private final SurveySelectionTableModel tableModel;
   private final JTable table;
   private final JTextField filterTextField = new JTextField("", 20);
   private final JDialog dialog;
   private final Component referenceComponent;

   public SurveySelectionDialog(List<Survey> allSurveys, Set<Survey> selectedSurveys, Component referenceComponent) {
      this.allSurveys = allSurveys;
      filteredSurveys = allSurveys;
      initiallySelectedSurveys = selectedSurveys;
      currentlySelectedSurveys = new HashSet<>(selectedSurveys);

      dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), "Select surveys", Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

      this.referenceComponent = referenceComponent;

      tableModel = new SurveySelectionTableModel();
      table = new JTable(tableModel);
      table.setAutoCreateRowSorter(true);

      TableColumn selectionColumn = table.getColumnModel().getColumn(0);
      selectionColumn.setHeaderRenderer(new SelectionHeaderRenderer());
      selectionColumn.setMaxWidth(30);
      selectionColumn.setMinWidth(30);

      table.getColumnModel().getColumn(1).setPreferredWidth(400);
      table.getColumnModel().getColumn(2).setPreferredWidth(200);
      table.getColumnModel().getColumn(3).setPreferredWidth(200);

      table.getTableHeader().addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            int column = TableUtils.pointToModelColumn(table, e.getPoint());
            if (column == 0) {
               if (currentlySelectedSurveys.containsAll(filteredSurveys)) {
                  filteredSurveys.forEach(currentlySelectedSurveys::remove);
               } else {
                  currentlySelectedSurveys.addAll(filteredSurveys);
               }
               update();
            }
         }
      });

      JPanel bottomPanel = new JPanel(new BorderLayout());
      bottomPanel.add(PreferredSizeLayout.wrap(createFilterPanel(), PreferredSizeLayout.HorizontalAlignment.LEFT));
      bottomPanel.add(createButtonPanel(), BorderLayout.EAST);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(new JScrollPane(table));
      mainPanel.add(bottomPanel, BorderLayout.SOUTH);

      dialog.add(mainPanel);
   }

   private JPanel createFilterPanel() {
      filterTextField.getDocument().addDocumentListener(new SimpleDocumentListener(_ -> {
         TextFilter filter = new TextFilter(filterTextField.getText());
         filteredSurveys = allSurveys.stream()
               .filter(survey -> passes(filter, survey))
               .toList();
         update();
      }));

      JLabel filterLabel = new JLabel("Filter: ");
      filterLabel.setToolTipText("<html>Displays only matching surveys<br>Prefix a word with - to exclude");
      filterLabel.setDisplayedMnemonic(KeyEvent.VK_F);
      GuiUtils.setAccelerator(filterLabel, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.ALT_DOWN_MASK), filterTextField::requestFocusInWindow);

      JPanel filterPanel = new JPanel(new GridBagLayout());
      GridBagConstraints filterPanelConstraints = new GridBagConstraints();
      filterPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
      filterPanelConstraints.weightx = 1;
      filterPanel.add(Box.createHorizontalStrut(5));
      filterPanel.add(filterLabel);
      filterPanel.add(filterTextField, filterPanelConstraints);
      return filterPanel;
   }

   private JPanel createButtonPanel() {
      JButton okButton = new JButton("OK");
      okButton.addActionListener(_ -> {
         initiallySelectedSurveys.clear();
         initiallySelectedSurveys.addAll(currentlySelectedSurveys);
         dialog.dispose();
      });
      dialog.getRootPane().setDefaultButton(okButton);

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(_ -> dialog.dispose());
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE, () -> {
         if (filterTextField.getText().isEmpty()) {
            dialog.dispose();
         } else {
            filterTextField.setText("");
            filterTextField.requestFocusInWindow();
         }
      });
      SwingUtilities.invokeLater(filterTextField::requestFocusInWindow);

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.add(okButton);
      panel.add(cancelButton);
      return panel;
   }

   public void show() {
      dialog.setSize(900, 600);
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   private void update() {
      tableModel.fireTableDataChanged();
      table.getTableHeader().repaint();
   }

   private static boolean passes(TextFilter filter, Survey survey) {
      return filter.test(List.of(
            survey.getPlatform().getNation().getNationName(),
            survey.getPlatform().findPlatformName(survey),
            survey.getSurveyTitle()
      ));
   }

   private final class SelectionHeaderRenderer extends JCheckBox implements TableCellRenderer {
      private SelectionHeaderRenderer() {
         setBorder(BorderFactory.createCompoundBorder(
               UIManager.getBorder("TableHeader.cellBorder"),
               BorderFactory.createEmptyBorder(0, 0, 0, 3) // For centering
         ));
         setBorderPainted(true);
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         boolean notEmpty = !filteredSurveys.isEmpty();
         setSelected(notEmpty && currentlySelectedSurveys.containsAll(filteredSurveys));
         setEnabled(notEmpty);
         setHorizontalAlignment(CENTER);
         return this;
      }
   }

   private final class SurveySelectionTableModel extends AbstractTableModel {
      private final List<Column<Survey, ?>> columns = List.of(
            new Column<>("", Boolean.class, currentlySelectedSurveys::contains),
            new Column<>("Survey", String.class, Survey::getSurveyTitle),
            new Column<>("Platform", String.class, survey -> survey.getPlatform().findPlatformName(survey)),
            new Column<>("Nation", String.class, survey -> survey.getPlatform().getNation().getNationName())
      );

      private SurveySelectionTableModel() {
      }

      @Override
      public int getRowCount() {
         return filteredSurveys.size();
      }

      @Override
      public int getColumnCount() {
         return columns.size();
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         return columns.get(columnIndex).valueExtractor.apply(filteredSurveys.get(rowIndex));
      }

      @Override
      public String getColumnName(int columnIndex) {
         return columns.get(columnIndex).title;
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
         return columns.get(columnIndex).columnClass;
      }

      @Override
      public boolean isCellEditable(int rowIndex, int columnIndex) {
         return columnIndex == 0;
      }

      @Override
      public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
         if (columnIndex == 0) {
            Survey survey = filteredSurveys.get(rowIndex);
            if ((Boolean) aValue) {
               currentlySelectedSurveys.add(survey);
            } else {
               currentlySelectedSurveys.remove(survey);
            }
            table.getTableHeader().repaint();
         }
      }
   }

   private record Column<T, C>(String title, Class<C> columnClass, Function<T, C> valueExtractor) {
   }
}
