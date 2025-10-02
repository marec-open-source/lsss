package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.Curve;
import no.imr.korona.region.Layer;
import no.imr.korona.region.RegionManager;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.korona.region.KoronaRegionLSSS;
import no.imr.lsss.modules.korona.region.KoronaRegionModule;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.DoubleParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.swing.WorkerDialog;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.stream.LongStream;

final class AutomaticRegionDefinition {
   private final DoubleParameter horizontalLayerSize = new DoubleParameter(
         new Name("HorizontalLayerSize", "Horizontal layer size"),
         5, Unit.NAUTICAL_MILES, ValueConstraints.gt(0.0));

   private final FloatParameter verticalLayerSize = new FloatParameter(
         new Name("VerticalLayerSize", "Vertical layer size"),
         50, Unit.METER, ValueConstraints.gt(0f));

   private final FloatParameter bottomLayerThickness = new FloatParameter(
         new Name("BottomLayerThickness", "Bottom layer thickness"),
         5, Unit.METER, ValueConstraints.gt(0f));

   private final BooleanParameter koronaSchools = new BooleanParameter(
         new Name("KoronaSchools", "Use schools detected by KORONA"),
         true);

   private final LSSS lsss;

   AutomaticRegionDefinition(LSSS lsss) {
      this.lsss = lsss;
   }

   void run(@Nullable Component referenceComponent) {
      List<? extends ValueParameter<? extends Serializable>> parameters = List.of(
            horizontalLayerSize,
            verticalLayerSize,
            bottomLayerThickness,
            koronaSchools
      );
      ParameterEditor parameterEditor = new ParameterEditor(parameters);
      boolean ok = new ConfigurableGUIDialog(referenceComponent, "Automatic region definition", new ParameterCollection(parameters))
            .setGUI(parameterEditor.getEditorComponent())
            .show();
      if (!ok) {
         return;
      }
      if (!lsss.getActions().resetInterpretation()) {
         return;
      }
      new WorkerDialog(referenceComponent, "Defining regions...")
            .start(this::run);
   }

   void run(AsyncHandle asyncHandle) {
      RegionManager regionManager = lsss.getRegionManager();

      List<PingRange> subPingRanges = findSubPingRanges();

      for (int i = 1; i < subPingRanges.size(); i++) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         regionManager.addVerticalDivider(subPingRanges.get(i).begin());
      }

      for (PingRange pingRange : subPingRanges) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         initPingRange(pingRange);
      }

      if (koronaSchools.getBooleanValue()) {
         defineSchools(asyncHandle);
      }
   }

   private List<PingRange> findSubPingRanges() {
      DataFileSet dataFileSet = lsss.getInterpretationSettings().getDataFileSet();
      PingRange pingRange = lsss.getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return List.of();
      }
      double deltaDistance = horizontalLayerSize.getDoubleValue();
      PingMapping pingMapping = PingMapping.DISTANCE;
      long iMin = (long) Math.ceil(pingMapping.valueOf(pingRange.begin()) / deltaDistance);
      long iMax = (long) Math.floor(pingMapping.valueOf(pingRange.end()) / deltaDistance);
      List<PingIndex> internalPingIndices = LongStream.rangeClosed(iMin, iMax)
            .mapToObj(i -> dataFileSet.getClampedContainingPingIndex(i * deltaDistance, pingMapping))
            .toList();
      if (internalPingIndices.isEmpty()) {
         return List.of(pingRange);
      }
      List<PingRange> subPingRanges = new ArrayList<>();
      PingIndex first = internalPingIndices.getFirst();
      if (first != pingRange.begin()) {
         subPingRanges.add(PingRange.of(pingRange.begin(), first));
      }
      for (int i = 1; i < internalPingIndices.size(); i++) {
         PingIndex a = internalPingIndices.get(i - 1);
         PingIndex b = internalPingIndices.get(i);
         subPingRanges.add(PingRange.of(a, b));
      }
      PingIndex last = internalPingIndices.getLast();
      if (last != pingRange.end()) {
         subPingRanges.add(PingRange.of(last, pingRange.end()));
      }
      return subPingRanges;
   }

   private void initPingRange(PingRange pingRange) {
      DataFileSet dataFileSet = lsss.getInterpretationSettings().getDataFileSet();
      RegionManager regionManager = lsss.getRegionManager();

      List<Layer> layers = regionManager.getLayerManager().getLayersIntersectingPingRange(pingRange);
      Layer layer = layers.getFirst();
      Curve lowerBoundaryCurve = layer.getLowerCurveBoundaries().getFirst().getCurve();
      float[] lowerDepths = lowerBoundaryCurve.getDepths();
      int maxDepthIndex = ArrayMath.maxIndex(lowerDepths);
      float maxDepth = lowerDepths[maxDepthIndex];
      PingIndex maxDepthPingIndex = dataFileSet.getPingIndex(pingRange.begin().getPingNumber() + maxDepthIndex);
      float minDepth = lsss.getConfigurationManager().getSurveyMiscConf().topBoundaryOffset.getFloatValue();
      float bottomThickness = bottomLayerThickness.getFloatValue();

      if (maxDepth - minDepth <= bottomThickness) {
         return;
      }
      regionManager.getLayerManager().addCurveBoundary(maxDepthPingIndex, pingIndex -> {
         return lowerBoundaryCurve.getClampedDepth(pingIndex) - bottomThickness;
      });
      maxDepth -= bottomThickness;

      float deltaDepth = verticalLayerSize.getFloatValue();
      int iMin = (int) Math.ceil(minDepth / deltaDepth);
      int iMax = (int) Math.floor(maxDepth / deltaDepth);
      for (int i = iMin; i <= iMax; i++) {
         float depth = i * deltaDepth;
         regionManager.getLayerManager().addCurveBoundary(maxDepthPingIndex, __ -> depth);
      }
   }

   private void defineSchools(AsyncHandle asyncHandle) {
      RegionManager regionManager = lsss.getRegionManager();
      PingRange pingRange = lsss.getInterpretationSettings().getPingRange();
      KoronaRegionModule koronaRegionModule = lsss.getModuleManager().getModule(KoronaRegionModule.class);

      for (KoronaRegionLSSS koronaRegion : koronaRegionModule.getKoronaRegions()) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         if (!koronaRegion.getPingRange().intersects(pingRange)) {
            continue;
         }
         NavigableMap<PingIndex, FloatRangeSet> mask = koronaRegion.getMask().subMap(pingRange.begin(), true, pingRange.end(), false);
         regionManager.addSchool(mask);
      }
   }
}
