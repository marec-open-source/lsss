package no.imr.korona.data.util;

import no.imr.korona.util.KoronaUtils;
import no.marec.lsss.api.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class Nmea {
   private final Type type;
   private final List<String> fields;

   private Nmea(Type type, List<String> fields) {
      this.type = type;
      this.fields = fields;
   }

   public static Nmea of(String line) {
      List<String> fields = toFields(line);
      Type type = Type.getType(fields);
      return new Nmea(type, fields);
   }

   public Type getType() {
      return type;
   }

   public OptionalDouble getMeterPerSec() {
      return type.getMetersPerSecond(fields);
   }

   public Optional<GeoPoint> getGeographicalPosition() {
      return type.getGeographicalPosition(fields);
   }

   public OptionalDouble getHeading() {
      return type.getHeading(fields);
   }

   public OptionalDouble getVesselDistance() {
      return type.getVesselDistance(fields);
   }

   public static List<String> toFields(String line) {
      int beginIndex = line.startsWith("$") ? 1 : 0;
      List<String> fields = new ArrayList<>();
      while (true) {
         int endIndex = line.indexOf(',', beginIndex);
         if (endIndex < 0) {
            endIndex = line.indexOf('*', beginIndex);
            if (endIndex < 0) {
               endIndex = line.length();
            }
            fields.add(line.substring(beginIndex, endIndex));
            break;
         }
         fields.add(line.substring(beginIndex, endIndex));
         beginIndex = endIndex + 1;
      }
      return fields;
   }

   /**
    * The known NMEA types.
    */
   public enum Type {
      /**
       * Global positioning system fix data.
       * <p>
       * Example: $GPGGA,042822,6100.2300,N,00207.0613,E,2,06,01.9,30.7,M,46.1,M,07.0,0685
       */
      GGA {
         @Override
         Optional<GeoPoint> getGeographicalPosition(List<String> fields) {
            return parseGeographicalPosition(fields, 2);
         }
      },

      /**
       * Geographic position - latitude/longitude.
       * <p>
       * Example: $GPGLL,6100.23,N,00207.06,E,042822,A,D
       */
      GLL {
         @Override
         Optional<GeoPoint> getGeographicalPosition(List<String> fields) {
            return parseGeographicalPosition(fields, 1);
         }
      },
      /**
       * Course over ground and ground speed.
       * <p>
       * Example: $GPVTG,090,T,094,M,11.2,N,20.7,K,D
       */
      VTG {
         @Override
         OptionalDouble getMetersPerSecond(List<String> fields) {
            return parseMetersPerSecondFromKnotsField(fields, 5);
         }
      },

      /**
       * Heading.
       * <p>
       * Example: $HEHDT,163.10,T
       */
      HDT {
         @Override
         OptionalDouble getHeading(List<String> fields) {
            return parseDouble(fields, 1);
         }
      },

      /**
       * VLW - Distance Traveled through Water.
       * <p>
       * Example: $SDVLW,5787.261,N,5787.261,N
       */
      VLW {
         @Override
         OptionalDouble getVesselDistance(List<String> fields) {
            return parseDouble(fields, 1);
         }
      },

      UNKNOWN;

      OptionalDouble getMetersPerSecond(List<String> fields) {
         return OptionalDouble.empty();
      }

      Optional<GeoPoint> getGeographicalPosition(List<String> fields) {
         return Optional.empty();
      }

      OptionalDouble getHeading(List<String> fields) {
         return OptionalDouble.empty();
      }

      OptionalDouble getVesselDistance(List<String> fields) {
         return OptionalDouble.empty();
      }

      private static OptionalDouble parseDouble(List<String> fields, int index) {
         if (fields.size() <= index) {
            return OptionalDouble.empty();
         }

         try {
            return OptionalDouble.of(Double.parseDouble(fields.get(index)));
         } catch (NumberFormatException e) {
            return OptionalDouble.empty();
         }
      }

      private static OptionalDouble parseMetersPerSecondFromKnotsField(List<String> fields, int index) {
         if (fields.size() <= index + 1 || !fields.get(index + 1).equals("N")) {
            return OptionalDouble.empty();
         }

         try {
            double knots = Double.parseDouble(fields.get(index));
            double meterPerSeconds = KoronaUtils.knotsToMeterPerSecond(knots);
            return OptionalDouble.of(meterPerSeconds);
         } catch (NumberFormatException e) {
            return OptionalDouble.empty();
         }
      }

      private static Optional<GeoPoint> parseGeographicalPosition(List<String> fields, int index) {
         if (fields.size() <= index + 3) {
            return Optional.empty();
         }

         boolean north;
         String northSouth = fields.get(index + 1);
         switch (northSouth) {
            case "N" -> north = true;
            case "S" -> north = false;
            default -> {
               return Optional.empty();
            }
         }

         boolean east;
         String eastWest = fields.get(index + 3);
         switch (eastWest) {
            case "E" -> east = true;
            case "W" -> east = false;
            default -> {
               return Optional.empty();
            }
         }

         try {
            double lat = parseDegrees(fields.get(index), 2);
            if (!north) {
               lat = -lat;
            }
            double lon = parseDegrees(fields.get(index + 2), 3);
            if (!east) {
               lon = -lon;
            }
            return Optional.of(new GeoPoint(lon, lat));
         } catch (NumberFormatException e) {
            return Optional.empty();
         }
      }

      private static double parseDegrees(String field, int digitsForDegree) {
         if (field.length() < digitsForDegree) {
            throw new NumberFormatException();
         }
         double degrees = Double.parseDouble(field.substring(0, digitsForDegree));
         double minutes = Double.parseDouble(field.substring(digitsForDegree));
         return degrees + minutes / 60;
      }

      private static Type getType(List<String> fields) {
         if (fields.isEmpty()) {
            return UNKNOWN;
         }

         try {
            if (fields.getFirst().length() != 5) {
               return UNKNOWN;
            }
            return valueOf(fields.getFirst().substring(2));
         } catch (IllegalArgumentException e) {
            return UNKNOWN;
         }
      }
   }
}
