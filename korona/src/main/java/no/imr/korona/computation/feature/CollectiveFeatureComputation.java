package no.imr.korona.computation.feature;

import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.categorization.Neighbor;
import no.imr.korona.computation.categorization.Neighborhood;
import no.imr.korona.util.KoronaPreferences;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public final class CollectiveFeatureComputation {
   private static final String PREFERENCE_LAST_GRID_SIZE_PING_INDEX = "lastGridSizePingIndex";
   private static final String PREFERENCE_LAST_GRID_SIZE_SAMPLE_INDEX = "lastGridSizeSampleIndex";
   private static final String PREFERENCE_LAST_CELL_FILL_FRACTION = "lastCellFillFraction";

   private static final int DEFAULT_GRID_SIZE_PING_INDEX = 20;
   private static final int DEFAULT_GRID_SIZE_SAMPLE_INDEX = 40;
   private static final float DEFAULT_CELL_FILL_FRACTION = 0.5f;

   public static final Color CELL_COLOR = Color.MAGENTA;
   public static final Color ALL_COLOR = Color.BLACK;

   private final @Nullable EchogramWindow echogramWindow;
   private Neighborhood cellNeighborhood = new Neighborhood();
   private Neighborhood allNeighborhood = new Neighborhood();

   private final ChangeManager changeManager = new ChangeManager();

   private static final class CellIndexedNeighbor extends Neighbor {
      private final int pingCellIndex;
      private final int sampleCellIndex;
      private final int gridSizePingIndex;
      private final int gridSizeSampleIndex;

      private CellIndexedNeighbor(Collection<Feature> features, int pingCellIndex, int sampleCellIndex,
                                  int gridSizePingIndex, int gridSizeSampleIndex) {
         super(features);

         this.pingCellIndex = pingCellIndex;
         this.sampleCellIndex = sampleCellIndex;
         this.gridSizePingIndex = gridSizePingIndex;
         this.gridSizeSampleIndex = gridSizeSampleIndex;
      }

      private int getPingCellIndex() {
         return pingCellIndex;
      }

      private int getSampleCellIndex() {
         return sampleCellIndex;
      }

      private int getGridSizePingIndex() {
         return gridSizePingIndex;
      }

      private int getGridSizeSampleIndex() {
         return gridSizeSampleIndex;
      }
   }

   public CollectiveFeatureComputation(EchogramWindow echogramWindow) {
      this.echogramWindow = echogramWindow;

      echogramWindow.getChangeManager().addListener(this::updateGridding);

      updateGridding();
   }

   public CollectiveFeatureComputation() {
      echogramWindow = null;
   }

   private void updateGridding() {
      if (echogramWindow == null) {
         return;
      }

      Configurator configurator = echogramWindow.getConfigurator();

      List<Neighbor> gridCellNeighbors = new ArrayList<>();
      int horGridSize = getGridSizePingIndex();
      int vertGridSize = getGridSizeSampleIndex();
      int maxCellCount = horGridSize * vertGridSize;
      Map<Integer, CellAveragedSv> totalAverages = new HashMap<>();
      List<CellAveragedSv> cellAverages = new ArrayList<>();
      for (FeatureExtractor featureExtractor : configurator.getFeatureExtractors()) {
         if (featureExtractor instanceof FeatureExtractor.FrequencyFeatureExtractor ffe) {
            int index = configurator.getFrequencyIndex(ffe.getKHz());
            if (index >= 0) {
               cellAverages.add(new CellAveragedSv(index, ffe.getFeatureName(), ffe.getRange()));
               totalAverages.put(index, new CellAveragedSv(index, ffe.getFeatureName(), ffe.getRange()));
            }
         }
      }
      //add also average for main frequency
      CellAveragedSv cellAveragedSv38 = new CellAveragedSv(configurator.getReferenceChannel() - 1, FeatureExtractor.ADDITIONAL_FEATURE_SV38, FloatRange.ALL);
      CellAveragedSv totalAveragedSv38 = new CellAveragedSv(configurator.getReferenceChannel() - 1, FeatureExtractor.ADDITIONAL_FEATURE_SV38, FloatRange.ALL);
      for (int horIndex = 0; horIndex < echogramWindow.getWidth() / horGridSize + 1; horIndex++) {
         for (int vertIndex = 0; vertIndex < echogramWindow.getHeight() / vertGridSize + 1; vertIndex++) {
            for (CellAveragedSv cellAverage : cellAverages) {
               cellAverage.clear();
            }
            cellAveragedSv38.clear();
            for (int i = horIndex * horGridSize; i < Math.min((horIndex + 1) * horGridSize, echogramWindow.getWidth()); i++) {
               for (int j = vertIndex * vertGridSize; j < Math.min((vertIndex + 1) * vertGridSize, echogramWindow.getHeight()); j++) {
                  for (CellAveragedSv cellAveragedSv : cellAverages) {
                     //todo: should be echogramWindow.getRange()
                     if (echogramWindow.isActive(i, j)) {
                        cellAveragedSv.update(echogramWindow.getSv(i, j)[cellAveragedSv.getFrequencyIndex()], echogramWindow.getDepth(j));
                     }
                  }
                  if (echogramWindow.isActive(i, j)) {
                     float sv38 = echogramWindow.getSv(i, j)[cellAveragedSv38.getFrequencyIndex()];
                     cellAveragedSv38.update(sv38, Float.NEGATIVE_INFINITY);
                  }
               }
            }
            for (CellAveragedSv cellAverage : cellAverages) {
               totalAverages.get(cellAverage.getFrequencyIndex()).update(cellAverage);
            }
            totalAveragedSv38.update(cellAveragedSv38);
            if ((float) cellAveragedSv38.getCount() / maxCellCount > getCellFillFraction()) {
               List<Feature> gridCellFeatures = new ArrayList<>();
               for (CellAveragedSv cellAveragedSv : cellAverages) {
                  if ((float) cellAveragedSv.getCount() / maxCellCount > getCellFillFraction()) {
                     gridCellFeatures.add(new Feature(cellAveragedSv.getName(), KoronaUtils.toDB(cellAveragedSv.getMeanSv() / cellAveragedSv38.getMeanSv())));
                  }
               }
               gridCellFeatures.add(new Feature(cellAveragedSv38.getName(), KoronaUtils.toDB(cellAveragedSv38.getMeanSv())));
               gridCellNeighbors.add(new CellIndexedNeighbor(gridCellFeatures, horIndex, vertIndex, getGridSizePingIndex(), getGridSizeSampleIndex()));
            }
         }
      }
      cellNeighborhood = new Neighborhood(gridCellNeighbors);

      //create a neighborhood for all points
      if (totalAveragedSv38.getCount() > 0) {
         List<Feature> allFeatures = new ArrayList<>();
         for (CellAveragedSv cellAveragedSv : totalAverages.values()) {
            if (cellAveragedSv.getCount() > 0) {
               allFeatures.add(new Feature(cellAveragedSv.getName(), KoronaUtils.toDB(cellAveragedSv.getMeanSv() / totalAveragedSv38.getMeanSv())));
            }
         }
         allFeatures.add(new Feature(totalAveragedSv38.getName(), KoronaUtils.toDB(totalAveragedSv38.getMeanSv())));
         Neighbor allNeighbor = new Neighbor(allFeatures);
         allNeighborhood = new Neighborhood(List.of(allNeighbor));
      } else {
         allNeighborhood = new Neighborhood();
      }

      changeManager.notifyListeners();
   }

   public Neighborhood getCellNeighborhood() {
      return cellNeighborhood;
   }

   public Neighborhood getAllNeighborhood() {
      return allNeighborhood;
   }

   private static int getGridSizePingIndex() {
      return getPreferences().getInt(PREFERENCE_LAST_GRID_SIZE_PING_INDEX, DEFAULT_GRID_SIZE_PING_INDEX);
   }

   private static int getGridSizeSampleIndex() {
      return getPreferences().getInt(PREFERENCE_LAST_GRID_SIZE_SAMPLE_INDEX, DEFAULT_GRID_SIZE_SAMPLE_INDEX);
   }

   private static float getCellFillFraction() {
      return getPreferences().getFloat(PREFERENCE_LAST_CELL_FILL_FRACTION, DEFAULT_CELL_FILL_FRACTION);
   }

   public static void setGridSizePingIndex(int gridSizePingIndex) {
      if (gridSizePingIndex >= 1) {
         getPreferences().putInt(PREFERENCE_LAST_GRID_SIZE_PING_INDEX, gridSizePingIndex);
      }
   }

   public static void setGridSizeSampleIndex(int gridSizeSampleIndex) {
      if (gridSizeSampleIndex >= 1) {
         getPreferences().putInt(PREFERENCE_LAST_GRID_SIZE_SAMPLE_INDEX, gridSizeSampleIndex);
      }
   }

   public static void setCellFillFraction(float cellFillFraction) {
      if (cellFillFraction > 0 && cellFillFraction <= 1) {
         getPreferences().putFloat(PREFERENCE_LAST_CELL_FILL_FRACTION, cellFillFraction);
      }
   }

   public static void resetUserPrefs() {
      Preferences preferences = getPreferences();
      preferences.remove(PREFERENCE_LAST_GRID_SIZE_PING_INDEX);
      preferences.remove(PREFERENCE_LAST_GRID_SIZE_SAMPLE_INDEX);
      preferences.remove(PREFERENCE_LAST_CELL_FILL_FRACTION);
   }

   private static Preferences getPreferences() {
      return KoronaPreferences.node("featureComputation");
   }

   public void markSelectedGridCell(Rectangle2D selectionRectangle, String xAxisName, String yAxisName, boolean clearMarking, boolean marking) {
      if (echogramWindow == null) {
         return;
      }

      if (clearMarking) {
         echogramWindow.clearMarking();
      }
      //test if any of the gridded features intersects the selection point

      Neighbor bestFitNeighbor = null;
      double bestFitDist = Double.MAX_VALUE;
      for (Neighbor neighbor : cellNeighborhood.getNeighbors()) {
         Feature xFeature = neighbor.getFeature(xAxisName);
         Feature yFeature = neighbor.getFeature(yAxisName);
         if (xFeature != null && yFeature != null) {
            if (selectionRectangle.contains(xFeature.value(), yFeature.value())) {
               double normX = Math.abs(selectionRectangle.getCenterX() - xFeature.value()) / selectionRectangle.getWidth();
               double normY = Math.abs(selectionRectangle.getCenterY() - yFeature.value()) / selectionRectangle.getHeight();
               double fit = normX * normX + normY * normY;
               if (fit < bestFitDist) {
                  bestFitNeighbor = neighbor;
                  bestFitDist = fit;
               }
            }
         }
      }
      if (bestFitNeighbor instanceof CellIndexedNeighbor cellIndexed) {
         int horIndex = cellIndexed.getPingCellIndex();
         int vertIndex = cellIndexed.getSampleCellIndex();
         int horGridSize = cellIndexed.getGridSizePingIndex();
         int vertGridSize = cellIndexed.getGridSizeSampleIndex();
         for (int i = horIndex * horGridSize; i < Math.min((horIndex + 1) * horGridSize, echogramWindow.getWidth()); i++) {
            for (int j = vertIndex * vertGridSize; j < Math.min((vertIndex + 1) * vertGridSize, echogramWindow.getHeight()); j++) {
               echogramWindow.mark(i, j, marking);
            }
         }
      }
   }

   public static void editGriddingSettings(Component referenceComponent) {
      IntParameter horizontalGridSize = new IntParameter(
            new Name("HorizontalGridSize", "Horizontal grid size"),
            getGridSizePingIndex(), Unit.NONE);
      horizontalGridSize.subscribe(CollectiveFeatureComputation::setGridSizePingIndex);

      IntParameter verticalGridSize = new IntParameter(
            new Name("VerticalGridSize", "Vertical grid size"),
            getGridSizeSampleIndex(), Unit.NONE);
      verticalGridSize.subscribe(CollectiveFeatureComputation::setGridSizeSampleIndex);

      FloatParameter cellFillFraction = new FloatParameter(
            new Name("CellFillFraction", "Cell fill fraction"),
            getCellFillFraction(), Unit.NONE);
      cellFillFraction.subscribe(CollectiveFeatureComputation::setCellFillFraction);

      List<BaseParameter<?>> parameters = List.of(horizontalGridSize, verticalGridSize, cellFillFraction);
      ParameterEditor parameterEditor = new ParameterEditor(parameters);

      new ConfigurableGUIDialog(referenceComponent, "Gridding configuration", new ParameterCollection(parameters))
            .setCloseOnOk(parameterEditor::commitEdits)
            .setGUI(parameterEditor.getEditorComponent())
            .show();
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }
}
