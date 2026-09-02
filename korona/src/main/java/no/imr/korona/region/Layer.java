package no.imr.korona.region;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.Curve;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.tools.Utils;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * A region bounded by (horizontal and vertical) layer boundaries.
 */
public final class Layer extends Region {
   private static final String XML_LAYER = "layer";

   //Using copy-on-write lists to avoid concurrent modification exceptions.
   private final List<CurveBoundary> upperCurveBoundaries = new CopyOnWriteArrayList<>();
   private final List<CurveBoundary> lowerCurveBoundaries = new CopyOnWriteArrayList<>();
   private final List<VerticalBoundary> verticalBoundaries = new CopyOnWriteArrayList<>();

   Layer(RegionManager regionManager) {
      super(regionManager);
   }

   Element toXml() {
      return baseToXml(XML_LAYER);
   }

   void fromXml(Element element) {
      baseFromXml(element);
   }

   @Override
   public boolean isReadOnly() {
      return upperCurveBoundaries.stream()
            .map(CurveBoundary::getPingRange)
            .anyMatch(getRegionManager()::isReadOnly);
   }

   void addVerticalBoundary(VerticalBoundary verticalBoundary) {
      addBoundary(verticalBoundaries, verticalBoundary);
   }

   void addUpperBoundary(CurveBoundary upperBoundary) {
      addBoundary(upperCurveBoundaries, upperBoundary);
   }

   void addLowerBoundary(CurveBoundary lowerBoundary) {
      addBoundary(lowerCurveBoundaries, lowerBoundary);
   }

   private <B extends LayerBoundary> void addBoundary(List<B> boundaries, B newBoundary) {
      if (boundaries.contains(newBoundary)) {
         return;
      }
      boundaries.add(newBoundary);
      newBoundary.addLayer(this);
   }

   boolean isUpperBoundary(CurveBoundary curveBoundary) {
      return upperCurveBoundaries.contains(curveBoundary);
   }

   boolean isLowerBoundary(CurveBoundary curveBoundary) {
      return lowerCurveBoundaries.contains(curveBoundary);
   }

   void removeBoundary(LayerBoundary layerBoundary) {
      switch (layerBoundary) {
         case CurveBoundary curveBoundary -> {
            upperCurveBoundaries.remove(curveBoundary);
            lowerCurveBoundaries.remove(curveBoundary);
         }
         case VerticalBoundary verticalBoundary -> {
            verticalBoundaries.remove(verticalBoundary);
         }
      }
   }

   void removeAll() {
      for (CurveBoundary curveBoundary : upperCurveBoundaries) {
         curveBoundary.removeLayer(this);
      }
      for (CurveBoundary curveBoundary : lowerCurveBoundaries) {
         curveBoundary.removeLayer(this);
      }
      for (VerticalBoundary verticalBoundary : verticalBoundaries) {
         verticalBoundary.removeLayer(this);
      }
      upperCurveBoundaries.clear();
      lowerCurveBoundaries.clear();
      verticalBoundaries.clear();
   }

   public List<CurveBoundary> getUpperCurveBoundaries() {
      return upperCurveBoundaries;
   }

   public List<CurveBoundary> getLowerCurveBoundaries() {
      return lowerCurveBoundaries;
   }

   List<CurveBoundary> getCurveBoundaries() {
      // Copy to new list instead of using UnionList since upperCurveBoundaries and lowerCurveBoundaries can change concurrently
      return Utils.toList(upperCurveBoundaries, lowerCurveBoundaries);
   }

   public List<VerticalBoundary> getVerticalBoundaries() {
      return verticalBoundaries;
   }

   Set<LayerConnector> getConnectors() {
      Set<LayerConnector> connectors = new HashSet<>();
      addConnectors(connectors, upperCurveBoundaries);
      addConnectors(connectors, lowerCurveBoundaries);
      addConnectors(connectors, verticalBoundaries);
      return connectors;
   }

   private static void addConnectors(Set<LayerConnector> connectors, Collection<? extends LayerBoundary> layerBoundaries) {
      for (LayerBoundary layerBoundary : layerBoundaries) {
         connectors.add(layerBoundary.getStartConnector());
         connectors.add(layerBoundary.getEndConnector());
      }
   }

