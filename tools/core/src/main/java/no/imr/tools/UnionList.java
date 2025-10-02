package no.imr.tools;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

/**
 * A list that is the union of two other lists.
 */
public final class UnionList<T> extends AbstractList<T> implements RandomAccess {
   private final List<? extends T> firstList;
   private final List<? extends T> secondList;

   /**
    * Creates a list that is the union of two other lists.
    *
    * @param firstList  the first list
    * @param secondList the second list
    */
   public UnionList(List<? extends T> firstList, List<? extends T> secondList) {
      this.firstList = firstList;
      this.secondList = secondList;
   }

   @Override
   public T get(int index) {
      if (index < firstList.size()) {
         return firstList.get(index);
      } else {
         return secondList.get(index - firstList.size());
      }
   }

   @Override
   public int size() {
      return firstList.size() + secondList.size();
   }
}
