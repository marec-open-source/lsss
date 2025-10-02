package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.geometry.depth.PerPingDepthTransform;
import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.Region;
import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.tools.logging.Log;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.RangeMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Grid {
   private final ScatterTypeEnum scatterTypeEnum;
   private final float verticalResolution;
   private final FloatRange interpretationZRange;
   private final List<GridColumn> gridColumns = new ArrayList<>();
   private final RangeMap<PingIndex, GridColumn> pingIndexToGridColumn = new ArrayRangeMap<>();
   private final int transducerCount;
   private int objectNumber = -1;

   Grid(@Nullable Grid masterGrid, double horizontalResolution, float verticalResolution, GridParameters.PerInterval perInterval, ScatterTypeEnum scatterTypeEnum) {
      this.scatterTypeEnum = scatterTypeEnum;
      this.verticalResolution = verticalResolution;
      GridParameters gridParameters = perInterval.gridParameters;
      interpretationZRange = scatterTypeEnum.isPelagic() ? FloatRange.ALL : gridParameters.bottomInterpretationZRange;
      transducerCount = perInterval.dataFileSet.getRawFileConfiguration().getTransducerCount();
      new GridIndexFinder(perInterval.dataFileSet, perInterval.pingRange, horizontalResolution, gridParameters.gridPingMapping).gridColumnIntervals()
            .flatMap(gridParameters.interpretationModule::storableGridColumnIntervals)
            .filter(gridColumnInterval -> {
               return masterGrid == null || masterGrid.pingIndexToGridColumn.containsAllKeys(gridColumnInterval.pingRange());
            })
            .forEach(gridColumnInterval -> {
               float bubbleCorrection = gridParameters.lsss.getRegionManager().getBubbleCorrectionManager().getBubbleCorrection(gridColumnInterval.pingRange());
               GridColumn gridColumn = new GridColumn(this, gridColumnInterval, bubbleCorrection);
               gridColumns.add(gridColumn);
               pingIndexToGridColumn.put(gridColumnInterval.pingRange(), gridColumn);
            });
   }

   void integrate(PingIndex pingIndex, double horizontalDistance, List<? extends Region> regions, GridParameters.PerPing perPing) {
      GridColumn gridColumn = pingIndexToGridColumn.get(pingIndex);
      if (gridColumn == null) {
         return;
      }

      GridParameters.PerInterval perInterval = perPing.perInterval;
      GridParameters gridParameters = perInterval.gridParameters;
      PerPingDepthTransform perPingDepthTransform = perPing.getPerPingDepthTransform(this);
      FloatRange interpretationDepthRange = perPingDepthTransform.zToDepth(interpretationZRange);

      int minGridY = (int) Math.ceil(interpretationZRange.min() / verticalResolution);
      int maxGridY = (int) Math.floor(interpretationZRange.max() / verticalResolution);

      for (int channel : perInterval.channels) {
         GridParameters.PerChannel perChannel = perPing.perChannels.get(channel);
         PowerData powerData = perChannel.powerData;
         if (powerData == null) {
            continue;
         }

         gridColumn.accumulateDistance(channel, horizontalDistance);

         FloatRange dataDepthRange = powerData.getDepthRange();
         FloatRange dataZRange = perPingDepthTransform.depthToZ(dataDepthRange);
         gridColumn.accumulateDataZRange(channel, dataZRange);

         for (Region region : regions) {
            for (FloatRange regionDepthRange : perChannel.getDepthRanges(region)) {
               FloatRange depthRange = regionDepthRange.intersection(interpretationDepthRange);
               if (depthRange.isEmpty()) {
                  continue;
               }

               FloatRange zRange = perPingDepthTransform.depthToZ(depthRange);
               gridColumn.accumulateInterpretationZRange(zRange);
               int gridY0 = Math.max(minGridY, (int) Math.floor(zRange.min() / verticalResolution));
               int gridY1 = Math.min(maxGridY, (int) Math.ceil(zRange.max() / verticalResolution));

               for (int gridY = gridY0; gridY < gridY1; gridY++) {
                  GridCell gridCell = gridColumn.getGridCell(gridY);
                  FloatRange cellDepthRange = perPingDepthTransform.zToDepth(gridCell.getZRange()).intersection(depthRange);
                  if (cellDepthRange.isEmpty()) {
                     continue;
                  }
                  double verticalIntegral = powerData.getVerticalIntegralSv(cellDepthRange, perPing.svRange);
                  if (verticalIntegral == 0) {
                     continue;
                  }
                  double integral = horizontalDistance * verticalIntegral;

                  ChannelInterpretation channelInterpretation = region.getChannelInterpretation(channel);
                  for (Map.Entry<Integer, Float> entry : channelInterpretation.getAssignments().entrySet()) {
                     AcousticCategory acousticCategory = gridParameters.acousticCategoryMap.get(entry.getKey());
                     if (acousticCategory == null) {
                        Log.global.warning("Acoustic category not found: " + entry.getKey());
                        continue;
                     }
                     float assignment = entry.getValue();
                     if (assignment > 0) {
                        gridCell.addSa(channel, acousticCategory, (float) (assignment * integral));
                     }
                  }

                  AcousticCategory rawDataAcousticCategory = gridParameters.rawDataAcousticCategory;
                  if (rawDataAcousticCategory != null) {
                     gridCell.addSa(channel, rawDataAcousticCategory, (float) integral);
                  }
               }
            }
         }
      }
   }

   boolean intersects(Region region) {
      return pingIndexToGridColumn.containsAnyKey(region.getPingRange());
   }

   void integrationFinished() {
      gridColumns.forEach(GridColumn::integrationFinished);
   }

   ScatterTypeEnum getScatterTypeEnum() {
      return scatterTypeEnum;
   }

   public List<GridColumn> getGridColumns() {
      return gridColumns;
   }

   float getVerticalResolution() {
      return verticalResolution;
   }

   int getTransducerCount() {
      return transducerCount;
   }

   boolean isPelagic() {
      return scatterTypeEnum.isPelagic();
   }

   int getObjectNumber() {
      return objectNumber;
   }

   void setObjectNumber(int objectNumber) {
      this.objectNumber = objectNumber;
   }
}
