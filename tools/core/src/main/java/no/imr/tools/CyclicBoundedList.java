package no.imr.tools;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * A cyclic list with a maximum size.
 */
public final class CyclicBoundedList<E> extends AbstractList<E> implements RandomAccess {
   private final Object[] elements;
   private int size;
   private int origin;

   public CyclicBoundedList(int maxSize) {
      elements = new Object[maxSize];
   }

   @Override
   @SuppressWarnings("unchecked")
   public E get(int index) {
      Objects.checkIndex(index, size);
      return (E) elements[(origin + index) % elements.length];
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public boolean add(E e) {
      elements[(origin + size) % elements.length] = e;
      if (size == elements.length) {
         origin = (origin + 1) % elements.length;
      } else {
         size++;
      }
      return true;
   }

   @Override
   public void addFirst(E e) {
      origin = origin == 0 ? elements.length - 1 : origin - 1;
      elements[origin] = e;
      if (size < elements.length) {
         size++;
      }
   }

   @Override
   public void clear() {
      Arrays.fill(elements, null);
      size = 0;
      origin = 0;
   }

   int getOrigin() {
      return origin;
   }
}
