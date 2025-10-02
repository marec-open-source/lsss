package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeSet;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LayerManagerLoader {
   private LayerManagerLoader() {
   }

   static List<Layer> fromXml(Element parentElement, PingRange pingRange, RegionManager regionManager) throws WorkFileException {
      Element element = parentElement.element(LayerManagerSaver.XML_LAYER_INTERPRETATION);

      Map<String, LayerConnector> idToConnector = createConnectors(element, regionManager.getPingContainer(), pingRange.begin());

      Map<String, LayerBoundary> idToBoundary = createBoundaries(element, idToConnector);

      List<Layer> layers = createLayers(element, regionManager, idToBoundary);

      checkLayers(layers, pingRange);

      return layers;
   }

   private static void checkLayers(List<Layer> layers, PingRange pingRange) throws WorkFileException {
      if (layers.isEmpty()) {
         throw new WorkFileException("No layers");
      }
      RangeSet<PingIndex> pingRangeSet = new ArrayRangeSet<>();
      for (Layer layer : layers) {
         String error = LayerManagerUtils.checkLayer(layer);
         if (error != null) {
            throw new WorkFileException(error);
         }
         pingRangeSet.add(layer.getPingRange());
      }

      List<Range<PingIndex>> actualPingRanges = pingRangeSet.stream()
            .toList();
      if (!(actualPingRanges.size() == 1 && actualPingRanges.getFirst().equals(pingRange))) {
         throw new WorkFileException("Expected ping range " + pingRange + ", but got " + actualPingRanges);
      }
   }

   private static Map<String, LayerConnector> createConnectors(Element element, PingContainer pingContainer, PingIndex referencePingIndex) {
      Map<String, LayerConnector> idToConnectorMap = new HashMap<>();
      for (Element connectorElement : element.element(LayerManagerSaver.XML_CONNECTORS).elements()) {
         String id = connectorElement.attributeValue(LayerManagerSaver.XML_ID);
         idToConnectorMap.put(id, new LayerConnector(pingContainer, referencePingIndex, connectorElement));
      }
      return idToConnectorMap;
   }

   private static Map<String, LayerBoundary> createBoundaries(Element element, Map<String, LayerConnector> idToConnector) throws WorkFileException {
      Map<String, LayerBoundary> idToBoundary = new HashMap<>();
      for (Element boundaryElement : element.element(LayerManagerSaver.XML_BOUNDARIES).elements()) {
         String id = boundaryElement.attributeValue(LayerManagerSaver.XML_ID);
         String startConnectorId = boundaryElement.attributeValue(LayerManagerSaver.XML_START_CONNECTOR);
         String endConnectorId = boundaryElement.attributeValue(LayerManagerSaver.XML_END_CONNECTOR);
         LayerConnector startConnector = idToConnector.get(startConnectorId);
         LayerConnector endConnector = idToConnector.get(endConnectorId);
         if (boundaryElement.getName().equals(LayerManagerSaver.XML_CURVE_BOUNDARY)) {
            CurveBoundary curveBoundary = new CurveBoundary(startConnector, endConnector, boundaryElement);
            idToBoundary.put(id, curveBoundary);
         } else if (boundaryElement.getName().equals(LayerManagerSaver.XML_VERTICAL_BOUNDARY)) {
            VerticalBoundary verticalBoundary = new VerticalBoundary(startConnector, endConnector);
            idToBoundary.put(id, verticalBoundary);
         }
      }
      return idToBoundary;
   }

   private static List<Layer> createLayers(Element element, RegionManager regionManager, Map<String, LayerBoundary> idToBoundary) {
      List<Layer> layers = new ArrayList<>();
      for (Element layerElement : element.element(LayerManagerSaver.XML_LAYER_DEFINITIONS).elements()) {
         Layer layer = new Layer(regionManager);
         layer.fromXml(layerElement);
         for (Element boundaryElement : layerElement.element(LayerManagerSaver.XML_BOUNDARIES).elements()) {
            LayerBoundary boundary = idToBoundary.get(boundaryElement.attributeValue(LayerManagerSaver.XML_ID));
            if (boundaryElement.getName().equals(LayerManagerSaver.XML_CURVE_BOUNDARY)) {
               CurveBoundary curveBoundary = (CurveBoundary) boundary;
               if (Boolean.parseBoolean(boundaryElement.attributeValue(LayerManagerSaver.XML_IS_UPPER_BOUNDARY))) {
                  layer.addUpperBoundary(curveBoundary);
               } else {
                  layer.addLowerBoundary(curveBoundary);
               }
            } else if (boundaryElement.getName().equals(LayerManagerSaver.XML_VERTICAL_BOUNDARY)) {
               layer.addVerticalBoundary((VerticalBoundary) boundary);
            }
         }
         layers.add(layer);
      }
      return layers;
   }
}
