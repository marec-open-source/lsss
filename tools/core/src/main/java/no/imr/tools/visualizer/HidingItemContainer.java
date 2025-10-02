package no.imr.tools.visualizer;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class HidingItemContainer<T> implements ItemContainer<T> {
   private final ItemContainer<T> itemContainer;
   private Set<T> hiddenItems = Set.of();
   private @Nullable List<T> allItems;
   private @Nullable Set<T> selectedItems;

   HidingItemContainer(ItemContainer<T> itemContainer) {
      this.itemContainer = itemContainer;
   }

   @Override
   public Collection<T> getAllItems() {
      return allItems != null ? allItems : itemContainer.getAllItems();
   }

   @Override
   public Set<T> getSelectedItems() {
      return selectedItems != null ? selectedItems : itemContainer.getSelectedItems();
   }

   @Override
   public void setSelectedItems(Set<T> items) {
      itemContainer.setSelectedItems(items);
   }

   Set<T> getHiddenItems() {
      return hiddenItems;
   }

   void hideItems(Set<T> items) {
      Set<T> tmp = new HashSet<>(hiddenItems);
      tmp.addAll(items);
      Set<T> allItemsAsSet = new HashSet<>(itemContainer.getAllItems());
      tmp.retainAll(allItemsAsSet);
      hiddenItems = Set.copyOf(tmp);
      update();
   }

   void includeHiddenItems() {
      hiddenItems = Set.of();
      update();
   }

   void update() {
      if (hiddenItems.isEmpty()) {
         allItems = null;
         selectedItems = null;
         return;
      }
      Predicate<T> notHidden = Predicate.not(hiddenItems::contains);
      allItems = itemContainer.getAllItems().stream()
            .filter(notHidden)
            .toList();
      selectedItems = itemContainer.getSelectedItems().stream()
            .filter(notHidden)
            .collect(Collectors.toSet());
   }
}
