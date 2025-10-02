package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.tools.Max;
import no.imr.tools.Min;
import no.imr.tools.Utils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A point which connects layer boundaries. A connector should be attached to each endpoint of a
 * layer boundary, and should connect two or more layer boundaries.
 */
public final class LayerConnector {
   private static final String XML_POINT_REP = "connectorRep";
   private static final String XML_PING_OFFSET = "pingOffset";
   private static final String XML_DEPTH = "depth";

   private EchogramPoint point;
   private final CopyOnWriteArrayList<CurveBoundary> curveBoundaries = new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<VerticalBoundary> verticalBoundaries = new CopyOnWriteArrayList<>();

   LayerConnector(EchogramPoint point) {
      this.point = point;
   }

   LayerConnector(PingContainer pingContainer, PingIndex referencePingIndex, Element element) {
      Element rep = element.element(XML_POINT_REP);
      long offset = Long.parseLong(rep.attributeValue(XML_PING_OFFSET));
      PingIndex pingIndex = pingContainer.getPingIndex(referencePingIndex.getPingNumber() + offset);
      point = new EchogramPoint(pingIndex, Float.parseFloat(rep.attributeValue(XML_DEPTH)));
   }

   Element toXml(PingIndex referencePingIndex) {
      return DocumentHelper.createElement(XML_POINT_REP)
            .addAttribute(XML_PING_OFFSET, Long.toString(point.pingIndex().getPingNumber() - referencePingIndex.getPingNumber()))
            .addAttribute(XML_DEPTH, Float.toString(point.depth()));
   }

   @Override
   public String toString() {
      return point.toString();
   }

   public PingIndex getPingIndex() {
      return point.pingIndex();
   }

   public float getDepth() {
      return point.depth();
   }

   public void setDepth(float depth) {
      point = point.withDepth(depth);
   }

   public EchogramPoint getPoint() {
      return point;
   }

   private void notifyListeners(@Nullable LayerBoundary layerBoundary, DepthTransform depthTransform, PingContainer pingContainer, boolean activelyMovingConnector) {
      List<CurveBoundary> sortedCurveBoundaries = new ArrayList<>();

      CurveBoundary lastCurveBoundary = null;

      CurveBoundary startConnectorCurve = null;
      CurveBoundary endConnectorCurve = null;
      for (CurveBoundary curve : curveBoundaries) {
         //sort the curve boundaries so that the one that is to be made shorter is modified first.
         if (curve.getStartConnector().equals(this)) {
            startConnectorCurve = curve;
         }
         if (curve.getEndConnector().equals(this)) {
            endConnectorCurve = curve;
         }
         if ((curve.getStartConnector().equals(this) && curve.getPingRange().begin().compareTo(point.pingIndex()) <= 0) ||
               (curve.getEndConnector().equals(this) && curve.getPingRange().end().compareTo(point.pingIndex()) > 0)) {
            if (activelyMovingConnector) {
               sortedCurveBoundaries.addFirst(curve);
            } else {
               lastCurveBoundary = curve;
            }
         } else {
            sortedCurveBoundaries.add(curve);
         }
      }
      if (lastCurveBoundary != null) {
         sortedCurveBoundaries.add(lastCurveBoundary);
         //System.out.println("lastCurveBoundary pingRange = " + ((CurveBoundary) lastCurveBoundary).getPingRange());
      }

      for (CurveBoundary curveBoundary : sortedCurveBoundaries) {
         if (curveBoundary != layerBoundary) {
            curveBoundary.modifyLayerBoundary(this, depthTransform, pingContainer, activelyMovingConnector);
         }
      }
      if (startConnectorCurve != null) {
         setDepth(startConnectorCurve.getCurve().getStartDepth());
      } else if (!activelyMovingConnector && endConnectorCurve != null) {
         setDepth(endConnectorCurve.getCurve().getLastDepth());
      }

      point = constrain(point, null, pingContainer);

      for (VerticalBoundary verticalBoundary : verticalBoundaries) {
         if (verticalBoundary != layerBoundary) {
            verticalBoundary.moveVerticalBoundary(point.pingIndex(), depthTransform, pingContainer, this);
         }
      }
   }

