package no.imr.korona.data.ping;

import no.imr.tools.range.DoubleRange;
import no.imr.tools.time.NTDate;

/**
 * Different horizontal mappings of pings.
 */
public enum PingMapping {
   /**
    * Mapping based on ping number.
    */
   NUMBER("Ping", "pings") {
      @Override
      public double valueOf(PingMappingArgument a) {
         return a.getPingNumber();
      }

      @Override
      public double distance(PingMappingArgument a, PingMappingArgument b) {
         return b.getPingNumber() - a.getPingNumber();
      }
   },

   /**
    * Mapping based on vessel distance.
    */
   DISTANCE("Distance", "nmi") {
      @Override
      public double valueOf(PingMappingArgument a) {
         return a.getVesselDistance();
      }

      @Override
      public double distance(PingMappingArgument a, PingMappingArgument b) {
         return b.getVesselDistance() - a.getVesselDistance();
      }
   },

   /**
    * Mapping based on time. Use
    * {@link #millisToTimeValue(long)}
    * or
    * {@link #ntDateToTimeValue(long)}
    * for correct time format.
    */
   TIME("Time", "seconds") {
      @Override
      public double valueOf(PingMappingArgument a) {
         return (double) a.getTimeInMillis() / 1000.0;
      }

      @Override
      public double distance(PingMappingArgument a, PingMappingArgument b) {
         return (double) (b.getTimeInMillis() - a.getTimeInMillis()) / 1000.0;
      }
   };

   /**
    * Returns the value of a time according to {@link #TIME}.
    *
    * @param millis a time in milliseconds
    * @return the value according to {@link #TIME}
    */
   public static double millisToTimeValue(long millis) {
      return (double) millis / 1000.0;
   }

   public static long timeValueToMillis(double timeValue) {
      return (long) (timeValue * 1000);
   }

   /**
    * Returns the value of a time according to {@link #TIME}.
    *
    * @param ntDate a time in NT date format
    * @return the value according to {@link #TIME}
    */
   public static double ntDateToTimeValue(long ntDate) {
      return millisToTimeValue(NTDate.ntDateToTimeInMillis(ntDate));
   }

   public static long timeValueToNTDate(double timeValue) {
      return NTDate.timeInMillisToNTDate(timeValueToMillis(timeValue));
   }

   private final String name;
   private final String unit;

   PingMapping(String name, String unit) {
      this.name = name;
      this.unit = unit;
   }

   /**
    * Returns the unit of this PingMapping.
    *
    * @return "pings", "nmi" or "seconds"
    */
   public String getUnitString() {
      return unit;
   }

   /**
    * Returns the value of a ping index according to this PingMapping.
    *
    * @param a a ping index
    * @return the value of a
    */
   public abstract double valueOf(PingMappingArgument a);

   /**
    * Returns the distance between two {@link PingMappingArgument}s.
    * It holds that {@code distance(a, b) == -distance(b, a)}.
    *
    * @param a first argument
    * @param b second argument
    * @return the distance from a to b
    */
   public abstract double distance(PingMappingArgument a, PingMappingArgument b);

   /**
    * Returns the distance from the first to the last ping in a PingRange according to this PingMapping.
    *
    * @param pingRange a PingRange
    * @return the size of the PingRange
    */
   public double distance(PingRange pingRange) {
      return distance(pingRange.begin(), pingRange.end());
   }

   public DoubleRange toValueRange(PingRange pingRange) {
      if (pingRange.isEmpty()) {
         return DoubleRange.of(0, 0);
      }
      return DoubleRange.of(valueOf(pingRange.begin()), valueOf(pingRange.end()));
   }

   @Override
   public String toString() {
      return name;
   }
}
