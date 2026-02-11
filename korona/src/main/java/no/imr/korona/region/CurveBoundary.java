package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.Curve;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.ToFloatFunction;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A part of a layer boundary, which can be described as a curve
 * dependent only on the ping number.
 */
public final class CurveBoundary extends LayerBoundary {
   private static final String XML_CURVE_REP = "curveRep";
   private static final String XML_DEPTHS = "depths";

   private Curve curve;

   static class BoundaryIntersectionFilter {
      private final PingContainer pingContainer;
      private final Collection<Layer> layers;
      private final CurveBoundary editBoundary;

      BoundaryIntersectionFilter(PingContainer pingContainer, Collection<Layer> layers, CurveBoundary editBoundary) {
         this.pingContainer = pingContainer;
         this.layers = layers;
         this.editBoundary = editBoundary;
      }

      List<CurveBoundary> getUpperBoundaries() {
         List<CurveBoundary> upperBoundaries = new ArrayList<>();
         for (Layer layer : layers) {
            for (CurveBoundary curveBoundary : layer.getUpperCurveBoundaries()) {
               if (curveBoundary.equals(editBoundary) || upperBoundaries.contains(curveBoundary)) {
                  continue;
               }
               upperBoundaries.add(curveBoundary);
            }
         }
         // Exclude any upper boundaries which are lower boundaries in any layer editBoundary is a member of.
         for (Layer layer : editBoundary.getLayers()) {
            upperBoundaries.removeAll(layer.getLowerCurveBoundaries());
         }
         return upperBoundaries;
      }

      List<CurveBoundary> getLowerBoundaries() {
         List<CurveBoundary> lowerBoundaries = new ArrayList<>();
         for (Layer layer : layers) {
            for (CurveBoundary curveBoundary : layer.getLowerCurveBoundaries()) {
               if (curveBoundary.equals(editBoundary) || lowerBoundaries.contains(curveBoundary)) {
                  continue;
               }
               lowerBoundaries.add(curveBoundary);
            }
         }
         // Exclude any lower boundaries which are upper boundaries in any layer editBoundary is a member of.
         for (Layer layer : editBoundary.getLayers()) {
            lowerBoundaries.removeAll(layer.getUpperCurveBoundaries());
         }
         return lowerBoundaries;
      }

      private PingRange adjustCurve(PingRange pingRange, ToFloatFunction<PingIndex> pingIndexToDepth) {
         PingRange editRange = editBoundary.getPingRange().intersection(pingRange);
         editBoundary.getCurve().adjust(pingIndexToDepth, editRange, pingContainer);
         constrainCurve(editRange);
         return editRange;
      }

      private void constrainCurve(PingRange range) {
         //if more than one upper curve intersects the ping range, check the crossings between the intersecting
         //curves. Only the uppermost part should be a constraint
         List<CurveBoundary> upperBoundaries = getUpperBoundaries();
         Set<CurveBoundary> canBeRemovedUpperBoundaries = new HashSet<>();
         for (int i = 0; i < upperBoundaries.size(); i++) {
            for (int j = i; j < upperBoundaries.size(); j++) {
               PingRange intersection = range.intersection(upperBoundaries.get(i).getPingRange());
               intersection = intersection.intersection(upperBoundaries.get(j).getPingRange());
               if (!intersection.isEmpty()) {
                  for (PingIndex pingIndex : pingContainer.getPingIndices(intersection)) {
                     if (upperBoundaries.get(i).getCurve().getDepth(pingIndex) < upperBoundaries.get(j).getCurve().getDepth(pingIndex)) {
                        //j can be removed
                        canBeRemovedUpperBoundaries.add(upperBoundaries.get(j));
                     } else if (upperBoundaries.get(i).getCurve().getDepth(pingIndex) > upperBoundaries.get(j).getCurve().getDepth(pingIndex)) {
                        //i can be removed
                        canBeRemovedUpperBoundaries.add(upperBoundaries.get(i));
                     }
                  }
                  //if none of the boundaries were removed, they are identical in the given range
               }
            }
         }

         for (CurveBoundary upperBoundary : upperBoundaries) {
            if (canBeRemovedUpperBoundaries.contains(upperBoundary)) {
               continue;
            }
            editBoundary.getCurve().adjust(upperBoundary.getCurve(), range, Math::max);
         }

         List<CurveBoundary> lowerBoundaries = getLowerBoundaries();
         Set<CurveBoundary> canBeRemovedLowerBoundaries = new HashSet<>();
         for (int i = 0; i < lowerBoundaries.size(); i++) {
            for (int j = i; j < lowerBoundaries.size(); j++) {
               PingRange intersection = range.intersection(lowerBoundaries.get(i).getPingRange());
               intersection = intersection.intersection(lowerBoundaries.get(j).getPingRange());
               if (!intersection.isEmpty()) {
                  for (PingIndex pingIndex : pingContainer.getPingIndices(intersection)) {
                     if (lowerBoundaries.get(i).getCurve().getDepth(pingIndex) < lowerBoundaries.get(j).getCurve().getDepth(pingIndex)) {
                        //i can be removed
                        canBeRemovedLowerBoundaries.add(lowerBoundaries.get(i));
                     } else if (lowerBoundaries.get(i).getCurve().getDepth(pingIndex) > lowerBoundaries.get(j).getCurve().getDepth(pingIndex)) {
                        //j can be removed
                        canBeRemovedLowerBoundaries.add(lowerBoundaries.get(j));
                     }
                  }
                  //if none of the boundaries were removed, they are identical in the given range
               }
            }
         }

         for (CurveBoundary lowerBoundary : lowerBoundaries) {
            if (canBeRemovedLowerBoundaries.contains(lowerBoundary)) {
               continue;
            }
            editBoundary.getCurve().adjust(lowerBoundary.getCurve(), range, Math::min);
         }
      }
   }

