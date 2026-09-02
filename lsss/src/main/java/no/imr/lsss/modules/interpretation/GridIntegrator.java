package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Layer;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.ObservationTypeEnum;
import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterData;
import no.imr.lsss.database.tables.hibernate.ScatterDataPK;
import no.imr.lsss.database.tables.hibernate.ScatterObject;
import no.imr.lsss.database.tables.hibernate.ScatterObjectPK;
import no.imr.lsss.database.tables.hibernate.ScatterPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.framework.config.survey.GridConf;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.database.DatabaseUtils;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Integrates the interpretation over the defined grid and saves the result to the database.
 */
public final class GridIntegrator {
   private final LSSS lsss;
   private final GridParameters.PerInterval perInterval;
   private boolean useSchoolGrids = true;

   private final Grid pelagicGrid;
   private final Grid bottomGrid;
   private final List<Grid> pelagicSchoolGrids = new ArrayList<>();
   private final List<Grid> bottomSchoolGrids = new ArrayList<>();

   GridIntegrator(GridParameters.PerInterval perInterval) {
      lsss = perInterval.gridParameters.lsss;
      this.perInterval = perInterval;

      GridConf gridConf = lsss.getConfigurationManager().getGridConf();
      double horizontalGridSize = gridConf.horizontalGridSize.getDoubleValue();
      pelagicGrid = new Grid(null, horizontalGridSize, gridConf.verticalGridSizePelagic.getFloatValue(), perInterval, ScatterTypeEnum.PELAGIC);
      bottomGrid = new Grid(pelagicGrid, horizontalGridSize, gridConf.verticalGridSizeBottom.getFloatValue(), perInterval, ScatterTypeEnum.BOTTOM);
   }

   void skipSchoolGrids() {
      useSchoolGrids = false;
   }

   public List<Grid> getGrids(ScatterTypeEnum scatterTypeEnum) {
      return switch (scatterTypeEnum) {
         case PELAGIC -> List.of(pelagicGrid);
         case BOTTOM -> List.of(bottomGrid);
         case PELAGIC_SCHOOL -> pelagicSchoolGrids;
         case BOTTOM_SCHOOL -> bottomSchoolGrids;
      };
   }

   private <T extends Region> List<T> getIntersectingRegions(Set<T> regions) {
      return regions.stream()
            .filter(pelagicGrid::intersects)
            .toList();
   }

   void integrate(List<Ping> pings, @Nullable Integer echogramObjectNumber, AsyncHandle asyncHandle) {
      if (pings.isEmpty()) {
         return;
      }

      List<Layer> layers = getIntersectingRegions(lsss.getRegionManager().getLayerManager().getLayers());
      List<School> schools = getIntersectingRegions(lsss.getRegionManager().getSchoolManager().getSchools());
      List<Region> regions = Utils.toList(layers, schools);

      if (echogramObjectNumber != null) {
         pelagicGrid.setObjectNumber(echogramObjectNumber);
         bottomGrid.setObjectNumber(echogramObjectNumber);
      }

      GridConf gridConf = lsss.getConfigurationManager().getGridConf();
      double schoolHorizontalGridSize = gridConf.schoolHorizontalGridSize.getDoubleValue();
      float schoolVerticalGridSizePelagic = gridConf.schoolVerticalGridSizePelagic.getFloatValue();
      float schoolVerticalGridSizeBottom = gridConf.schoolVerticalGridSizeBottom.getFloatValue();

      Map<Grid, List<? extends Region>> allGrids = new HashMap<>();
      allGrids.put(pelagicGrid, regions);
      if (useSchoolGrids) {
         for (School school : schools) {
            Grid pelagicSchoolGrid = new Grid(pelagicGrid, schoolHorizontalGridSize, schoolVerticalGridSizePelagic, perInterval, ScatterTypeEnum.PELAGIC_SCHOOL);
            pelagicSchoolGrid.setObjectNumber(school.getObjectNumber());
            allGrids.put(pelagicSchoolGrid, List.of(school));
            pelagicSchoolGrids.add(pelagicSchoolGrid);
         }
      }

      if (!perInterval.intervalConfig.pelagicMode()) {
         allGrids.put(bottomGrid, regions);
         if (useSchoolGrids) {
            for (School school : schools) {
               Grid bottomSchoolGrid = new Grid(pelagicGrid, schoolHorizontalGridSize, schoolVerticalGridSizeBottom, perInterval, ScatterTypeEnum.BOTTOM_SCHOOL);
               bottomSchoolGrid.setObjectNumber(school.getObjectNumber());
               allGrids.put(bottomSchoolGrid, List.of(school));
               bottomSchoolGrids.add(bottomSchoolGrid);
            }
         }
      }

      for (int i = 0; i < pings.size(); i++) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         Ping ping = pings.get(i);
         PingIndex pingIndex = ping.getPingIndex();
         PingIndex nextPingIndex = i + 1 == pings.size() ? perInterval.pingRange.end() : pings.get(i + 1).getPingIndex();
         double horizontalDistance = perInterval.gridParameters.gridPingMapping.distance(pingIndex, nextPingIndex);
         if (horizontalDistance == 0) {
            continue;
         }
         GridParameters.PerPing perPing = new GridParameters.PerPing(perInterval, ping);
         allGrids.forEach((grid, gridRegions) -> {
            grid.integrate(pingIndex, horizontalDistance, gridRegions, perPing);
         });
      }

