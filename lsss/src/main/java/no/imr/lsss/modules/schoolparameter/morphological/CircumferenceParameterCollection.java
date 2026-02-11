package no.imr.lsss.modules.schoolparameter.morphological;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.util.geometry.EchogramUtils;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.korona.region.SchoolBoundaryObject;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;

import java.util.List;
import java.util.Map;

public final class CircumferenceParameterCollection implements MorphologicalParameterCollection {
   private static final SchoolParameter CIRCUMFERENCE = new SchoolParameter(new Name("circumference", "Circumference"), Unit.METER);

   public CircumferenceParameterCollection() {
   }

   @Override
   public List<SchoolParameter> getParameters() {
      return List.of(CIRCUMFERENCE);
   }

   @Override
   public Map<String, Float> computeValues(DataFileSet dataFileSet, RegionManager regionManager, School school) {
      return Map.of(CIRCUMFERENCE.getPersistentName(), computeCircumference(school));
   }

   private static float computeCircumference(School school) {
      double circumference = 0;
      for (SchoolBoundaryObject boundaryObject : school.getBoundaryObjects()) {
         circumference += EchogramUtils.computeCircumference(boundaryObject.getBoundary());
      }
      return (float) circumference;
   }
}