   @Override
   public boolean contains(PingIndex pingIndex) {
      return getPingRange().contains(pingIndex);
   }

   @Override
   public boolean intersectsPingRange(PingRange pingRange) {
      return getPingRange().intersects(pingRange);
   }

   private List<LayerBoundary> getLayerPath(LayerBoundary startBoundary, boolean posDir, LayerConnector stopConnector) {
      LayerConnector firstLayerConnector = posDir ? startBoundary.getEndConnector() : startBoundary.getStartConnector();

      List<LayerBoundary> layerBoundaries = new ArrayList<>();
      startBoundary.getBoundariesConnectedToLayer(layerBoundaries, this, firstLayerConnector, stopConnector);
      return layerBoundaries;
   }

   @Override
   float getRepresentativeMinDepth(PingIndex pingIndex) {
      return getRepresentativeDepth(upperCurveBoundaries, pingIndex);
   }

   @Override
   float getRepresentativeMaxDepth(PingIndex pingIndex) {
      return getRepresentativeDepth(lowerCurveBoundaries, pingIndex);
   }

   private float getRepresentativeDepth(List<CurveBoundary> curveBoundaries, PingIndex pingIndex) {
      Curve containingCurve = getContainingCurve(curveBoundaries, pingIndex);
      if (containingCurve == null) {
         PingRange pingRange = getPingRange();
         pingIndex = pingIndex.compareTo(pingRange.begin()) < 0
               ? pingRange.begin()
               : getPingContainer().previousOrSame(pingRange.end());
         containingCurve = getContainingCurve(curveBoundaries, pingIndex);
         if (containingCurve == null) {
            return 0;
         }
      }
      return containingCurve.getDepth(pingIndex);
   }

   @Override
   public PingRange getPingRange() {
      Iterator<CurveBoundary> iterator = upperCurveBoundaries.iterator();
      if (!iterator.hasNext()) {
         return PingRange.EMPTY_RANGE;
      }
      PingRange firstPingRange = iterator.next().getPingRange();
      if (!iterator.hasNext()) {
         return firstPingRange;
      }
      PingIndex a = firstPingRange.begin();
      PingIndex b = firstPingRange.end();
      while (iterator.hasNext()) {
         PingRange pingRange = iterator.next().getPingRange();
         if (pingRange.begin().compareTo(a) < 0) {
            a = pingRange.begin();
         }
         if (pingRange.end().compareTo(b) > 0) {
            b = pingRange.end();
         }
      }
      return PingRange.of(a, b);
   }

   @Override
   FloatRangeSet getDepthRanges(PingIndex pingIndex) {
      FloatRange depthRange = getDepthRange(pingIndex);
      return FloatRangeSet.of(depthRange);
   }

   FloatRange getDepthRange(PingIndex pingIndex) {
      Curve upper = getContainingCurve(upperCurveBoundaries, pingIndex);
      if (upper == null) {
         return FloatRange.EMPTY_RANGE;
      }
      Curve lower = getContainingCurve(lowerCurveBoundaries, pingIndex);
      if (lower == null) {
         return FloatRange.EMPTY_RANGE;
      }
      return FloatRange.of(upper.getDepth(pingIndex), lower.getDepth(pingIndex));
   }

   private static @Nullable Curve getContainingCurve(List<CurveBoundary> curveBoundaries, PingIndex pingIndex) {
      for (CurveBoundary curveBoundary : curveBoundaries) {
         Curve curve = curveBoundary.getCurve();
         if (curve.getPingRange().contains(pingIndex)) {
            return curve;
         }
      }
      return null;
   }

   @Nullable CurveBoundary findUpperBoundary(PingIndex pingIndex) {
      CurveBoundary upper = null;
      for (CurveBoundary upperCurveBoundary : upperCurveBoundaries) {
         PingRange pingRange = upperCurveBoundary.getPingRange();
         if (pingRange.containsIncludingEnd(pingIndex)) {
            boolean atEnd = pingRange.end().equals(pingIndex);
            if ((!atEnd && upper == null) || (atEnd && isLowestUpperBoundary(upperCurveBoundary.getEndConnector()))) {
               upper = upperCurveBoundary;
            }
         }
      }
      return upper;
   }

