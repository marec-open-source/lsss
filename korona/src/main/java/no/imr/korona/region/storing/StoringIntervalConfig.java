package no.imr.korona.region.storing;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedSet;
import no.imr.tools.xml.XmlParse;
import no.imr.tools.xml.XmlParseException;
import org.dom4j.Element;

import java.util.Comparator;

public record StoringIntervalConfig(
      ImmutableSortedSet<Integer> kHz, // Sorted for repeatable serialization
      short dataDir,
      boolean pelagicMode,
      short quality
) {
   public static final short DATA_DIR_RAW = 0;
   public static final short DATA_DIR_KORONA = 1;

   static StoringIntervalConfig fromXml(Element element) throws XmlParseException {
      String frequencies = XmlParse.stringAttribute(element, "frequencies", "");
      ImmutableSortedSet<Integer> kHz = Splitter.on(',').trimResults().omitEmptyStrings().splitToStream(frequencies)
            .map(Integer::parseInt)
            .collect(ImmutableSortedSet.toImmutableSortedSet(Comparator.naturalOrder()));
      short dataDir = XmlParse.shortAttribute(element, "dataDir", (short) 0);
      boolean pelagicMode = XmlParse.booleanAttribute(element, "pelagicMode", false);
      short quality = XmlParse.shortAttribute(element, "quality", (short) 1);
      return new StoringIntervalConfig(kHz, dataDir, pelagicMode, quality);
   }

   void toXml(Element element) {
      element
            .addAttribute("frequencies", Joiner.on(',').join(kHz))
            .addAttribute("dataDir", String.valueOf(dataDir))
            .addAttribute("pelagicMode", String.valueOf(pelagicMode))
            .addAttribute("quality", String.valueOf(quality));
   }
}
