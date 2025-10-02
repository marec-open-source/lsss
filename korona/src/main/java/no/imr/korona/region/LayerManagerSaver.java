package no.imr.korona.region;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

final class LayerManagerSaver {
   static final String XML_LAYER_INTERPRETATION = "layerInterpretation";
   static final String XML_BOUNDARIES = "boundaries";
   static final String XML_CONNECTORS = "connectors";
   static final String XML_CURVE_BOUNDARY = "curveBoundary";
   static final String XML_ID = "id";
   static final String XML_VERTICAL_BOUNDARY = "verticalBoundary";
   private static final String XML_CONNECTOR = "connector";
   static final String XML_START_CONNECTOR = "startConnector";
   static final String XML_END_CONNECTOR = "endConnector";
   static final String XML_LAYER_DEFINITIONS = "layerDefinitions";
   static final String XML_IS_UPPER_BOUNDARY = "isUpper";

   private final PingRange pingRange;
   private final VirtualDividerManager startVirtualDividerManager;
   private final VirtualDividerManager endVirtualDividerManager;
   private final PingIndex referencePingIndex;

   private int boundaryId = 0;
   private final Map<LayerBoundary, String> boundaryToId = new HashMap<>();
   private final Map<VirtualVerticalBoundary, String> virtualBoundaryToId = new HashMap<>();

   private int connectorId = 0;
   private final Map<LayerConnector, String> connectorToId = new HashMap<>();

   private final Element interpretation = DocumentHelper.createElement(XML_LAYER_INTERPRETATION);
   private final Element boundaryDefs = interpretation.addElement(XML_BOUNDARIES);
   private final Element connectorDefs = interpretation.addElement(XML_CONNECTORS);

   private LayerManagerSaver(PingRange pingRange) {
      this.pingRange = pingRange;
      startVirtualDividerManager = new VirtualDividerManager(pingRange, true);
      endVirtualDividerManager = new VirtualDividerManager(pingRange, false);
      referencePingIndex = pingRange.begin();
   }

   static Element toXml(PingRange pingRange, List<Layer> layers) {
      LayerManagerSaver saver = new LayerManagerSaver(pingRange);
      return saver.toXml(layers);
   }

   private Element toXml(List<Layer> layers) {
      layers.sort(Comparator.comparingInt(Layer::getObjectNumber));

      Element layerDefs = interpretation.addElement(XML_LAYER_DEFINITIONS);

      for (Layer layer : layers) {
         Element layerElement = layer.toXml();
         layerDefs.add(layerElement);

         Element boundaries = layerElement.addElement(XML_BOUNDARIES);

         // Update virtual before adding boundaries
         VirtualVerticalBoundary startVirtualVerticalBoundary = startVirtualDividerManager.updateVirtualDividers(layer);
         VirtualVerticalBoundary endVirtualVerticalBoundary = endVirtualDividerManager.updateVirtualDividers(layer);

         intersectingCurveBoundaries(layer.getUpperCurveBoundaries()).forEach(upperBoundary -> {
            boundaries.addElement(XML_CURVE_BOUNDARY)
                  .addAttribute(XML_ID, addCurveBoundary(upperBoundary))
                  .addAttribute(XML_IS_UPPER_BOUNDARY, "true");
         });
         intersectingCurveBoundaries(layer.getLowerCurveBoundaries()).forEach(lowerBoundary -> {
            boundaries.addElement(XML_CURVE_BOUNDARY)
                  .addAttribute(XML_ID, addCurveBoundary(lowerBoundary))
                  .addAttribute(XML_IS_UPPER_BOUNDARY, "false");
         });

         if (startVirtualVerticalBoundary != null) {
            boundaries.addElement(XML_VERTICAL_BOUNDARY)
                  .addAttribute(XML_ID, addVerticalBoundary(startVirtualVerticalBoundary));
         }

         layer.getVerticalBoundaries().stream()
               .filter(verticalBoundary -> pingRange.containsExcludingBegin(verticalBoundary.getPingIndex()))
               .sorted(verticalBoundaryComparator())
               .forEach(verticalBoundary -> {
                  boundaries.addElement(XML_VERTICAL_BOUNDARY)
                        .addAttribute(XML_ID, addVerticalBoundary(verticalBoundary));
               });

         if (endVirtualVerticalBoundary != null) {
            boundaries.addElement(XML_VERTICAL_BOUNDARY)
                  .addAttribute(XML_ID, addVerticalBoundary(endVirtualVerticalBoundary));
         }
      }

      return interpretation;
   }

   private Stream<CurveBoundary> intersectingCurveBoundaries(List<CurveBoundary> curveBoundaries) {
      return curveBoundaries.stream()
            .filter(curveBoundary -> pingRange.intersects(curveBoundary.getPingRange()))
            .sorted(Comparator.comparing(curveBoundary -> curveBoundary.getPingRange().begin()));
   }

