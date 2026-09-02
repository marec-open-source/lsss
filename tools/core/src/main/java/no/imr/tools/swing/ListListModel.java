package no.imr.tools.swing;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;
import java.util.List;

/**
 * A {@link ListModel} using a {@link List} to store all items.
 */
public class ListListModel<T extends @Nullable Object> extends AbstractListModel<T> {
   private final List<T> items;

   public ListListModel(List<T> items) {
      this.items = items;
   }

   @Override
   public int getSize() {
      return items.size();
   }

   @Override
   public T getElementAt(int index) {
      return items.get(index);
   }

   public List<T> getItems() {
      return items;
   }
}
