package no.imr.korona.data.formats.ek60;

import no.imr.tools.time.TimeUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * One line in a xyz-file.
 * <p>
 * Three versions according to Sverre Berg (email 14.01.2021):
 * <p>
 * Ver 1: {@code AAAA.AAAAAAA BBBB.BBBBBBB d.dd ddMMyyyy HHmmss.ff t.ttt}
 * <ul>
 *    <li>Latitude and longitude are signed and multiplied by 100</li>
 * </ul>
 * <br>
 * Ver 2: {@code AA.AAAAAAA BB.BBBBBBB d.dd ddMMyyyy HHmmss.ff t.ttt}
 * <ul>
 *    <li>Latitude and longitude are signed and NOT multiplied by 100</li>
 * </ul>
 * <br>
 * Ver3: {@code AA.AAAAAAA X BB.BBBBBBB Y d.dd ddMMyyyy HHmmss.ff t.ttt}
 * <ul>
 *    <li>{@code AA.AAAAAAA}: Latitude in degrees (with its decimals)</li>
 *    <li>{@code X}: N or S</li>
 *    <li>{@code BB.BBBBBBB}: Longitude in degrees (with its decimals)</li>
 *    <li>{@code Y}: W or E</li>
 *    <li>{@code d.dd}: Depth below surface [m]</li>
 *    <li>{@code ddMMyyyy}: Date (day, month, year)</li>
 *    <li>{@code HHmmss.ff}: Time</li>
 *    <li>{@code t.ttt}: Transducer offset [m]</li>
 * </ul>
 * <br>
 * <p>
 *    "Min anbefaling er å se på tallverdien og vurder om dette er lagret med denne multiplikasjonsfaktoren og/eller se på SW versjon i den tilhørende raw filen."
 * </p>
 *
 * @see <a href="https://www.simrad.online/ek80/interface/ek80_interface_en_a4.pdf">https://www.simrad.online/ek80/interface/ek80_interface_en_a4.pdf</a>
 */
final class XyzLine {
   private static final DateTimeFormatter DATE_FORMATTER = TimeUtils.createUTCDateTimeFormatter("ddMMyyyy");
   private static final DateTimeFormatter TIME_FORMATTER = TimeUtils.createUTCDateTimeFormatter("HHmmss.SS");

   final double latitude;
   final double longitude;
   final float depth;
   final Instant instant;
   final float transducerOffset;

   XyzLine(String line) {
      String[] parts = line.split(" "); // Sverre Berg confirmed in email 18.12.2020 that the separator is always space.
      int i;
      if (parts.length >= 8 && "NS".contains(parts[1])) { // Version 3
         latitude = Double.parseDouble(parts[0]) * (parts[1].equals("S") ? -1 : 1);
         longitude = Double.parseDouble(parts[2]) * (parts[3].equals("W") ? -1 : 1);
         i = 4;
      } else {
         double lat = Double.parseDouble(parts[0]);
         double lon = Double.parseDouble(parts[1]);
         if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
            lat /= 100;
            lon /= 100;
         }
         latitude = lat;
         longitude = lon;
         i = 2;
      }
      depth = Float.parseFloat(parts[i++]);
      LocalDate localDate = DATE_FORMATTER.parse(parts[i++], LocalDate::from);
      LocalTime localTime = TIME_FORMATTER.parse(parts[i++], LocalTime::from);
      instant = localDate.atTime(localTime).toInstant(ZoneOffset.UTC);
      transducerOffset = Float.parseFloat(parts[i]);
   }

   @Override
   public String toString() {
      return "time: " + instant + ", depth: " + depth;
   }
}
