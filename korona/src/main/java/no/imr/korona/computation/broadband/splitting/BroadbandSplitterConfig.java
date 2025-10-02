package no.imr.korona.computation.broadband.splitting;

import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterException;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

public final class BroadbandSplitterConfig extends Configurable {
   private final List<BroadbandSplitterBand> bands = new ArrayList<>();

   public BroadbandSplitterConfig() {
      super(new Name(BroadbandSplitterBandsFileService.SPLITTER_BANDS_XML));
   }

   public BroadbandSplitterConfig(Element element) {
      this();

      fromXml(element);
   }

   public List<BroadbandSplitterBand> getBands() {
      return bands;
   }

   @Override
   public Element toXml() {
      Element element = createElement();
      bands.sort(BroadbandSplitterBand::compareTo);
      bands.forEach(band -> {
         Element bandElement = element.addElement(BroadbandSplitterBandsFileService.BAND_XML);
         band.getParameters().forEach(parameter -> {
            bandElement.addAttribute(parameter.getPersistentName(), parameter.getStringValue());
         });
      });
      return element;
   }

   @Override
   public void fromXml(Element element) {
      bands.clear();
      element.elements().forEach(bandElement -> {
         BroadbandSplitterBand band = new BroadbandSplitterBand();
         try {
            band.getParameters().forEach(parameter -> {
               String value = bandElement.attributeValue(parameter.getPersistentName());
               if (value != null) {
                  parameter.setStringValue(value);
               }
            });
            bands.add(band);
         } catch (ParameterException e) {
            Log.global.warning("Error parsing splitter band: " + XmlUtils.toDefaultString(bandElement));
         }
      });
      bands.sort(BroadbandSplitterBand::compareTo);
   }
}
