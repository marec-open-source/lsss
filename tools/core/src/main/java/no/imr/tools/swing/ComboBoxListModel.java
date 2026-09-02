package no.imr.tools.swing;

import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxModel;
import java.util.List;
import java.util.Objects;

/**
 * A {@link ComboBoxModel} using a {@link List} to store all items.
 */
public final class ComboBoxListModel<T extends @Nullable Object> extends ListListModel<T> implements ComboBoxModel<T> {
   private @Nullable Object selectedItem;

   public ComboBoxListModel(@Nullable T selectedItem, List<T> items) {
      this(selectedItem, items, true);
   }

   public ComboBoxListModel(@Nullable T selectedItem, List<T> items, boolean checkSelectedItem) {
      super(items);
      if (!checkSelectedItem || items.contains(selectedItem)) {
         this.selectedItem = selectedItem;
      } else if (!items.isEmpty()) {
         this.selectedItem = items.getFirst();
      } else {
         this.selectedItem = null;
      }
   }

   @Override
   public void setSelectedItem(@Nullable Object anItem) {
      if (!Objects.equals(selectedItem, anItem)) {
         selectedItem = anItem;
         fireContentsChanged(this, -1, -1);
      }
   }

   @Override
   public @Nullable Object getSelectedItem() {
      return selectedItem;
   }
}
