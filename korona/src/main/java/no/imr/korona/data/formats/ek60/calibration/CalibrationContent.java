package no.imr.korona.data.formats.ek60.calibration;

import com.google.common.collect.ImmutableMap;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public final class CalibrationContent {
   private final CalibrationType defaultCalibrationType;
   private final ImmutableMap<String, CalibrationType> calibrationTypes;

   public CalibrationContent() {
      defaultCalibrationType = new CalibrationType();
      calibrationTypes = ImmutableMap.of();
   }

   public CalibrationContent(CalibrationType defaultCalibrationType, ImmutableMap<String, CalibrationType> calibrationTypes) {
      this.defaultCalibrationType = defaultCalibrationType;
      this.calibrationTypes = calibrationTypes;
   }

   public CalibrationType getDefaultType() {
      return defaultCalibrationType;
   }

   public ImmutableMap<String, CalibrationType> getTypes() {
      return calibrationTypes;
   }

   public CalibrationType getType(String name) {
      CalibrationType calibrationType = calibrationTypes.get(name);
      return calibrationType != null ? calibrationType : defaultCalibrationType;
   }

   public Element toXml() {
      Element element = DocumentHelper.createElement(CalibrationXml.CALIBRATION);
      defaultCalibrationType.addXml(element);
      calibrationTypes.forEach((name, calibrationType) -> {
         Element typeElement = element.addElement(CalibrationXml.TYPE)
               .addAttribute(CalibrationXml.NAME, name);
         calibrationType.addXml(typeElement);
      });
      return element;
   }
}
