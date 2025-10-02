package no.imr.lsss.modules.interpretation;

import no.imr.korona.region.Interpretation;
import no.imr.lsss.resources.LsssIcons;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.table.TableCellButton;
import no.imr.tools.swing.table.TableCellFloat;
import no.imr.tools.swing.table.TableCellMultiClass;
import no.imr.tools.swing.table.TableCellSlider;
import no.imr.tools.swing.table.TableCellString;
import no.imr.tools.swing.table.TableUtils;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A table for assigning sA to acoustic categories.
 */
public final class InterpretationTable extends JTable {
   private int columnIndex = -1;

   public InterpretationTable(InterpretationTableModel interpretationTableModel) {
      super(interpretationTableModel);

      DefaultTableCellRenderer rightAlignedCellRenderer = TableUtils.defaultTableCellRenderer(DefaultTableCellRenderer.RIGHT);
      TableCellFloat.Editor tableCellFloatEditor = new TableCellFloat.Editor();

      addMouseMotionListener(new MouseAdapter() {
         @Override
         public void mouseMoved(MouseEvent e) {
            columnIndex = TableUtils.pointToModelColumn(InterpretationTable.this, e.getPoint());
            if (columnIndex == InterpretationTableModel.KORONA_COLUMN) {
               requestFocusInWindow();
            }
         }
      });
      addKeyListener(new KeyAdapter() {
         @Override
         public void keyTyped(KeyEvent e) {
            if (columnIndex == InterpretationTableModel.KORONA_COLUMN && e.getKeyChar() == ' ') {
               interpretationTableModel.shiftKoronaAction(e.isShiftDown() ? -1 : 1);
            }
         }
      });

      setBackground(InterpretationModuleView.BACKGROUND_COLOR);
      setCellSelectionEnabled(false);
      getTableHeader().setReorderingAllowed(false);

      TableColumn nameColumn = getColumnModel().getColumn(InterpretationTableModel.SPECIES_COLUMN);
      nameColumn.setCellRenderer(new TableCellString.Renderer(new TableCellString.RenderSettings(null, null, Color.BLACK)));
      nameColumn.setPreferredWidth(120);

      TableColumn assignmentColumn = getColumnModel().getColumn(InterpretationTableModel.ASSIGNMENT_COLUMN);
      TableCellMultiClass.Renderer sliderColumnRenderer = new TableCellMultiClass.Renderer();
      sliderColumnRenderer.addRenderer(TableCellSlider.SliderSetting.class, new TableCellSlider.Renderer());
      assignmentColumn.setCellRenderer(sliderColumnRenderer);
      assignmentColumn.setCellEditor(new TableCellSlider.Editor());
      assignmentColumn.setPreferredWidth(200);

      TableColumn percentColumn = getColumnModel().getColumn(InterpretationTableModel.PERCENT_COLUMN);
      percentColumn.setCellRenderer(new PercentColumnRenderer());
      percentColumn.setCellEditor(tableCellFloatEditor);
      percentColumn.setPreferredWidth(70);

      TableColumn saColumn = getColumnModel().getColumn(InterpretationTableModel.SA_COLUMN);
      saColumn.setCellRenderer(rightAlignedCellRenderer);
      saColumn.setCellEditor(tableCellFloatEditor);
      saColumn.setPreferredWidth(100);

      TableColumn restColumn = getColumnModel().getColumn(InterpretationTableModel.REST_COLUMN);
      TableCellMultiClass.Renderer restColumnRenderer = new TableCellMultiClass.Renderer();
      restColumn.setCellRenderer(restColumnRenderer);
      restColumn.setPreferredWidth(45);

      TableColumn koronaColumn = getColumnModel().getColumn(InterpretationTableModel.KORONA_COLUMN);
      TableCellMultiClass.Renderer koronaColumnRenderer = new TableCellMultiClass.Renderer();
      koronaColumnRenderer.addRenderer(TableCellButton.ButtonSetting.class, new TableCellButton.Renderer());
      koronaColumn.setCellRenderer(koronaColumnRenderer);
      koronaColumn.setCellEditor(new TableCellButton.Editor());
      koronaColumn.setPreferredWidth(25);
      JLabel koronaLabel = LsssIcons.KORONA.on(new JLabel());
      koronaColumn.setHeaderRenderer((table, value, isSelected, hasFocus, row, column) -> koronaLabel);
   }

   private static final class PercentColumnRenderer extends DefaultTableCellRenderer {
      private PercentColumnRenderer() {
         setHorizontalAlignment(RIGHT);
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         Color foreground = null;
         String tooltip = null;
         if (value instanceof InterpretationTableModel.PercentColumnValue v) {
            value = v.toString();
            if (row == table.getRowCount() - 1 && !Interpretation.isCompletelyAssigned(v.assignment())
                  && !((InterpretationTableModel) table.getModel()).getInterpretationManager().getInterpretationContainers().isEmpty()
            ) {
               foreground = ColorUtils.TOMATO;
               tooltip = "Total assignment " + Interpretation.assignmentToFullPresicionPercentString(v.assignment()) + "% ≠ 100%";
            }
         }
         setForeground(foreground);
         setToolTipText(tooltip);
         return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }
   }
}
