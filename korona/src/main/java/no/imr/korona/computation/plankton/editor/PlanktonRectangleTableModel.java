package no.imr.korona.computation.plankton.editor;

import no.imr.korona.computation.plankton.PlanktonRectangle;
import no.imr.korona.computation.plankton.SizeHistogram;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.swing.table.TableColumnInfo;
import no.imr.tools.time.DateTimeMillis;
import org.jspecify.annotations.Nullable;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * TableModel for PlanktonRectangles.
 * The model holds a List of PlanktonRectanglesRows,
 * which consists of a PlanktonRectangle and the model name.
 * The model decides how many columns are needed as
 * new histograms are added and removed.
 */
final class PlanktonRectangleTableModel extends AbstractTableModel {
   private final List<PlanktonRectangleRow> planktonRectangleRows = new ArrayList<>();
   private final List<TableColumnInfo> columnNames = new ArrayList<>();

   private int hiddenColumns = 0;

   private double sizeFactor;

   private boolean showAll = false;

   private @Nullable JDialog dialog;

   private int numberOfColumns = 0;
   private int histogramColumnOffset = 0;

   static final int USE_COLUMN = 0;
   static final int CLASS_COLUMN = 1;
   static final int SPECIES_COLUMN = 2;
   static final int START_DATE_COLUMN = 3;
   static final int START_TIME_COLUMN = 4;
   static final int STOP_DATE_COLUMN = 5;
   static final int STOP_TIME_COLUMN = 6;
   static final int UPPER_COLUMN = 7;
   static final int LOWER_COLUMN = 8;
   static final int COUNT_COLUMN = 9;
   static final int SIZES_COLUMN = 10;

   void setShowAll(boolean showAll) {
      this.showAll = showAll;
      recalculateColumns();
   }

   static final class PlanktonRectangleRow {
      private final PlanktonRectangle planktonRectangle;
      private String algClass;

      private PlanktonRectangleRow(PlanktonRectangle planktonRectangle, String algClass) {
         this.planktonRectangle = planktonRectangle;
         this.algClass = algClass;
      }

      PlanktonRectangle getPlanktonRectangle() {
         return planktonRectangle;
      }

      private void setAlgClass(String algClass) {
         this.algClass = algClass;
      }

      String getAlgClass() {
         return algClass;
      }
   }

   PlanktonRectangleTableModel(double sizeFactor, JDialog dialog) {
      this(sizeFactor);
      this.dialog = dialog;
   }

   PlanktonRectangleTableModel(double sizeFactor) {
      initializeColumnNames();
      this.sizeFactor = sizeFactor;
   }

   private void initializeColumnNames() {
      columnNames.clear();
      columnNames.add(new TableColumnInfo("Use", Boolean.class));
      columnNames.add(new TableColumnInfo("Class", String.class));
      if (showAll) {
         columnNames.add(new TableColumnInfo("Species", String.class));
         columnNames.add(new TableColumnInfo("Start date\n[YYYY-MM-DD]", String.class));
         columnNames.add(new TableColumnInfo("Start time\n[HH:MM]", String.class));
         columnNames.add(new TableColumnInfo("Stop date\n[YYYY-MM-DD]", String.class));
         columnNames.add(new TableColumnInfo("Stop time\n[HH:MM]", String.class));
         columnNames.add(new TableColumnInfo("Upper\n[-m]", Float.class));
         columnNames.add(new TableColumnInfo("Lower\n[-m]", Float.class));
         columnNames.add(new TableColumnInfo("Count", Double.class));
         hiddenColumns = 0;
      } else {
         hiddenColumns = 8;
      }
      columnNames.add(new TableColumnInfo("Sizes", Integer.class));
      columnNames.add(new TableColumnInfo("SS", Long.class));
      numberOfColumns = columnNames.size();
      histogramColumnOffset = columnNames.size() - 1;
   }

   @Override
   public String getColumnName(int col) {
      return columnNames.get(col).name();
   }

   @Override
   public Class<?> getColumnClass(int c) {
      return columnNames.get(c).columnClass();
   }

   @Override
   public int getRowCount() {
      return planktonRectangleRows.size();
   }

   PlanktonRectangleRow getRow(int i) {
      return planktonRectangleRows.get(i);
   }

   @Override
   public int getColumnCount() {
      return numberOfColumns;
   }