      allGrids.keySet().forEach(Grid::integrationFinished);
   }

   GridIntegratorResult createScatters() {
      GridIntegratorResult result = new GridIntegratorResult();
      for (int channel : perInterval.channels) {
         createScatters(result, channel);
      }
      return result;
   }

   private void createScatters(GridIntegratorResult result, int channel) {
      createScatters(result, channel, pelagicGrid);
      createScatters(result, channel, pelagicSchoolGrids);

      if (!perInterval.intervalConfig.pelagicMode()) {
         createScatters(result, channel, bottomGrid);
         createScatters(result, channel, bottomSchoolGrids);
      }
   }

   private void createScatters(GridIntegratorResult result, int channel, List<Grid> grids) {
      for (Grid grid : grids) {
         createScatters(result, channel, grid);
      }
   }

   private void createScatters(GridIntegratorResult result, int channel, Grid grid) {
      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      if (survey == null) {
         return;
      }
      DataConfiguration dataConfiguration = perInterval.dataFileSet.getDataConfiguration();

      for (GridColumn gridColumn : grid.getGridColumns()) {
         if (gridColumn.getGridCells().isEmpty() && grid.getScatterTypeEnum().isSchool()) {
            continue;
         }
         // Grid columns with no grid cells are also stored for non-school grids

         PingRange pingRange = gridColumn.getPingRange();

         DatabaseTime databaseTime = new DatabaseTime(pingRange.begin().getInstant());
         ObservationPK observationPK = StoreUtils.createObservationPK(survey.getCompId(), databaseTime, grid.getScatterTypeEnum().getObservationTypeEnum());
         Observation observation = result.observations.computeIfAbsent(observationPK, pk -> {
            return StoreUtils.createObservation(pk, perInterval.dataFileSet, pingRange.begin());
         });

         DatabaseTime endDatabaseTime = new DatabaseTime(pingRange.end().getInstant());
         ObservationPK endObservationPK = StoreUtils.createObservationPK(survey.getCompId(), endDatabaseTime, ObservationTypeEnum.NAVIGATION_DATA_INPUT);
         result.observations.computeIfAbsent(endObservationPK, pk -> {
            return StoreUtils.createObservation(pk, perInterval.dataFileSet, pingRange.end());
         });

         ScatterPK scatterPK = new ScatterPK(
               survey.getCompId().getNation(),
               survey.getCompId().getPlatform(),
               survey.getCompId().getSurvey(),
               grid.getObjectNumber(),
               databaseTime.getDate(),
               databaseTime.getTime(),
               (int) perInterval.dataFileSet.getFrequency(channel),
               (short) channel,
               grid.getScatterTypeEnum().getValue());

         Scatter scatter = new Scatter(scatterPK);

         scatter.setObservationType(observation.getCompId().getObservationType());

         long durationMillis = databaseTime.getInstant().until(DatabaseTime.truncatedInstant(pingRange.end().getInstant()), ChronoUnit.MILLIS);
         scatter.setDuration((int) (durationMillis / 10));
         scatter.setDistanceInterval((float) gridColumn.getGridColumnInterval().horizontalSize());

         FloatRange bottomPhysicalDepthRange;
         if (dataConfiguration.isSeabedMounted()) {
            float bottomPhysicalDepth = dataConfiguration.getSeabedMountedSeabedPhysicalDepth();
            bottomPhysicalDepthRange = FloatRange.of(bottomPhysicalDepth, bottomPhysicalDepth);
         } else {
            bottomPhysicalDepthRange = perInterval.dataFileSet.getBottomDepthRange(pingRange, channel);
         }
         scatter.setMinBottomDepth(bottomPhysicalDepthRange.min());
         scatter.setMaxBottomDepth(bottomPhysicalDepthRange.max());

         scatter.setThreshold(lsss.getRegionManager().getThresholdManager().getMinLowerThreshold(gridColumn.getPingRange()));
         scatter.setBubbleCorrection(gridColumn.getBubbleCorrection());
         scatter.setChannelThickness(grid.getVerticalResolution());

         scatter.setQuality(perInterval.intervalConfig.quality());

         if (gridColumn.getGridCells().isEmpty()) {
            scatter.setUpperDepth(0);
            scatter.setLowerDepth(0);
            scatter.setUpperInterpretationDepth(0);
            scatter.setLowerInterpretationDepth(0);
         } else {
            // Note: z coordinates should be used even though database column names contain the word 'depth'

            FloatRange dataZRange = gridColumn.getDataZRange(channel);
            scatter.setUpperDepth(dataZRange.min());
            scatter.setLowerDepth(dataZRange.max());

            FloatRange interpretationZRange = gridColumn.getInterpretationZRange().intersection(gridColumn.getStoredZRange());
            scatter.setUpperInterpretationDepth(interpretationZRange.min());
            scatter.setLowerInterpretationDepth(interpretationZRange.max());
         }

         scatter.setBottomActive((short) (perInterval.intervalConfig.pelagicMode() ? 0 : 1));

         //For this scatter, get the scatterObject and add it. If it doesn't exist, create.
         ScatterObject scatterObject = result.scatterObjects.get(scatterPK.getObject());
         if (scatterObject == null) {
            ScatterObjectPK scatterObjectPK = new ScatterObjectPK(
                  scatterPK.getNation(),
                  scatterPK.getPlatform(),
                  scatterPK.getSurvey(),
                  scatterPK.getObject());

            short observationType = grid.getScatterTypeEnum().getScatterObjectObservationTypeEnum().getValue();
            //Dummy values, will be updated when saving anyway.
            scatterObject = new ScatterObject(scatterObjectPK, 0, 0, observationType, 0);

            result.scatterObjects.put(scatterPK.getObject(), scatterObject);
         }

         result.scatters.add(scatter);

         double sumInterpretedSa = 0;
         double sumRawDataSa = 0;

         for (AcousticCategory acousticCategory : gridColumn.getAcousticCategories()) {
            boolean storeScatterData = !acousticCategory.rawData()
                  || lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().storeRawDataSpecies.getBooleanValue();

            double sumCategorySa = 0;
            for (GridCell gridCell : gridColumn.getGridCells()) {
               float categorySa = gridCell.getCategorySa(acousticCategory, channel);
               if (categorySa == 0) {
                  continue;
               }
               sumCategorySa += categorySa;

               if (storeScatterData) {
                  ScatterDataPK scatterDataPK = new ScatterDataPK(scatterPK, gridCell.getChannelNumber(), acousticCategory.getCompId().getAcousticCategory());
                  ScatterData scatterData = new ScatterData(scatterDataPK, categorySa);
                  result.scatterDatas.add(scatterData);
               }
            }

            if (acousticCategory.rawData()) {
               sumRawDataSa += sumCategorySa;
            } else {
               sumInterpretedSa += sumCategorySa;
            }

            if (storeScatterData) {
               ScatterDataPK scatterDataPK = new ScatterDataPK(scatterPK, 0, acousticCategory.getCompId().getAcousticCategory());
               ScatterData scatterData = new ScatterData(scatterDataPK, DatabaseUtils.toFiniteFloat(sumCategorySa));
               result.scatterDatas.add(scatterData);
            }
         }

         scatter.setSa(DatabaseUtils.toFiniteFloat(sumInterpretedSa));
         scatter.setPctSa(sumRawDataSa == 0 ? 0 : DatabaseUtils.toFiniteFloat(100 * sumInterpretedSa / sumRawDataSa));
      }
   }
}
