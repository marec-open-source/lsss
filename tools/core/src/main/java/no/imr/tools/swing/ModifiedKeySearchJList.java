package no.imr.tools.swing;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.text.Position;

/**
 * A JList that modifies the search result when typing.
 */
public abstract class ModifiedKeySearchJList<T> extends JList<T> {
   protected ModifiedKeySearchJList() {
   }

   protected abstract String valueToString(T value);

   @Override
   public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
      JList<String> tmpList = new JList<>(new ModifiedListModel());
      return tmpList.getNextMatch(prefix, startIndex, bias);
   }

   private final class ModifiedListModel extends AbstractListModel<String> {
      private ModifiedListModel() {
      }

      @Override
      public int getSize() {
         return getModel().getSize();
      }

      @Override
      public String getElementAt(int index) {
         return valueToString(getModel().getElementAt(index));
      }
   }
}
