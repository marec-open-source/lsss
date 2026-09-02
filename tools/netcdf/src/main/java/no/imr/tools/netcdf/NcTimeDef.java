package no.imr.tools.netcdf;

import ucar.nc2.Variable;
import ucar.nc2.constants.CF;

import java.time.Instant;

public record NcTimeDef(
      Instant referenceTime,
      long timeValueToNanosFactor
) {
   public static NcTimeDef fromVariable(Variable variable) throws NetcdfDataException {
      String calendar = variable.findAttributeString(CF.CALENDAR, "");
      if (!calendar.equals("proleptic_gregorian")) {
         throw new NetcdfDataException("Cannot parse calendar: \"" + calendar + "\"");
      }
      String units = variable.findAttributeString(CF.UNITS, "");
      String nanosecondsSince = "nanoseconds since ";
      String millisecondsSince = "milliseconds since ";
      String referenceTimeAsString;
      long timeValueToNanosFactor;
      if (units.startsWith(nanosecondsSince)) {
         referenceTimeAsString = units.substring(nanosecondsSince.length());
         timeValueToNanosFactor = 1;
      } else if (units.startsWith(millisecondsSince)) {
         referenceTimeAsString = units.substring(millisecondsSince.length());
         timeValueToNanosFactor = 1_000_000;
      } else {
         throw new NetcdfDataException("Cannot parse units: \"" + units + "\"");
      }
      if (!referenceTimeAsString.endsWith("Z")) {
         referenceTimeAsString += "Z";
      }
      Instant referenceTime = Instant.parse(referenceTimeAsString);
      return new NcTimeDef(referenceTime, timeValueToNanosFactor);
   }

   public Instant timeValueToInstant(long timeValue) {
      return referenceTime.plusNanos(Math.multiplyExact(timeValue, timeValueToNanosFactor));
   }
}
