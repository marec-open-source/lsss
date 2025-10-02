package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;
import no.marec.lsss.api.util.GeoPoint;

import java.util.List;

public class GeographicalPositionSchoolParameter extends PointSchoolParameter<GeoPoint> {
   public GeographicalPositionSchoolParameter(Name name) {
      super(name, "°", "%.6f");
   }

   @Override
   GeoPoint createValue(double x, double y) {
      return new GeoPoint(x, y);
   }

   protected String getExportNamePrefix() {
      return getName().persistentName();
   }

   @Override
   public List<String> getExportNames() {
      String prefix = getExportNamePrefix();
      return List.of(prefix + ".lon", prefix + ".lat");
   }

   @Override
   public int getMinExportWidth() {
      return 10;
   }
}
