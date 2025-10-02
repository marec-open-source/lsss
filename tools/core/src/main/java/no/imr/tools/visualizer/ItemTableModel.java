package no.imr.tools.visualizer;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import javax.swing.table.AbstractTableModel;
import java.util.List;

final class ItemTableModel<T> extends AbstractTableModel {
   private final List<ItemFeature<T>> features;
   private List<T> items = List.of();

   ItemTableModel(List<ItemFeature<T>> features) {
      this.features = features;
   }

   @Override
   public int getRowCount() {
      return items.size();
   }

   @Override
   public int getColumnCount() {
      return features.size();
   }

   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      return features.get(columnIndex).itemToString.apply(items.get(rowIndex));
   }

   @Override
   public String getColumnName(int column) {
      ItemFeature<T> feature = features.get(column);
      List<String> words = Lists.newArrayList(feature.getNameAndUnit().split(" "));
      int totalLength = words.stream()
            .mapToInt(String::length)
            .sum()
            + words.size() - 2; // n - 1 spaces and subtract line break
      int i = 1;
      int line1Length = words.getFirst().length();
      while (i + 1 < words.size()) {
         int nextLength = words.get(i).length() + 1; // Plus one because of space
         if (line1Length + nextLength >= totalLength - line1Length) {
            break;
         }
         line1Length += nextLength;
         i++;
      }
      String line1 = Joiner.on(' ').join(words.subList(0, i));
      String line2 = Joiner.on(' ').join(words.subList(i, words.size()));
      if (line2.isEmpty()) {
         line2 = " ";
      }
      return line1 + '\n' + line2;
   }

   @Override
   public Class<?> getColumnClass(int columnIndex) {
      return Double.class;
   }

   List<T> getItems() {
      return items;
   }

   void update(ItemContainer<T> itemContainer) {
      List<T> newItems = List.copyOf(itemContainer.getAllItems());
      if (items.equals(newItems)) {
         return;
      }
      items = newItems;
      fireTableDataChanged();
   }
}