   void addBoundary(LayerBoundary boundary) {
      switch (boundary) {
         case CurveBoundary curveBoundary -> addBoundary(curveBoundary);
         case VerticalBoundary verticalBoundary -> addBoundary(verticalBoundary);
      }
   }

   void addBoundary(CurveBoundary boundary) {
      curveBoundaries.addIfAbsent(boundary);
   }

   void addBoundary(VerticalBoundary boundary) {
      verticalBoundaries.addIfAbsent(boundary);
   }

   void removeBoundary(LayerBoundary boundary) {
      switch (boundary) {
         case CurveBoundary curveBoundary -> removeBoundary(curveBoundary);
         case VerticalBoundary verticalBoundary -> removeBoundary(verticalBoundary);
      }
   }

   void removeBoundary(CurveBoundary boundary) {
      curveBoundaries.remove(boundary);
   }

   void removeBoundary(VerticalBoundary boundary) {
      verticalBoundaries.remove(boundary);
   }

   int getBoundaryCount() {
      return curveBoundaries.size() + verticalBoundaries.size();
   }

   public List<LayerBoundary> getBoundaries() {
      return Utils.toList(curveBoundaries, verticalBoundaries);
   }

   public Collection<Layer> getLayers() {
      Set<Layer> layers = new HashSet<>();
      for (CurveBoundary boundary : curveBoundaries) {
         layers.addAll(boundary.getLayers());
      }
      for (VerticalBoundary boundary : verticalBoundaries) {
         layers.addAll(boundary.getLayers());
      }
      return layers;
   }

   List<VerticalBoundary> getVerticalBoundaries() {
      return verticalBoundaries;
   }

   List<CurveBoundary> getCurveBoundaries() {
      return curveBoundaries;
   }

   @Nullable CurveBoundary getBoundaryLeft() {
      for (CurveBoundary boundary : curveBoundaries) {
         if (boundary.getEndConnector().equals(this)) {
            return boundary;
         }
      }
      return null;
   }

   @Nullable CurveBoundary getBoundaryRight() {
      for (CurveBoundary boundary : curveBoundaries) {
         if (boundary.getStartConnector().equals(this)) {
            return boundary;
         }
      }
      return null;
   }

   @Nullable VerticalBoundary getBoundaryAbove() {
      for (VerticalBoundary boundary : verticalBoundaries) {
         if (boundary.getEndConnector().equals(this)) {
            return boundary;
         }
      }
      return null;
   }

   @Nullable VerticalBoundary getBoundaryBelow() {
      for (VerticalBoundary boundary : verticalBoundaries) {
         if (boundary.getStartConnector().equals(this)) {
            return boundary;
         }
      }
      return null;
   }

   boolean isBelow(LayerConnector connector) {
      return !isAbove(connector);
   }

   boolean isAbove(LayerConnector connector) {
      while (true) {
         LayerConnector connectorAbove = connector.getConnectorAbove();
         if (connectorAbove == null) {
            return false;
         }
         if (connectorAbove.equals(this)) {
            return true;
         }
         connector = connectorAbove;
      }
   }

