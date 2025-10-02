package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.Curve;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.ValueOrError;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.RangeSet;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class LayerManager extends BaseRegionManager<Layer> {
   private final Set<Layer> layers = ConcurrentHashMap.newKeySet();

   public LayerManager(RegionManager regionManager) {
      super(regionManager);
   }

   @Override
   Set<Layer> getRegions() {
      return layers;
   }

   public Set<Layer> getLayers() {
      return layers;
   }

   @Nullable Layer getLayer(EchogramPoint echogramPoint) {
      return getRegion(echogramPoint);
   }

   public Stream<Layer> layersIntersectingPingRange(PingRange pingRange) {
      return layers.stream()
            .filter(layer -> layer.getPingRange().intersects(pingRange));
   }

   public List<Layer> getLayersIntersectingPingRange(PingRange pingRange) {
      List<Layer> result = new ArrayList<>();
      for (Layer layer : layers) {
         if (layer.getPingRange().intersects(pingRange)) {
            result.add(layer);
         }
      }
      return result;
   }

   private List<LayerConnector> getConnectorsAtPingIndex(PingIndex pingIndex) {
      Set<LayerConnector> connectors = new HashSet<>();
      for (Layer layer : layers) {
         if (layer.getPingRange().containsIncludingEnd(pingIndex)) {
            for (LayerConnector layerConnector : layer.getConnectors()) {
               if (layerConnector.getPingIndex().equals(pingIndex)) {
                  connectors.add(layerConnector);
               }
            }
         }
      }
      return new ArrayList<>(connectors);
   }

   private List<Layer> getLayers(PingIndex pingIndex) {
      return layers.stream()
            .filter(layer -> layer.getPingRange().contains(pingIndex))
            .toList();
   }

   public List<String> mergeSelectedLayers() {
      return mergeLayers(selectedRegions);
   }

   public List<String> mergeLayers(Collection<Layer> layersToMerge) {
      if (layersToMerge.size() < 2) {
         return List.of("Select at least two layers");
      }
      Set<String> possibleErrors = new TreeSet<>();
      Set<Layer> splitLayers = new HashSet<>();
      List<Layer> layerList = new ArrayList<>(layersToMerge); // copy to ensure no changes
      for (Layer layer : layerList) {
         if (layer.isReadOnly()) {
            possibleErrors.add("Cannot merge read-only layers");
            continue;
         }
         List<Layer> leftSplitLayers = new ArrayList<>();
         if (layer.getPingRange().containsExcludingBegin(getVisiblePingRange().begin())) {
            leftSplitLayers.addAll(splitLayerAtPingIndex(getVisiblePingRange().begin(), layer));
         } else {
            leftSplitLayers.add(layer);
         }
         for (Layer layer1 : leftSplitLayers) {
            if (layer1.getPingRange().intersects(getVisiblePingRange())) {
               splitLayers.add(layer1);
            }
         }
         List<Layer> rightSplitLayers = new ArrayList<>();
         for (Layer layer1 : splitLayers) {
            if (layer1.getPingRange().contains(getVisiblePingRange().end())) {
               List<Layer> after = splitLayerAtPingIndex(getVisiblePingRange().end(), layer1);
               for (Layer layer2 : after) {
                  if (!rightSplitLayers.contains(layer2)) {
                     rightSplitLayers.add(layer2);
                  }
               }
            }
         }
         for (Layer layer1 : rightSplitLayers) {
            if (layer1.getPingRange().intersects(getVisiblePingRange())) {
               splitLayers.add(layer1);
            }
         }
      }
      List<Layer> splitLayersList = new ArrayList<>(splitLayers);
      for (int i = 0; i < splitLayersList.size(); i++) {
         innerLoop:
         for (int j = i + 1; j < splitLayersList.size(); j++) {
            if (layers.contains(splitLayersList.get(i)) && layers.contains(splitLayersList.get(j))) {
               ValueOrError<Layer> mergedLayer = mergeLayers(splitLayersList.get(i), splitLayersList.get(j));
               switch (mergedLayer) {
                  case ValueOrError.Error<Layer> e -> {
                     possibleErrors.add(e.error());
                  }
                  case ValueOrError.Value<Layer> v -> {
                     if (splitLayersList.get(i).equals(v.value())) {
                        i--;
                        break innerLoop;
                     }
                  }
               }
            }
         }
      }
      getRegionManager().notifyRegionListenersSelectedRegions();
      return layersToMerge.size() == 1 ? List.of() : List.copyOf(possibleErrors);
   }

   public ValueOrError<Layer> mergeLayers(CurveBoundary curveBoundary) {
      Layer layerAbove = curveBoundary.getLayerAbove();
      if (layerAbove == null) {
         return ValueOrError.error("Cannot merge: No layer above");
      }
      Layer layerBelow = curveBoundary.getLayerBelow();
      if (layerBelow == null) {
         return ValueOrError.error("Cannot merge: No layer below");
      }
      return mergeLayers(layerAbove, layerBelow);
   }

   public ValueOrError<Layer> mergeLayers(Layer layerA, Layer layerB) {
      //find common boundaries, and remove them from both layers
      boolean couldMerge = false;
      List<LayerBoundary> toBeRemovedBoundaries = new ArrayList<>();
      for (CurveBoundary curveBoundary : layerA.getCurveBoundaries()) {
         if (curveBoundary.getLayers().contains(layerB)) {
            toBeRemovedBoundaries.add(curveBoundary);
            couldMerge = true;
         }
      }
      for (VerticalBoundary verticalBoundary : layerA.getVerticalBoundaries()) {
         if (verticalBoundary.getLayers().contains(layerB)) {
            toBeRemovedBoundaries.add(verticalBoundary);
            couldMerge = true;
         }
      }

      //check if the resulting layer remains simple.
      for (CurveBoundary curveBoundary : layerA.getUpperCurveBoundaries()) {
         if (toBeRemovedBoundaries.contains(curveBoundary)) {
            continue;
         }
         for (CurveBoundary boundary : layerB.getUpperCurveBoundaries()) {
            if (toBeRemovedBoundaries.contains(boundary)) {
               continue;
            }
            if (curveBoundary.getPingRange().intersects(boundary.getPingRange())) {
               return ValueOrError.error("Cannot merge layers since the resulting layer would have unconnected depth ranges");
            }
         }
      }

      if (!couldMerge) {
         return ValueOrError.error("Cannot merge layers since they do not share boundaries");
      }

      boolean visitedStatus = layerA.isSelectedAtLeastOnce() && layerB.isSelectedAtLeastOnce() && layerA.hasEqualInterpretationTo(layerB);

      layers.remove(layerB);
      selectedRegions.remove(layerB);
      layers.remove(layerA);
      selectedRegions.remove(layerA);

      for (LayerBoundary boundary : toBeRemovedBoundaries) {
         boundary.detach();
      }

      for (CurveBoundary curveBoundary : layerB.getCurveBoundaries()) {
         if (layerB.isUpperBoundary(curveBoundary)) {
            layerA.addUpperBoundary(curveBoundary);
         } else {
            layerA.addLowerBoundary(curveBoundary);
         }
         curveBoundary.removeLayer(layerB);
      }
      for (VerticalBoundary verticalBoundary : layerB.getVerticalBoundaries()) {
         layerA.addVerticalBoundary(verticalBoundary);
         verticalBoundary.removeLayer(layerB);
      }
      removeRedundantVerticalBoundaries(layerA);
      removeRedundantConnectors(layerA);
      //Add layer B's labels to layer A
      layerA.addLabels(layerB.getLabels());
      layerA.setSelectedAtLeastOnce(visitedStatus);
      layers.add(layerA);
      verifyAndAdjustConnectors(layerA);
      if (layerA.isSelected()) {
         selectedRegions.add(layerA);
      }
      getRegionManager().notifyRegionListenersRegionDeleted(layerB);
      getRegionManager().notifyRegionBoundaryChanged(getVisiblePingRange(), layerA);
      return ValueOrError.of(layerA);
   }

   private static void removeRedundantConnectors(Layer layer) {
      for (LayerConnector connector : layer.getConnectors()) {
         if (connector.getVerticalBoundaries().isEmpty()) {
            CurveBoundary left = connector.getBoundaryLeft();
            CurveBoundary right = connector.getBoundaryRight();
            if (left != null && right != null) {
               left.mergeWith(right);
               right.detach();
            }
            connector.detach();
         } else if (connector.getCurveBoundaries().isEmpty()) {
            VerticalBoundary above = connector.getBoundaryAbove();
            VerticalBoundary below = connector.getBoundaryBelow();
            if (above != null && below != null) {
               above.mergeWith(below);
               below.detach();
            }
            connector.detach();
         }
      }
   }

   private static void removeRedundantVerticalBoundaries(Layer layer) {
      List<LayerConnector> toBeRemovedConnectors = new ArrayList<>();
      List<VerticalBoundary> toBeRemovedBoundaries = new ArrayList<>();
      //check if any vertical boundaries can be removed
      for (VerticalBoundary verticalBoundary : layer.getVerticalBoundaries()) {
         boolean edge = layer.getPingRange().isBeginOrEnd(verticalBoundary.getPingIndex());
         if (edge) {
            continue;
         }
         List<CurveBoundary> curveBoundariesStart = verticalBoundary.getStartConnector().getCurveBoundaries();
         List<CurveBoundary> curveBoundariesEnd = verticalBoundary.getEndConnector().getCurveBoundaries();
         if (curveBoundariesStart.size() == 1 && curveBoundariesEnd.size() == 1) {
            CurveBoundary cb1 = curveBoundariesStart.getFirst();
            CurveBoundary cb2 = curveBoundariesEnd.getFirst();
            if (!cb1.getPingRange().intersects(cb2.getPingRange())) {
               LayerConnector cb1Connector = verticalBoundary.getStartConnector();
               LayerConnector cb2Connector = verticalBoundary.getEndConnector();
               // can remove a connector if it is connected to only two boundaries
               LayerConnector canRemoveConnector = Stream.of(cb1Connector, cb2Connector)
                     .filter(connector -> connector.getBoundaryCount() == 2)
                     .findFirst()
                     .orElse(null);
               if (canRemoveConnector == null) {
                  continue;
               }
               if (cb1Connector.equals(cb1.getEndConnector())) {
                  if (canRemoveConnector.equals(cb1Connector)) {
                     cb1.setEndConnector(cb2Connector);
                     cb2Connector.addBoundary(cb1);
                  } else {
                     cb2.setStartConnector(cb1Connector);
                     cb1Connector.addBoundary(cb2);
                  }
               } else {
                  if (canRemoveConnector.equals(cb1Connector)) {
                     cb1.setStartConnector(cb2Connector);
                     cb2Connector.addBoundary(cb1);
                  } else {
                     cb2.setEndConnector(cb1Connector);
                     cb1Connector.addBoundary(cb2);
                  }
               }
               toBeRemovedConnectors.add(canRemoveConnector);
               toBeRemovedBoundaries.add(verticalBoundary);
            }
         }
      }
      toBeRemovedConnectors.forEach(LayerConnector::detach);
      toBeRemovedBoundaries.forEach(LayerBoundary::detach);
   }

   private List<Layer> splitLayerAtPingIndex(PingIndex pingIndex, Layer layer) {
      LayerAndBoundaryPair<VerticalBoundary> pair = addVerticalBoundary(layer, pingIndex);
      if (pair != null) {
         return List.of(layer, pair.layer());
      }
      return List.of(layer);
   }

   private void clear() {
      getRegionManager().notifyRegionListenersRegionsDeleted(layers);
      layers.clear();
   }

   void setupInitialLayerBoundaries(ToFloatFunction<PingIndex> upperDepth, ToFloatFunction<PingIndex> lowerDepth) {
      PingRange totalPingRange = getPingContainer().getTotalRange();

      clear();

      if (totalPingRange.isEmpty()) {
         return;
      }

      Curve upperCurve = new Curve(totalPingRange);
      Curve lowerCurve = new Curve(totalPingRange);

      for (PingIndex pingIndex : getPingContainer().getPingIndices(totalPingRange)) {
         float minDepth = upperDepth.applyAsFloat(pingIndex);
         float maxDepth = Math.max(minDepth, lowerDepth.applyAsFloat(pingIndex));
         upperCurve.adjust(pingIndex, minDepth);
         lowerCurve.adjust(pingIndex, maxDepth);
      }

      LayerConnector upperLeftConnector = new LayerConnector(upperCurve.getStartPoint());
      LayerConnector lowerLeftConnector = new LayerConnector(lowerCurve.getStartPoint());
      LayerConnector upperRightConnector = new LayerConnector(upperCurve.getEndPoint());
      LayerConnector lowerRightConnector = new LayerConnector(lowerCurve.getEndPoint());

      VerticalBoundary leftVB = new VerticalBoundary(upperLeftConnector, lowerLeftConnector);
      VerticalBoundary rightVB = new VerticalBoundary(upperRightConnector, lowerRightConnector);

      CurveBoundary upperBoundary = new CurveBoundary(upperLeftConnector, upperRightConnector, upperCurve);
      CurveBoundary lowerBoundary = new CurveBoundary(lowerLeftConnector, lowerRightConnector, lowerCurve);

      Layer newLayer = new Layer(getRegionManager());
      newLayer.addUpperBoundary(upperBoundary);
      newLayer.addLowerBoundary(lowerBoundary);
      newLayer.addVerticalBoundary(leftVB);
      newLayer.addVerticalBoundary(rightVB);
      layers.add(newLayer);
   }

   public Collection<CurveBoundary> getIntersectingCurveBoundaries(PingRange pingRange) {
      Set<CurveBoundary> intersectingLayerBoundaries = new HashSet<>();
      for (Layer layer : layers) {
         layer.addIntersectingCurveBoundaries(pingRange, intersectingLayerBoundaries);
      }
      return intersectingLayerBoundaries;
   }

   public Collection<VerticalBoundary> getIntersectingVerticalBoundaries(PingRange pingRange) {
      Set<VerticalBoundary> intersectingLayerBoundaries = new HashSet<>();
      for (Layer layer : layers) {
         layer.addIntersectingVerticalBoundaries(pingRange, intersectingLayerBoundaries);
      }
      return intersectingLayerBoundaries;
   }

   private enum Side {LEFT, RIGHT}

   private enum Level {UPPER, LOWER}

   private CurveBoundary getOuterBoundary(PingIndex pingIndex, Level level) {
      return level == Level.UPPER ? getUpperBoundary(pingIndex) : getBottomBoundary(pingIndex);
   }

   private @Nullable VerticalBoundary insertVerticalIntoOuterBoundary(EchogramPoint echogramPoint, Side side, Level level) {
      //get the upper boundary on the 'inside' of echogramPoint
      CurveBoundary firstOuterBoundary = side == Side.LEFT
            ? getOuterBoundary(echogramPoint.pingIndex(), level)
            : getOuterBoundary(getPingContainer().previousOrSame(echogramPoint.pingIndex()), level);
      Layer layer = level == Level.UPPER
            ? firstOuterBoundary.getLayerBelow()
            : firstOuterBoundary.getLayerAbove();
      if (layer == null) {
         return null;
      }
      LayerConnector innerConnector = new LayerConnector(new EchogramPoint(echogramPoint.pingIndex(), firstOuterBoundary.getCurve().getClampedDepth(echogramPoint.pingIndex())));
      LayerBoundaryAndConnectorPair<CurveBoundary> endPair = firstOuterBoundary.split(echogramPoint);
      CurveBoundary splitRemainder = endPair.endBoundary();
      LayerConnector outerConnector = endPair.connector();
      boolean noSplit = splitRemainder == firstOuterBoundary || splitRemainder == null;
      if (noSplit) {
         outerConnector.removeBoundary(firstOuterBoundary);
         List<VerticalBoundary> removeBoundaries = new ArrayList<>();
         for (VerticalBoundary layerBoundary : outerConnector.getVerticalBoundaries()) {
            if (level == Level.UPPER) {
               layerBoundary.setStartConnector(innerConnector);
            } else {
               layerBoundary.setEndConnector(innerConnector);
            }
            removeBoundaries.add(layerBoundary);
            innerConnector.addBoundary(layerBoundary);
         }
         for (VerticalBoundary removeBoundary : removeBoundaries) {
            outerConnector.removeBoundary(removeBoundary);
         }
         if (side == Side.LEFT) {
            firstOuterBoundary.setStartConnector(innerConnector);
         } else {
            firstOuterBoundary.setEndConnector(innerConnector);
         }
         innerConnector.addBoundary(firstOuterBoundary);
      } else {
         if (side == Side.LEFT) {
            outerConnector.removeBoundary(splitRemainder);
            splitRemainder.setStartConnector(innerConnector);
            innerConnector.addBoundary(splitRemainder);
         } else {
            outerConnector.removeBoundary(firstOuterBoundary);
            firstOuterBoundary.setEndConnector(innerConnector);
            innerConnector.addBoundary(firstOuterBoundary);
         }
      }
      VerticalBoundary vb;
      if (level == Level.UPPER) {
         outerConnector.setDepth(Math.min(echogramPoint.depth(), innerConnector.getDepth()));
         vb = new VerticalBoundary(outerConnector, innerConnector);
      } else {
         outerConnector.setDepth(Math.max(echogramPoint.depth(), innerConnector.getDepth()));
         vb = new VerticalBoundary(innerConnector, outerConnector);
      }
      if (noSplit) {
         for (Layer layer1 : outerConnector.getLayers()) {
            if (!layer1.getCurveBoundaries().contains(firstOuterBoundary)) {
               layer1.addVerticalBoundary(vb);
            }
         }
      } else {
         layer.addVerticalBoundary(vb);
      }
      return vb;
   }

   private @Nullable LayerAndBoundaryPair<CurveBoundary> addCurveBoundaryOutsideInterior(PingIndex pingIndex, ToFloatFunction<PingIndex> depthFunction) {
      PingRange pingRange = getRegionManager().writeablePingRanges(getVisiblePingRange()).stream()
            .filter(range -> range.contains(pingIndex))
            .findFirst()
            .map(PingRange::of)
            .orElse(null);
      if (pingRange == null) {
         return null;
      }

      Level level = depthFunction.applyAsFloat(pingIndex) < getBoundaryDepthRange(pingIndex).min() ? Level.UPPER : Level.LOWER;

      Curve curve = new Curve(pingRange);
      curve.adjust(depthFunction, getPingContainer());

      if (level == Level.UPPER) {
         for (CurveBoundary upperBoundary : getUpperBoundaries(pingRange)) {
            curve.adjust(upperBoundary.getCurve(), Math::min);
         }
      } else {
         for (CurveBoundary bottomBoundary : getBottomBoundaries(pingRange)) {
            curve.adjust(bottomBoundary.getCurve(), Math::max);
         }
      }

      EchogramPoint startSplitPoint = curve.getStartPoint();
      VerticalBoundary leftVerticalBoundary = insertVerticalIntoOuterBoundary(startSplitPoint, Side.LEFT, level);
      if (leftVerticalBoundary == null) {
         return null;
      }

      EchogramPoint endSplitPoint = curve.getEndPoint();
      VerticalBoundary rightVerticalBoundary = insertVerticalIntoOuterBoundary(endSplitPoint, Side.RIGHT, level);
      if (rightVerticalBoundary == null) {
         return null;
      }

      CurveBoundary curveBoundary;
      if (level == Level.UPPER) {
         curveBoundary = new CurveBoundary(leftVerticalBoundary.getStartConnector(), rightVerticalBoundary.getStartConnector(), curve);
      } else {
         curveBoundary = new CurveBoundary(leftVerticalBoundary.getEndConnector(), rightVerticalBoundary.getEndConnector(), curve);
      }

      Layer newLayer = new Layer(getRegionManager());
      if (level == Level.UPPER) {
         newLayer.addUpperBoundary(curveBoundary);
      } else {
         newLayer.addLowerBoundary(curveBoundary);
      }

      List<CurveBoundary> outerBoundaries = level == Level.UPPER ? getUpperBoundaries(pingRange) : getBottomBoundaries(pingRange);
      for (CurveBoundary boundary : outerBoundaries) {
         if (level == Level.UPPER) {
            newLayer.addLowerBoundary(boundary);
         } else {
            newLayer.addUpperBoundary(boundary);
         }
      }
      newLayer.addVerticalBoundary(leftVerticalBoundary);
      newLayer.addVerticalBoundary(rightVerticalBoundary);
      layers.add(newLayer);
      ensureAllConnectedVerticalsAreConsistent(leftVerticalBoundary.getStartConnector());
      ensureAllConnectedVerticalsAreConsistent(rightVerticalBoundary.getStartConnector());
      getRegionManager().notifyRegionBoundaryChanged(pingRange, newLayer);
      return new LayerAndBoundaryPair<>(newLayer, curveBoundary);
   }

   public @Nullable LayerAndBoundaryPair<CurveBoundary> addCurveBoundary(EchogramPoint point, DepthTransform depthTransform) {
      return addCurveBoundary(point.pingIndex(), depthTransform.constantZToDepthFunction(point));
   }

   public @Nullable LayerAndBoundaryPair<CurveBoundary> addCurveBoundary(PingIndex pingIndex, ToFloatFunction<PingIndex> depthFunction) {
      if (getRegionManager().isReadOnly(pingIndex)) {
         throw new IllegalEditException();
      }
      EchogramPoint point = new EchogramPoint(pingIndex, depthFunction.applyAsFloat(pingIndex));
      //1a. Go through all visible vertical boundaries, and find the ones closest to the right and left of aPingNumber.
      //      -there may be no vertical boundary at (at least) one of the directions,
      //       if so create one at the edge of the screen.
      Layer splitLayer = getLayer(point);
      if (splitLayer == null) {
         return addCurveBoundaryOutsideInterior(pingIndex, depthFunction);
      }
      if (splitLayer.pointAtCurveBoundary(point)) {
         return null;
      }
      PingRange pingRange = getVisiblePingRange();
      pingRange = pingRange.intersection(splitLayer.getPingRange());
      //ensure that there are vertical boundaries at the end points.
      addVerticalBoundary(splitLayer, pingRange.begin());
      splitLayer = getLayer(point);
      assert splitLayer != null;
      addVerticalBoundary(splitLayer, pingRange.end());
      splitLayer = getLayer(point);
      assert splitLayer != null;

      pingRange = pingRange.intersection(splitLayer.getPingRange());
      //1b. Create a new curve. Adjust the new curve to the other curves in the layer.
      Curve newCurve = splitLayer.createNonIntersectingCurve(pingRange, depthFunction);

      List<@Nullable VerticalBoundary> leftRightBoundary = splitLayer.findLeftAndRightBoundary(newCurve, point);
      VerticalBoundary leftVB;
      VerticalBoundary rightVB;
      leftVB = leftRightBoundary.get(0);
      rightVB = leftRightBoundary.get(1);
      if (leftVB == null) {
         Log.global.warning("left vertical boundary is null");
      }
      if (rightVB == null) {
         Log.global.warning("right vertical boundary is null");
      }
      assert leftVB != null && rightVB != null;

      PingRange rangeBetweenVerticals = PingRange.ofUnsorted(leftVB.getPingIndex(), rightVB.getPingIndex());

      //if the range between the vertical boundaries is smaller than the range of the curve, the curve must be cut.
      if (!rangeBetweenVerticals.equals(newCurve.getPingRange())) {
         newCurve = newCurve.intersect(rangeBetweenVerticals);
      }

      //3. Split the layer
      boolean splitLayerSelected = splitLayer.isSelected();
      if (splitLayerSelected) {
         deselectRegion(splitLayer);
      }
      float endDepth = rightVB.getDepthRange().clamp(newCurve.getLastDepth());
      float startDepth = leftVB.getDepthRange().clamp(newCurve.getStartDepth());

      Curve finalNewCurve = newCurve;
      LayerAndBoundaryPair<CurveBoundary> newPair = splitLayer.split(leftVB, rightVB,
            (startConnector, endConnector) -> new CurveBoundary(startConnector, endConnector, finalNewCurve),
            new EchogramPoint(leftVB.getPingIndex(), startDepth),
            new EchogramPoint(rightVB.getPingIndex(), endDepth));

      //4. Go through new layer and add new layer boundaries to global list
      layers.add(newPair.layer());

      List<Layer> bothLayers = List.of(splitLayer, newPair.layer());
      if (splitLayerSelected) {
         selectRegions(bothLayers);
      }
      getRegionManager().notifyRegionBoundaryChanged(pingRange, bothLayers);
      return newPair;
   }

   /**
    * Add a vertical boundary at a given position.
    * The vertical boundary extent should be from the nearest curve boundary above
    * and below.
    *
    * @param point the position where to insert a vertical boundary
    */
   public @Nullable LayerAndBoundaryPair<VerticalBoundary> addVerticalBoundary(EchogramPoint point) {
      if (getRegionManager().isReadOnly(point.pingIndex())) {
         throw new IllegalEditException();
      }
      Layer layer = getLayer(point);
      if (layer == null) {
         return null;
      }
      return addVerticalBoundary(layer, point.pingIndex());
   }

   private @Nullable LayerAndBoundaryPair<VerticalBoundary> addVerticalBoundary(Layer layer, PingIndex pingIndex) {
      if (!layer.canInsertVerticalBoundary(pingIndex)) {
         return null;
      }
      //1. Go through all curve boundaries, and find the ones closest above and below aSampleNumber.
      PingRange pingRange = getVisiblePingRange();
      CurveBoundary upperBoundary = layer.findUpperBoundary(pingIndex);
      CurveBoundary lowerBoundary = layer.findLowerBoundary(pingIndex);
      if (upperBoundary == null || lowerBoundary == null) {
         Log.global.warning("No upper/lower boundaries for " + pingIndex);
         return null;
      }

      //2. Create a new vertical boundary.
      boolean upperIsConnector = !upperBoundary.getPingRange().containsExcludingBegin(pingIndex);
      boolean lowerIsConnector = !lowerBoundary.getPingRange().containsExcludingBegin(pingIndex);

      float startDepth = !upperIsConnector ?
            upperBoundary.getCurve().getDepth(pingIndex) :
            (upperBoundary.getPingRange().begin().equals(pingIndex) ?
                  upperBoundary.getStartConnector().getDepth() : upperBoundary.getEndConnector().getDepth());
      float endDepth = !lowerIsConnector ?
            lowerBoundary.getCurve().getDepth(pingIndex) :
            (lowerBoundary.getPingRange().begin().equals(pingIndex) ?
                  lowerBoundary.getStartConnector().getDepth() : lowerBoundary.getEndConnector().getDepth());

      if (upperIsConnector) {
         endDepth = Math.max(endDepth, startDepth);
         //only for debugging
         /*
         LayerConnector connector = !upperBoundary.getPingRange().contains(pingIndex)
               ? upperBoundary.getEndPointConnector()
               : upperBoundary.getStartPointConnector();
         if (connectedVerticals(connector) > 1) {
            System.out.println("break");
         }
         */
      }
      if (lowerIsConnector) {
         startDepth = Math.min(startDepth, endDepth);
         //only for debugging
         /*
         LayerConnector connector = !lowerBoundary.getPingRange().contains(pingIndex)
               ? lowerBoundary.getEndPointConnector()
               : lowerBoundary.getStartPointConnector();
         if (connectedVerticals(connector) > 1) {
            System.out.println("break");
         }
         */
      }

      //3. Split the layer
      boolean splitLayerSelected = layer.isSelected();
      if (splitLayerSelected) {
         deselectRegion(layer);
      }
      LayerAndBoundaryPair<VerticalBoundary> newPair = layer.split(upperBoundary, lowerBoundary,
            VerticalBoundary::new,
            new EchogramPoint(pingIndex, startDepth),
            new EchogramPoint(pingIndex, endDepth));

      //4. Go through new layer and add new layer boundaries to global list
      layers.add(newPair.layer());
      ensureAllConnectedVerticalsAreConsistent(newPair.boundary().getStartConnector());
      List<Layer> bothLayers = List.of(layer, newPair.layer());
      if (splitLayerSelected) {
         selectRegions(bothLayers);
      }
      getRegionManager().notifyRegionBoundaryChanged(pingRange, bothLayers);
      return newPair;
   }

   void addVerticalDivider(PingIndex pingIndex, boolean inheritVisitedStatus) {
      for (Layer layer : getLayers(pingIndex)) {
         LayerAndBoundaryPair<VerticalBoundary> pair = addVerticalBoundary(layer, pingIndex);
         if (pair != null) {
            if (inheritVisitedStatus) {
               pair.layer().setSelectedAtLeastOnce(layer.isSelectedAtLeastOnce());
            }
         }
      }
   }

   /**
    * Adds horizontal boundaries starting at the selected point, and recursively adds horizontal boundaries to the layers to
    * the left and right of the added boundary.
    *
    * @param point          the point to start at
    * @param depthTransform the depth transform
    */
   void addHorizontalDivider(EchogramPoint point, DepthTransform depthTransform) {
      LayerAndBoundaryPair<CurveBoundary> layerAndBoundaryPair = addCurveBoundary(point, depthTransform);
      if (layerAndBoundaryPair != null) {
         CurveBoundary boundary = layerAndBoundaryPair.boundary();
         addHorizontalBoundariesToTheLeft(boundary.getStartConnector(), point.depth(), depthTransform, getPingContainer());
         addHorizontalBoundariesToTheRight(boundary.getEndConnector(), point.depth(), depthTransform, getPingContainer());
      }
   }

   private void addHorizontalBoundariesToTheLeft(LayerConnector connector, float depth, DepthTransform depthTransform, PingContainer pingContainer) {
      long leftPingNumber = connector.getPingIndex().getPingNumber() - 1;
      if (getVisiblePingRange().containsPingNumber(leftPingNumber) && !getRegionManager().isReadOnlyIncludingEnd(connector.getPingIndex())) {
         EchogramPoint leftPoint = clampedEchogramPoint(connector, depth, pingContainer, leftPingNumber);
         LayerAndBoundaryPair<CurveBoundary> layerAndBoundaryPair = addCurveBoundary(leftPoint, depthTransform);
         if (layerAndBoundaryPair != null) {
            CurveBoundary boundary = layerAndBoundaryPair.boundary();
            moveBoundary(new EchogramPoint(leftPoint.pingIndex(), depth), boundary);
            verifyAndAdjustConnectors(boundary.getLayers());
            LayerConnector leftPointConnector = boundary.getStartConnector();
            addHorizontalBoundariesToTheLeft(leftPointConnector, depth, depthTransform, pingContainer);
         }
      }
   }

   private void addHorizontalBoundariesToTheRight(LayerConnector connector, float depth, DepthTransform depthTransform, PingContainer pingContainer) {
      long rightPingNumber = connector.getPingIndex().getPingNumber();
      if (getVisiblePingRange().containsPingNumber(rightPingNumber) && !getRegionManager().isReadOnly(connector.getPingIndex())) {
         EchogramPoint rightPoint = clampedEchogramPoint(connector, depth, pingContainer, rightPingNumber);
         LayerAndBoundaryPair<CurveBoundary> layerAndBoundaryPair = addCurveBoundary(rightPoint, depthTransform);
         if (layerAndBoundaryPair != null) {
            CurveBoundary boundary = layerAndBoundaryPair.boundary();
            moveBoundary(new EchogramPoint(rightPoint.pingIndex(), depth), boundary);
            verifyAndAdjustConnectors(boundary.getLayers());
            LayerConnector rightPointConnector = layerAndBoundaryPair.boundary().getEndConnector();
            addHorizontalBoundariesToTheRight(rightPointConnector, depth, depthTransform, pingContainer);
         }
      }
   }

   private static EchogramPoint clampedEchogramPoint(LayerConnector connector, float depth, PingContainer pingContainer, long pingNumber) {
      PingIndex pingIndex = pingContainer.getPingIndex(pingNumber);
      //clamp depth to a layer connected to connector
      float bestClamp = clampDepth(connector, depth, pingIndex);
      return new EchogramPoint(pingIndex, bestClamp);
   }

   private static float clampDepth(LayerConnector connector, float depth, PingIndex pingIndex) {
      float clampDist = Float.MAX_VALUE;
      float bestClamp = depth;
      for (Layer layer : connector.getLayers()) {
         if (layer.getPingRange().contains(pingIndex)) {
            FloatRange depthRange = layer.getDepthRange(pingIndex);
            float clamp = depthRange.clamp(depth);
            if (!depthRange.contains(clamp)) {
               //to ensure that the clamped value is contained in the layer
               clamp = Math.nextDown(clamp);
            }
            float dist = Math.abs(depth - clamp);
            if (dist < clampDist) {
               clampDist = dist;
               bestClamp = clamp;
            }
         }
      }
      return bestClamp;
   }

   public void verifyAndAdjustConnectors(Collection<Layer> layersToVerify) {
      List<Layer> adjustedLayers = new ArrayList<>();
      PingRange adjustedRange = PingRange.EMPTY_RANGE;
      for (Layer layer : layersToVerify) {
         if (verifyAndAdjustConnectors(layer)) {
            adjustedLayers.add(layer);
            adjustedRange = adjustedRange.union(layer.getPingRange());
         }
      }
      if (!adjustedLayers.isEmpty()) {
         getRegionManager().notifyRegionBoundaryChanged(adjustedRange, adjustedLayers);
      }
   }

   private boolean verifyAndAdjustConnectors(Layer layer) {
      boolean didAdjust = false;
      for (CurveBoundary curveBoundary : layer.getCurveBoundaries()) {
         LayerConnector startConnector = curveBoundary.getStartConnector();
         if (startConnector.getCurveBoundaries().size() == 1) {
            didAdjust = true;
            //This is a start point connector connecting a curve and vertical boundaries.
            //Adjust the depth of the curve boundary to match the position of the connector
            editBoundary(startConnector.getPoint(), startConnector.getPoint(), IdentityDepthTransform.INSTANCE, curveBoundary);
         }
         ensureAllConnectedVerticalsAreConsistent(startConnector);

         LayerConnector endConnector = curveBoundary.getEndConnector();
         if (endConnector.getCurveBoundaries().size() == 1) {
            didAdjust = true;
            //This is an end point connector connecting a curve and vertical boundaries.
            //Adjust the connector position to the end point of the curve boundary clamped within the layer,
            //and adjust the lengths of the vertical boundaries accordingly.
            float lastCurveDepth = curveBoundary.getCurve().getLastDepth();
            FloatRange depthRange = layer.getDepthRange(curveBoundary.getLastPingIndex(getPingContainer()));
            if (!depthRange.isEmpty()) {
               lastCurveDepth = depthRange.clamp(lastCurveDepth);
            }
            for (VerticalBoundary vb : endConnector.getVerticalBoundaries()) {
               if (vb.getEndConnector().equals(endConnector)) {
                  lastCurveDepth = Math.max(lastCurveDepth, vb.getStartConnector().getDepth());
               } else {
                  lastCurveDepth = Math.min(lastCurveDepth, vb.getEndConnector().getDepth());
               }
               endConnector.setDepth(lastCurveDepth);
               break;
            }
         }
         ensureAllConnectedVerticalsAreConsistent(endConnector);
      }
      return didAdjust;
   }

   public @Nullable String checkForError() {
      return layers.parallelStream()
            .map(LayerManagerUtils::checkLayer)
            .filter(Objects::nonNull)
            .findAny()
            .orElse(null);
   }

   public void checkValidity() throws WorkaroundRegionException {
      String error = checkForError();
      if (error != null) {
         throw new WorkaroundRegionException(error);
      }
      PingRange totalRange = getPingContainer().getTotalRange();
      RangeSet<PingIndex> union = new ArrayRangeSet<>();
      for (Layer layer : layers) {
         //check that all boundaries in a layer is connected to another boundary in the same layer
         for (CurveBoundary curveBoundary : layer.getCurveBoundaries()) {
            if (!LayerManagerUtils.isConnected(curveBoundary, curveBoundary.getStartConnector(), layer)) {
               throw new WorkaroundRegionException();
            }
            if (!LayerManagerUtils.isConnected(curveBoundary, curveBoundary.getEndConnector(), layer)) {
               throw new WorkaroundRegionException();
            }
         }
         for (VerticalBoundary verticalBoundary : layer.getVerticalBoundaries()) {
            LayerConnector startConnector = verticalBoundary.getStartConnector();
            LayerConnector endConnector = verticalBoundary.getEndConnector();
            if (!startConnector.getPingIndex().equals(endConnector.getPingIndex())) {
               throw new WorkaroundRegionException();
            }
            if (startConnector.getDepth() > endConnector.getDepth()) {
               throw new WorkaroundRegionException();
            }
            if (!LayerManagerUtils.isConnected(verticalBoundary, startConnector, layer)) {
               throw new WorkaroundRegionException();
            }
            if (!LayerManagerUtils.isConnected(verticalBoundary, endConnector, layer)) {
               throw new WorkaroundRegionException();
            }
         }
         union.add(layer.getPingRange());
      }
      if (!union.containsAll(totalRange)) {
         throw new WorkaroundRegionException();
      }
      for (Layer layer : layers) {
         for (PingIndex pingIndex : getPingContainer().getPingIndices(layer.getPingRange())) {
            CurveBoundary upperBoundary = layer.findUpperBoundary(pingIndex);
            CurveBoundary lowerBoundary = layer.findLowerBoundary(pingIndex);
            if (upperBoundary == null || lowerBoundary == null) {
               throw new WorkaroundRegionException();
            }

            //Check range of a potential new vertical boundary
            boolean upperIsConnector = !upperBoundary.getPingRange().containsExcludingBegin(pingIndex);
            boolean lowerIsConnector = !lowerBoundary.getPingRange().containsExcludingBegin(pingIndex);

            float startDepth = !upperIsConnector ?
                  upperBoundary.getCurve().getDepth(pingIndex) :
                  (upperBoundary.getPingRange().begin().equals(pingIndex) ?
                        upperBoundary.getStartConnector().getDepth() : upperBoundary.getEndConnector().getDepth());
            float endDepth = !lowerIsConnector ?
                  lowerBoundary.getCurve().getDepth(pingIndex) :
                  (lowerBoundary.getPingRange().begin().equals(pingIndex) ?
                        lowerBoundary.getStartConnector().getDepth() : lowerBoundary.getEndConnector().getDepth());

            if (upperIsConnector) {
               endDepth = Math.max(endDepth, startDepth);
            }
            if (lowerIsConnector) {
               startDepth = Math.min(startDepth, endDepth);
            }
            if (endDepth < startDepth) {
               throw new WorkaroundRegionException();
            }
         }
         //check vertical boundaries
         for (VerticalBoundary verticalBoundary : layer.getVerticalBoundaries()) {
            List<VerticalBoundary> allConnected = getAllAffectedBoundaries(verticalBoundary);
            for (VerticalBoundary boundary : allConnected) {
               if (boundary.equals(verticalBoundary)) {
                  continue;
               }
               if (verticalBoundary.isBelow(boundary)) {
                  if (boundary.getMaxDepth() > verticalBoundary.getMinDepth()) {
                     throw new WorkaroundRegionException();
                  }
                  if (boundary.getMinDepth() > verticalBoundary.getMinDepth()) {
                     throw new WorkaroundRegionException();
                  }
               }
               if (verticalBoundary.isAbove(boundary)) {
                  if (boundary.getMaxDepth() < verticalBoundary.getMaxDepth()) {
                     throw new WorkaroundRegionException();
                  }
                  if (boundary.getMinDepth() < verticalBoundary.getMaxDepth()) {
                     throw new WorkaroundRegionException();
                  }
               }
            }
         }
         //check that all connectors connect at least two boundaries of different type
         for (LayerConnector layerConnector : layer.getConnectors()) {
            if (layerConnector.getPingIndex().compareTo(totalRange.begin()) >= 0 &&
                  layerConnector.getPingIndex().compareTo(totalRange.end()) <= 0) {
               if (layerConnector.getBoundaryCount() < 2) {
                  throw new WorkaroundRegionException();
               }
               boolean edgeConnector = layerConnector.getPingIndex().compareTo(totalRange.begin()) == 0 ||
                     layerConnector.getPingIndex().compareTo(totalRange.end()) == 0;
               //allow no connection to curve boundary if this connector is at the edge of the check range, otherwise
               //one boundary if each type is required
               if ((layerConnector.getCurveBoundaries().isEmpty() && !edgeConnector) || layerConnector.getVerticalBoundaries().isEmpty()) {
                  throw new WorkaroundRegionException();
               }
            }
         }
         //check that every vertical boundary is connected to 2 layers, except if the index is the first or the last
         for (VerticalBoundary verticalBoundary : layer.getVerticalBoundaries()) {
            if (verticalBoundary.getPingIndex().compareTo(totalRange.begin()) > 0 &&
                  verticalBoundary.getPingIndex().compareTo(totalRange.end()) < 0) {
               if (verticalBoundary.getLayers().size() != 2) {
                  throw new WorkaroundRegionException();
               }
            }
         }
      }
   }

   public @Nullable CurveBoundary findClosestCurveBoundary(EchogramPoint point, FloatRange depthRange) {
      Layer layer = getLayer(point);
      if (layer == null) {
         // The point is either above upper boundary or below bottom boundary.
         CurveBoundary upperBoundary = getUpperBoundary(point.pingIndex());
         float upperBoundaryDepth = upperBoundary.getCurve().getDepth(point.pingIndex());
         if (depthRange.contains(upperBoundaryDepth) && point.depth() <= upperBoundaryDepth) {
            return upperBoundary;
         }
         CurveBoundary bottomBoundary = getBottomBoundary(point.pingIndex());
         float bottomBoundaryDepth = bottomBoundary.getCurve().getDepth(point.pingIndex());
         if (depthRange.contains(bottomBoundaryDepth) && point.depth() >= upperBoundaryDepth) {
            return bottomBoundary;
         }
         return null;
      }
      return Stream.of(layer.findUpperBoundary(point.pingIndex()), layer.findLowerBoundary(point.pingIndex()))
            .filter(Objects::nonNull)
            .filter(curveBoundary -> depthRange.contains(curveBoundary.getCurve().getClampedDepth(point.pingIndex())))
            .min(Comparator.comparingDouble(curveBoundary -> Math.abs(curveBoundary.getCurve().getClampedDepth(point.pingIndex()) - point.depth())))
            .orElse(null);
   }

   public @Nullable LayerConnector findClosestLayerConnector(EchogramPoint point) {
      float depthDist = Float.MAX_VALUE;
      long pingDist = Long.MAX_VALUE;
      List<LayerConnector> candidates = new ArrayList<>();
      LayerConnector candidate = null;
      Layer currentLayer = getLayer(point);
      Set<LayerConnector> connectors;
      if (currentLayer == null) {
         //must point either above upper boundary or below bottom boundary.
         connectors = new HashSet<>();
         for (Layer layer : layers) {
            if (layer.getPingRange().contains(point.pingIndex())) {
               connectors.addAll(layer.getConnectors());
            }
         }
      } else {
         connectors = currentLayer.getConnectors();
      }
      for (LayerConnector connector : connectors) {
         long p = Math.abs(connector.getPingIndex().getPingNumber() - point.pingIndex().getPingNumber());
         if (p < pingDist) {
            candidates.clear();
         }
         if (p <= pingDist) {
            candidates.add(connector);
            pingDist = p;
         }
      }
      for (LayerConnector connector : candidates) {
         float d = Math.abs(connector.getDepth() - point.depth());
         if (d < depthDist) {
            depthDist = d;
            candidate = connector;
         }
      }
      return candidate;
   }

   public @Nullable VerticalBoundary findClosestVerticalBoundary(EchogramPoint point, FloatRange zRange, DepthTransform depthTransform) {
      FloatRange depthRange = depthTransform.zToDepth(zRange, point.pingIndex());
      Layer currentLayer = getLayer(point);
      if (currentLayer == null) {
         return null;
      }
      long pingDist = Long.MAX_VALUE;
      VerticalBoundary candidate = null;
      for (VerticalBoundary verticalBoundary : currentLayer.getVerticalBoundaries()) {
         if (verticalBoundary.getDepthRange().intersects(depthRange) &&
               verticalBoundary.getDepthRange().contains(point.depth())) {
            long p = Math.abs(verticalBoundary.getPingIndex().getPingNumber() - point.pingIndex().getPingNumber());
            if (p < pingDist) {
               pingDist = p;
               candidate = verticalBoundary;
            }
         }
      }
      return candidate;
   }

   public void editBoundary(PingRange pingRange, ToFloatFunction<PingIndex> pingIndexToDepth, CurveBoundary curveBoundary) {
      if (getRegionManager().isReadOnly(pingRange)) {
         throw new IllegalEditException();
      }
      PingRange adjustRange = curveBoundary.editBoundary(pingRange, pingIndexToDepth, getPingContainer(), new CurveBoundary.BoundaryIntersectionFilter(getPingContainer(), curveBoundary.getLayers(), curveBoundary));
      if (adjustRange.isEmpty()) {
         return;
      }
      getRegionManager().getSchoolManager().constrainSchools(adjustRange);
      getRegionManager().notifyRegionBoundaryChanged(adjustRange, curveBoundary.getLayers());
   }

   public void editBoundary(EchogramPoint fromPoint, EchogramPoint toPoint, DepthTransform depthTransform, CurveBoundary curveBoundary) {
      PingRange pingRange = PingRange.from(List.of(fromPoint.pingIndex(), toPoint.pingIndex()), getPingContainer())
            .intersection(curveBoundary.getPingRange());
      editBoundary(pingRange, depthTransform.linearZToDepthFunction(fromPoint, toPoint), curveBoundary);
   }

   public void moveBoundary(EchogramPoint toPoint, CurveBoundary curveBoundary) {
      if (getRegionManager().isReadOnly(curveBoundary.getPingRange())) {
         throw new IllegalEditException();
      }
      float deltaDepth = toPoint.depth() - curveBoundary.getCurve().getClampedDepth(toPoint.pingIndex());
      ToFloatFunction<PingIndex> pingIndexToDepth = pingIndex -> curveBoundary.getCurve().getClampedDepth(pingIndex) + deltaDepth;
      PingRange moveRange = getVisiblePingRange().intersection(curveBoundary.getPingRange());
      editBoundary(moveRange, pingIndexToDepth, curveBoundary);
   }

   public void editConnector(EchogramPoint toPoint, DepthTransform depthTransform, LayerConnector connector) {
      if (getRegionManager().isReadOnlyIncludingEnd(connector.getPingIndex())) {
         throw new IllegalEditException();
      }
      PingRange adjustRange = connector.moveConnectorActively(toPoint, depthTransform, getPingContainer());

      ensureNoCurveCrossings(connector, adjustRange);

      ensureAllConnectedVerticalsAreConsistent(connector);

      getRegionManager().getSchoolManager().constrainSchools(adjustRange);

      getRegionManager().notifyRegionBoundaryChanged(adjustRange, connector.getLayers());
   }

   private void ensureNoCurveCrossings(LayerConnector connector, PingRange pingRange) {
      //Perform a new test for curve crossings in case this is an end point connector
      //If the connector is an end point connector, constraining curve boundaries must be extended
      //before curve intersection test is made.
      PingRange adjustRange = PingRange.of(pingRange.begin(), getPingContainer().nextOrSame(pingRange.end()));

      //find all affected verticals.
      List<VerticalBoundary> verticalBoundaries = connector.getVerticalBoundaries();
      if (verticalBoundaries.isEmpty()) {
         return;
      }
      VerticalBoundary vb = verticalBoundaries.getFirst();
      List<VerticalBoundary> allAffectedVerticals = getAllAffectedBoundaries(vb);
      //sort so the 'deepest' verticals are first
      allAffectedVerticals.sort((o1, o2) -> {
         if (o1.equals(o2)) {
            return 0;
         }
         return o1.isBelow(o2) ? -1 : 1;
      });

      //find all affected connectors
      List<LayerConnector> allAffectedConnectors = new ArrayList<>();
      for (VerticalBoundary verticalBoundary : allAffectedVerticals) {
         LayerConnector endConnector = verticalBoundary.getEndConnector();
         if (!allAffectedConnectors.contains(endConnector)) {
            allAffectedConnectors.add(endConnector);
         }
         LayerConnector startConnector = verticalBoundary.getStartConnector();
         if (!allAffectedConnectors.contains(startConnector)) {
            allAffectedConnectors.add(startConnector);
         }
      }

      //adjust all curve boundaries not connected to other curve boundaries.
      List<CurveBoundary> needsFurtherConstraintBoundaries = new ArrayList<>();
      for (LayerConnector layerConnector : allAffectedConnectors) {
         List<CurveBoundary> connectedCurves = layerConnector.getCurveBoundaries();
         if (connectedCurves.size() == 1) {
            CurveBoundary cb = connectedCurves.getFirst();
            if (adjustRange.intersects(cb.getPingRange())) {
               needsFurtherConstraintBoundaries.add(cb);
            }
         }
      }
      //constrain against given curves.
      for (CurveBoundary curveBoundary : needsFurtherConstraintBoundaries) {
         constrainFromBelow(adjustRange, curveBoundary);
      }
      Collections.reverse(needsFurtherConstraintBoundaries);
      for (CurveBoundary curveBoundary : needsFurtherConstraintBoundaries) {
         constrainFromAbove(adjustRange, curveBoundary);
      }
      //constrain actively moved boundary against the other boundaries
      for (CurveBoundary cb : connector.getCurveBoundaries()) {
         if (adjustRange.intersects(cb.getPingRange())) {
            cb.constrain(adjustRange, new CurveBoundary.BoundaryIntersectionFilter(getPingContainer(), cb.getLayers(), cb));
         }
      }
   }

   private void constrainFromBelow(PingRange adjustRange, CurveBoundary curveBoundary) {
      List<Layer> layers = curveBoundary.getLayers();
      curveBoundary.constrain(adjustRange, new CurveBoundary.BoundaryIntersectionFilter(getPingContainer(), layers, curveBoundary) {
         @Override
         List<CurveBoundary> getUpperBoundaries() {
            return List.of(); // No upper constraints.
         }
      });
   }

   private void constrainFromAbove(PingRange adjustRange, CurveBoundary curveBoundary) {
      List<Layer> layers = curveBoundary.getLayers();
      curveBoundary.constrain(adjustRange, new CurveBoundary.BoundaryIntersectionFilter(getPingContainer(), layers, curveBoundary) {
         @Override
         List<CurveBoundary> getLowerBoundaries() {
            return List.of(); // No lower constraints.
         }
      });
   }

   private static void ensureAllConnectedVerticalsAreConsistent(LayerConnector connector) {
      for (VerticalBoundary layerBoundary : connector.getVerticalBoundaries()) {
         List<VerticalBoundary> allVerticalBoundaries = getAllAffectedBoundaries(layerBoundary);
         allVerticalBoundaries.sort((o1, o2) -> {
            if (o1.equals(o2)) {
               return 0;
            }
            return o1.isAbove(o2) ? -1 : 1;
         });
         float currentMinDepth = -Float.MAX_VALUE;
         for (VerticalBoundary verticalBoundary : allVerticalBoundaries) {
            float startDepth = verticalBoundary.getStartConnector().getDepth();
            float endDepth = verticalBoundary.getEndConnector().getDepth();
            startDepth = Math.max(currentMinDepth, startDepth);
            endDepth = Math.max(endDepth, startDepth);
            verticalBoundary.getStartConnector().setDepth(startDepth);
            verticalBoundary.getEndConnector().setDepth(endDepth);
            currentMinDepth = endDepth;
         }
         break;
      }
   }

   public void editVerticalBoundary(EchogramPoint toPoint, DepthTransform depthTransform, VerticalBoundary verticalBoundary) {
      if (getRegionManager().isReadOnlyIncludingEnd(verticalBoundary.getPingIndex())) {
         throw new IllegalEditException();
      }
      PingRange adjustRange = verticalBoundary.moveVerticalBoundary(toPoint.pingIndex(), depthTransform, getPingContainer(), null);

      //Perform a new test for curve crossings in case this is an end point connector
      //If the connector is an end point connector, constraining curve boundaries must be extended
      //before curve intersection test is made.
      ensureNoCurveCrossings(verticalBoundary.getStartConnector(), adjustRange);

      ensureAllConnectedVerticalsAreConsistent(verticalBoundary.getStartConnector());

      List<Layer> affectedLayers = getAllAffectedBoundaries(verticalBoundary).stream()
            .flatMap(affectedBoundary -> affectedBoundary.getLayers().stream())
            .distinct()
            .toList();
      getRegionManager().notifyRegionBoundaryChanged(adjustRange, affectedLayers);
   }

   private static List<VerticalBoundary> getAllAffectedBoundaries(VerticalBoundary verticalBoundary) {
      List<VerticalBoundary> connectedBoundaries = new ArrayList<>();
      connectedBoundaries.add(verticalBoundary);
      //follow startConnector
      LayerConnector startConnector = verticalBoundary.getStartConnector();
      VerticalBoundary above = startConnector.getBoundaryAbove();
      while (above != null) {
         connectedBoundaries.addFirst(above);
         startConnector = above.getStartConnector();
         above = startConnector.getBoundaryAbove();
      }
      LayerConnector endConnector = verticalBoundary.getEndConnector();
      VerticalBoundary below = endConnector.getBoundaryBelow();
      while (below != null) {
         connectedBoundaries.add(below);
         endConnector = below.getEndConnector();
         below = endConnector.getBoundaryBelow();
      }
      return connectedBoundaries;
   }

   private List<CurveBoundary> getUpperBoundaries(PingRange pingRange) {
      List<CurveBoundary> upperBoundaries = new ArrayList<>();
      CurveBoundary upperBoundary = getUpperBoundary(pingRange.begin());
      upperBoundaries.add(upperBoundary);
      while (upperBoundary.getPingRange().end().compareTo(pingRange.end()) < 0) {
         upperBoundary = getUpperBoundary(upperBoundary.getPingRange().end());
         upperBoundaries.add(upperBoundary);
      }
      return upperBoundaries;
   }

   private List<CurveBoundary> getBottomBoundaries(PingRange pingRange) {
      List<CurveBoundary> bottomBoundaries = new ArrayList<>();
      CurveBoundary bottomBoundary = getBottomBoundary(pingRange.begin());
      bottomBoundaries.add(bottomBoundary);
      while (bottomBoundary.getPingRange().end().compareTo(pingRange.end()) < 0) {
         bottomBoundary = getBottomBoundary(bottomBoundary.getPingRange().end());
         bottomBoundaries.add(bottomBoundary);
      }
      return bottomBoundaries;
   }

   public CurveBoundary getUpperBoundary(PingIndex pingIndex) {
      if (pingIndex.equals(getPingContainer().getTotalRange().end())) {
         pingIndex = getPingContainer().previousOrSame(pingIndex);
      }
      for (Layer layer : getLayers(pingIndex)) {
         for (CurveBoundary curveBoundary : layer.getUpperCurveBoundaries()) {
            if (curveBoundary.getPingRange().contains(pingIndex)) {
               if (curveBoundary.getLayerAbove() == null) {
                  //this is the uppermost curve boundary
                  return curveBoundary;
               }
            }
         }
      }
      throw new ShouldNotHappenException("No upper boundary for " + pingIndex);
   }

   public CurveBoundary getBottomBoundary(PingIndex pingIndex) {
      if (pingIndex.equals(getPingContainer().getTotalRange().end())) {
         pingIndex = getPingContainer().previousOrSame(pingIndex);
      }
      for (Layer layer : getLayers(pingIndex)) {
         for (CurveBoundary curveBoundary : layer.getLowerCurveBoundaries()) {
            if (curveBoundary.getPingRange().contains(pingIndex)) {
               if (curveBoundary.getLayerBelow() == null) {
                  //this is the lowermost curve boundary
                  return curveBoundary;
               }
            }
         }
      }
      throw new ShouldNotHappenException("No bottom boundary for " + pingIndex);
   }

   public FloatRange getBoundaryDepthRange(PingIndex pingIndex) {
      if (pingIndex.equals(getPingContainer().getTotalRange().end())) {
         pingIndex = getPingContainer().previousOrSame(pingIndex);
      }
      float minDepth = Float.POSITIVE_INFINITY;
      float maxDepth = Float.NEGATIVE_INFINITY;
      for (Layer layer : layers) {
         if (!layer.getPingRange().contains(pingIndex)) {
            continue;
         }
         for (CurveBoundary curveBoundary : layer.getUpperCurveBoundaries()) {
            Curve curve = curveBoundary.getCurve();
            if (curve.getPingRange().contains(pingIndex)) {
               float depth = curve.getDepth(pingIndex);
               if (depth < minDepth) {
                  minDepth = depth;
               }
            }
         }
         for (CurveBoundary curveBoundary : layer.getLowerCurveBoundaries()) {
            Curve curve = curveBoundary.getCurve();
            if (curve.getPingRange().contains(pingIndex)) {
               float depth = curve.getDepth(pingIndex);
               if (depth > maxDepth) {
                  maxDepth = depth;
               }
            }
         }
      }
      return FloatRange.of(minDepth, maxDepth);
   }

   Element toXml(PingRange pingRange) {
      return LayerManagerSaver.toXml(pingRange, getLayersIntersectingPingRange(pingRange));
   }

   void fromXml(Element element, PingRange pingRange) throws WorkFileException {
      layers.clear();
      layers.addAll(LayerManagerLoader.fromXml(element, pingRange, getRegionManager()));
      possiblyUpdateLayerObjectNumbers();
   }

   void join(LayerManager left, LayerManager right) {
      try {
         left.getLayers().forEach(layer -> layer.setRegionManager(getRegionManager()));
         right.getLayers().forEach(layer -> layer.setRegionManager(getRegionManager()));

         layers.clear();
         layers.addAll(left.getLayers());
         PingIndex connectIndex = right.getPingContainer().getTotalRange().begin();
         LayerManagerJoiner.connectToExistingLayers(right.getConnectorsAtPingIndex(connectIndex), getConnectorsAtPingIndex(connectIndex));
         layers.addAll(right.getLayers());
         mergeLayersWithSameInterpretation(connectIndex);
         possiblyUpdateLayerObjectNumbers();
      } catch (RegionException e) {
         Log.global.log(java.util.logging.Level.WARNING, e.getMessage(), e);
      }
   }

   private void mergeLayersWithSameInterpretation(PingIndex pingIndex) {
      List<Layer> leftLayers = new ArrayList<>();
      Set<Layer> rightLayers = new HashSet<>();
      for (Layer layer : layers) {
         PingRange pingRange = layer.getPingRange();
         if (pingIndex.equals(pingRange.end())) {
            leftLayers.add(layer);
         } else if (pingIndex.equals(pingRange.begin())) {
            rightLayers.add(layer);
         }
      }
      outerLoop:
      for (Layer leftLayer : leftLayers) {
         for (Iterator<Layer> rightIterator = rightLayers.iterator(); rightIterator.hasNext(); ) {
            Layer rightLayer = rightIterator.next();
            if (leftLayer.hasObjectNumber() && rightLayer.hasObjectNumber() && leftLayer.getObjectNumber() != rightLayer.getObjectNumber()) {
               // Different object numbers.
               continue;
            }
            if (!leftLayer.hasEqualInterpretationTo(rightLayer)) {
               // Different interpretation.
               continue;
            }
            if (!mergeLayers(leftLayer, rightLayer).isPresent()) {
               // Could not be merged.
               continue;
            }
            // Merge ok.
            rightIterator.remove();
            continue outerLoop;
         }
      }
   }

   private void possiblyUpdateLayerObjectNumbers() {
      Set<Integer> usedObjectNumbers = new HashSet<>();
      for (Layer layer : layers) {
         if (layer.hasObjectNumber() && !usedObjectNumbers.add(layer.getObjectNumber())) {
            layer.setObjectNumber(getRegionManager().getRegionConfiguration().nextObjectNumber());
         }
      }
   }
}
