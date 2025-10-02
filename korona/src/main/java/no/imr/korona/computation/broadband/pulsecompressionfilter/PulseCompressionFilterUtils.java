package no.imr.korona.computation.broadband.pulsecompressionfilter;

import no.imr.korona.computation.broadband.PulseCompressionFilter;
import no.imr.korona.computation.broadband.PulseCompressionFilterChain;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.OptionalParameter;
import no.imr.tools.parameter.ParameterException;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.IntStream;

public final class PulseCompressionFilterUtils {
   private PulseCompressionFilterUtils() {
   }

   public static Element toXml(Map<String, List<PulseCompressionFilterConfig>> pulseCompressionFilterMap) {
      return mapToXml(DocumentHelper.createElement(PulseCompressionFiltersFileService.PULSE_COMPRESSION_FILTER_XML), pulseCompressionFilterMap);
   }

   public static Map<String, List<PulseCompressionFilterConfig>> fromXml(Element element) {
      Map<String, List<PulseCompressionFilterConfig>> pulseCompressionFilterMap = new HashMap<>();
      xmlToMap(element, pulseCompressionFilterMap);
      return Map.copyOf(pulseCompressionFilterMap);
   }

   static Element mapToXml(Element element, Map<String, List<PulseCompressionFilterConfig>> pulseCompressionFilterMap) {
      // Sort by channel ID to get consistent results.
      pulseCompressionFilterMap.keySet().stream().sorted().forEach(channelId -> {
         Element channelElement = element.addElement(PulseCompressionFiltersFileService.CHANNEL_XML)
               .addAttribute(PulseCompressionFiltersFileService.CHANNEL_ID_XML, channelId);
         pulseCompressionFilterMap.get(channelId).forEach(filter -> {
            Element filterElement = channelElement.addElement(PulseCompressionFiltersFileService.FILTER_XML);
            List<OptionalParameter<?>> specifiedPulseParameters = filter.getPulseParameters().stream()
                  .filter(parameters -> parameters.getValue().isPresent())
                  .toList();
            if (!specifiedPulseParameters.isEmpty()) {
               Element appliesToElement = filterElement.addElement(PulseCompressionFiltersFileService.APPLIES_TO_XML);
               specifiedPulseParameters.forEach(parameter -> {
                  appliesToElement.addAttribute(parameter.getPersistentName(), parameter.getStringValue());
               });
            }

            if (filter.refStage.getValue().isEmpty()) {
               filter.getFilterParameters().forEach(parameter -> {
                  String parameterAsString = parameter.getStringValue();
                  if (!parameterAsString.isEmpty()) {
                     filterElement.addElement(parameter.getPersistentName())
                           .addText(parameterAsString);
                  }
               });
            } else {
               filterElement.addAttribute(filter.refStage.getPersistentName(), filter.refStage.getStringValue());
            }
         });
      });
      return element;
   }

   static void xmlToMap(Element element, Map<String, List<PulseCompressionFilterConfig>> pulseCompressionFilterMap) {
      element.elements().forEach(channelElement -> {
         String channelID = channelElement.attributeValue(PulseCompressionFiltersFileService.CHANNEL_ID_XML);
         try {
            channelElement.elements(PulseCompressionFiltersFileService.FILTER_XML).forEach(filter -> {
               try {
                  PulseCompressionFilterConfig filterParameterContainer = new PulseCompressionFilterConfig();

                  String refStage = filter.attributeValue(PulseCompressionFiltersFileService.REF_STAGE);
                  if (refStage != null) {
                     filterParameterContainer.refStage.setStringValue(refStage);
                  }

                  Element appliesToElement = filter.element(PulseCompressionFiltersFileService.APPLIES_TO_XML);
                  if (appliesToElement != null) {
                     filterParameterContainer.getPulseParameters().forEach(parameter -> {
                        String value = appliesToElement.attributeValue(parameter.getPersistentName());
                        if (value != null) {
                           parameter.setStringValue(value);
                        }
                     });
                  }

                  if (filterParameterContainer.refStage.getValue().isEmpty()) {
                     filterParameterContainer.getFilterParameters().forEach(parameter -> {
                        Element filterParameterElement = filter.element(parameter.getPersistentName());
                        if (filterParameterElement != null) {
                           parameter.setStringValue(filterParameterElement.getStringValue());
                        }
                     });
                  }
                  pulseCompressionFilterMap.computeIfAbsent(channelID, key -> new ArrayList<>()).add(filterParameterContainer);
               } catch (ParameterException e) {
                  Log.global.warning("Error parsing pulse compression filter: " + XmlUtils.toDefaultString(filter));
               }
            });
         } catch (ParameterException e) {
            Log.global.warning("Error parsing pulse compression filters: " + XmlUtils.toDefaultString(channelElement));
         }
      });
   }

   public static PulseCompressionFilterChain possiblyReplaceFilterChain(PulseCompressionFilterChain originalFilterChain,
                                                                        List<PulseCompressionFilterConfig> filterConfigs) {
      if (filterConfigs.isEmpty()) {
         return originalFilterChain;
      }

      List<PulseCompressionFilter> originalStageFilters = originalFilterChain.getFilters();
      List<PulseCompressionFilter> updatedFilters = IntStream.range(0, filterConfigs.size())
            .mapToObj(i -> {
               PulseCompressionFilterConfig filterConfig = filterConfigs.get(i);
               Optional<Integer> refStage = filterConfig.refStage.getValue();
               if (refStage.isPresent()) {
                  PulseCompressionFilter originalStageFilter = originalStageFilters.get(refStage.get() - 1);
                  return new PulseCompressionFilter(i + 1, originalStageFilter.decimationFactor(), originalStageFilter.coefficients());
               } else {
                  return new PulseCompressionFilter(i + 1, filterConfig.decimationFactor.getValue().orElse(1), filterConfig.getCoefficients());
               }
            })
            .toList();
      PulseCompressionFilterChain newFilterChain = new PulseCompressionFilterChain(updatedFilters);
      if (newFilterChain.getTotalDecimationFactor() != originalFilterChain.getTotalDecimationFactor()) {
         Log.global.log(Level.WARNING,
               "Total decimation factor of updated pulse compression filters different from decimation factor defined by original stage filters (" +
                     newFilterChain.getTotalDecimationFactor() + " instead of " + originalFilterChain.getTotalDecimationFactor() + ").");
      }
      return newFilterChain;
   }
}
