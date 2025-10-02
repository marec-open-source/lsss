package no.imr.korona.computation.categorization.kdtree;

/**
 * A multidimensional box.
 */
record Box(Point min, Point max) {

   static Box infiniteBox(int dimension) {
      Point min = new Point(dimension, Float.NEGATIVE_INFINITY);
      Point max = new Point(dimension, Float.POSITIVE_INFINITY);
      return new Box(min, max);
   }

   int widestDimension() {
      int d = -1;
      float maxWidth = -1;
      for (int i = 0; i < min.length(); i++) {
         float width = max.x(i) - min.x(i);
         if (width > maxWidth) {
            maxWidth = width;
            d = i;
         }
      }
      return d;
   }

   float distance2(Point point, float maxDistance2) {
      float d2 = 0;
      for (int i = 0; i < point.length(); i++) {
         float x = point.x(i);
         if (x < min.x(i)) {
            float tmp = x - min.x(i);
            d2 += tmp * tmp;
            if (d2 > maxDistance2) {
               break;
            }
         } else if (x > max.x(i)) {
            float tmp = x - max.x(i);
            d2 += tmp * tmp;
            if (d2 > maxDistance2) {
               break;
            }
         }
      }
      return d2;
   }

   @Override
   public String toString() {
      return "[ " + min + ", " + max + " ]";
   }
}