   private static final class BoundariesInLayersWithExceptionsFilter extends BoundaryIntersectionFilter {
      private final @Nullable CurveBoundary exceptionBoundary;

      private BoundariesInLayersWithExceptionsFilter(PingContainer pingContainer, Collection<Layer> layers, CurveBoundary editBoundary, @Nullable CurveBoundary exceptionBoundary) {
         super(pingContainer, layers, editBoundary);

         this.exceptionBoundary = exceptionBoundary;
      }

      @Override
      List<CurveBoundary> getUpperBoundaries() {
         List<CurveBoundary> upperBoundaries = super.getUpperBoundaries();
         List<CurveBoundary> result = new ArrayList<>();
         for (CurveBoundary curveBoundary : upperBoundaries) {
            if (curveBoundary != exceptionBoundary) {
               result.add(curveBoundary);
            }
         }
         return result;
      }

      @Override
      List<CurveBoundary> getLowerBoundaries() {
         List<CurveBoundary> lowerBoundaries = super.getLowerBoundaries();
         List<CurveBoundary> result = new ArrayList<>();
         for (CurveBoundary curveBoundary : lowerBoundaries) {
            if (curveBoundary != exceptionBoundary) {
               result.add(curveBoundary);
            }
         }
         return result;
      }
   }

   CurveBoundary(LayerConnector startConnector, LayerConnector endConnector) {
      this(startConnector, endConnector, new Curve(PingRange.ofUnsorted(startConnector.getPingIndex(), endConnector.getPingIndex())));
   }

   CurveBoundary(LayerConnector startConnector, LayerConnector endConnector, Curve curve) {
      super(startConnector, endConnector);

      this.curve = curve;
   }

   CurveBoundary(LayerConnector startConnector, LayerConnector endConnector, Element element) throws WorkFileException {
      super(startConnector, endConnector);

      PingIndex startPingIndex = startConnector.getPingIndex();
      PingIndex endPingIndex = endConnector.getPingIndex();

      curve = new Curve(PingRange.ofUnsorted(startPingIndex, endPingIndex));

      Element depthsElement = element.element(XML_CURVE_REP).element(XML_DEPTHS);
      String s = depthsElement.getText();
      String[] tokens = s.split("\\s");
      float[] depths = curve.getDepths();
      if (tokens.length != depths.length) {
         throw new WorkFileException(tokens.length + " depths, but " + depths.length + " pings");
      }
      for (int i = 0; i < tokens.length; i++) {
         depths[i] = Float.parseFloat(tokens[i]);
      }
   }

   Element toXml(PingRange pingRange) {
      Element rep = DocumentHelper.createElement(XML_CURVE_REP);
      PingRange intersectionRange = pingRange.intersection(curve.getPingRange());

      int iBegin = curve.getIndex(intersectionRange.begin());
      int iEnd = curve.getIndex(intersectionRange.end());
      int n = iEnd - iBegin;
      float[] depths = curve.getDepths();
      StringBuilder sb = new StringBuilder(10 * n);
      for (int i = 0; i < n; i++) {
         if (i > 0) {
            sb.append(i % 10 == 0 ? '\n' : ' ');
         }
         sb.append(depths[i + iBegin]);
      }
      rep.addElement(XML_DEPTHS)
            .addText(sb.toString());

      return rep;
   }

