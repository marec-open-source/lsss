package no.imr.korona.computation.broadband.pulsecompressionfilter;

import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import org.dom4j.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PulseCompressionFilterModuleConfig extends Configurable {
   private final Map<String, List<PulseCompressionFilterConfig>> pulseCompressionFiltersPerChannel = new HashMap<>();

   public PulseCompressionFilterModuleConfig() {
      super(new Name(PulseCompressionFiltersFileService.PULSE_COMPRESSION_FILTER_XML));
   }

   public PulseCompressionFilterModuleConfig(Element element) {
      this();

      fromXml(element);
   }

   public Map<String, List<PulseCompressionFilterConfig>> getPulseCompressionFiltersPerChannel() {
      return pulseCompressionFiltersPerChannel;
   }

   @Override
   public Element toXml() {
      return PulseCompressionFilterUtils.mapToXml(createElement(), pulseCompressionFiltersPerChannel);
   }

   @Override
   public void fromXml(Element element) {
      pulseCompressionFiltersPerChannel.clear();
      PulseCompressionFilterUtils.xmlToMap(element, pulseCompressionFiltersPerChannel);
   }
}
