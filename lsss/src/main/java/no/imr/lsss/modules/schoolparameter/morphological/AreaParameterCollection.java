package no.imr.lsss.modules.schoolparameter.morphological;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.Map;

public final class AreaParameterCollection implements MorphologicalParameterCollection {
   public static final SchoolParameter AREA = new SchoolParameter(new Name("area", "Area"), Unit.METER_2);

   public AreaParameterCollection() {
   }

   @Override
   public List<SchoolParameter> getParameters() {
      return List.of(AREA);
   }

   @Override
   public Map<String, Float> computeValues(DataFileSet dataFileSet, RegionManager regionManager, School school) {
      return Map.of(AREA.getPersistentName(), computeArea(dataFileSet, regionManager, school));
   }

   private static float computeArea(DataFileSet dataFileSet, RegionManager regionManager, School school) {
      double area = 0;
      for (PingIndex pingIndex : dataFileSet.getPingIndices(school.getPingRange())) {
         double pingWidthMeters = DataUtils.getPingWidthMeters(dataFileSet, pingIndex);
         double depths = 0;
         for (FloatRange floatRange : regionManager.getNonMaskedRegionDepthRanges(school, pingIndex)) {
            depths += floatRange.getSize();
         }
         area += pingWidthMeters * depths;
      }
      return (float) area;
   }
}
