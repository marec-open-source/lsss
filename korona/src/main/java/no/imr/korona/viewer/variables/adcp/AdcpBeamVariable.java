package no.imr.korona.viewer.variables.adcp;

import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.Utils;
import no.imr.tools.netcdf.NetcdfDataException;
import no.imr.tools.netcdf.NetcdfUtils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import ucar.nc2.Variable;

import java.io.IOException;

final class AdcpBeamVariable extends ContinuousAdcpVariable {
   private final int beamIndex;

   AdcpBeamVariable(String variablePath, ContinuousVariableSettings settings, Unit unit, int beamIndex) {
      super(variablePath, new Name(variablePath + ", beam " + (beamIndex + 1)), settings, unit, ExportTransform.identity());
      this.beamIndex = beamIndex;
   }

   @Override
   float[] evaluate(Variable variable, int timeIndex) throws IOException {
      return switch (variable.getDataType()) {
         case FLOAT -> NetcdfUtils.readVariableLengthFloatArray(variable, timeIndex, beamIndex);
         case INT -> {
            int[] intValues = NetcdfUtils.readVariableLengthIntArray(variable, timeIndex, beamIndex);
            yield Utils.toFloats(intValues);
         }
         default -> throw new NetcdfDataException(variable.getFullName() + ": Unhandled data type: " + variable.getDataType());
      };
   }
}
