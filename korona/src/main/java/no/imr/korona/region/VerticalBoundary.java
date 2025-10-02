package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.tools.logging.Log;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A vertical part of a layer boundary.
 */
public final class VerticalBoundary extends LayerBoundary {
   public VerticalBoundary(LayerConnector startConnector, LayerConnector endConnector) {
      super(startConnector, endConnector);
   }

   @Override
   public String toString() {
      return getPingIndex() + ", " + getDepthRange();
   }

   public PingIndex getPingIndex() {
      return getStartConnector().getPingIndex();
   }

   public float getMinDepth() {
      return getStartConnector().getDepth();
   }

   public float getMaxDepth() {
      return getEndConnector().getDepth();
   }

   public FloatRange getDepthRange() {
      return FloatRange.of(getMinDepth(), getMaxDepth());
   }

   PingRange moveVerticalBoundary(PingIndex toIndex, DepthTransform depthTransform, PingContainer pingContainer, @Nullable LayerConnector connector) {
      PingIndex constrainedIndex = constrainPingIndex(toIndex, connector, pingContainer);
      PingRange pingRange = PingRange.from(List.of(getPingIndex(), constrainedIndex), pingContainer);
      if (getStartConnector() != connector) {
         getStartConnector().setPoint(this, depthTransform, pingContainer, new EchogramPoint(constrainedIndex, getStartConnector().getDepth()));
      }
      if (getEndConnector() != connector) {
         getEndConnector().setPoint(this, depthTransform, pingContainer, new EchogramPoint(constrainedIndex, getEndConnector().getDepth()));
      }
      constrainDepth(connector);
      return pingRange;
   }

   private void constrainDepth(@Nullable LayerConnector connector) {
      if (getStartConnector().equals(connector) && getStartConnector().getDepth() > getEndConnector().getDepth()) {
         getStartConnector().setDepth(getEndConnector().getDepth());
      } else if (getEndConnector().equals(connector) && getEndConnector().getDepth() < getStartConnector().getDepth()) {
         getEndConnector().setDepth(getStartConnector().getDepth());
      } else if (getStartConnector().getDepth() > getEndConnector().getDepth()) {
         getStartConnector().setDepth(getEndConnector().getDepth());
      }
   }

   @Override
   LayerBoundaryAndConnectorPair<VerticalBoundary> split(EchogramPoint echogramPoint) {
      //vertical boundaries are *always* split, even if it results in an 'empty' vertical boundary.
      LayerConnector midConnector = new LayerConnector(echogramPoint);
      VerticalBoundary endBoundary = new VerticalBoundary(midConnector, getEndConnector());
      getEndConnector().removeBoundary(this);
      setEndConnector(midConnector);
      midConnector.addBoundary(this);
      for (Layer layer : getLayers()) {
         layer.addVerticalBoundary(endBoundary);
      }
      return new LayerBoundaryAndConnectorPair<>(endBoundary, midConnector);
   }

   void mergeWith(VerticalBoundary other) {
      if (!getEndConnector().equals(other.getStartConnector())) {
         Log.global.warning("The other boundary is not a continuation of this boundary");
         return;
      }
      setEndConnector(other.getEndConnector());
      getEndConnector().removeBoundary(other);
      getEndConnector().addBoundary(this);
   }

   boolean isBelow(VerticalBoundary verticalBoundary) {
      return getStartConnector().isBelow(verticalBoundary.getStartConnector());
   }

   boolean isAbove(VerticalBoundary verticalBoundary) {
      return getStartConnector().isAbove(verticalBoundary.getStartConnector());
   }

   PingIndex constrainPingIndex(PingIndex toIndex, @Nullable LayerConnector connector, PingContainer pingContainer) {
      PingIndex constrainedIndex = toIndex;

      if (pingContainer.getTotalRange().isBeginOrEnd(getPingIndex())) {
         return getPingIndex();
      }

      boolean movingToTheRight = toIndex.compareTo(getPingIndex()) > 0;
      if (!getStartConnector().equals(connector)) {
         EchogramPoint startConstraint = getStartConnector().constrain(new EchogramPoint(constrainedIndex, getStartConnector().getDepth()), this, pingContainer);
         if (movingToTheRight && startConstraint.pingIndex().compareTo(constrainedIndex) < 0) {
            constrainedIndex = startConstraint.pingIndex();
         }
         if (!movingToTheRight && startConstraint.pingIndex().compareTo(constrainedIndex) > 0) {
            constrainedIndex = startConstraint.pingIndex();
         }
      }
      if (!getEndConnector().equals(connector)) {
         EchogramPoint endConstraint = getEndConnector().constrain(new EchogramPoint(constrainedIndex, getEndConnector().getDepth()), this, pingContainer);
         if (movingToTheRight && endConstraint.pingIndex().compareTo(constrainedIndex) < 0) {
            constrainedIndex = endConstraint.pingIndex();
         }
         if (!movingToTheRight && endConstraint.pingIndex().compareTo(constrainedIndex) > 0) {
            constrainedIndex = endConstraint.pingIndex();
         }
      }
      return constrainedIndex;
   }

   @Override
   boolean touchesPingRange(PingRange pingRange) {
      return pingRange.containsIncludingEnd(getPingIndex());
   }
}
