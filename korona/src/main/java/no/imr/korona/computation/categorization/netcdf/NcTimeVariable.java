package no.imr.korona.computation.categorization.netcdf;

import no.imr.tools.math.ArrayMath;
import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

final class NcTimeVariable {
   private final Instant referenceTime;
   private final long[] timeValues;

   NcTimeVariable(Variable variable) throws IOException {
      String calendar = variable.findAttributeString(CF.CALENDAR, "");
      if (!calendar.equals("proleptic_gregorian")) {
         throw new IOException("Cannot parse calendar: \"" + calendar + "\"");
      }
      String units = variable.findAttributeString(CF.UNITS, "");
      String nanosecondsSince = "nanoseconds since ";
      String millisecondsSince = "milliseconds since ";
      String referenceTimeAsString;
      long timeValueFactor;
      if (units.startsWith(nanosecondsSince)) {
         referenceTimeAsString = units.substring(nanosecondsSince.length());
         timeValueFactor = 1;
      } else if (units.startsWith(millisecondsSince)) {
         referenceTimeAsString = units.substring(millisecondsSince.length());
         timeValueFactor = 1_000_000;
      } else {
         throw new IOException("Cannot parse units: \"" + units + "\"");
      }
      if (!referenceTimeAsString.endsWith("Z")) {
         referenceTimeAsString += "Z";
      }
      referenceTime = Instant.parse(referenceTimeAsString);
      timeValues = (long[]) variable.read().get1DJavaArray(DataType.LONG);
      if (timeValueFactor != 1) {
         ArrayMath.multiply(timeValues, timeValueFactor);
      }
   }

   int timeToIndex(Instant time) {
      long timeValue = referenceTime.until(time, ChronoUnit.NANOS);
      return Arrays.binarySearch(timeValues, timeValue);
   }
}