   @Nullable CurveBoundary findLowerBoundary(PingIndex pingIndex) {
      CurveBoundary lower = null;
      for (CurveBoundary lowerCurveBoundary : lowerCurveBoundaries) {
         PingRange pingRange = lowerCurveBoundary.getPingRange();
         if (pingRange.containsIncludingEnd(pingIndex)) {
            boolean atEnd = pingRange.end().equals(pingIndex);
            if ((!atEnd && lower == null) || (atEnd && isShallowestLowerBoundary(lowerCurveBoundary.getEndConnector()))) {
               lower = lowerCurveBoundary;
            }
         }
      }
      return lower;
   }

   private boolean isLowestUpperBoundary(LayerConnector endConnector) {
      if (upperCurveBoundaries.size() == 1) {
         return true;
      }
      int intersectingCount = 0;
      PingIndex pingIndex = endConnector.getPingIndex();
      for (CurveBoundary upperCurveBoundary : upperCurveBoundaries) {
         if (upperCurveBoundary.getPingRange().containsIncludingEnd(pingIndex)) {
            intersectingCount++;
         }
      }
      if (intersectingCount == 1) {
         return true;
      }
      //more thorough test - if the end connector of the boundary is connected to the end point of a vertical boundary
      //in the same layer, this is the lowermost upper boundary.
      for (VerticalBoundary vb : endConnector.getVerticalBoundaries()) {
         if (verticalBoundaries.contains(vb) && vb.getEndConnector().equals(endConnector)) {
            return true;
         }
      }
      return false;
   }

   private boolean isShallowestLowerBoundary(LayerConnector endConnector) {
      if (lowerCurveBoundaries.size() == 1) {
         return true;
      }
      int intersectingCount = 0;
      PingIndex pingIndex = endConnector.getPingIndex();
      for (CurveBoundary lowerCurveBoundary : lowerCurveBoundaries) {
         if (lowerCurveBoundary.getPingRange().containsIncludingEnd(pingIndex)) {
            intersectingCount++;
         }
      }
      if (intersectingCount == 1) {
         return true;
      }
      //more thorough test - if the end connector of the boundary is connected to the start point of a vertical boundary
      //in the same layer, this is the lowermost upper boundary.
      for (VerticalBoundary vb : endConnector.getVerticalBoundaries()) {
         if (verticalBoundaries.contains(vb) && vb.getStartConnector().equals(endConnector)) {
            return true;
         }
      }
      return false;
   }

   Curve createNonIntersectingCurve(PingRange pingRange, ToFloatFunction<PingIndex> depthFunction) {
      Curve newBoundaryCurve = new Curve(pingRange);
      newBoundaryCurve.adjust(depthFunction, getPingContainer());
      for (CurveBoundary upperCurveBoundary : upperCurveBoundaries) {
         newBoundaryCurve.adjust(upperCurveBoundary.getCurve(), Math::max);
      }
      for (CurveBoundary lowerCurveBoundary : lowerCurveBoundaries) {
         newBoundaryCurve.adjust(lowerCurveBoundary.getCurve(), Math::min);
      }
      return newBoundaryCurve;
   }

