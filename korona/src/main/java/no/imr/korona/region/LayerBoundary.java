package no.imr.korona.region;

import no.imr.korona.data.util.geometry.EchogramPoint;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract sealed class LayerBoundary permits CurveBoundary, VerticalBoundary {
   //Using copy-on-write list to avoid concurrent modification exceptions.
   private final List<Layer> layers = new CopyOnWriteArrayList<>();

   private LayerConnector startConnector;
   private LayerConnector endConnector;

   LayerBoundary(LayerConnector startConnector, LayerConnector endConnector) {
      this.startConnector = startConnector;
      this.endConnector = endConnector;

      startConnector.addBoundary(this);
      endConnector.addBoundary(this);
   }

   public LayerConnector getStartConnector() {
      return startConnector;
   }

   void setStartConnector(LayerConnector startConnector) {
      this.startConnector = startConnector;
   }

   public LayerConnector getEndConnector() {
      return endConnector;
   }

   void setEndConnector(LayerConnector endConnector) {
      this.endConnector = endConnector;
   }

   void addLayer(Layer layer) {
      layers.add(layer);
   }

   void removeLayer(Layer layer) {
      layers.remove(layer);

      if (layers.isEmpty()) {
         startConnector.removeBoundary(this);
         endConnector.removeBoundary(this);
      }
   }

   void detach() {
      for (Layer layer : layers) {
         layer.removeBoundary(this);
      }
      layers.clear();
      startConnector.removeBoundary(this);
      endConnector.removeBoundary(this);
   }

   public List<Layer> getLayers() {
      return layers;
   }

   abstract LayerBoundaryAndConnectorPair<?> split(EchogramPoint echogramPoint);

   void getBoundariesConnectedToLayer(List<LayerBoundary> layerBoundaries, Layer layer, LayerConnector firstConnector,
                                      LayerConnector stopConnector) {
      if (firstConnector.equals(stopConnector)) {
         return;
      }
      for (LayerBoundary layerBoundary : firstConnector.getBoundaries()) {
         if (!layerBoundary.equals(this) && layerBoundary.getLayers().contains(layer)) {
            if (layerBoundaries.contains(layerBoundary)) {
               //We are back at the start point.
               layerBoundaries.add(layerBoundary);
               return;
            }
            layerBoundaries.add(layerBoundary);
            if (layerBoundary.getStartConnector().equals(firstConnector)) {
               layerBoundary.getBoundariesConnectedToLayer(layerBoundaries, layer, layerBoundary.getEndConnector(), stopConnector);
            } else if (layerBoundary.getEndConnector().equals(firstConnector)) {
               layerBoundary.getBoundariesConnectedToLayer(layerBoundaries, layer, layerBoundary.getStartConnector(), stopConnector);
            }
         }
      }
   }
}