   @Override
   public String toString() {
      return curve.toString();
   }

   public Curve getCurve() {
      return curve;
   }

   private void setCurve(Curve curve) {
      this.curve = curve;
   }

   public PingRange getPingRange() {
      return curve.getPingRange();
   }

   boolean modifyLayerBoundary(LayerConnector connector, DepthTransform depthTransform, PingContainer pingContainer, boolean activelyMovingConnector) {
      //either extend or decrease the extent of the curve.
      if (connector.equals(getStartConnector())) {
         if (getStartConnector().getPingIndex().getPingNumber() < curve.getStartPing().getPingNumber()) {
            //extend curve at start point.
            EchogramPoint startPoint = curve.getStartPoint();
            PingRange adjustRange = PingRange.ofUnsorted(getStartConnector().getPingIndex(), startPoint.pingIndex());
            if (activelyMovingConnector) {
               curve = curve.extend(adjustRange);
               editBoundary(getStartConnector().getPoint(), startPoint, depthTransform, pingContainer, connector,
                     new BoundariesInLayersWithExceptionsFilter(pingContainer, getStartConnector().getLayers(), this, getStartConnector().getBoundaryLeft()));
            } else {
               List<Curve> subCurves = searchForCurvesInPingRange(adjustRange);
               //todo: Check whether the union of the pingRanges of the curves covers the whole adjustRange.
               PingRange union = PingRange.EMPTY_RANGE;
               for (Curve subCurve : subCurves) {
                  union = union.union(subCurve.getPingRange());
               }

               if (subCurves.size() > 1) {
                  Log.global.fine("More than one curve section in adjust range");
               }
               EchogramPoint firstPoint = !subCurves.isEmpty() ? subCurves.getFirst().getStartPoint() : curve.getStartPoint();
               curve = curve.extend(adjustRange);
               curve.adjust(subCurves);
               if (!union.equals(adjustRange)) {
                  //System.out.println("firstPoint = " + firstPoint);
                  //System.out.println("mStartPointConnector.getPoint() = " + mStartPointConnector.getPoint());
                  //Log.global.fine("union of curves does not cover whole adjustRange");
                  editBoundary(getStartConnector().getPoint(), firstPoint, depthTransform, pingContainer, getStartConnector(), new BoundaryIntersectionFilter(pingContainer, getStartConnector().getLayers(), this));
               }
            }
            //mStartPointConnector.getPoint().setDepth(mCurve.getDepth(mCurve.getStartPing())); //test
            return true;
         } else if (getStartConnector().getPingIndex().getPingNumber() >= curve.getStartPing().getPingNumber()) {
            curve = curve.intersect(PingRange.ofUnsorted(getStartConnector().getPingIndex(), curve.getEndPing()));
            if (curve.isEmpty()) {
               //if there is a curve to the left of this one, the end point connector to that curve should be set equal
               //to this curve's end point connector.
               CurveBoundary leftCurve = getStartConnector().getBoundaryLeft();
               if (leftCurve != null) {
                  leftCurve.setEndConnector(getEndConnector());
               }
               getStartConnector().removeBoundary(this);
               getEndConnector().removeBoundary(this);
               //remove boundary from all layers
               for (Layer layer : getStartConnector().getLayers()) {
                  layer.removeBoundary(this);
               }
            } else {
               if (activelyMovingConnector) {
                  editBoundary(getStartConnector().getPoint(),
                        curve.getStartPoint(), depthTransform, pingContainer, connector, new BoundaryIntersectionFilter(pingContainer, getStartConnector().getLayers(), this));
               }
            }
            //mStartPointConnector.getPoint().setDepth(mCurve.getDepth(mCurve.getStartPing())); //test
            return true;
         }
      }

      if (connector.equals(getEndConnector())) {
         if (getEndConnector().getPingIndex().getPingNumber() <= curve.getEndPing().getPingNumber()) {
            curve = curve.intersect(PingRange.ofUnsorted(curve.getStartPing(), getEndConnector().getPingIndex()));
            if (curve.isEmpty()) {
               //if there is a curve to the right of this one, the end point connector to that curve should be set equal
               //to this curve's end point connector.
               CurveBoundary rightCurve = getEndConnector().getBoundaryRight();
               if (rightCurve != null) {
                  rightCurve.setStartConnector(getStartConnector());
               }
               getStartConnector().removeBoundary(this);
               getEndConnector().removeBoundary(this);
               //remove boundary from all layers
               for (Layer layer : getEndConnector().getLayers()) {
                  layer.removeBoundary(this);
               }
            } else {
               //editBoundary(mEndPointConnector.getPoint(), new EchogramPoint(mCurve.getEndPingIndex(), mCurve.getLastDepth()), aDepthTransform);
            }
            /*
            if (!mEndPointConnector.isActivelyMoving())
               mEndPointConnector.getPoint().setDepth(mCurve.getLastDepth()); //test
               */
            return true;
         } else if (getEndConnector().getPingIndex().getPingNumber() > curve.getEndPing().getPingNumber()) {
            //extend curve at end point.
            //EchogramPoint endPoint = new EchogramPoint(LSSS.getInstance().getDataManager().getPingIndex(mCurve.getEndPingIndex().getPingNumber()-1), mCurve.getLastDepth());
            EchogramPoint endPoint = curve.getEndPoint();
            PingRange adjustRange = PingRange.ofUnsorted(endPoint.pingIndex(), getEndConnector().getPingIndex());
            if (activelyMovingConnector) {
               //todo when moving only one ping index, make a sound adjustment to the last index.
               //endPoint = new EchogramPoint(LSSS.getInstance().getDataManager().getPingIndex(mCurve.getEndPingIndex().getPingNumber() - 1), mCurve.getLastDepth());
               curve = curve.extend(adjustRange);
               editBoundary(endPoint, getEndConnector().getPoint(), depthTransform, pingContainer, connector,
                     new BoundariesInLayersWithExceptionsFilter(pingContainer, getEndConnector().getLayers(), this, getEndConnector().getBoundaryRight()));
            } else {
               List<Curve> subCurves = searchForCurvesInPingRange(adjustRange);
               //todo check whether the union of the pingRanges of the curves covers the whole adjustRange
               PingRange union = PingRange.EMPTY_RANGE;
               for (Curve subCurve : subCurves) {
                  union = union.union(subCurve.getPingRange());
               }
               if (subCurves.size() > 1) {
                  Log.global.fine("more than one curve section in adjustRange");
               }
               EchogramPoint lastPoint = !subCurves.isEmpty() ? subCurves.getFirst().getEndPoint() : endPoint;
               curve = curve.extend(adjustRange);
               curve.adjust(subCurves);
               if (!union.equals(adjustRange)) {
                  //Log.global.fine("union of curves does not cover whole adjustRange");
                  editBoundary(lastPoint, getEndConnector().getPoint(), depthTransform, pingContainer, connector, new BoundaryIntersectionFilter(pingContainer, getEndConnector().getLayers(), this));
               }
               //mEndPointConnector.getPoint().setDepth(mCurve.getLastDepth()); //test
            }
            //
            return true;
         }
      }

      return false;
   }

