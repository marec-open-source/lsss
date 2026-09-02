package no.imr.lsss.modules.echogramplot;

import no.imr.tools.math.Quantile;
import no.imr.tools.math.WelfordsMethod;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.plot.XYInfo;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.table.TableUtils;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;

final class EchogramPlotStatisticsDialog {
   private final JDialog dialog;
   private final MyTableModel tableModel = new MyTableModel();

   EchogramPlotStatisticsDialog(EchogramPlotModule module) {
      dialog = new JDialog(
            GuiUtils.windowForComponent(module.getComponent()),
            module.getDisplayName() + " - Statistics",
            Dialog.ModalityType.MODELESS);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            module.setStatisticsDialog(null);
         }
      });

      JTable table = new JTable(tableModel);
      TableColumn nameColumn = table.getColumnModel().getColumn(0);
      nameColumn.setPreferredWidth(200);
      table.setDefaultRenderer(float.class, TableUtils.defaultTableCellRenderer(DefaultTableCellRenderer.RIGHT));

      dialog.add(new JScrollPane(table));
      dialog.setSize(1200, 200);
      dialog.setLocationRelativeTo(module.getComponent());
      GuiUtils.clampToScreen(dialog);
      dialog.setVisible(true);
   }

   JDialog getDialog() {
      return dialog;
   }

   void update(List<EchogramPlotDataset> dataSets) {
      List<Row> rows = dataSets.stream()
            .map(EchogramPlotStatisticsDialog::computeRow)
            .toList();
      SwingUtilities.invokeLater(() -> tableModel.setRows(rows));
   }

   private static Row computeRow(EchogramPlotDataset dataSet) {
      float[] nanIncludingValues = dataSet.getYValues();
      float[] values = new float[nanIncludingValues.length];
      int n = 0;
      for (float value : nanIncludingValues) {
         if (!Float.isNaN(value)) {
            values[n++] = value;
         }
      }
      if (n == 0) {
         return new Row(dataSet.getSeriesKey(0),
               Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN);
      }
      Arrays.sort(values, 0, n);

      WelfordsMethod welfordsMethod = new WelfordsMethod();
      for (int i = 0; i < n; i++) {
         welfordsMethod.update(values[i]);
      }

      XYInfo xyInfo = dataSet.getXYInfo(0);
      ExportTransform transform = xyInfo.y().transform();

      return new Row(
            dataSet.getSeriesKey(0),
            (float) transform.applyAsDouble(welfordsMethod.getMean()),
            (float) transform.applyAsDouble(welfordsMethod.getStdDev()),
            (float) transform.applyAsDouble(values[0]),
            (float) transform.applyAsDouble(values[Quantile.quantileIndex(0.1, n)]),
            (float) transform.applyAsDouble(values[n / 2]),
            (float) transform.applyAsDouble(values[Quantile.quantileIndex(0.9, n)]),
            (float) transform.applyAsDouble(values[n - 1])
      );
   }

   private record Row(
         String name,
         float mean,
         float stdDev,
         float min,
         float quantile01,
         float median,
         float quantile09,
         float max
   ) {
   }

   private static final class MyTableModel extends AbstractTableModel {
      private List<Row> rows = List.of();

      private MyTableModel() {
      }

      private void setRows(List<Row> rows) {
         this.rows = rows;
         fireTableDataChanged();
      }

      @Override
      public int getRowCount() {
         return rows.size();
      }

      @Override
      public int getColumnCount() {
         return 8;
      }

      @Override
      public Class<?> getColumnClass(int columnIndex) {
         return columnIndex == 0 ? String.class : float.class;
      }

      @Override
      public String getColumnName(int column) {
         return switch (column) {
            case 0 -> "Name";
            case 1 -> "Mean";
            case 2 -> "Std dev";
            case 3 -> "Min";
            case 4 -> "0.1 quantile";
            case 5 -> "Median";
            case 6 -> "0.9 quantile";
            case 7 -> "Max";
            default -> "";
         };
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
         Row row = rows.get(rowIndex);
         return switch (columnIndex) {
            case 0 -> row.name();
            case 1 -> row.mean();
            case 2 -> row.stdDev();
            case 3 -> row.min();
            case 4 -> row.quantile01();
            case 5 -> row.median();
            case 6 -> row.quantile09();
            case 7 -> row.max();
            default -> "";
         };
      }
   }
}
