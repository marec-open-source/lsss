package no.imr.korona.computation.categorization.kdtree;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * For efficient nearest neighbor searching.
 */
public final class KDTree {
   private final int dim;
   private Node root;

   /**
    * Constructs a KDTree with keys of the specified dimensionality.
    *
    * @param dim the dimensionality
    */
   public KDTree(int dim) {
      this.dim = dim;
      root = new Node(Box.infiniteBox(dim));
   }

   /**
    * Adds a new entry (key, value) to this KDTree.
    * If the key already exists in the tree,
    * the old entry is replaced by the new one.
    *
    * @param key   the key
    * @param value the value associated with that key
    * @throws IllegalArgumentException if the key has wrong dimensionality
    */
   public void add(float[] key, Object value) {
      checkKey(key);
      root.add(new Point(key), value);
   }

   /**
    * Returns the value associated with the specified key.
    *
    * @param key the key
    * @return the value associated with that key, or {@code null} if no such key is found
    * @throws IllegalArgumentException if the key has wrong dimensionality
    */
   public @Nullable Object get(float[] key) {
      checkKey(key);
      return root.find(new Point(key));
   }

   /**
    * Returns the value whose key is nearest the specified key.
    *
    * @param key the key
    * @return the value whose key is nearest the specified key, or {@code null} if the tree is empty
    * @throws IllegalArgumentException if the key has wrong dimensionality
    */
   public @Nullable Object nearestNeighbor(float[] key) {
      checkKey(key);
      Node.Search result = new Node.Search(new Point(key));
      root.nearestNeighbor(result);
      return result.result;
   }

   /**
    * Rebuilds this tree to improve search efficiency.
    */
   public void rebuild() {
      int n = root.nodeCount();
      List<Node.Entry> entries = new ArrayList<>(n);
      root.getAllEntries(entries);
      root = new Node(Box.infiniteBox(dim), entries);
   }

   private void checkKey(float[] key) {
      if (key.length != dim) {
         throw new IllegalArgumentException("Wrong key dimension");
      }
   }
}