   public @Nullable Layer getLayerAbove() {
      return getLayers().stream()
            .filter(layer -> layer.getLowerCurveBoundaries().contains(this))
            .findFirst()
            .orElse(null);
   }

   public @Nullable Layer getLayerBelow() {
      return getLayers().stream()
            .filter(layer -> layer.getUpperCurveBoundaries().contains(this))
            .findFirst()
            .orElse(null);
   }

   List<Curve> searchForCurvesInPingRange(PingRange pingRange) {
      List<Curve> curveBoundaryPairs = new ArrayList<>();
      if (pingRange.isEmpty()) {
         return curveBoundaryPairs;
      }
      // First check if own ping range fully intersects pingRange.
      PingRange intersection = curve.getPingRange().intersection(pingRange);
      if (!intersection.isEmpty()) {
         curveBoundaryPairs.add(curve.copyOf(intersection));
      }
      if (!intersection.equals(pingRange)) {
         // If the aPingRange is not fully contained in aPingRange, search curve boundaries connected to this boundary, and append the depths.
         if (pingRange.begin().compareTo(curve.getPingRange().begin()) < 0) {
            //search for a curve boundary to the left of this one.
            CurveBoundary leftCurve = getStartConnector().getBoundaryLeft();
            if (leftCurve != null) {
               curveBoundaryPairs.addAll(0, leftCurve.searchForCurvesInPingRange(PingRange.ofUnsorted(pingRange.begin(), leftCurve.getPingRange().end())));
            }
         }
         if (pingRange.end().compareTo(curve.getPingRange().end()) > 0) {
            //search for a curve boundary to the right of this one.
            CurveBoundary rightCurve = getEndConnector().getBoundaryRight();
            if (rightCurve != null) {
               curveBoundaryPairs.addAll(rightCurve.searchForCurvesInPingRange(PingRange.ofUnsorted(rightCurve.getPingRange().begin(), pingRange.end())));
            }
         }
      }
      return curveBoundaryPairs;
   }

