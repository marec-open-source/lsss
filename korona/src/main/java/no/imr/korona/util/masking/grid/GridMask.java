package no.imr.korona.util.masking.grid;

import no.imr.korona.util.masking.grid.surfaces.Surface;
import no.imr.tools.range.IntRange;
import no.imr.tools.range.IntRangeSet;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Rectangle2D;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GridMask {
   private final double deltaX;
   private final double deltaY;
   private final double deltaZ;

   private double minZ = Double.POSITIVE_INFINITY;
   private double maxZ = Double.NEGATIVE_INFINITY;

   private final Map<Integer, Map<Integer, IntRangeSet>> columns = new HashMap<>();

   public GridMask(double delta) {
      this(delta, delta, delta);
   }

   public GridMask(double deltaX, double deltaY, double deltaZ) {
      this.deltaX = deltaX;
      this.deltaY = deltaY;
      this.deltaZ = deltaZ;
   }

   public GridMask(DataInputStream in) throws IOException {
      deltaX = in.readDouble();
      deltaY = in.readDouble();
      deltaZ = in.readDouble();
      minZ = in.readDouble();
      maxZ = in.readDouble();
      int numColumns = in.readInt();
      int colNum = 0;
      while (colNum < numColumns) {
         int i = in.readInt();
         Map<Integer, IntRangeSet> jValues = new HashMap<>();
         int numRows = in.readInt();
         int rowNumber = 0;
         while (rowNumber < numRows) {
            int j = in.readInt();
            int numIndexes = in.readInt();
            List<Integer> indexes = new ArrayList<>(numIndexes);
            int indexNum = 0;
            int maxIndex = Integer.MIN_VALUE;
            while (indexNum < numIndexes) {
               int index = in.readInt();
               indexes.add(index);
               maxIndex = index;
               indexNum++;
            }
            IntRangeSet intRangeSet = new IntRangeSet(indexes, maxIndex);
            jValues.put(j, intRangeSet);
            rowNumber++;
         }
         columns.put(i, jValues);
         colNum++;
      }
   }

   public double getDeltaX() {
      return deltaX;
   }

   public double getDeltaY() {
      return deltaY;
   }

   public double getDeltaZ() {
      return deltaZ;
   }

   public double getCellVolume() {
      return deltaX * deltaY * deltaZ;
   }

   public Map<Integer, Map<Integer, IntRangeSet>> getColumns() {
      return columns;
   }

   public Map<Integer, IntRangeSet> getColumnsForI(Integer i) {
      return columns.computeIfAbsent(i, k -> new HashMap<>());
   }

   public void add(GridMask gridMask) {
      for (Map.Entry<Integer, Map<Integer, IntRangeSet>> iEntry : gridMask.getColumns().entrySet()) {
         Map<Integer, IntRangeSet> map = getColumnsForI(iEntry.getKey());
         for (Map.Entry<Integer, IntRangeSet> jEntry : iEntry.getValue().entrySet()) {
            IntRangeSet intRangeSet = map.get(jEntry.getKey());
            if (intRangeSet == null) {
               map.put(jEntry.getKey(), jEntry.getValue());
            } else {
               intRangeSet.add(jEntry.getValue());
            }
         }
      }
      minZ = Math.min(minZ, gridMask.minZ);
      maxZ = Math.max(maxZ, gridMask.maxZ);
   }

   public void add(List<Surface> surfaces) {
      Rectangle2D box = getBoundingBox(surfaces);
      if (box == null) {
         return;
      }

      int iBegin = (int) Math.round(box.getMinX() / deltaX);
      int iEnd = (int) Math.round(box.getMaxX() / deltaX);
      int jBegin = (int) Math.round(box.getMinY() / deltaY);
      int jEnd = (int) Math.round(box.getMaxY() / deltaY);

      for (int i = iBegin; i < iEnd; i++) {
         double x = deltaX * (i + 0.5);
         Map<Integer, IntRangeSet> map = getColumnsForI(i);

         for (int j = jBegin; j < jEnd; j++) {
            double y = deltaY * (j + 0.5);
            double kMinZ = Double.POSITIVE_INFINITY;
            double kMaxZ = Double.NEGATIVE_INFINITY;

            for (Surface surface : surfaces) {
               if (surface.contains(x, y)) {
                  double z = surface.z(x, y);
                  if (z < kMinZ) {
                     kMinZ = z;
                  }
                  if (z > kMaxZ) {
                     kMaxZ = z;
                  }
               }
            }

            if (kMinZ < kMaxZ) {
               minZ = Math.min(minZ, kMinZ);
               maxZ = Math.max(maxZ, kMaxZ);
               int kBegin = (int) Math.round(kMinZ / deltaZ);
               int kEnd = (int) Math.round(kMaxZ / deltaZ);
               IntRangeSet column = map.get(j);
               if (column == null) {
                  map.put(j, new IntRangeSet(kBegin, kEnd));
               } else {
                  column.add(kBegin, kEnd);
               }
            }
         }
      }
   }

   public double getVerticalExtent() {
      return maxZ - minZ;
   }

   private static @Nullable Rectangle2D getBoundingBox(List<Surface> surfaces) {
      Rectangle2D box = null;
      for (Surface surface : surfaces) {
         Rectangle2D b = surface.getBoundingBox();
         if (box == null) {
            box = new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight());
         } else {
            box.add(b);
         }
      }
      return box;
   }

   public int getColumnCount() {
      int columnCount = 0;
      for (Map<Integer, IntRangeSet> map : columns.values()) {
         columnCount += map.size();
      }
      return columnCount;
   }

   public long getCellCount() {
      long cellCount = 0;
      for (Map<Integer, IntRangeSet> map : columns.values()) {
         for (IntRangeSet column : map.values()) {
            for (IntRange range : column) {
               cellCount += range.getSize();
            }
         }
      }
      return cellCount;
   }

   public double getVolume() {
      return getCellCount() * getCellVolume();
   }

   public void save(DataOutputStream out) throws IOException {
      out.writeDouble(deltaX);
      out.writeDouble(deltaY);
      out.writeDouble(deltaZ);
      out.writeDouble(minZ);
      out.writeDouble(maxZ);
      out.writeInt(columns.size());
      for (Map.Entry<Integer, Map<Integer, IntRangeSet>> iEntry : columns.entrySet()) {
         int i = iEntry.getKey();
         out.writeInt(i);
         out.writeInt(iEntry.getValue().size());
         for (Map.Entry<Integer, IntRangeSet> jEntry : iEntry.getValue().entrySet()) {
            int j = jEntry.getKey();
            out.writeInt(j);
            IntRangeSet intRangeSet = jEntry.getValue();
            out.writeInt(intRangeSet.getIndexes().length);
            for (int index : intRangeSet.getIndexes()) {
               out.writeInt(index);
            }
         }
      }
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof GridMask that
            && Double.doubleToLongBits(deltaX) == Double.doubleToLongBits(that.deltaX)
            && Double.doubleToLongBits(deltaY) == Double.doubleToLongBits(that.deltaY)
            && Double.doubleToLongBits(deltaZ) == Double.doubleToLongBits(that.deltaZ)
            && Double.doubleToLongBits(maxZ) == Double.doubleToLongBits(that.maxZ)
            && Double.doubleToLongBits(minZ) == Double.doubleToLongBits(that.minZ)
            && columns.equals(that.columns);
   }

   @Override
   public int hashCode() {
      int result = Double.hashCode(deltaX);
      result = 31 * result + Double.hashCode(deltaY);
      result = 31 * result + Double.hashCode(deltaZ);
      result = 31 * result + Double.hashCode(minZ);
      result = 31 * result + Double.hashCode(maxZ);
      result = 31 * result + columns.hashCode();
      return result;
   }
}
