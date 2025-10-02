package no.imr.korona.data.formats.ek60.calibration;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.RangeMap;
import org.dom4j.Element;

import java.time.Instant;

public final class CalibrationType {
   private final RangeMap<Long, CalibrationEntry> calibrationEntries = new ArrayRangeMap<>();

   public CalibrationType() {
   }

   public RangeMap<Long, CalibrationEntry> getEntries() {
      return calibrationEntries;
   }

   public CalibrationEntry getEntry(RawFileConfiguration rawFileConfiguration) {
      return getEntry(rawFileConfiguration.getTimeInMillis());
   }

   public CalibrationEntry getEntry(long timeInMillis) {
      return calibrationEntries.getOrDefault(timeInMillis, CalibrationEntry.EMPTY);
   }

   public void putEntry(long begin, long end, CalibrationEntry entry) {
      calibrationEntries.put(begin, end, entry);
   }

   void addXml(Element element) {
      calibrationEntries.forEach(entry -> {
         Element calibrationElement = element.addElement(CalibrationXml.CALIBRATION)
               .addAttribute(CalibrationXml.BEGIN, entry.range().begin() != Long.MIN_VALUE ? Instant.ofEpochMilli(entry.range().begin()).toString() : null)
               .addAttribute(CalibrationXml.END, entry.range().end() != Long.MAX_VALUE ? Instant.ofEpochMilli(entry.range().end()).toString() : null);
         entry.value().addXml(calibrationElement);
      });
   }
}