   @Override
   LayerBoundaryAndConnectorPair<CurveBoundary> split(EchogramPoint echogramPoint) {
      if (!curve.getPingRange().contains(echogramPoint.pingIndex())) {
         return new LayerBoundaryAndConnectorPair<>(null, getEndConnector());
      }
      if (curve.getStartPing().equals(echogramPoint.pingIndex())) {
         return new LayerBoundaryAndConnectorPair<>(this, getStartConnector());
      }

      Curve startCurve = curve.copyOf(PingRange.ofUnsorted(curve.getStartPing(), echogramPoint.pingIndex()));
      Curve endCurve = curve.copyOf(PingRange.ofUnsorted(echogramPoint.pingIndex(), curve.getEndPing()));

      setCurve(startCurve);

      LayerConnector midPointConnector = new LayerConnector(echogramPoint);
      CurveBoundary endCurveBoundary = new CurveBoundary(midPointConnector, getEndConnector(), endCurve);
      getEndConnector().removeBoundary(this);
      setEndConnector(midPointConnector);
      midPointConnector.addBoundary(this);
      for (Layer layer : getLayers()) {
         if (layer.isUpperBoundary(this)) {
            layer.addUpperBoundary(endCurveBoundary);
         } else {
            layer.addLowerBoundary(endCurveBoundary);
         }
      }
      return new LayerBoundaryAndConnectorPair<>(endCurveBoundary, midPointConnector);
   }

   void mergeWith(CurveBoundary other) {
      if (!getEndConnector().equals(other.getStartConnector())) {
         Log.global.warning("The other boundary is not a continuation of this boundary");
         return;
      }

      curve = curve.extend(other.getPingRange());
      curve.adjust(other.getCurve());

      setEndConnector(other.getEndConnector());
      getEndConnector().removeBoundary(other);
      getEndConnector().addBoundary(this);
   }

   PingIndex getLastPingIndex(PingContainer pingContainer) {
      return pingContainer.previousOrSame(curve.getEndPing());
   }

   private void notifyConnectors(@Nullable LayerConnector connector, PingRange pingRange, DepthTransform depthTransform, PingContainer pingContainer) {
      if (getStartConnector() != connector && pingRange.contains(getStartConnector().getPingIndex())) {
         getStartConnector().setPoint(this, depthTransform, pingContainer, curve.getStartPoint());
      }
      if (getEndConnector() != connector && pingRange.contains(getEndConnector().getPingIndex())) {
         getEndConnector().setPoint(this, depthTransform, pingContainer, curve.getEndPoint());
      }
   }

   PingRange editBoundary(EchogramPoint fromPoint, EchogramPoint toPoint, DepthTransform depthTransform, PingContainer pingContainer, @Nullable LayerConnector connector, BoundaryIntersectionFilter boundaryFilter) {
      PingRange pingRange = PingRange.from(List.of(fromPoint.pingIndex(), toPoint.pingIndex()), pingContainer);
      PingRange adjustRange = boundaryFilter.adjustCurve(pingRange, depthTransform.linearZToDepthFunction(fromPoint, toPoint));
      notifyConnectors(connector, adjustRange, depthTransform, pingContainer);
      return adjustRange;
   }

   PingRange editBoundary(PingRange pingRange, ToFloatFunction<PingIndex> pingIndexToDepth, PingContainer pingContainer, BoundaryIntersectionFilter boundaryIntersectionFilter) {
      PingRange adjustRange = boundaryIntersectionFilter.adjustCurve(pingRange, pingIndexToDepth);
      notifyConnectors(null, adjustRange, IdentityDepthTransform.INSTANCE, pingContainer);
      return adjustRange;
   }

   void constrain(PingRange pingRange, BoundaryIntersectionFilter boundaryFilter) {
      PingRange range = curve.getPingRange().intersection(pingRange);
      boundaryFilter.constrainCurve(range);
   }
}