   private static @Nullable String getDateRangeAndCheckForDefault(Instant input, Instant defaultValue) {
      if (!input.equals(defaultValue)) {
         LocalDate localDate = LocalDate.ofInstant(input, ZoneOffset.UTC);
         return DateTimeMillis.localDateToString(localDate);
      } else {
         return null;
      }
   }

   private static @Nullable String getTimeRangeAndCheckForDefault(Instant input, Instant defaultValue) {
      if (!input.equals(defaultValue)) {
         LocalTime localTime = LocalTime.ofInstant(input, ZoneOffset.UTC);
         return DateTimeMillis.localTimeToCentisString(localTime);
      } else {
         return null;
      }
   }

   @Override
   public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
      PlanktonRectangleRow planktonRectangleRow = planktonRectangleRows.get(rowIndex);
      if (columnIndex == USE_COLUMN) {
         return planktonRectangleRow.getPlanktonRectangle().isUse();
      }
      if (columnIndex == CLASS_COLUMN) {
         return planktonRectangleRow.getAlgClass();
      }
      if (columnIndex == SPECIES_COLUMN && showAll) {
         return planktonRectangleRow.getPlanktonRectangle().getSpecies();
      }
      if (columnIndex == START_DATE_COLUMN && showAll) {
         return getDateRangeAndCheckForDefault(planktonRectangleRow.getPlanktonRectangle().getTimeRange().begin(),
               PlanktonRectangle.DEFAULT_START_TIME);
      }
      if (columnIndex == START_TIME_COLUMN && showAll) {
         return getTimeRangeAndCheckForDefault(planktonRectangleRow.getPlanktonRectangle().getTimeRange().begin(),
               PlanktonRectangle.DEFAULT_START_TIME);
      }
      if (columnIndex == STOP_DATE_COLUMN && showAll) {
         return getDateRangeAndCheckForDefault(planktonRectangleRow.getPlanktonRectangle().getTimeRange().end(),
               PlanktonRectangle.DEFAULT_STOP_TIME);
      }
      if (columnIndex == STOP_TIME_COLUMN && showAll) {
         return getTimeRangeAndCheckForDefault(planktonRectangleRow.getPlanktonRectangle().getTimeRange().end(),
               PlanktonRectangle.DEFAULT_STOP_TIME);
      }
      if (columnIndex == UPPER_COLUMN && showAll) {
         if (planktonRectangleRow.getPlanktonRectangle().getDepthRange().begin() != Float.NEGATIVE_INFINITY) {
            return planktonRectangleRow.getPlanktonRectangle().getDepthRange().begin();
         } else {
            return null;
         }
      }
      if (columnIndex == LOWER_COLUMN && showAll) {
         if (planktonRectangleRow.getPlanktonRectangle().getDepthRange().end() != Float.POSITIVE_INFINITY) {
            return planktonRectangleRow.getPlanktonRectangle().getDepthRange().end();
         } else {
            return null;
         }
      }
      if (columnIndex == COUNT_COLUMN && showAll) {
         double sum = 0;
         for (int i = 0; i < planktonRectangleRow.getPlanktonRectangle().getSizeHistogram().getAbundances().length; i++) {
            sum += planktonRectangleRow.getPlanktonRectangle().getSizeHistogram().getAbundances()[i];
         }
         return sum;
      }
      if (columnIndex == SIZES_COLUMN - hiddenColumns) {
         return planktonRectangleRow.getPlanktonRectangle().getSizeHistogram().getAbundances().length;
      } else {
         //For histogram bins and edges
         if (columnIndex < numberOfColumns - 1 && columnIndex >= histogramColumnOffset) {
            int relativeIndex = columnIndex - histogramColumnOffset;
            if (!showAll) {
               relativeIndex *= 2;
            }

            if (relativeIndex / 2 < planktonRectangleRow.getPlanktonRectangle().getSizeHistogram().getAbundances().length) {
               if (relativeIndex % 2 == 0) {
                  return Math.round(planktonRectangleRow.getPlanktonRectangle().getSizeHistogram().getDividers()[relativeIndex] / sizeFactor);
               } else {
                  return planktonRectangleRow.getPlanktonRectangle().getSizeHistogram().getAbundances()[relativeIndex / 2];
               }
            }
         } else if (columnIndex == numberOfColumns - 1) { // For the last edge (SS)
            if (planktonRectangleRow.getPlanktonRectangle().getSizeHistogram().getDividers().length > 0) {
               return Math.round(planktonRectangleRow.getPlanktonRectangle().getSizeHistogram().getDividers()[planktonRectangleRow.getPlanktonRectangle().getSizeHistogram().getDividers().length - 1] / sizeFactor);
            }
         }
      }
      return null;
   }

   private boolean checkTimeRangeValidity(Instant start, Instant stop) {
      if (start.isAfter(stop)) {
         JOptionPane.showMessageDialog(dialog,
               "Start date > stop date, please select an earlier start date or later stop date.", "LSSS", JOptionPane.INFORMATION_MESSAGE);
         return false;
      } else {
         return true;
      }
   }

   @Override
   public void setValueAt(@Nullable Object value, int row, int col) {
      if (value == null) {
         return;
      }
      PlanktonRectangleRow planktonRectangleRow = planktonRectangleRows.get(row);
      PlanktonRectangle planktonRectangle = planktonRectangleRow.getPlanktonRectangle();

      if (col == USE_COLUMN) {
         planktonRectangle.setUse((Boolean) value);
      } else if (col == CLASS_COLUMN) {
         planktonRectangleRow.setAlgClass((String) value);
      } else if (col == SPECIES_COLUMN && showAll) {
         planktonRectangle.setSpecies((String) value);
      } else if (col == START_DATE_COLUMN && showAll) {
         if (!((String) value).isEmpty()) {
            LocalDate startDate = null;
            try {
               startDate = DateTimeMillis.toLocalDate((String) value).orElse(null);
            } catch (DateTimeException e) {
               JOptionPane.showMessageDialog(dialog,
                     "Invalid date. Please enter as YYYY-MM-DD\n(" + e + ")", "", JOptionPane.INFORMATION_MESSAGE);
            }
            if (startDate != null) {
               LocalTime time = LocalTime.ofInstant(planktonRectangle.getTimeRange().begin(), ZoneOffset.UTC);
               Instant instant = startDate.atTime(time).toInstant(ZoneOffset.UTC);
               if (checkTimeRangeValidity(instant, planktonRectangle.getTimeRange().end())) {
                  planktonRectangle.setTimeRange(new DefaultRange<>(instant, planktonRectangle.getTimeRange().end()));
                  fireTableDataChanged();
               }
            }
         }
      } else if (col == START_TIME_COLUMN && showAll) {
         if (!((String) value).isEmpty()) {
            LocalDate date = LocalDate.ofInstant(planktonRectangle.getTimeRange().begin(), ZoneOffset.UTC);
            if (DateTimeMillis.localDateToInt(date) == 1970_01_01) {
               date = LocalDate.now();
            }
            LocalTime time = DateTimeMillis.centisTimeToLocalTime((String) value);
            Instant instant = date.atTime(time).toInstant(ZoneOffset.UTC);
            if (checkTimeRangeValidity(instant, planktonRectangle.getTimeRange().end())) {
               planktonRectangle.setTimeRange(new DefaultRange<>(instant, planktonRectangle.getTimeRange().end()));
               fireTableDataChanged();
            }
         }
      } else if (col == STOP_DATE_COLUMN && showAll) {
         if (!((String) value).isEmpty()) {
            LocalDate stopDate = null;
            try {
               stopDate = DateTimeMillis.toLocalDate((String) value).orElse(null);
            } catch (DateTimeException e) {
               JOptionPane.showMessageDialog(dialog,
                     "Invalid date. Please enter as YYYY-MM-DD\n(" + e + ")", "", JOptionPane.INFORMATION_MESSAGE);
            }
            if (stopDate != null) {
               LocalTime time = LocalTime.ofInstant(planktonRectangle.getTimeRange().end(), ZoneOffset.UTC);
               Instant instant = stopDate.atTime(time).toInstant(ZoneOffset.UTC);
               if (checkTimeRangeValidity(planktonRectangle.getTimeRange().begin(), instant)) {
                  planktonRectangle.setTimeRange(new DefaultRange<>(planktonRectangle.getTimeRange().begin(), instant));
                  fireTableDataChanged();
               }
            }
         }
      } else if (col == STOP_TIME_COLUMN && showAll) {
         if (!((String) value).isEmpty()) {
            LocalDate date = LocalDate.ofInstant(planktonRectangle.getTimeRange().end(), ZoneOffset.UTC);
            if (DateTimeMillis.localDateToInt(date) == 9999_12_31) {
               date = LocalDate.now();
            }
            Instant instant = date.atTime(DateTimeMillis.centisTimeToLocalTime((String) value)).toInstant(ZoneOffset.UTC);
            if (checkTimeRangeValidity(planktonRectangle.getTimeRange().begin(), instant)) {
               planktonRectangle.setTimeRange(new DefaultRange<>(planktonRectangle.getTimeRange().begin(), instant));
               fireTableDataChanged();
            }
         }
      } else if (col == UPPER_COLUMN && showAll) {
         planktonRectangle.setDepthRange(new DefaultRange<>((Float) value, planktonRectangle.getDepthRange().end()));
      } else if (col == LOWER_COLUMN && showAll) {
         planktonRectangle.setDepthRange(new DefaultRange<>(planktonRectangle.getDepthRange().begin(), (Float) value));
      } else if (col == SIZES_COLUMN - hiddenColumns) {
         int nBins = (Integer) value;

         double[] oldDividers = planktonRectangle.getSizeHistogram().getDividers();
         double[] oldAbundances = planktonRectangle.getSizeHistogram().getAbundances();
         double[] newDividers = new double[2 * nBins];

         //Copy old dividers to new array with desired dimension.
         for (int i = 0; i < newDividers.length; i++) {
            if (i < oldDividers.length) {
               newDividers[i] = oldDividers[i];
            } else if (i == oldDividers.length && i > 0) {
               newDividers[i] = oldDividers[i - 1];
            } else {
               newDividers[i] = 0;
            }
         }

         //Insert new array.
         planktonRectangle.getSizeHistogram().copyDividers(newDividers);

         //Copy old abundances.
         for (int i = 0; i < nBins; i++) {
            if (i < oldAbundances.length) {
               planktonRectangle.getSizeHistogram().getAbundances()[i] = oldAbundances[i];
            } else {
               planktonRectangle.getSizeHistogram().getAbundances()[i] = 0;
            }
         }

         recalculateColumns();
      } else {
         //Whenever something is typed into the histogram columns.
         int relativeIndex = col - histogramColumnOffset;
         if (!showAll) {
            relativeIndex *= 2;
         }
         if (relativeIndex >= 0 && relativeIndex < planktonRectangle.getSizeHistogram().getAbundances().length * 2) {
            //The bin edges
            if (relativeIndex % 2 == 0) {
               //We need to check that this value is larger than the previous value.
               double theValue = (Long) value * sizeFactor;
               if (SizeHistogram.checkDividerConsistency(
                     relativeIndex,
                     planktonRectangle.getSizeHistogram().getDividers(),
                     theValue)) {
                  planktonRectangle.getSizeHistogram().getDividers()[relativeIndex] = theValue;
                  if (relativeIndex > 1) {
                     planktonRectangle.getSizeHistogram().getDividers()[relativeIndex - 1] = theValue;
                  }
               } else {
                  int answer = JOptionPane.showOptionDialog(dialog, "This divider is not between the previous and the next divider. Do you want to adjust surrounding dividers" +
                        " to match this divider?", "Inconsistent divider", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                  if (answer == JOptionPane.YES_OPTION) {
                     planktonRectangle.getSizeHistogram().adjustDividers(relativeIndex, theValue);
                  }
               }

               if (relativeIndex == 0 && dialog != null && dialog.isShowing()) {
                  String thisBin = "first";
                  String thatBin = "last";
                  int answer = JOptionPane.showOptionDialog(dialog, "You entered a value in the " + thisBin + " bin. Do you want to distribute values evenly between this and the " + thatBin + " bin?",
                        "Redistribute", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
                  if (answer == JOptionPane.YES_OPTION) {
                     double[] dividers = planktonRectangle.getSizeHistogram().getDividers();
                     planktonRectangle.getSizeHistogram().distributeDividers(theValue, dividers[dividers.length - 1]);
                  }
               }
            } else { // Bin contents
               planktonRectangle.getSizeHistogram().getAbundances()[relativeIndex / 2] = (Double) value;
            }

            fireTableDataChanged();
         } else if (col == numberOfColumns - 1 && planktonRectangle.getSizeHistogram().getDividers().length > 0) { // The SS column.
            double theValue = (Long) value * sizeFactor;
            if (SizeHistogram.checkDividerConsistency(
                  planktonRectangle.getSizeHistogram().getDividers().length - 1,
                  planktonRectangle.getSizeHistogram().getDividers(),
                  theValue)) {
               planktonRectangle.getSizeHistogram().getDividers()[planktonRectangle.getSizeHistogram().getDividers().length - 1] = theValue;
            } else {
               int answer = JOptionPane.showOptionDialog(dialog, "This divider is not between the previous and the next divider. Do you want to adjust surrounding dividers" +
                     " to match this divider?", "Inconsistent divider", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
               if (answer == JOptionPane.YES_OPTION) {
                  planktonRectangle.getSizeHistogram().adjustDividers(planktonRectangle.getSizeHistogram().getDividers().length - 1, theValue);
               }
            }

            if (dialog != null && dialog.isShowing()) {
               String thisBin = "last";
               String thatBin = "first";
               int answer = JOptionPane.showOptionDialog(dialog, "You entered a value in the " + thisBin + " bin. Do you want to distribute values evenly between this and the " + thatBin + " bin?",
                     "Redistribute", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
               if (answer == JOptionPane.YES_OPTION) {
                  double[] dividers = planktonRectangle.getSizeHistogram().getDividers();
                  planktonRectangle.getSizeHistogram().distributeDividers(dividers[0], theValue);
               }
            }

            fireTableDataChanged();
         }
      }
   }

   @Override
   public boolean isCellEditable(int rowIndex, int columnIndex) {
      if (columnIndex == COUNT_COLUMN && showAll) {
         return false;
      }
      if (columnIndex < histogramColumnOffset) {
         return true;
      }
      Object a = getValueAt(rowIndex, columnIndex);
      return a != null;
   }

   void addRow(PlanktonRectangle planktonRectangle, String algClass) {
      planktonRectangleRows.add(new PlanktonRectangleRow(planktonRectangle, algClass));
      updateNumberOfColumns(planktonRectangle);
      fireTableRowsInserted(0, planktonRectangleRows.size() - 1);
   }

   void deleteRow(int i) {
      planktonRectangleRows.remove(i);
      fireTableRowsDeleted(0, planktonRectangleRows.size());
      recalculateColumns();
   }

   void recalculateColumns() {
      initializeColumnNames();
      for (PlanktonRectangleRow row : planktonRectangleRows) {
         updateNumberOfColumns(row.getPlanktonRectangle());
      }
      fireTableStructureChanged();
   }

   void updateNumberOfColumns(PlanktonRectangle planktonRectangle) {
      if ((numberOfColumns < 2 * planktonRectangle.getSizeHistogram().getAbundances().length + histogramColumnOffset + 1 && showAll) ||
            (numberOfColumns < planktonRectangle.getSizeHistogram().getAbundances().length + histogramColumnOffset + 1 && !showAll)) {
         //Need to take action!
         int start;
         if (showAll) {
            start = (numberOfColumns - (histogramColumnOffset + 1)) / 2;
         } else {
            start = numberOfColumns - (histogramColumnOffset + 1);
         }
         for (int i = start; i < planktonRectangle.getSizeHistogram().getAbundances().length; i++) {
            columnNames.add(columnNames.size() - 1, new TableColumnInfo("S" + (i + 1), Long.class));
            numberOfColumns++;
            if (showAll) {
               columnNames.add(columnNames.size() - 1, new TableColumnInfo("N" + (i + 1), Double.class));
               numberOfColumns++;
            }
         }
      }
   }

   double getSizeFactor() {
      return sizeFactor;
   }

   void setSizeFactor(double sizeFactor) {
      this.sizeFactor = sizeFactor;
      // Need to update the sizeHistograms because of the new size factor.
      updateSizeHistogramsFromTableData();
   }

   void updateSizeHistogramsFromTableData() {
      for (int i = 0; i < getRowCount(); i++) {
         for (int j = histogramColumnOffset; j < numberOfColumns; j++) {
            setValueAt(getValueAt(i, j), i, j);
         }
      }
   }
}
