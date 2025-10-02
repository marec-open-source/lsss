package no.imr.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class CyclicList<E> extends ArrayList<E> {
   public CyclicList() {
   }

   public CyclicList(Collection<? extends E> c) {
      super(c);
   }

   public CyclicList(int initialCapacity) {
      super(initialCapacity);
   }

   @Override
   public E get(int index) {
      index = moddedIndex(index);
      return super.get(index);
   }

   private int moddedIndex(int index) {
      int n = size();
      return n > 0 ? Utils.mod(index, n) : 0;
   }

   @Override
   public List<E> subList(int fromIndex, int toIndex) {
      if (fromIndex == toIndex) {
         return Collections.emptyList();
      }
      int from = moddedIndex(fromIndex);
      int to = moddedIndex(toIndex);
      if (to == 0) {
         to = size();
      }
      if (from >= to) { // across zero
         List<E> result = new ArrayList<>();
         result.addAll(super.subList(from, size()));
         result.addAll(super.subList(0, to));
         return result;
      }
      return super.subList(from, to);
   }
}
