package no.imr.korona.computation.categorization;

import com.google.common.primitives.Ints;
import no.imr.korona.computation.BaseBufferedPingModule;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.computation.categorization.apriori.PerPingAPriori;
import no.imr.korona.computation.feature.CellAveragedSv;
import no.imr.korona.computation.feature.Feature;
import no.imr.korona.computation.feature.FeatureExtractor;
import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.data.datagrams.RegionBorderDatagram;
import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datagrams.RegionTableOfContentsDatagram;
import no.imr.korona.data.datagrams.subdatagrams.graphical.EchogramOffsetPoint;
import no.imr.korona.data.datagrams.subdatagrams.graphical.GraphicalInfoSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.graphical.GraphicalInfoTocSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.graphical.OffsetPolygon;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class RegionCategorizationModule extends BaseBufferedPingModule {
   private static final String SV38_FEATURE_NAME = FeatureExtractor.ADDITIONAL_FEATURE_SV38;
   private static final String LOW_FILL_FACTOR = "LowFillFactor";

   private enum CellEvaluationResult {
      OneCategory, MultiCategory, Unresolvable
   }

   public final BooleanParameter useCellCategorization = new BooleanParameter(
         new Name("UseCellCategorization", "Use cell categorization"),
         true,
         "If enabled, use cell as input to school categorization");

   public final IntParameter horizontalGridCellSize = new IntParameter(
         new Name("Horizontal cell size", "Horizontal cell size"),
         16, Unit.COUNT, ValueConstraints.gt(0),
         "Cell size in number of pings");

   public final IntParameter verticalGridCellSize = new IntParameter(
         new Name("Vertical cell size", "Vertical cell size"),
         50, Unit.COUNT, ValueConstraints.gt(0),
         "Cell size in number of samples");

   public final IntParameter upperOffset = new IntParameter(
         new Name("Upper sample offset", "Upper sample offset"),
         5, Unit.COUNT, ValueConstraints.gte(0),
         "Number of samples excluded at the top of a region");

   public final IntParameter lowerOffset = new IntParameter(
         new Name("Lower sample offset", "Lower sample offset"),
         5, Unit.COUNT, ValueConstraints.gte(0),
         "Number of samples excluded at the bottom of a region");

   public final IntParameter horizontalOffset = new IntParameter(
         new Name("HorizontalOffset", "Horizontal offset"),
         0, Unit.COUNT, ValueConstraints.gte(0),
         "Number of pings excluded at the left and right edges of a region");

   public final FloatParameter secondaryCategoryFraction = new FloatParameter(
         new Name("SecondaryCategoryFraction", "Secondary category fraction"),
         0.1f, Unit.DIMENSIONLESS, ValueConstraints.gteLte(0f, 0.5f),
         "Fraction of cells in a region that can be allowed to be different from the dominant category");

   public final FloatParameter fillFactor = new FloatParameter(
         new Name("Fill factor for cells", "Fill factor for cells"),
         0.5f, Unit.DIMENSIONLESS, ValueConstraints.gte(0f),
         "The fill factor of cells must be above this value for cells to be used in the analysis of schools");

   public final FloatParameter minFractionCategorizedCells = new FloatParameter(
         new Name("MinFractionCategorizedCells", "Minimum fraction categorized cells"),
         0.5f, Unit.DIMENSIONLESS, ValueConstraints.gte(0f),
         "The fraction of categorized cells has to be above this factor for the school to be categorized");

   public final IntParameter minCells = new IntParameter(
         new Name("MinCells", "Minimum number of cells"),
         3, Unit.COUNT, ValueConstraints.gte(1),
         "If the number of cells is lower than this, the data will be merged");

   public final ObjectParameter<Category.DistributionLevel> schoolCategorizationDistribution = new ObjectParameter<>(
         new Name("SchoolCategorizationDistribution", "School categorization distribution"),
         Category.DistributionLevel.CELL, Category.DistributionLevel.values(),
         "Distribution to use when categorizing whole schools");

   public final BooleanParameter enableUncategorizedCategory = new BooleanParameter(
         new Name("EnableUncategorizedCategory", "Enable the uncategorized category"),
         false,
         "If enabled, the schools where it cannot be determined if they are single og multi species are set to this category. Otherwise no region category is given.");

   public final BooleanParameter enableUnknownCategory = new BooleanParameter(
         new Name("EnableUnknownCategory", "Enable the unknown category"),
         false,
         "If enabled, the unknown category can be used as a valid school category for schools determined to be single species. Otherwise no region category is given.");

   public RegionCategorizationModule() {
      useCellCategorization.addListenerAndNotify(useCells -> {
         horizontalGridCellSize.setEnabled(useCells);
         verticalGridCellSize.setEnabled(useCells);
         upperOffset.setEnabled(useCells);
         lowerOffset.setEnabled(useCells);
         horizontalOffset.setEnabled(useCells);
         secondaryCategoryFraction.setEnabled(useCells);
         fillFactor.setEnabled(useCells);
         minFractionCategorizedCells.setEnabled(useCells);
         minCells.setEnabled(useCells);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            useCellCategorization,
            horizontalGridCellSize,
            verticalGridCellSize,
            upperOffset,
            lowerOffset,
            horizontalOffset,
            secondaryCategoryFraction,
            fillFactor,
            minFractionCategorizedCells,
            minCells,
            schoolCategorizationDistribution,
            enableUncategorizedCategory,
            enableUnknownCategory
      );
   }

   @Override
   public @Nullable GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      //todo: Check for only one CategorizationModule? Use Cac0Datagram instead?
      CategorizationModule categorizationModule = getModuleContainer().getModule(CategorizationModule.class);
      if (categorizationModule == null) {
         Log.global.warning("Categorization module not found. RegionCategorizationModule is deactivated");
         return null;
      }
      return new RegionCategorizationModuleComputation(this, computationContext, pingSource, categorizationModule);
   }

   public static final class RegionCategorizationModuleComputation extends BaseBufferedPingModuleComputation {
      private final RegionCategorizationModule module;
      private final CategorizationModule categorizationModule;
      private final GaussCategorizer gaussCellCategorizer;
      private final GaussCategorizer gaussSchoolCategorizer;
      private final Category unknownCategory;
      private final Category uncategorizedCategory;

      private final List<String> activeFeatureNames = new ArrayList<>();
      private final Map<Integer, FeatureExtractor.FrequencyFeatureExtractor> frequencyFeatureExtractorMap = new TreeMap<>();

      private final Configurator configurator;

      private final Map<GridCellKey, GridCellValue> gridCells = new HashMap<>();
      private int counter = 0;

      private final List<Long> graphicalInfoNTDates = new ArrayList<>();

      private final RegionBorderBufferList regionBorderBufferList = new RegionBorderBufferList();

      private RegionCategorizationModuleComputation(RegionCategorizationModule module, ComputationContext computationContext, PingSource pingSource, CategorizationModule categorizationModule) {
         super(module, computationContext, pingSource);

         this.module = module;
         this.categorizationModule = categorizationModule;
         setPingsToBuffer(module.horizontalOffset.getIntValue());

         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         configurator = new Configurator(computationContext.getModuleContainer().getConfigFileSettings(), pingConfiguration.getRawFileConfiguration());

         for (BooleanParameter parameter : categorizationModule.activeCategories.getParameters()) {
            Category category = configurator.getCategory(parameter.getPersistentName());
            if (category != null) {
               category.setActive(parameter.getBooleanValue());
            }
         }

         gaussCellCategorizer = new GaussCategorizer(Category.DistributionLevel.CELL, true, configurator);

         Category.DistributionLevel schoolDistributionLevel = module.schoolCategorizationDistribution.getValue();
         gaussSchoolCategorizer = new GaussCategorizer(schoolDistributionLevel, true, configurator);

         unknownCategory = configurator.getSpecialCategory(Configurator.UNKNOWN_CATEGORY_NAME);
         uncategorizedCategory = configurator.getSpecialCategory(Configurator.UNCATEGORIZED_NAME);

         frequencyFeatureExtractorMap.clear();
         for (FeatureExtractor featureExtractor : configurator.getFeatureExtractors()) {
            if (featureExtractor instanceof FeatureExtractor.FrequencyFeatureExtractor ffe) {
               frequencyFeatureExtractorMap.put(ffe.getKHz(), ffe);
            }
         }

         updateActiveFeatures();
      }

      public void updateActiveFeatures() {
         activeFeatureNames.clear();
         for (BooleanParameter booleanParameter : categorizationModule.activeFeatures.getParameters()) {
            if (booleanParameter.getBooleanValue()) {
               activeFeatureNames.add(booleanParameter.getPersistentName());
            }
         }
      }

      private void updateGridCells(Ping ping, Map<Integer, FloatRangeSet> regionRanges) {
         PowerData referenceDatagram = ping.getPowerData(configurator.getReferenceChannel());
         if (referenceDatagram == null) {
            return;
         }
         if (Utils.hzToKHz(referenceDatagram.getFrequency()) != Utils.hzToKHz(configurator.getReferenceFrequency())) {
            return;
         }

         for (Map.Entry<Integer, FloatRangeSet> integerFloatRangeSetEntry : regionRanges.entrySet()) {
            int id = integerFloatRangeSetEntry.getKey();
            int horGridCell = counter / module.horizontalGridCellSize.getIntValue();
            for (ChannelData channelData : ping.getChannelDatas()) {
               if (channelData == null) {
                  continue;
               }
               PowerData powerData = channelData.getPowerData();

               int channel = powerData.getChannel();

               ResampledFloatArray resampledFloatArray = ResampledFloatArray.create(powerData.getSv(), powerData, referenceDatagram);

               int kHz = Utils.hzToKHz(powerData.getFrequency());
               FeatureExtractor.FrequencyFeatureExtractor ffe = frequencyFeatureExtractorMap.get(kHz);
               // No need for the data if frequency extractor is null unless this is the reference channel
               if (ffe == null && channel != configurator.getReferenceChannel()) {
                  continue;
               }
               FloatRangeSet depthRanges = regionBorderBufferList.getCenterBuffer().getBorderRanges().get(id);
               //remove upper and lower samples
               List<FloatRange> offsetModifiedDepthRanges = new ArrayList<>();
               for (FloatRange floatRange : depthRanges) {
                  float min = floatRange.min() + referenceDatagram.getSampleDistance() * module.upperOffset.getIntValue();
                  float max = floatRange.max() - referenceDatagram.getSampleDistance() * module.lowerOffset.getIntValue();
                  if (min < max) {
                     offsetModifiedDepthRanges.add(FloatRange.of(min, max));
                  }
               }
               FloatRangeSet offsetModifiedDepthRangeSet = FloatRangeSet.of(offsetModifiedDepthRanges);
               FloatRangeSet intersectionRanges = offsetModifiedDepthRangeSet.intersection(integerFloatRangeSetEntry.getValue());
               for (FloatRange floatRange : intersectionRanges) {
                  int startSample = Math.max(0, referenceDatagram.depthToSampleIndex(floatRange.min()));
                  int endSample = Math.min(powerData.getCount(), referenceDatagram.depthToSampleIndex(floatRange.max()));
                  for (int sample = startSample; sample < endSample; sample++) {
                     int vertGridCell = sample / module.verticalGridCellSize.getIntValue();
                     GridCellKey key = new GridCellKey(horGridCell, vertGridCell, id);
                     GridCellValue value = gridCells.get(key);
                     if (value == null) {
                        value = new GridCellValue();
                        gridCells.put(key, value);
                     }
                     CellAveragedSv cellAveragedSv = value.getGridAverageValue(channel);
                     if (cellAveragedSv == null) {
                        //No test for range on reference channel since this channel is needed in the relative frequency
                        FloatRange range = channel == configurator.getReferenceChannel() ? FloatRange.ALL : ffe.getRange();
                        cellAveragedSv = new CellAveragedSv(configurator.getFrequencyIndex(kHz), "", range);
                        value.putGridAverageValue(channel, cellAveragedSv);
                     }
                     if (sample >= resampledFloatArray.getBeginReferenceIndex() && sample < resampledFloatArray.getEndReferenceIndex()) {
                        float svSample = resampledFloatArray.getValueForReferenceIndex(sample);
                        float sampleRange = powerData.getSampleRange(sample);
                        cellAveragedSv.update(svSample, sampleRange);
                     }
                  }
               }
            }
         }
      }

      private @Nullable Pixel analyzeSchool(RegionInfoDatagram regionInfoDatagram, GraphicalInfoSubDatagram graphicalInfoSubDatagram, PowerData referenceDatagram, PerPingAPriori perPingAPriori) {
         Map<SimplifiedGridCellKey, GridCellValue> mergedCells = new HashMap<>();
         List<GridCellKey> toBeRemovedKeys = new ArrayList<>();
         for (Map.Entry<GridCellKey, GridCellValue> cellEntry : gridCells.entrySet()) {
            if (Ints.contains(regionInfoDatagram.getBorderIds(), cellEntry.getKey().borderId)) {
               toBeRemovedKeys.add(cellEntry.getKey());
               if (regionInfoDatagram.isAccepted()) {
                  SimplifiedGridCellKey simplifiedKey = new SimplifiedGridCellKey(cellEntry.getKey());
                  if (!mergedCells.containsKey(simplifiedKey)) {
                     mergedCells.put(simplifiedKey, cellEntry.getValue());
                  } else {
                     mergedCells.get(simplifiedKey).mergeWith(cellEntry.getValue());
                  }
               }
            }
         }
         //throw away used grid cells with id's in this regionInfo datagram
         for (GridCellKey cellKey : toBeRemovedKeys) {
            gridCells.remove(cellKey);
         }
         if (regionInfoDatagram.isAccepted()) {
            //perform analysis
            if (!module.useCellCategorization.getBooleanValue()) {
               return categorizeSchool(mergedCells.values(), graphicalInfoSubDatagram, perPingAPriori); //OneCategory
            } else {
               CellEvaluationResult cellEvaluationResult = evaluateCells(mergedCells, graphicalInfoSubDatagram, referenceDatagram, perPingAPriori);
               return switch (cellEvaluationResult) {
                  case MultiCategory -> null;
                  case Unresolvable -> {
                     if (!module.enableUncategorizedCategory.getBooleanValue()) {
                        yield null;
                     }
                     Pixel pixel = new Pixel(Float.NaN); // LogSv38 does not matter since final category is assigned
                     pixel.setFinalCategory(uncategorizedCategory);
                     yield pixel;
                  }
                  case OneCategory -> categorizeSchool(mergedCells.values(), graphicalInfoSubDatagram, perPingAPriori);
               };
            }
         }
         return null;
      }

      private Map<String, Integer> cellCategorizationHistogram(Map<SimplifiedGridCellKey, GridCellValue> mergedCells, GraphicalInfoSubDatagram graphicalInfoSubDatagram, PowerData referenceDatagram, PerPingAPriori perPingAPriori) {
         Map<String, Integer> histogram = new HashMap<>();
         int samplesInCell = module.horizontalGridCellSize.getIntValue() * module.verticalGridCellSize.getIntValue();
         for (Map.Entry<SimplifiedGridCellKey, GridCellValue> gridCellValueEntry : mergedCells.entrySet()) {
            GridCellValue gridCellValue = gridCellValueEntry.getValue();
            Pixel result = categorize(gridCellValue, gaussCellCategorizer, perPingAPriori, true);
            CategoryData bestCategory = result != null ? result.getBestCategory() : null;
            if (bestCategory != null) {
               String categoryName = bestCategory.getCategory().getName();
               CellAveragedSv referenceValue = gridCellValue.gridAverages.get(configurator.getReferenceChannel());
               //This seems to be wrong since fill-factor of 1 will always result in LOW_FILL_FACTOR: <= fillFactor.getFloatValue())
               //if (referenceValue == null || referenceValue.getFillFactor(samplesInCell) <= fillFactor.getFloatValue())
               if (referenceValue == null || referenceValue.getFillFactor(samplesInCell) < module.fillFactor.getFloatValue()) {
                  // rename unknown category to low fill factor
                  categoryName = LOW_FILL_FACTOR;
               }
               histogram.merge(categoryName, 1, Integer::sum);
               graphicalInfoSubDatagram.addGraphicalObject(createEchogramPolygon(gridCellValueEntry.getKey(),
                     bestCategory.getCategory().getColor(), !categoryName.equals(LOW_FILL_FACTOR), referenceDatagram, result, categoryName));
            }
         }
         return histogram;
      }

      private OffsetPolygon createEchogramPolygon(SimplifiedGridCellKey gridCellKey, Color color, boolean filled, PowerData referenceDatagram, Pixel pixel, String categoryName) {
         int startOffset = gridCellKey.horizontalIndex * module.horizontalGridCellSize.getIntValue() - counter;
         int endOffset = Math.min((gridCellKey.horizontalIndex + 1) * module.horizontalGridCellSize.getIntValue() - counter, 0);
         int startSample = gridCellKey.verticalIndex * module.verticalGridCellSize.getIntValue();
         int endSample = startSample + module.verticalGridCellSize.getIntValue();
         float startDepth = referenceDatagram.getSampleDepth(startSample);
         float endDepth = referenceDatagram.getSampleDepth(endSample);
         List<EchogramOffsetPoint> echogramOffsetPoints = List.of(
               new EchogramOffsetPoint(startOffset, startDepth),
               new EchogramOffsetPoint(endOffset, startDepth),
               new EchogramOffsetPoint(endOffset, endDepth),
               new EchogramOffsetPoint(startOffset, endDepth)
         );
         CategoryData bestCategory = pixel.getBestCategory();
         List<String> texts = List.of(
               "Category: " + categoryName,
               "Category similarity: " + (bestCategory != null ? Utils.format("%.2f", bestCategory.getBoundedProbability()) : "N/A")
         );
         return new OffsetPolygon(texts, color, filled, echogramOffsetPoints);
      }

      private CellEvaluationResult evaluateCells(Map<SimplifiedGridCellKey, GridCellValue> mergedCells, GraphicalInfoSubDatagram graphicalInfoSubDatagram, PowerData referenceDatagram, PerPingAPriori perPingAPriori) {
         Map<String, Integer> cellCategories = cellCategorizationHistogram(mergedCells, graphicalInfoSubDatagram, referenceDatagram, perPingAPriori);
         int bestCount = 0;
         int totalKnowns = 0;
         int cellsCategorized = 0;
         for (Map.Entry<String, Integer> entry : cellCategories.entrySet()) {
            if (!entry.getKey().equals(LOW_FILL_FACTOR)) {
               cellsCategorized += entry.getValue();
            }
            if (entry.getKey().equals(unknownCategory.getName()) || entry.getKey().equals(LOW_FILL_FACTOR)) {
               continue;
            }
            totalKnowns += entry.getValue();
            if (bestCount < entry.getValue()) {
               bestCount = entry.getValue();
            }
         }
         if (totalKnowns == 0 || cellsCategorized < module.minCells.getIntValue()) {
            //if (cellsCategorized > 0 ) //&& module.minCells.getIntValue() < cellsCategorized)
            if (totalKnowns == 0 && cellsCategorized >= module.minCells.getIntValue()) {
               // There is not a problem with low fill factor. There is at least one cell with enough samples which
               // must have been categorized as unknown.
               //String tooltipText = "Enough cells above fill factor limit, but no cells are categorized to a known category";
               String tooltipText = "Enough cells " + cellsCategorized + " >= " + module.minCells.getIntValue() + " above fill factor limit, but no cells are categorized to a known category";
               graphicalInfoSubDatagram.addText(tooltipText);
               return CellEvaluationResult.Unresolvable;
            } else {
               // All cells have a too low fill factor or no cells got a real category. Try to merge all cells and recategorize.
               Category resultOfMerging = mergeCellsAndCategorize(mergedCells, perPingAPriori);
               if (resultOfMerging.equals(unknownCategory) || resultOfMerging.equals(uncategorizedCategory)) {
                  // Merged cell category is unknown or the fill factor is still to low to conclude.
                  String tooltipText = "Cells have been merged, but the category is still unknown";
                  graphicalInfoSubDatagram.addText(tooltipText);
                  return CellEvaluationResult.Unresolvable;
               }
               return CellEvaluationResult.OneCategory;
            }
         }
         float fractionCategorized = (float) totalKnowns / cellsCategorized;
         if (fractionCategorized <= module.minFractionCategorizedCells.getFloatValue()) {
            // The number of cells categorized as unknown is too high compared to cells given a 'real' category.
            String tooltipText = "Fraction categorized cells: " + Utils.format("%.2f", fractionCategorized) + " &lt; " + module.minFractionCategorizedCells.getFloatValue();
            graphicalInfoSubDatagram.addText(tooltipText);
            return CellEvaluationResult.Unresolvable;
         }
         float secondaryFraction = (totalKnowns - bestCount) / (float) totalKnowns;
         if (secondaryFraction <= module.secondaryCategoryFraction.getFloatValue()) {
            return CellEvaluationResult.OneCategory;
         } else {
            String tooltipText = "Secondary fraction: " + Utils.format("%.2f", secondaryFraction) + " &gt; " + module.secondaryCategoryFraction.getFloatValue();
            graphicalInfoSubDatagram.addText(tooltipText);
            return CellEvaluationResult.MultiCategory;
         }
      }

      private Category mergeCellsAndCategorize(Map<SimplifiedGridCellKey, GridCellValue> mergedCells, PerPingAPriori perPingAPriori) {
         GridCellValue mergedCell = new GridCellValue();
         for (GridCellValue gridCellValue : mergedCells.values()) {
            mergedCell.mergeWith(gridCellValue);
         }
         int samplesInCell = module.horizontalGridCellSize.getIntValue() * module.verticalGridCellSize.getIntValue();
         CellAveragedSv referenceValue = mergedCell.gridAverages.get(configurator.getReferenceChannel());
         if (referenceValue != null && referenceValue.getFillFactor(samplesInCell) >= module.fillFactor.getFloatValue()) {
            Pixel p = categorize(mergedCell, gaussCellCategorizer, perPingAPriori, true);
            CategoryData bestCategory = p != null ? p.getBestCategory() : null;
            if (bestCategory != null) {
               return bestCategory.getCategory();
            }
         }
         return uncategorizedCategory;
      }

      private @Nullable Pixel categorize(GridCellValue gridCellValue, GaussCategorizer gaussCategorizer, PerPingAPriori perPingAPriori, boolean enableUnknownCategory) {
         CellAveragedSv referenceValue = gridCellValue.gridAverages.get(configurator.getReferenceChannel());
         float logSv38 = referenceValue != null ? PowerData.svToLogSv((float) referenceValue.getMeanSv()) : 0;
         Pixel pixel = new Pixel(logSv38);

         int samplesInCell = module.horizontalGridCellSize.getIntValue() * module.verticalGridCellSize.getIntValue();
         if (referenceValue != null && referenceValue.getFillFactor(samplesInCell) >= module.fillFactor.getFloatValue()) {
            double referenceAverage = referenceValue.getMeanSv();

            for (FeatureExtractor.FrequencyFeatureExtractor ffe : frequencyFeatureExtractorMap.values()) {
               if (activeFeatureNames.contains(ffe.getFeatureName())) {
                  if (gridCellValue.gridAverages.containsKey(configurator.getFrequencyIndex(ffe.getKHz()) + 1)) {
                     //create cell features corresponding to the feature extractors.
                     CellAveragedSv cellValue = gridCellValue.gridAverages.get(configurator.getFrequencyIndex(ffe.getKHz()) + 1);
                     if (cellValue.getFillFactor(samplesInCell) >= module.fillFactor.getFloatValue()) {
                        pixel.addFeature(new Feature(ffe.getFeatureName(), KoronaUtils.toDB(cellValue.getMeanSv() / referenceAverage)));
                     }
                  }
               }
            }
            if (configurator.getFeatureExtractor(SV38_FEATURE_NAME) != null && activeFeatureNames.contains(SV38_FEATURE_NAME)) {
               pixel.addFeature(new Feature(SV38_FEATURE_NAME, KoronaUtils.toDB(referenceAverage)));
            }

            gaussCategorizer.categorize(pixel, perPingAPriori);
            //update discriminant
            //todo: use a configurable discriminant module for this?
            for (CategoryData cd : pixel.getCategoryDatas()) {
               //a priori discriminant
               cd.setDiscriminant(cd.getNormalizedProbability() * cd.getTotalApriori());
            }
         }
         List<CategoryData> categories = pixel.getAcceptableCategoryDatas();
         if (categories.isEmpty()) {
            if (!enableUnknownCategory) {
               return null;
            }
            pixel.setFinalCategory(unknownCategory);
         }

         return pixel;
      }

      private @Nullable Pixel categorizeSchool(Collection<GridCellValue> gridCellValues, GraphicalInfoSubDatagram graphicalInfoSubDatagram, PerPingAPriori perPingAPriori) {
         //merge all grid cell values into one
         GridCellValue mergedValue = new GridCellValue();
         for (GridCellValue gridCellValue : gridCellValues) {
            mergedValue.mergeWith(gridCellValue);
         }
         Pixel pixel = categorize(mergedValue, gaussSchoolCategorizer, perPingAPriori, module.enableUnknownCategory.getBooleanValue());
         CategoryData bestCategory = pixel != null ? pixel.getBestCategory() : null;
         if (bestCategory == null || bestCategory.getCategory().getName().equals(unknownCategory.getName())) {
            String tooltipText = "School categorization was attempted, but resulted in unknown category";
            graphicalInfoSubDatagram.addText(tooltipText);
         }
         return pixel;
      }

      private void updateBuffers(Ping currentPing) {
         regionBorderBufferList.shiftIndex();
         int index = 0;
         if (index > regionBorderBufferList.getLastAddedIndex()) {
            regionBorderBufferList.add(currentPing, index);
         }
         index = 1;
         for (Ping ping : getPingOutputQueue()) {
            if (index > regionBorderBufferList.getLastAddedIndex()) {
               regionBorderBufferList.add(ping, index);
            }
            index++;
         }
         regionBorderBufferList.removeOld(-module.horizontalOffset.getIntValue());
      }

      @Override
      protected @Nullable Ping generateOutput() throws IOException {
         Ping next = pollFirstPingInBuffer();
         if (next == null) {
            return null;
         }
         processPing(next);
         return next;
      }

      private void processPing(Ping ping) {
         ping.removeAll(Cas0Datagram.class);

         updateBuffers(ping);

         Map<Integer, FloatRangeSet> intersectedRegionRanges = regionBorderBufferList.getIntersectedRegionRanges();
         List<RegionBorderDatagram> regionBorderDatagrams = ping.getPingItems(RegionBorderDatagram.class).toList();
         if (!regionBorderDatagrams.isEmpty()) {
            updateGridCells(ping, intersectedRegionRanges);
         }

         PerPingAPriori perPingAPriori = null;
         for (RegionInfoDatagram regionInfoDatagram : ping.getPingItems(RegionInfoDatagram.class).toList()) {
            GraphicalInfoSubDatagram graphicalInfoSubDatagram = new GraphicalInfoSubDatagram(ping.getNTDate());
            PowerData referenceDatagram = ping.getPowerData(configurator.getReferenceChannel());
            if (perPingAPriori == null) {
               perPingAPriori = new PerPingAPriori(configurator, ping);
            }
            Pixel result = analyzeSchool(regionInfoDatagram, graphicalInfoSubDatagram, referenceDatagram, perPingAPriori);

            if (!graphicalInfoSubDatagram.getGraphicalObjects().isEmpty()) {
               ping.add(graphicalInfoSubDatagram);
               graphicalInfoNTDates.add(ping.getNTDate());
            }
            if (result != null) {
               //school is single species, and has been categorized as one.
               List<CategoryData> categoryDatas = result.getAcceptableCategoryDatas();
               Cas0Datagram cas0 = new Cas0Datagram(ping.getNTDate(), categoryDatas.size(),
                     regionInfoDatagram.getRegionId());
               categoryDatas.sort(null);
               int priority = 0;
               for (CategoryData categoryData : categoryDatas) {
                  cas0.setCategory(priority, categoryData.getCategory().getNumber(), categoryData.getDiscriminant(), categoryData.getBoundedProbability());
                  priority++;
               }
               ping.add(cas0);
            }
         }
         counter++;
         if (ping.getPingItem(RegionTableOfContentsDatagram.class) != null) {
            ping.add(new GraphicalInfoTocSubDatagram(ping.getNTDate(), Utils.toLongs(graphicalInfoNTDates)));
         }
      }

      private static final class RegionBorderBuffer {
         private int index;
         private final Map<Integer, FloatRangeSet> borderRanges = new HashMap<>();

         private RegionBorderBuffer(int index, Collection<RegionBorderDatagram.BorderInfo> borderInfos) {
            this.index = index;
            for (RegionBorderDatagram.BorderInfo borderInfo : borderInfos) {
               int id = borderInfo.id();
               borderRanges.merge(id, FloatRangeSet.of(FloatRange.of(borderInfo.startDepth(), borderInfo.endDepth())), FloatRangeSet::add);
            }
         }

         private Map<Integer, FloatRangeSet> getBorderRanges() {
            return borderRanges;
         }

         private int getIndex() {
            return index;
         }

         private void setIndex(int index) {
            this.index = index;
         }
      }

      private static final class RegionBorderBufferList {
         private final Deque<RegionBorderBuffer> regionBorderBufferDeque = new ArrayDeque<>();
         private @Nullable RegionBorderBuffer centerBorderBuffer;

         private RegionBorderBufferList() {
         }

         private void shiftIndex() {
            for (RegionBorderBuffer regionBorderBuffer : regionBorderBufferDeque) {
               int newIndex = regionBorderBuffer.getIndex() - 1;
               if (newIndex == 0) {
                  centerBorderBuffer = regionBorderBuffer;
               }
               regionBorderBuffer.setIndex(newIndex);
            }
         }

         private @Nullable RegionBorderBuffer getCenterBuffer() {
            return centerBorderBuffer;
         }

         private int getLastAddedIndex() {
            if (regionBorderBufferDeque.isEmpty()) {
               return -1;
            }
            return regionBorderBufferDeque.peekLast().getIndex();
         }

         private void add(Ping ping, int index) {
            List<RegionBorderDatagram.BorderInfo> borderInfos = ping.getPingItems(RegionBorderDatagram.class)
                  .flatMap(regionBorderDatagram -> regionBorderDatagram.getBorderInfos().stream())
                  .toList();
            RegionBorderBuffer borderBuffer = new RegionBorderBuffer(index, borderInfos);
            if (index == 0) {
               centerBorderBuffer = borderBuffer;
            }
            regionBorderBufferDeque.addLast(borderBuffer);
         }

         private void removeOld(int minimumIndex) {
            int index = regionBorderBufferDeque.peekFirst().getIndex();
            while (index < minimumIndex) {
               regionBorderBufferDeque.pollFirst();
               index++;
            }
         }

         private Map<Integer, FloatRangeSet> getIntersectedRegionRanges() {
            Map<Integer, FloatRangeSet> result = new TreeMap<>(centerBorderBuffer.getBorderRanges());

            for (RegionBorderBuffer regionBorderBuffer : regionBorderBufferDeque) {
               if (regionBorderBuffer.getIndex() == 0) {
                  continue; // already done
               }
               // Intersect the region range with all region ranges in the buffer, regardless of id.
               // The remainder is guaranteed be at least the half-width of the buffer away from any school boundary.
               for (Map.Entry<Integer, FloatRangeSet> entry : result.entrySet()) {
                  Map<Integer, FloatRangeSet> otherBorderRanges = regionBorderBuffer.getBorderRanges();
                  if (otherBorderRanges.isEmpty()) {
                     entry.setValue(FloatRangeSet.of()); //set to an empty float range set
                  } else {
                     FloatRangeSet centerRanges = entry.getValue();
                     FloatRangeSet otherRanges = FloatRangeSet.of();
                     for (FloatRangeSet floatRanges : otherBorderRanges.values()) {
                        otherRanges = otherRanges.add(floatRanges);
                     }
                     centerRanges = centerRanges.intersection(otherRanges);
                     entry.setValue(centerRanges);
                  }
               }
            }
            return result;
         }

         private void clear() {
            centerBorderBuffer = null;
            regionBorderBufferDeque.clear();
         }
      }

      private record GridCellKey(int horizontalIndex, int verticalIndex, int borderId) {
         @Override
         public String toString() {
            return "[ " + horizontalIndex + ", " + verticalIndex + ", " + borderId + " ]";
         }
      }

      private record SimplifiedGridCellKey(int horizontalIndex, int verticalIndex) {
         private SimplifiedGridCellKey(GridCellKey gridCellKey) {
            this(gridCellKey.horizontalIndex, gridCellKey.verticalIndex);
         }

         @Override
         public String toString() {
            return "[ " + horizontalIndex + ", " + verticalIndex + " ]";
         }
      }

      private static final class GridCellValue {
         private final Map<Integer, CellAveragedSv> gridAverages = new HashMap<>();

         private GridCellValue() {
         }

         private void putGridAverageValue(int channel, CellAveragedSv cellAveragedSv) {
            gridAverages.put(channel, cellAveragedSv);
         }

         private @Nullable CellAveragedSv getGridAverageValue(int channel) {
            return gridAverages.get(channel);
         }

         private void mergeWith(GridCellValue otherGridCellValue) {
            for (Map.Entry<Integer, CellAveragedSv> entry : otherGridCellValue.gridAverages.entrySet()) {
               if (gridAverages.containsKey(entry.getKey())) {
                  gridAverages.get(entry.getKey()).mergeWith(entry.getValue());
               } else {
                  gridAverages.put(entry.getKey(), entry.getValue());
               }
            }
         }
      }
   }
}
