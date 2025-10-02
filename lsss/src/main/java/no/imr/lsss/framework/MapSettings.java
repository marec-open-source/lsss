package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.lsss.LSSS;
import no.imr.tools.geo.GeoBoxBuilder;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.ChangeManager;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MapSettings {
   private final ArgChangeManager<MapSelection> mapSelectionChangeManager = new ArgChangeManager<>();

   private final Map<Object, Rectangle2D> boundingBoxes = new ConcurrentHashMap<>();
   private final ChangeManager boundingBoxesChangeManager = new ChangeManager();
   private final ExtendedSurveyLine extendedSurveyLine;

   MapSettings(LSSS lsss) {
      extendedSurveyLine = new ExtendedSurveyLine(lsss);
   }

   void setup(LSSS lsss) {
      RegionManager regionManager = lsss.getRegionManager();
      mapSelectionChangeManager.addListener(mapSelection -> {
         List<Region> regions = regionManager.getGeoIntersectingRegions(mapSelection.geoRect());
         regionManager.doSelection(regions, mapSelection.action());
      });
   }

   public ArgChangeManager<MapSelection> getMapSelectionChangeManager() {
      return mapSelectionChangeManager;
   }

   public void doMapSelection(MapSelection mapSelection) {
      mapSelectionChangeManager.notifyListeners(mapSelection);
   }

   public ChangeManager getBoundingBoxesChangeManager() {
      return boundingBoxesChangeManager;
   }

   public void setBoundingBox(Object key, DataFileSet dataFileSet) {
      setBoundingBox(key, DataUtils.getGeographicalBoundingBox(dataFileSet.getPingIndices()));
   }

   private void setBoundingBox(Object key, @Nullable Rectangle2D boundingBox) {
      if (boundingBox == null) {
         boundingBoxes.remove(key);
      } else {
         boundingBoxes.put(key, boundingBox);
      }
      boundingBoxesChangeManager.notifyListeners();
   }

   public @Nullable Rectangle2D getGeographicalBoundingBox() {
      GeoBoxBuilder geoBoxBuilder = new GeoBoxBuilder();
      boundingBoxes.values().forEach(geoBoxBuilder::add);
      return geoBoxBuilder.build();
   }

   public ExtendedSurveyLine getExtendedSurveyLine() {
      return extendedSurveyLine;
   }
}