   List<@Nullable VerticalBoundary> findLeftAndRightBoundary(Curve curve, EchogramPoint point) {
      VerticalBoundary leftBoundary = null;
      VerticalBoundary rightBoundary = null;
      //float epsilon = 1e-2f; //1cm
      float bestVerticalDistAtEndPoint = Float.MAX_VALUE;
      float bestVerticalDistAtStartPoint = Float.MAX_VALUE;
      float leftDist = Float.MAX_VALUE;
      float rightDist = Float.MAX_VALUE;
      for (VerticalBoundary verticalBoundary : verticalBoundaries) {
         if (curve.getPingRange().containsIncludingEnd(verticalBoundary.getPingIndex())) {
            PingIndex pingIndex = verticalBoundary.getPingIndex();
            boolean isLastIndex = curve.getPingRange().end().equals(verticalBoundary.getPingIndex());
            boolean intersectsDepthRange = !isLastIndex && verticalBoundary.getDepthRange().containsIncludingEnd(curve.getDepth(pingIndex));
            //if (intersectsDepthRange || isLastIndex)
            //{
            float dist = point.pingIndex().getPingNumber() - verticalBoundary.getPingIndex().getPingNumber();
            if (dist >= 0 && dist < leftDist && isLeftSideVerticalBoundary(verticalBoundary)) {
               if (intersectsDepthRange) {
                  leftDist = dist;
                  leftBoundary = verticalBoundary;
                  bestVerticalDistAtStartPoint = 0;
               } else if (bestVerticalDistAtStartPoint > 0) { // no 'best' boundary found
                  float verticalDist = verticalBoundary.getDepthRange().distanceTo(curve.getDepth(pingIndex));
                  if (verticalDist < bestVerticalDistAtStartPoint) {
                     leftBoundary = verticalBoundary;
                     bestVerticalDistAtStartPoint = verticalDist;
                  }
               }
            }
            if (dist < 0 && -dist < rightDist && isRightSideVerticalBoundary(verticalBoundary)) {
               if (intersectsDepthRange) {
                  rightDist = -dist;
                  rightBoundary = verticalBoundary;
                  bestVerticalDistAtEndPoint = 0;
               } else if (bestVerticalDistAtEndPoint > 0) { // last index, and no vertical boundary found
                  float verticalDist = verticalBoundary.getDepthRange().distanceTo(curve.getLastDepth());
                  if (verticalDist < bestVerticalDistAtEndPoint) {
                     rightBoundary = verticalBoundary;
                     bestVerticalDistAtEndPoint = verticalDist;
                  }
               }
            }
            //}
         }
      }
      return Arrays.asList(leftBoundary, rightBoundary);
   }

   <T extends LayerBoundary> LayerAndBoundaryPair<T> split(LayerBoundary firstExistingBoundary, LayerBoundary secondExistingBoundary,
                                                           BiFunction<LayerConnector, LayerConnector, T> newBoundaryFactory,
                                                           EchogramPoint startPoint, EchogramPoint endPoint) {
      //Providing the end point of the new boundary, because the end point is not included in the range of the new boundary.

      //1. Split the existing boundaries, if necessary
      LayerBoundaryAndConnectorPair<?> firstPair = firstExistingBoundary.split(startPoint);
      LayerBoundaryAndConnectorPair<?> secondPair = secondExistingBoundary.split(endPoint);
      LayerBoundary firstEndBoundary = firstPair.endBoundary();
      LayerBoundary secondEndBoundary = secondPair.endBoundary();

      LayerConnector startConnector = firstPair.connector();
      LayerConnector endConnector = secondPair.connector();

      //3. Attach layer connectors to new boundary.
      T newBoundary = newBoundaryFactory.apply(startConnector, endConnector);

      switch (newBoundary) {
         case VerticalBoundary verticalBoundary -> addVerticalBoundary(verticalBoundary);
         case CurveBoundary curveBoundary -> addLowerBoundary(curveBoundary);
      }

      //5. If firstEndBoundary or secondEndBoundary is undefined, search for boundaries to the right/below
      //    firstEndBoundary and/or secondEndBoundary.
      ConnectedBoundaries firstConnectedBoundaries = findConnectedBoundaries(firstExistingBoundary, newBoundary, firstEndBoundary);
      firstEndBoundary = firstConnectedBoundaries.endBoundary();
      boolean firstPosDir = firstConnectedBoundaries.posDir();
      ConnectedBoundaries secondConnectedBoundaries = findConnectedBoundaries(secondExistingBoundary, newBoundary, secondEndBoundary);
      secondEndBoundary = secondConnectedBoundaries.endBoundary();
      boolean secondPosDir = secondConnectedBoundaries.posDir();

      //6. Add new layers and update affected layers.
      List<LayerBoundary> overlappingBoundaries = getLayerPath(firstEndBoundary, firstPosDir,
            secondPosDir ? secondEndBoundary.getEndConnector() : secondEndBoundary.getStartConnector());

      overlappingBoundaries.add(firstEndBoundary);
      overlappingBoundaries.add(secondEndBoundary);

      Layer newLayer = new Layer(getRegionManager());
      for (LayerBoundary boundary : overlappingBoundaries) {
         switch (boundary) {
            case CurveBoundary curveBoundary -> {
               if (isUpperBoundary(curveBoundary)) {
                  newLayer.addUpperBoundary(curveBoundary);
               } else {
                  newLayer.addLowerBoundary(curveBoundary);
               }
            }
            case VerticalBoundary verticalBoundary -> {
               newLayer.addVerticalBoundary(verticalBoundary);
            }
         }
      }
      switch (newBoundary) {
         case VerticalBoundary verticalBoundary -> newLayer.addVerticalBoundary(verticalBoundary);
         case CurveBoundary curveBoundary -> newLayer.addUpperBoundary(curveBoundary);
      }

      for (LayerBoundary boundary : overlappingBoundaries) {
         removeBoundary(boundary);
         boundary.removeLayer(this);
      }
      newLayer.getInterpretation().copyFrom(getInterpretation());
      newLayer.setLabels(getLabels());
      return new LayerAndBoundaryPair<>(newLayer, newBoundary);
   }

