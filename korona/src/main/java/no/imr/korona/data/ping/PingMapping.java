package no.imr.korona.data.ping;

import no.imr.tools.range.DoubleRange;
import no.imr.tools.time.TimeUtils;

import java.time.Instant;

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
    * Mapping based on time in unit of seconds.
    */
   TIME("Time", "seconds") {
      @Override
      public double valueOf(PingMappingArgument a) {
         return instantToTimeValue(a.getInstant());
      }

      @Override
      public double distance(PingMappingArgument a, PingMappingArgument b) {
         return TimeUtils.toSeconds(a.getInstant(), b.getInstant());
      }
   };

   public static double instantToTimeValue(Instant instant) {
      return instant.getEpochSecond() + instant.getNano() / 1e9;
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
