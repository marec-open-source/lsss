package no.imr.tools.parameter.gui;

import no.imr.tools.Utils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.gui.input.ParameterGuiUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A table model to be used by {@link ParameterTableGUI}.
 */
public final class ParameterTableModel<T extends ParameterContainer> extends AbstractTableModel {
   private final Supplier<T> newRowSupplier;
   private final List<ValueParameter<?>> headerParameters = new ArrayList<>();
   private final Map<Integer, Integer> columnIndexToParameterIndex = new HashMap<>();
   private final List<T> rows;
   private boolean editable = true;
   private Function<ValueParameter<?>, String> parameterToColumnName = ValueParameter::getDisplayName;
   private Function<ValueParameter<?>, @Nullable String> parameterToHeaderToolTip = ParameterGuiUtils::getNameToolTip;
   private Function<ValueParameter<?>, @Nullable String> parameterToInputToolTip = ParameterGuiUtils::getInputToolTip;

   public ParameterTableModel(Supplier<T> newRowSupplier, List<T> rows) {
      this.newRowSupplier = newRowSupplier;
      List<? extends BaseParameter<?>> allParameters = newRowSupplier.get().getParameters();
      for (int i = 0; i < allParameters.size(); i++) {
         BaseParameter<?> parameter = allParameters.get(i);
         if (parameter.isVisible() && parameter instanceof ValueParameter<?> valueParameter) {
            columnIndexToParameterIndex.put(headerParameters.size(), i);
            headerParameters.add(valueParameter);
         }
      }
      this.rows = rows;
   }

   public Supplier<T> getNewRowSupplier() {
      return newRowSupplier;
   }

   boolean isEditable() {
      return editable;
   }

   public ParameterTableModel<T> setEditable(boolean editable) {
      this.editable = editable;
      return this;
   }

   public ParameterTableModel<T> setParameterToColumnName(Function<ValueParameter<?>, String> parameterToColumnName) {
      this.parameterToColumnName = parameterToColumnName;
      return this;
   }

   Function<ValueParameter<?>, @Nullable String> getParameterToHeaderToolTip() {
      return parameterToHeaderToolTip;
   }

   public ParameterTableModel<T> setParameterToHeaderToolTip(Function<ValueParameter<?>, @Nullable String> parameterToHeaderToolTip) {
      this.parameterToHeaderToolTip = parameterToHeaderToolTip;
      return this;
   }

   Function<ValueParameter<?>, @Nullable String> getParameterToInputToolTip() {
      return parameterToInputToolTip;
   }

   public ParameterTableModel<T> setParameterToInputToolTip(Function<ValueParameter<?>, @Nullable String> parameterToInputToolTip) {
      this.parameterToInputToolTip = parameterToInputToolTip;
      return this;
   }

   public List<T> getRows() {
      return rows;
   }

   public void addRow() {
      addRow(rows.size());
   }

   public void addRow(int rowIndex) {
      addRow(rowIndex, newRowSupplier.get());
   }

   public void addRow(int rowIndex, T row) {
      rows.add(rowIndex, row);
      fireTableRowsInserted(rowIndex, rowIndex);
   }

   public void removeRow(int rowIndex) {
      rows.remove(rowIndex);
      fireTableRowsDeleted(rowIndex, rowIndex);
   }

   public void copyRow(int rowIndex, int destinationRowIndex) {
      Element xml = new ParameterCollection(rows.get(rowIndex)).toXml();
      T copy = newRowSupplier.get();
      new ParameterCollection(copy).fromXml(xml);
      addRow(destinationRowIndex, copy);
   }

   @Override
   public int getRowCount() {
      return rows.size();
   }

   @Override
   public int getColumnCount() {
      return headerParameters.size();
   }

   @Override
   public String getColumnName(int column) {
      ValueParameter<?> parameter = headerParameters.get(column);
      String name = parameterToColumnName.apply(parameter);
      return Utils.nameAndUnit(name, parameter.getUnit());
   }

   @Override
   public Class<?> getColumnClass(int columnIndex) {
      ValueParameter<?> parameter = getHeaderParameter(columnIndex);
      if (parameter instanceof BooleanParameter) {
         return Boolean.class;
      }
      return String.class;
   }

   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      ValueParameter<?> parameter = getParameter(rowIndex, columnIndex);
      if (parameter instanceof BooleanParameter) {
         return parameter.getValue();
      }
      return parameter.getStringValue();
   }

   @Override
   public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      getParameter(rowIndex, columnIndex).setStringValue(aValue.toString());
      fireTableCellUpdated(rowIndex, columnIndex);
   }

   @Override
   public boolean isCellEditable(int rowIndex, int columnIndex) {
      return editable && getParameter(rowIndex, columnIndex).isEnabled();
   }

   public ValueParameter<?> getHeaderParameter(int columnIndex) {
      return headerParameters.get(columnIndex);
   }

   ValueParameter<?> getParameter(int rowIndex, int columnIndex) {
      T row = rows.get(rowIndex);
      int parameterIndex = columnIndexToParameterIndex.get(columnIndex);
      return (ValueParameter<?>) row.getParameters().get(parameterIndex);
   }
}