   private @Nullable VerticalBoundary getVerticalBoundaryAbove(VerticalBoundary boundary) {
      for (VerticalBoundary verticalBoundary : boundary.getStartConnector().getVerticalBoundaries()) {
         if (!verticalBoundary.equals(boundary)) {
            if (verticalBoundaries.contains(verticalBoundary)) {
               return verticalBoundary;
            }
         }
      }
      return null;
   }

   private @Nullable VerticalBoundary getVerticalBoundaryBelow(VerticalBoundary boundary) {
      for (VerticalBoundary verticalBoundary : boundary.getEndConnector().getVerticalBoundaries()) {
         if (!verticalBoundary.equals(boundary)) {
            if (verticalBoundaries.contains(verticalBoundary)) {
               return verticalBoundary;
            }
         }
      }
      return null;
   }

   boolean canInsertVerticalBoundary(PingIndex pingIndex) {
      for (VerticalBoundary verticalBoundary : verticalBoundaries) {
         if (verticalBoundary.getPingIndex().equals(pingIndex)) {
            if (isConnectedToUpperBoundary(verticalBoundary)) {
               VerticalBoundary vb = verticalBoundary;
               boolean isConnectedToLowerBoundary = isConnectedToLowerBoundary(vb);
               while (!isConnectedToLowerBoundary && vb != null) {
                  vb = getVerticalBoundaryBelow(vb);
                  if (vb != null) {
                     isConnectedToLowerBoundary = isConnectedToLowerBoundary(vb);
                  }
               }
               //if there is a vertical boundary at the location that connects an upper boundary to a lower boundary,
               //no additional vertical boundary can be added.
               if (isConnectedToLowerBoundary) {
                  return false;
               }
            }
         }
      }
      return true;
   }

   private boolean isConnectedToLowerBoundary(VerticalBoundary verticalBoundary) {
      for (CurveBoundary curveBoundary : verticalBoundary.getEndConnector().getCurveBoundaries()) {
         if (isLowerBoundary(curveBoundary)) {
            return true;
         }
      }
      return false;
   }

   private boolean isConnectedToUpperBoundary(VerticalBoundary verticalBoundary) {
      for (CurveBoundary curveBoundary : verticalBoundary.getStartConnector().getCurveBoundaries()) {
         if (isUpperBoundary(curveBoundary)) {
            return true;
         }
      }
      return false;
   }

   private record CurveConnectorPair(CurveBoundary curveBoundary, LayerConnector connector) {
   }

   private @Nullable CurveConnectorPair getCurveBoundaryInStartConnectorDirection(VerticalBoundary boundary) {
      VerticalBoundary verticalBoundary = boundary;
      while (verticalBoundary != null) {
         for (CurveBoundary cb : verticalBoundary.getStartConnector().getCurveBoundaries()) {
            if (upperCurveBoundaries.contains(cb) || lowerCurveBoundaries.contains(cb)) {
               if (cb.getStartConnector().equals(verticalBoundary.getStartConnector())) {
                  return new CurveConnectorPair(cb, cb.getStartConnector());
               } else {
                  return new CurveConnectorPair(cb, cb.getEndConnector());
               }
            }
         }
         verticalBoundary = getVerticalBoundaryAbove(verticalBoundary);
      }
      return null;
   }

