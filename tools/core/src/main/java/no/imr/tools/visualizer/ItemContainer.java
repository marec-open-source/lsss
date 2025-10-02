package no.imr.tools.visualizer;

import no.imr.tools.Utils;
import no.imr.tools.misc.SelectionAction;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface ItemContainer<T> {
   Collection<T> getAllItems();

   Set<T> getSelectedItems();

   default Set<T> getUnselectedItems() {
      Set<T> unselectedItems = new HashSet<>(getAllItems());
      unselectedItems.removeAll(getSelectedItems());
      return unselectedItems;
   }

   void setSelectedItems(Set<T> items);

   default void addSelectedItems(Collection<T> items) {
      Set<T> selectedItems = new HashSet<>(getSelectedItems());
      selectedItems.addAll(items);
      setSelectedItems(selectedItems);
   }

   default void toggleSelectedItems(Collection<T> items) {
      Set<T> selectedItems = new HashSet<>(getSelectedItems());
      Utils.toggle(selectedItems, items);
      setSelectedItems(selectedItems);
   }

   default void selectItems(Set<T> items, MouseEvent mouseEvent) {
      switch (SelectionAction.fromModifiersEx(mouseEvent.getModifiersEx())) {
         case ADD -> addSelectedItems(items);
         case REPLACE -> setSelectedItems(items);
         case TOGGLE -> toggleSelectedItems(items);
      }
   }
}
