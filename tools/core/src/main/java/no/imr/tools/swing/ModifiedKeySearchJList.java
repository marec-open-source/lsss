package no.imr.tools.swing;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.text.Position;
import java.util.function.Function;

/**
 * A JList that modifies the search result when typing.
 */
public final class ModifiedKeySearchJList<T> extends JList<T> {
   private final JList<String> tmpList;

   public ModifiedKeySearchJList(Function<T, String> valueToString) {
      tmpList = new JList<>(new AbstractListModel<>() {
         @Override
         public int getSize() {
            return getModel().getSize();
         }

         @Override
         public String getElementAt(int index) {
            return valueToString.apply(getModel().getElementAt(index));
         }
      });
   }

   @Override
   public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
      return tmpList.getNextMatch(prefix, startIndex, bias);
   }
}
