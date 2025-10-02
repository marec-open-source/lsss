package no.imr.korona.region;

import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LayerManagerJoiner {
   private LayerManagerJoiner() {
   }

   static void connectToExistingLayers(List<LayerConnector> currentConnectors, List<LayerConnector> existingConnectors) throws WorkFileException {
      currentConnectors.sort(LayerManagerJoiner::connectorOrdering);

      //existing connectors
      if (!existingConnectors.isEmpty()) {
         existingConnectors.sort(LayerManagerJoiner::connectorOrdering);
      }

      if (!existingConnectors.isEmpty() && !currentConnectors.isEmpty()) {
         matchConnectorDepths(currentConnectors, existingConnectors);
         matchConnectors(currentConnectors, existingConnectors);
      }
      //detach existing connectors
      for (LayerConnector layerConnector : existingConnectors) {
         layerConnector.detach();
      }
   }

   private static int connectorOrdering(LayerConnector connectorA, LayerConnector connectorB) {
      if (connectorA.getDepth() < connectorB.getDepth()) {
         return -1;
      }
      if (connectorA.getDepth() > connectorB.getDepth()) {
         return 1;
      }
      if (connectorA.equals(connectorB)) {
         return 0;
      }
      return connectorA.isAbove(connectorB) ? -1 : 1;
   }

   private static void matchConnectors(List<LayerConnector> currentConnectors, List<LayerConnector> existingConnectors) throws WorkFileException {
      LayerConnector match1OfCurrent = currentConnectors.getFirst();
      LayerConnector match1OfExisting = existingConnectors.getFirst();
      LayerConnector match2OfCurrent;
      LayerConnector match2OfExisting;
      int nextExistingListIndex = 1;
      nextExistingListIndex = Math.min(nextExistingListIndex, existingConnectors.size() - 1);
      //find match 2.
      int currentIndex = 1;
      for (LayerConnector layerConnector : currentConnectors.subList(1, currentConnectors.size())) {
         match2OfCurrent = layerConnector;

         int existingIndex = firstMatchingConnectorIndex(existingConnectors.subList(0, existingConnectors.size() - 1), nextExistingListIndex, layerConnector);
         //always match the last connector to the last existing connector
         if (currentIndex == currentConnectors.size() - 1) {
            existingIndex = existingConnectors.size() - 1;
         }
         if (existingIndex >= 0) {
            match2OfExisting = existingConnectors.get(existingIndex);
            nextExistingListIndex = existingIndex + 1;
            nextExistingListIndex = Math.min(nextExistingListIndex, existingConnectors.size() - 1);
            //we have two sets of matching connectors. Connect the layers.
            connectLayers(match1OfCurrent, match1OfExisting, match2OfCurrent, match2OfExisting);
            match1OfCurrent = match2OfCurrent;
            match1OfExisting = match2OfExisting;
         }
         currentIndex++;
      }
   }

   private static int firstMatchingConnectorIndex(List<LayerConnector> connectors, int startIndex, LayerConnector connector) {
      for (int index = startIndex; index < connectors.size(); index++) {
         if (connectors.get(index).getDepth() == connector.getDepth()) {
            return index;
         }
      }
      return -1;
   }

   private static void connectLayers(LayerConnector upperCurrent, LayerConnector upperExisting,
                                     LayerConnector lowerCurrent, LayerConnector lowerExisting) throws WorkFileException {
      List<LayerConnector> currentConnectors = getConnectorsBetween(upperCurrent, lowerCurrent);
      List<LayerConnector> existingConnectors = getConnectorsBetween(upperExisting, lowerExisting);

      Map<LayerConnector, LayerConnector> mapFromExistingToNewConnector = new HashMap<>();
      mapFromExistingToNewConnector.put(upperExisting, upperCurrent);
      mapFromExistingToNewConnector.put(lowerExisting, lowerCurrent);

      if (existingConnectors.size() > 2) {
         List<VerticalBoundary> exclusionList = new ArrayList<>();
         for (int i = 1; i < existingConnectors.size() - 1; i++) {
            LayerConnector existingConnector = existingConnectors.get(i);
            EchogramPoint connectorPoint = existingConnector.getPoint();
            LayerConnector splitConnector;
            VerticalBoundary splitBoundary = findSplitBoundary(currentConnectors, connectorPoint.depth(), exclusionList);
            if (splitBoundary != null) {
               exclusionList.add(splitBoundary);
               splitConnector = splitBoundary.split(connectorPoint).connector();
               for (CurveBoundary layerBoundary : existingConnector.getCurveBoundaries()) {
                  if (layerBoundary.getEndConnector().equals(existingConnector)) {
                     layerBoundary.setEndConnector(splitConnector);
                  }
                  if (layerBoundary.getStartConnector().equals(existingConnector)) {
                     layerBoundary.setStartConnector(splitConnector);
                  }
                  splitConnector.addBoundary(layerBoundary);
               }
               currentConnectors = getConnectorsBetween(upperCurrent, lowerCurrent);
            } else {
               Log.global.warning("No split boundary found");
               splitConnector = new LayerConnector(connectorPoint);
            }
            mapFromExistingToNewConnector.put(existingConnector, splitConnector);
         }
      }
      replaceVerticalBoundaries(upperExisting, lowerExisting, mapFromExistingToNewConnector);

      for (CurveBoundary layerBoundary : upperExisting.getCurveBoundaries()) {
         upperCurrent.addBoundary(layerBoundary);
         if (layerBoundary.getStartConnector().equals(upperExisting)) {
            layerBoundary.setStartConnector(upperCurrent);
         }
         if (layerBoundary.getEndConnector().equals(upperExisting)) {
            layerBoundary.setEndConnector(upperCurrent);
         }
      }
      for (CurveBoundary layerBoundary : lowerExisting.getCurveBoundaries()) {
         lowerCurrent.addBoundary(layerBoundary);
         if (layerBoundary.getStartConnector().equals(lowerExisting)) {
            layerBoundary.setStartConnector(lowerCurrent);
         }
         if (layerBoundary.getEndConnector().equals(lowerExisting)) {
            layerBoundary.setEndConnector(lowerCurrent);
         }
      }
   }

   /**
    * When coupling together connectors from two files, the depths of the connectors must match, in the
    * sense that the first connector in the currently read file must share the same depth as the first existing
    * connector at the same ping index. The same must be the case for the last connector. Will also adjust the
    * depth of attached vertical boundaries.
    *
    * @param currentConnectors  the connectors for the current file
    * @param existingConnectors the connectors to match to
    */
   private static void matchConnectorDepths(List<LayerConnector> currentConnectors, List<LayerConnector> existingConnectors) {
      float startDepthExisting = existingConnectors.getFirst().getDepth();
      float startDepthCurrent = currentConnectors.getFirst().getDepth();
      float endDepthExisting = existingConnectors.getLast().getDepth();
      float endDepthCurrent = currentConnectors.getLast().getDepth();

      float currentSpan = endDepthCurrent - startDepthCurrent;
      float existingSpan = endDepthExisting - startDepthExisting;
      float compacting = currentSpan > 0 ? existingSpan / currentSpan : -1;

      //adjust the depth of the first and last current connector to match the existing connectors
      currentConnectors.getFirst().setDepth(startDepthExisting);
      currentConnectors.getLast().setDepth(endDepthExisting);

      int index = 1;
      int size = currentConnectors.size() - 1;
      float prevDepth = startDepthExisting;
      for (LayerConnector layerConnector : currentConnectors.subList(1, currentConnectors.size() - 1)) {
         if (compacting > 0) {
            //move connector if it does not lie between the depths of the previous connector and the depth of the last connector
            float connectorDepth = layerConnector.getDepth();
            if (connectorDepth < prevDepth || connectorDepth >= endDepthExisting) {
               // Clamp the depths of current connectors, so that they all lie between start and end for existing connectors.
               layerConnector.setDepth(Math.clamp(connectorDepth, prevDepth, endDepthExisting));
            }
         } else {
            //distribute evenly
            layerConnector.setDepth(startDepthExisting + existingSpan * (float) index / (float) size);
         }
         prevDepth = layerConnector.getDepth();
         index++;
      }
      //ensure that the first and last current connector has exactly the same depth as the last existing connector
      currentConnectors.getLast().setDepth(endDepthExisting);
      currentConnectors.getFirst().setDepth(existingConnectors.getFirst().getDepth());

      // Clamp depths in case of rounding errors.
      for (int i = 1; i < currentConnectors.size() - 1; i++) {
         LayerConnector layerConnector0 = currentConnectors.get(i - 1);
         LayerConnector layerConnector1 = currentConnectors.get(i);
         LayerConnector layerConnector2 = currentConnectors.get(i + 1);
         float depth0 = layerConnector0.getDepth();
         float depth1 = layerConnector1.getDepth();
         float depth2 = layerConnector2.getDepth();
         depth1 = Math.clamp(depth1, depth0, depth2);
         depth1 = Math.clamp(depth1, startDepthExisting, endDepthExisting);
         layerConnector1.setDepth(depth1);
      }
   }

   private static void replaceVerticalBoundaries(LayerConnector upperExisting, LayerConnector lowerExisting,
                                                 Map<LayerConnector, LayerConnector> existingToCurrentMap) throws WorkFileException {
      List<VerticalBoundary> existingVerticals = getVerticalBoundariesBetween(upperExisting, lowerExisting);

      // Iterate through existing verticals. For the start and end point connectors, find the corresponding new connectors.
      // Find the verticals between the new connectors. Remove the existing vertical, add new vertical(s).
      for (VerticalBoundary existingVertical : existingVerticals) {
         LayerConnector currentStartConnector = existingToCurrentMap.get(existingVertical.getStartConnector());
         LayerConnector currentEndConnector = existingToCurrentMap.get(existingVertical.getEndConnector());
         List<VerticalBoundary> boundariesBetween = getVerticalBoundariesBetween(currentStartConnector, currentEndConnector);
         for (Layer layer : existingVertical.getLayers()) { // should be only 1
            for (VerticalBoundary verticalBoundary : boundariesBetween) {
               layer.addVerticalBoundary(verticalBoundary);
            }
         }
      }
      for (VerticalBoundary existingVertical : existingVerticals) {
         existingVertical.detach();
      }
   }

   private static List<LayerConnector> getConnectorsBetween(LayerConnector upper, LayerConnector lower) {
      List<LayerConnector> connectorsBetween = new ArrayList<>();
      LayerConnector lowerCurrent = upper;
      connectorsBetween.add(lowerCurrent);
      while (lowerCurrent != lower) {
         for (VerticalBoundary layerBoundary : lowerCurrent.getVerticalBoundaries()) {
            if (layerBoundary.getStartConnector().equals(lowerCurrent)) {
               lowerCurrent = layerBoundary.getEndConnector();
               connectorsBetween.add(lowerCurrent);
               break;
            }
         }
      }
      ensureDepthSorted(connectorsBetween);
      return connectorsBetween;
   }

   private static void ensureDepthSorted(List<LayerConnector> connectors) {
      float minDepth = connectors.getFirst().getDepth();
      for (LayerConnector connector : connectors) {
         if (connector.getDepth() < minDepth) {
            connector.setDepth(minDepth);
         }
         minDepth = connector.getDepth();
      }
   }

   private static List<VerticalBoundary> getVerticalBoundariesBetween(LayerConnector upper, LayerConnector lower) throws WorkFileException {
      List<VerticalBoundary> boundariesBetween = new ArrayList<>();
      LayerConnector lowerCurrent = upper;
      while (lowerCurrent != lower) {
         boolean didSomething = false;
         for (VerticalBoundary layerBoundary : lowerCurrent.getVerticalBoundaries()) {
            if (layerBoundary.getStartConnector().equals(lowerCurrent)) {
               lowerCurrent = layerBoundary.getEndConnector();
               boundariesBetween.add(layerBoundary);
               didSomething = true;
               break;
            }
         }
         if (!didSomething) {
            throw new WorkFileException("Found unconnected layer connectors");
         }
      }
      return boundariesBetween;
   }

   private static @Nullable VerticalBoundary findSplitBoundary(List<LayerConnector> connectors, float depth, List<VerticalBoundary> exclusionList) {
      for (int i = 0; i < connectors.size() - 1; i++) {
         LayerConnector connector1 = connectors.get(i);
         LayerConnector connector2 = connectors.get(i + 1);
         if (depth >= connector1.getDepth() && depth <= connector2.getDepth()) {
            for (VerticalBoundary candidate : connector1.getVerticalBoundaries()) {
               //Do not split a boundary in the excluded set of boundaries.
               if (exclusionList.contains(candidate)) {
                  continue;
               }
               for (VerticalBoundary boundary : connector2.getVerticalBoundaries()) {
                  if (boundary == candidate) {
                     return candidate;
                  }
               }
            }
         }
      }
      Log.global.warning("No vertical boundary found");
      return null;
   }
}
