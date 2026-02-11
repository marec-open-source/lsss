package no.imr.korona.data.util;

import no.marec.lsss.api.util.GeoPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

final class NmeaTest {
   @Test
   void toFields() {
      List<String> expected = List.of("aa", "bb", "cc");
      assertEquals(expected, Nmea.toFields("aa,bb,cc"));
      assertEquals(expected, Nmea.toFields("$aa,bb,cc"));
      assertEquals(expected, Nmea.toFields("aa,bb,cc*00"));
      assertEquals(expected, Nmea.toFields("$aa,bb,cc*00"));
   }

   @Test
   void speedVGT() {
      Nmea nmea = Nmea.of("$GPVTG,253,T,250,M,007.5,N,014.0,K");
      assertEquals(Nmea.Type.VTG, nmea.getType());
      assertEquals(OptionalDouble.of(7.5), nmea.getKnots());
   }

   @Test
   void geoPosGGA() {
      Nmea nmea = Nmea.of("$GPGGA,042822,6100.2300,N,00207.0613,E,2,06,01.9,30.7,M,46.1,M,07.0,0685");
      assertEquals(Nmea.Type.GGA, nmea.getType());
      assertEquals(Optional.of(new GeoPoint(2 + 7.0613 / 60, 61 + 0.23 / 60)), nmea.getGeographicalPosition());
   }

   @Test
   void geoPosGLL() {
      Nmea nmea = Nmea.of("$GPGLL,6100.23,N,00207.06,E,042822,A,D");
      assertEquals(Nmea.Type.GLL, nmea.getType());
      assertEquals(Optional.of(new GeoPoint(2 + 7.06 / 60, 61 + 0.23 / 60)), nmea.getGeographicalPosition());
   }

   @Test
   void headingHDT() {
      Nmea nmea = Nmea.of("$HEHDT,163.10,T");
      assertEquals(Nmea.Type.HDT, nmea.getType());
      assertEquals(OptionalDouble.of(163.10), nmea.getHeading());
   }
}
