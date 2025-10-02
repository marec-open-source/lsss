package no.imr.lsss.framework.backup;

import java.util.Collection;
import java.util.stream.Stream;

final class SelectionItem<T> {
   private final T item;
   private boolean selected;

   SelectionItem(T item, boolean selected) {
      this.item = item;
      this.selected = selected;
   }

   T get() {
      return item;
   }

   boolean isSelected() {
      return selected;
   }

   void setSelected(boolean selected) {
      this.selected = selected;
   }

   static <T> Stream<T> selected(Collection<SelectionItem<T>> items) {
      return items.stream()
            .filter(SelectionItem::isSelected)
            .map(SelectionItem::get);
   }
}