   private String addCurveBoundary(CurveBoundary curveBoundary) {
      String id = boundaryToId.get(curveBoundary);
      if (id != null) {
         return id;
      }
      id = Integer.toString(boundaryId++);
      boundaryToId.put(curveBoundary, id);
      Element curveElement = boundaryDefs.addElement(XML_CURVE_BOUNDARY)
            .addAttribute(XML_ID, id)
            .addAttribute(XML_START_CONNECTOR, addConnector(startVirtualDividerManager.getRealOrVirtualConnector(curveBoundary)))
            .addAttribute(XML_END_CONNECTOR, addConnector(endVirtualDividerManager.getRealOrVirtualConnector(curveBoundary)));
      curveElement.add(curveBoundary.toXml(pingRange));
      return id;
   }

   private String addVerticalBoundary(VerticalBoundary verticalBoundary) {
      String id = boundaryToId.get(verticalBoundary);
      if (id != null) {
         return id;
      }
      id = Integer.toString(boundaryId++);
      boundaryToId.put(verticalBoundary, id);
      boundaryDefs.addElement(XML_VERTICAL_BOUNDARY)
            .addAttribute(XML_ID, id)
            .addAttribute(XML_START_CONNECTOR, addConnector(verticalBoundary.getStartConnector()))
            .addAttribute(XML_END_CONNECTOR, addConnector(verticalBoundary.getEndConnector()));
      return id;
   }

   private String addVerticalBoundary(VirtualVerticalBoundary virtualVerticalBoundary) {
      String id = virtualBoundaryToId.get(virtualVerticalBoundary);
      if (id != null) {
         return id;
      }
      id = Integer.toString(boundaryId++);
      virtualBoundaryToId.put(virtualVerticalBoundary, id);
      boundaryDefs.addElement(XML_VERTICAL_BOUNDARY)
            .addAttribute(XML_ID, id)
            .addAttribute(XML_START_CONNECTOR, addConnector(virtualVerticalBoundary.startConnector))
            .addAttribute(XML_END_CONNECTOR, addConnector(virtualVerticalBoundary.endConnector));
      return id;
   }

   private String addConnector(LayerConnector layerConnector) {
      String id = connectorToId.get(layerConnector);
      if (id != null) {
         return id;
      }
      id = Integer.toString(connectorId++);
      connectorToId.put(layerConnector, id);
      Element connectorElement = connectorDefs.addElement(XML_CONNECTOR)
            .addAttribute(XML_ID, id);
      connectorElement.add(layerConnector.toXml(referencePingIndex));
      return id;
   }

   private static Comparator<VerticalBoundary> verticalBoundaryComparator() {
      return (o1, o2) -> {
         if (o1.equals(o2)) {
            return 0;
         }
         int pingIndexComparison = o1.getPingIndex().compareTo(o2.getPingIndex());
         if (pingIndexComparison != 0) {
            return pingIndexComparison;
         }
         int minDepthComparison = Float.compare(o1.getMinDepth(), o2.getMinDepth());
         if (minDepthComparison != 0) {
            return minDepthComparison;
         }
         int maxDepthComparison = Float.compare(o1.getMaxDepth(), o2.getMaxDepth());
         if (maxDepthComparison != 0) {
            return maxDepthComparison;
         }
         return o1.isAbove(o2) ? -1 : 1;
      };
   }

   private static final class VirtualDividerManager {
      private final PingRange pingRange;
      private final boolean start;
      private final PingIndex pingIndex;
      private final Map<CurveBoundary, LayerConnector> virtualConnectors = new HashMap<>();

      private VirtualDividerManager(PingRange pingRange, boolean start) {
         this.pingRange = pingRange;
         this.start = start;
         pingIndex = start ? pingRange.begin() : pingRange.end();
      }

      private @Nullable VirtualVerticalBoundary updateVirtualDividers(Layer layer) {
         LayerConnector upper = findCurveBoundary(layer.getUpperCurveBoundaries());
         LayerConnector lower = findCurveBoundary(layer.getLowerCurveBoundaries());
         if (upper == null || lower == null) {
            return null;
         }
         return new VirtualVerticalBoundary(upper, lower);
      }

      private @Nullable LayerConnector findCurveBoundary(List<CurveBoundary> curveBoundaries) {
         return curveBoundaries.stream()
               .filter(boundary -> {
                  PingRange range = boundary.getPingRange();
                  return pingRange.intersects(range) && range.containsIncludingEnd(pingIndex);
               })
               .findFirst()
               .map(this::findConnector)
               .orElse(null);
      }

      private LayerConnector findConnector(CurveBoundary curveBoundary) {
         LayerConnector virtualConnector = virtualConnectors.get(curveBoundary);
         if (virtualConnector != null) {
            return virtualConnector;
         }
         int index = curveBoundary.getCurve().getIndex(pingIndex);
         float depth = curveBoundary.getCurve().getDepths()[start ? index : index - 1];
         virtualConnector = new LayerConnector(new EchogramPoint(pingIndex, depth));
         virtualConnectors.put(curveBoundary, virtualConnector);
         return virtualConnector;
      }

      private LayerConnector getRealOrVirtualConnector(CurveBoundary curveBoundary) {
         LayerConnector connector = virtualConnectors.get(curveBoundary);
         if (connector != null) {
            return connector;
         }
         return start ? curveBoundary.getStartConnector() : curveBoundary.getEndConnector();
      }
   }

   private record VirtualVerticalBoundary(LayerConnector startConnector, LayerConnector endConnector) {
   }
}
