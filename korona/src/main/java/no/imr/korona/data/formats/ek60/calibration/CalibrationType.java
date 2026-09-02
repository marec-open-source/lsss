package no.imr.korona.data.formats.ek60.calibration;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.RangeMap;
import org.dom4j.Element;

import java.time.Instant;

public final class CalibrationType {
   private final RangeMap<Instant, CalibrationEntry> calibrationEntries = new ArrayRangeMap<>();

   public CalibrationType() {
   }

   public RangeMap<Instant, CalibrationEntry> getEntries() {
      return calibrationEntries;
   }

   public CalibrationEntry getEntry(RawFileConfiguration rawFileConfiguration) {
      return getEntry(rawFileConfiguration.getInstant());
   }

   public CalibrationEntry getEntry(Instant time) {
      return calibrationEntries.getOrDefault(time, CalibrationEntry.EMPTY);
   }

   public void putEntry(Instant begin, Instant end, CalibrationEntry entry) {
      calibrationEntries.put(begin, end, entry);
   }

   void addXml(Element element) {
      calibrationEntries.forEach(entry -> {
         Element calibrationElement = element.addElement(CalibrationXml.CALIBRATION)
               .addAttribute(CalibrationXml.BEGIN, !entry.range().begin().equals(Instant.MIN) ? entry.range().begin().toString() : null)
               .addAttribute(CalibrationXml.END, !entry.range().end().equals(Instant.MAX) ? entry.range().end().toString() : null);
         entry.value().addXml(calibrationElement);
      });
   }
}
