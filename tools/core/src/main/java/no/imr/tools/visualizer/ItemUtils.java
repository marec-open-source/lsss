package no.imr.tools.visualizer;

import no.imr.tools.swing.TableToolTipBuilder;

import java.util.List;

final class ItemUtils {
   private ItemUtils() {
   }

   static <T> ItemFeature<T> findFeature(List<ItemFeature<T>> features, String name, int index) {
      return features.stream()
            .filter(feature -> feature.name.equals(name))
            .findFirst()
            .orElse(features.get(index));
   }

   static <T> String getToolTipText(T item, List<ItemFeature<T>> features) {
      TableToolTipBuilder builder = new TableToolTipBuilder();
      for (ItemFeature<T> feature : features) {
         builder.addRow(feature.getNameAndUnit(), feature.itemToString.apply(item));
      }
      return builder.build();
   }
}
