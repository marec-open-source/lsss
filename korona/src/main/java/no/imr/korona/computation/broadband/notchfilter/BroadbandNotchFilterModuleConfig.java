package no.imr.korona.computation.broadband.notchfilter;

import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterException;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

public final class BroadbandNotchFilterModuleConfig extends Configurable {
   private final List<BroadbandTemporalNotchFilterConfig> broadbandTemporalNotchFilterConfigs = new ArrayList<>();

   public BroadbandNotchFilterModuleConfig() {
      super(new Name(BroadbandNotchFiltersFileService.BROADBAND_NOTCH_FILTER_XML));
   }

   public BroadbandNotchFilterModuleConfig(Element element) {
      this();

      fromXml(element);
   }

   public List<BroadbandTemporalNotchFilterConfig> getBroadbandTemporalNotchFilterConfigs() {
      return broadbandTemporalNotchFilterConfigs;
   }

   @Override
   public Element toXml() {
      Element element = createElement();
      broadbandTemporalNotchFilterConfigs.sort(BroadbandTemporalNotchFilterConfig::compareTo);
      broadbandTemporalNotchFilterConfigs.forEach(filter -> {
         Element filterElement = element.addElement(BroadbandNotchFiltersFileService.FILTER_XML);
         filter.getParameters().forEach(parameter -> {
            String stringValue = parameter.getStringValue();
            if (!stringValue.isEmpty()) {
               filterElement.addAttribute(parameter.getPersistentName(), stringValue);
            }
         });
      });
      return element;
   }

   @Override
   public void fromXml(Element element) {
      broadbandTemporalNotchFilterConfigs.clear();
      element.elements().forEach(filterElement -> {
         BroadbandTemporalNotchFilterConfig filter = new BroadbandTemporalNotchFilterConfig();
         try {
            filter.getParameters().forEach(parameter -> {
               String value = filterElement.attributeValue(parameter.getPersistentName());
               if (value != null) {
                  parameter.setStringValue(value);
               }
            });
            broadbandTemporalNotchFilterConfigs.add(filter);
         } catch (ParameterException _) {
            Log.global.warning("Error parsing notch filter: " + XmlUtils.toDefaultString(filterElement));
         }
      });
      broadbandTemporalNotchFilterConfigs.sort(BroadbandTemporalNotchFilterConfig::compareTo);
   }
}
