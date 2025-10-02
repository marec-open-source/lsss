package no.imr.korona.computation.categorization.kdtree;

import org.jspecify.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A node in a KDTree.
 */
final class Node {
   private @Nullable Entry entry;
   private int pivotDimension;
   private float pivotValue;
   private final Box box;
   private @Nullable Branch branch;

   Node(Box box) {
      this.box = box;
   }

   Node(Box box, List<Entry> entries) {
      this.box = box;
      if (!entries.isEmpty()) {
         if (entries.size() == 1) {
            entry = entries.getFirst();
         } else {
            Box bb = boundingBox(entries);
            pivotDimension = bb.widestDimension();

            entry = centerEntry(entries, bb, pivotDimension);
            //entry = medianEntry(entries, pivotDimension);

            pivotValue = entry.key.x(pivotDimension);

            List<Entry> leftEntries = new ArrayList<>(entries.size());
            List<Entry> rightEntries = new ArrayList<>(entries.size());
            for (Entry e : entries) {
               if (e != entry) {
                  (left(e.key) ? leftEntries : rightEntries).add(e);
               }
            }

            Node left = new Node(new Box(box.min(), box.max().copyWith(pivotDimension, pivotValue)), leftEntries);
            Node right = new Node(new Box(box.min().copyWith(pivotDimension, pivotValue), box.max()), rightEntries);

            branch = new Branch(left, right);
         }
      }
   }

   private static Entry centerEntry(List<Entry> entries, Box boundingBox, int dimension) {
      float center = (boundingBox.max().x(dimension) + boundingBox.min().x(dimension)) / 2;
      Entry result = null;
      float distance = Float.POSITIVE_INFINITY;
      for (Entry entry : entries) {
         float d = Math.abs(center - entry.key.x(dimension));
         if (d < distance) {
            result = entry;
            distance = d;
         }
      }
      if (result == null) {
         throw new IllegalArgumentException("No entries.");
      }
      return result;
   }

   private static Entry medianEntry(List<Entry> entries, int dimension) {
      entries.sort((o1, o2) -> Float.compare(o1.key.x(dimension), o2.key.x(dimension)));
      return entries.get(entries.size() / 2);
   }

   void add(Point key, Object value) {
      Entry entry = this.entry;
      if (entry == null || entry.key.equals(key)) {
         this.entry = new Entry(key, value);
      } else {
         Branch branch = this.branch;
         if (branch == null) {
            pivotDimension = widestDimension(entry.key, key);
            pivotValue = entry.key.x(pivotDimension);

            Node left = new Node(new Box(box.min(), box.max().copyWith(pivotDimension, pivotValue)));
            Node right = new Node(new Box(box.min().copyWith(pivotDimension, pivotValue), box.max()));

            branch = new Branch(left, right);
            this.branch = branch;
         }
         (left(key) ? branch.left : branch.right).add(key, value);
      }
   }

   @Nullable Object find(Point key) {
      Entry entry = this.entry;
      if (entry == null) {
         return null;
      }
      if (entry.key.equals(key)) {
         return entry.value;
      }
      Branch branch = this.branch;
      if (branch == null) {
         return null;
      }
      return (left(key) ? branch.left : branch.right).find(key);
   }

   int nodeCount() {
      Branch branch = this.branch;
      return branch != null ? branch.left.nodeCount() + branch.right.nodeCount() + 1 : 1;
   }

   int leafNodeCount() {
      Branch branch = this.branch;
      return branch != null ? branch.left.leafNodeCount() + branch.right.leafNodeCount() : 1;
   }

   int entryCount() {
      Branch branch = this.branch;
      return branch != null ? branch.left.entryCount() + branch.right.entryCount() + 1 : (entry != null ? 1 : 0);
   }

   int depth() {
      Branch branch = this.branch;
      return branch != null ? Math.max(branch.left.depth(), branch.right.depth()) + 1 : 1;
   }

   void getAllEntries(List<Entry> entries) {
      Entry entry = this.entry;
      if (entry != null) {
         entries.add(entry);
      }
      Branch branch = this.branch;
      if (branch != null) {
         branch.left.getAllEntries(entries);
         branch.right.getAllEntries(entries);
      }
   }

   void nearestNeighbor(Search search) {
      Entry entry = this.entry;
      if (entry != null) {
         search.check(entry);
      }
      Branch branch = this.branch;
      if (branch != null) {
         boolean leftIsInside = left(search.key);
         Node inside = leftIsInside ? branch.left : branch.right;
         Node outside = leftIsInside ? branch.right : branch.left;
         inside.nearestNeighbor(search);

         float outerLowerBound = search.key.x(pivotDimension) - pivotValue;
         float outerLowerBound2 = outerLowerBound * outerLowerBound;
         if (outerLowerBound2 < search.minDistance2) {
            float outsideDistance2 = box.distance2(search.key, search.minDistance2);
            if (outsideDistance2 < search.minDistance2) {
               outside.nearestNeighbor(search);
            }
         }
      }
   }

   private boolean left(Point key) {
      return key.x(pivotDimension) < pivotValue;
   }

   private static int widestDimension(Point a, Point b) {
      int d = -1;
      float maxWidth = -1;
      for (int i = 0; i < a.length(); i++) {
         float width = Math.abs(a.x(i) - b.x(i));
         if (width > maxWidth) {
            maxWidth = width;
            d = i;
         }
      }
      return d;
   }

   private static Box boundingBox(List<Entry> entries) {
      int n = entries.getFirst().key.length();

      float[] min = new float[n];
      Arrays.fill(min, Float.POSITIVE_INFINITY);

      float[] max = new float[n];
      Arrays.fill(max, Float.NEGATIVE_INFINITY);

      for (Entry entry : entries) {
         for (int i = 0; i < n; i++) {
            min[i] = Math.min(min[i], entry.key.x(i));
            max[i] = Math.max(max[i], entry.key.x(i));
         }
      }
      return new Box(new Point(min), new Point(max));
   }

   void print(String prefix, PrintStream out) {
      Entry entry = this.entry;
      if (entry == null) {
         return;
      }
      out.println(prefix + " (" + pivotDimension + ") " + entry.key);
      Branch branch = this.branch;
      if (branch == null) {
         return;
      }
      branch.left.print(prefix + "L", out);
      branch.right.print(prefix + "R", out);
   }

   record Entry(Point key, Object value) {
   }

   private record Branch(Node left, Node right) {
   }

   static final class Search {
      private final Point key;
      private float minDistance2 = Float.POSITIVE_INFINITY;
      @Nullable Object result;

      Search(Point key) {
         this.key = key;
      }

      private void check(Entry entry) {
         float distance2 = entry.key.distance2(key, minDistance2);
         if (distance2 < minDistance2) {
            minDistance2 = distance2;
            result = entry.value;
         }
      }
   }
}
