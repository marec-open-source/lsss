package no.imr.korona.region;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.EchogramPoint;
import org.jspecify.annotations.Nullable;

final class LayerManagerUtils {
   private LayerManagerUtils() {
   }

   private static String toErrorString(EchogramPoint point) {
      PingIndex pingIndex = point.pingIndex();
      return pingIndex.getInstant() + " (ping number: " + pingIndex.getPingNumber() + ", depth: " + point.depth() + ")";
   }

   static @Nullable String checkLayer(Layer layer) {
      String error;
      error = checkConnectors(layer);
      if (error != null) {
         return error;
      }
      error = checkBoundaries(layer);
      return error;
   }

   private static @Nullable String checkConnectors(Layer layer) {
      // Check that connectors are connected to at most 2 curve boundaries and 2 vertical boundaries.
      for (LayerConnector layerConnector : layer.getConnectors()) {
         int numberOfCurveBoundaries = layerConnector.getCurveBoundaries().size();
         int numberOfVerticalBoundaries = layerConnector.getVerticalBoundaries().size();
         if (numberOfCurveBoundaries > 2 || numberOfVerticalBoundaries > 2) {
            return "Layer connector at " + toErrorString(layerConnector.getPoint()) + " is connected to more than 2 boundaries of the same type";
         }
      }
      return null;
   }

   private static @Nullable String checkBoundaries(Layer layer) {
      for (CurveBoundary curveBoundary : layer.getCurveBoundaries()) {
         if (!isConnected(curveBoundary, curveBoundary.getStartConnector(), layer)) {
            return "Start point connector at " + toErrorString(curveBoundary.getStartConnector().getPoint()) + " is not connected";
         }
         if (!isConnected(curveBoundary, curveBoundary.getEndConnector(), layer)) {
            return "End point connector at " + toErrorString(curveBoundary.getEndConnector().getPoint()) + " is not connected";
         }
      }
      for (VerticalBoundary verticalBoundary : layer.getVerticalBoundaries()) {
         if (!isConnected(verticalBoundary, verticalBoundary.getStartConnector(), layer)) {
            return "Start point connector at " + toErrorString(verticalBoundary.getStartConnector().getPoint()) + " is not connected";
         }
         if (!isConnected(verticalBoundary, verticalBoundary.getEndConnector(), layer)) {
            return "End point connector at " + toErrorString(verticalBoundary.getEndConnector().getPoint()) + " is not connected";
         }
      }
      return null;
   }

   static boolean isConnected(LayerBoundary boundary, LayerConnector connector, Layer layer) {
      for (VerticalBoundary verticalBoundary : connector.getVerticalBoundaries()) {
         if (verticalBoundary.equals(boundary)) {
            continue;
         }
         if (layer.getVerticalBoundaries().contains(verticalBoundary)) {
            return true;
         }
      }
      for (CurveBoundary curveBoundary : connector.getCurveBoundaries()) {
         if (curveBoundary.equals(boundary)) {
            continue;
         }
         if (layer.getUpperCurveBoundaries().contains(curveBoundary) || layer.getLowerCurveBoundaries().contains(curveBoundary)) {
            return true;
         }
      }
      return false;
   }
}
