package no.imr.korona.computation.broadband.notchfilter;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public final class BroadbandNotchFilterUtils {
   private BroadbandNotchFilterUtils() {
   }

   public static Element toXml(List<BroadbandNotchFilterConfig> notchFilterConfigs) {
      Element element = DocumentHelper.createElement(BroadbandNotchFiltersFileService.BROADBAND_NOTCH_FILTER_XML);
      notchFilterConfigs.stream()
            .map(BroadbandNotchFilterUtils::notchFilterToXml)
            .forEach(element::add);
      return element;
   }

   private static Element notchFilterToXml(BroadbandNotchFilterConfig notchFilterConfig) {
      return DocumentHelper.createElement(BroadbandNotchFiltersFileService.FILTER_XML)
            .addAttribute(BroadbandNotchFiltersFileService.REJECTION_FREQUENCY_KHZ, Utils.toString(notchFilterConfig.rejectionFrequency() / 1000))
            .addAttribute(BroadbandNotchFiltersFileService.BANDWIDTH_KHZ, Utils.toString(notchFilterConfig.bandwidth() / 1000));
   }

   public static List<BroadbandNotchFilterConfig> fromXml(Element element) {
      return element.elements().stream()
            .map(BroadbandNotchFilterUtils::xmlToNotchFilterConfig)
            .filter(Objects::nonNull)
            .toList();
   }

   private static @Nullable BroadbandNotchFilterConfig xmlToNotchFilterConfig(Element element) {
      try {
         float rejectionFrequency = 1000 * Float.parseFloat(element.attributeValue(BroadbandNotchFiltersFileService.REJECTION_FREQUENCY_KHZ));
         float bandwidth = 1000 * Float.parseFloat(element.attributeValue(BroadbandNotchFiltersFileService.BANDWIDTH_KHZ));
         return new BroadbandNotchFilterConfig(rejectionFrequency, bandwidth);
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error parsing notch filter: " + XmlUtils.toDefaultString(element), e);
         return null;
      }
   }
}