   private @Nullable CurveConnectorPair getCurveBoundaryInEndConnectorDirection(VerticalBoundary boundary) {
      VerticalBoundary verticalBoundary = boundary;
      while (verticalBoundary != null) {
         for (CurveBoundary cb : verticalBoundary.getEndConnector().getCurveBoundaries()) {
            if (upperCurveBoundaries.contains(cb) || lowerCurveBoundaries.contains(cb)) {
               if (cb.getStartConnector().equals(verticalBoundary.getEndConnector())) {
                  return new CurveConnectorPair(cb, cb.getStartConnector());
               } else {
                  return new CurveConnectorPair(cb, cb.getEndConnector());
               }
            }
         }
         verticalBoundary = getVerticalBoundaryBelow(verticalBoundary);
      }
      return null;
   }

   private boolean isRightSideVerticalBoundary(VerticalBoundary verticalBoundary) {
      CurveConnectorPair curveInStartDir = getCurveBoundaryInStartConnectorDirection(verticalBoundary);
      CurveConnectorPair curveInEndDir = getCurveBoundaryInEndConnectorDirection(verticalBoundary);

      assert curveInStartDir != null;
      assert curveInEndDir != null;

      boolean startConnectedToUpperEnd = false;
      boolean startConnectedToLowerStart = false;

      boolean endConnectedToUpperStart = false;
      boolean endConnectedToLowerEnd = false;

      if (upperCurveBoundaries.contains(curveInStartDir.curveBoundary)) {
         if (curveInStartDir.curveBoundary.getEndConnector().equals(curveInStartDir.connector)) {
            startConnectedToUpperEnd = true;
         }
      } else { // curveInStartDir.curveBoundary is a lower boundary
         if (curveInStartDir.curveBoundary.getStartConnector().equals(curveInStartDir.connector)) {
            startConnectedToLowerStart = true;
         }
      }
      if (upperCurveBoundaries.contains(curveInEndDir.curveBoundary)) {
         if (curveInEndDir.curveBoundary.getStartConnector().equals(curveInEndDir.connector)) {
            endConnectedToUpperStart = true;
         }
      } else { // curveInStartDir.curveBoundary is a lower boundary
         if (curveInEndDir.curveBoundary.getEndConnector().equals(curveInEndDir.connector)) {
            endConnectedToLowerEnd = true;
         }
      }
      return startConnectedToUpperEnd && endConnectedToUpperStart ||
            startConnectedToUpperEnd && endConnectedToLowerEnd ||
            startConnectedToLowerStart && endConnectedToLowerEnd;
   }

   private boolean isLeftSideVerticalBoundary(VerticalBoundary verticalBoundary) {
      return !isRightSideVerticalBoundary(verticalBoundary);
   }

   private record ConnectedBoundaries(LayerBoundary endBoundary, boolean posDir) {
   }

   private ConnectedBoundaries findConnectedBoundaries(LayerBoundary existingBoundary, LayerBoundary newBoundary, @Nullable LayerBoundary endBoundary) {
      boolean posDir;
      if (endBoundary != null) {
         posDir = true;
      } else {
         for (LayerBoundary boundary : existingBoundary.getEndConnector().getBoundaries()) {
            if (boundary == existingBoundary || boundary == newBoundary) {
               continue;
            }
            if (boundary instanceof CurveBoundary curveBoundary) {
               if (upperCurveBoundaries.contains(curveBoundary) || lowerCurveBoundaries.contains(curveBoundary)) {
                  endBoundary = boundary;
                  break;
               }
            }
            if (boundary instanceof VerticalBoundary verticalBoundary) {
               if (verticalBoundaries.contains(verticalBoundary)) {
                  endBoundary = boundary;
                  break;
               }
            }
         }
         assert endBoundary != null;
         posDir = !endBoundary.getEndConnector().equals(existingBoundary.getEndConnector());
      }
      return new ConnectedBoundaries(endBoundary, posDir);
   }
}
