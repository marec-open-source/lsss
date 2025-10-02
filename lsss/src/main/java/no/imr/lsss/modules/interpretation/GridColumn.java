package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.tools.range.FloatRange;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * A column of grid cells.
 */
public final class GridColumn {
   private final Grid grid;
   private final GridColumnInterval gridColumnInterval;
   private final float bubbleCorrection;
   private final double[] accumulatedDistance;
   private FloatRange storedZRange = FloatRange.EMPTY_RANGE;
   private FloatRange interpretationZRange = FloatRange.EMPTY_RANGE;
   private final FloatRange[] dataZRange;
   private final NavigableMap<Integer, GridCell> gridCells = new TreeMap<>();

   GridColumn(Grid grid, GridColumnInterval gridColumnInterval, float bubbleCorrection) {
      this.grid = grid;
      this.gridColumnInterval = gridColumnInterval;
      this.bubbleCorrection = bubbleCorrection;
      accumulatedDistance = new double[this.grid.getTransducerCount()];
      dataZRange = new FloatRange[this.grid.getTransducerCount()];
      Arrays.fill(dataZRange, FloatRange.EMPTY_RANGE);
   }

   @Override
   public String toString() {
      return "PingRange=" + getPingRange() + "; StoredZRange=" + storedZRange + "; cells=" + gridCells.size();
   }

   public Grid getGrid() {
      return grid;
   }

   public PingRange getPingRange() {
      return gridColumnInterval.pingRange();
   }

   GridColumnInterval getGridColumnInterval() {
      return gridColumnInterval;
   }

   float getBubbleCorrection() {
      return bubbleCorrection;
   }

   FloatRange getStoredZRange() {
      return storedZRange;
   }

   FloatRange getInterpretationZRange() {
      return interpretationZRange;
   }

   void accumulateInterpretationZRange(FloatRange zRange) {
      interpretationZRange = interpretationZRange.union(zRange);
   }

   FloatRange getDataZRange(int channel) {
      return dataZRange[channel - 1];
   }

   void accumulateDataZRange(int channel, FloatRange zRange) {
      dataZRange[channel - 1] = dataZRange[channel - 1].union(zRange);
   }

   public Collection<GridCell> getGridCells() {
      return gridCells.values();
   }

   void accumulateDistance(int channel, double distance) {
      accumulatedDistance[channel - 1] += distance;
   }

   Collection<AcousticCategory> getAcousticCategories() {
      Set<AcousticCategory> acousticCategories = new HashSet<>();
      for (GridCell gridCell : getGridCells()) {
         acousticCategories.addAll(gridCell.getAcousticCategories());
      }
      return acousticCategories;
   }

   GridCell getGridCell(int yIndex) {
      GridCell gridCell = gridCells.get(yIndex);
      if (gridCell == null) {
         float z = yIndex * grid.getVerticalResolution();
         gridCell = new GridCell(this, yIndex, FloatRange.ofMinAndSize(z, grid.getVerticalResolution()));
         gridCells.put(yIndex, gridCell);
      }
      return gridCell;
   }

   void integrationFinished() {
      if (!gridCells.isEmpty()) {
         float minZ = gridCells.firstKey() * grid.getVerticalResolution();
         float maxZ = (gridCells.lastKey() + 1) * grid.getVerticalResolution();
         storedZRange = FloatRange.of(minZ, maxZ);
      }

      gridCells.values().forEach(gridCell -> gridCell.integrationFinished(bubbleCorrection, accumulatedDistance));
   }
}
