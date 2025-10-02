package no.imr.tools.parameter;

public record Unit(String text, String formalName) {

   public static final Unit NONE = new Unit("");
   public static final Unit DIMENSIONLESS = new Unit("‒", "");

   public static final Unit CENTIMETER = new Unit("cm");
   public static final Unit COUNT = new Unit("#", "1");
   public static final Unit DB = new Unit("dB");
   public static final Unit DB_PER_METER = new Unit("dB/m");
   public static final Unit DEGREES = new Unit("deg", "degree");
   public static final Unit FRACTION = new Unit("1");
   public static final Unit GIGABYTES = new Unit("GB");
   public static final Unit HZ = new Unit("Hz");
   public static final Unit KHZ = new Unit("kHz");
   public static final Unit KILOGRAM = new Unit("kg");
   public static final Unit KNOTS = new Unit("knots");
   public static final Unit MEGABYTES = new Unit("MB");
   public static final Unit METER = new Unit("m");
   public static final Unit METER_2 = new Unit("m²", "m^2");
   public static final Unit METER_3 = new Unit("m³", "m^3");
   public static final Unit METER_PER_SECOND = new Unit("m/s");
   public static final Unit MILLIMETER = new Unit("mm");
   public static final Unit MILLISECONDS = new Unit("ms");
   public static final Unit MINUTES = new Unit("minutes");
   public static final Unit NAUTICAL_MILES = new Unit("nmi", "nautical_mile");
   public static final Unit OHM = new Unit("Ω", "ohm");
   public static final Unit PERCENT = new Unit("%");
   public static final Unit PT = new Unit("pt");
   public static final Unit SA = new Unit("m²/nmi²", "m^2/nautical_mile^2");
   public static final Unit SECONDS = new Unit("s");
   public static final Unit SECONDS_SINCE_EPOCH = new Unit("seconds since 1970-01-01T00:00:00Z");
   public static final Unit SV = new Unit("4π 1852² m²/m³", "4 pi 1852^2 m^2/m^3");
   public static final Unit UTC = new Unit("UTC");
   public static final Unit WATT = new Unit("W");

   public Unit(String text) {
      this(text, text);
   }

   @Override
   public String toString() {
      return text;
   }
}
