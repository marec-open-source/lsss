package no.imr.korona.computation.categorization.netcdf;

import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcTimeDef;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

final class NcTimeVariable {
   private final Instant referenceTime;
   private final long[] timeValues;

   NcTimeVariable(Variable variable) throws IOException {
      NcTimeDef ncTimeDef = NcTimeDef.fromVariable(variable);
      referenceTime = ncTimeDef.referenceTime();
      timeValues = (long[]) variable.read().get1DJavaArray(DataType.LONG);
      if (ncTimeDef.timeValueToNanosFactor() != 1) {
         ArrayMath.multiply(timeValues, ncTimeDef.timeValueToNanosFactor());
      }
   }

   int timeToIndex(Instant time) {
      long timeValue = referenceTime.until(time, ChronoUnit.NANOS);
      return Arrays.binarySearch(timeValues, timeValue);
   }
}