   private @Nullable LayerConnector getConnectorAbove() {
      VerticalBoundary verticalBoundaryAbove = getBoundaryAbove();
      if (verticalBoundaryAbove != null) {
         return verticalBoundaryAbove.getStartConnector();
      }
      // No vertical boundary directly above,
      // so instead search the upper boundaries of the layer above.

      PingIndex pingIndex = getPingIndex();
      Layer layerAbove = getCurveBoundaries().getFirst().getLayerAbove();

      layerAboveLoop:
      while (true) {
         if (layerAbove == null) {
            return null;
         }

         //    +-----+                        +-----+
         //    |     |                        |     |
         //    |     +-----+            +-----+     +
         //    |           |     or     |           |
         //    +-----+-----+            +-----+-----+
         //          ^                        ^
         // When stepping up in this kind of situation, we must select
         // the curve boundary connector without a vertical boundary below it.

         for (CurveBoundary curveBoundaryAbove : layerAbove.getUpperCurveBoundaries()) {
            if (curveBoundaryAbove.getStartConnector().getPingIndex().equals(pingIndex)) {
               if (curveBoundaryAbove.getStartConnector().getBoundaryBelow() != null) {
                  continue;
               }
               return curveBoundaryAbove.getStartConnector();
            }
            if (curveBoundaryAbove.getEndConnector().getPingIndex().equals(pingIndex)) {
               if (curveBoundaryAbove.getEndConnector().getBoundaryBelow() != null) {
                  continue;
               }
               return curveBoundaryAbove.getEndConnector();
            }
            if (curveBoundaryAbove.getPingRange().containsExcludingBegin(pingIndex)) {
               layerAbove = curveBoundaryAbove.getLayerAbove();
               continue layerAboveLoop;
            }
         }
         assert false; // Should not reach here.
         return null;
      }
   }

   void detach() {
      curveBoundaries.clear();
      verticalBoundaries.clear();
   }

   public EchogramPoint constrain(EchogramPoint toPoint, @Nullable LayerBoundary layerBoundary, PingContainer pingContainer) {
      //Check if any of the connected layer boundaries imposes constraints.
      PingIndex constrainedPingIndex = toPoint.pingIndex();
      float constrainedDepth = toPoint.depth();
      boolean movingToTheRight = toPoint.pingIndex().compareTo(point.pingIndex()) > 0;
      for (VerticalBoundary vb : getVerticalBoundaries()) {
         if (vb == layerBoundary) {
            continue;
         }
         PingIndex constrainIndex = vb.constrainPingIndex(constrainedPingIndex, this, pingContainer);
         if (movingToTheRight) {
            constrainedPingIndex = Min.of(constrainedPingIndex, constrainIndex);
         } else {
            constrainedPingIndex = Max.of(constrainedPingIndex, constrainIndex);
         }
         if (vb.getStartConnector().equals(this)) {
            constrainedDepth = Math.min(constrainedDepth, vb.getMaxDepth());
         } else if (vb.getEndConnector().equals(this)) {
            constrainedDepth = Math.max(constrainedDepth, vb.getMinDepth());
         }
      }
      for (CurveBoundary cb : getCurveBoundaries()) {
         if (cb == layerBoundary) {
            continue;
         }
         if (movingToTheRight && cb.getStartConnector().equals(this)) {
            PingIndex constrainIndex = pingContainer.previousOrSame(cb.getEndConnector().getPingIndex());
            constrainedPingIndex = Min.of(constrainedPingIndex, constrainIndex);
         }
         if (!movingToTheRight && cb.getEndConnector().equals(this)) {
            PingIndex constrainIndex = pingContainer.nextOrSame(cb.getStartConnector().getPingIndex());
            constrainedPingIndex = Max.of(constrainedPingIndex, constrainIndex);
         }
      }
      return new EchogramPoint(constrainedPingIndex, constrainedDepth);
   }

   PingRange moveConnectorActively(EchogramPoint toPoint, DepthTransform depthTransform, PingContainer pingContainer) {
      toPoint = constrain(toPoint, null, pingContainer);

      PingRange adjustRange = PingRange.from(List.of(point.pingIndex(), toPoint.pingIndex()), pingContainer);

      point = toPoint;
      notifyListeners(null, depthTransform, pingContainer, true);

      return adjustRange;
   }

   void setPoint(LayerBoundary layerBoundary, DepthTransform depthTransform, PingContainer pingContainer, EchogramPoint toPoint) {
      toPoint = constrain(toPoint, null, pingContainer);

      if (!toPoint.equals(point)) {
         point = toPoint;
         notifyListeners(layerBoundary, depthTransform, pingContainer, false); //do not send the message back to originator.
      }
   }
}
