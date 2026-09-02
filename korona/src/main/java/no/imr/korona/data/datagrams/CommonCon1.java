package no.imr.korona.data.datagrams;

import no.imr.tools.Utils;
import no.imr.tools.math.GeometryUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;

public final class CommonCon1 {
   public static final String PARAMETER = "parameter";
   public static final String NAME = "name";
   public static final String VALUE = "value";

   public static final String CONFIGURATION = "Configuration";
   public static final String BEAM_ARRAYS = "BeamArrays";

   private CommonCon1() {
   }

   private static GeometryUtils.TrigAngle[] calculateTrigAngles(int size, boolean vertical, float[] steeringRxX, float[] steeringRxY) {
      GeometryUtils.TrigAngle[] retVal = new GeometryUtils.TrigAngle[size];
      for (int i = 0; i < size; i++) {
         double angle = vertical ? getVerticalAngle(i + 1, steeringRxX) : getHorizontalAngle(i + 1, steeringRxY);
         retVal[i] = new GeometryUtils.TrigAngle(angle);
      }
      return retVal;
   }

   private static float[] parseFloatArray(Element element) throws DatagramFormatException {
      return parseFloatArray(element, VALUE);
   }

   private static float[] parseFloatArray(Element element, String attributeName) throws DatagramFormatException {
      String[] parts = attributeValue(element, attributeName).split(";");
      float[] array = new float[parts.length];
      for (int i = 0; i < parts.length; i++) {
         array[i] = Float.parseFloat(parts[i]);
      }
      return array;
   }

   public static Element element(Element parentElement, String childName) throws DatagramFormatException {
      Element element = parentElement.element(childName);
      if (element == null) {
         throw new DatagramFormatException("Missing " + childName + " in " + XmlUtils.getPath(parentElement));
      }
      return element;
   }

   public static String attributeValue(Element element, String attributeName) throws DatagramFormatException {
      String attribute = element.attributeValue(attributeName);
      if (attribute == null) {
         throw new DatagramFormatException("Missing " + attributeName + " in " + XmlUtils.getPath(element));
      }
      return attribute;
   }

   /**
    * The horizontal angle of a channel.
    * Simrad uses Longitude and Latitude for defining beam angles, while
    * we use a spherical and a cartesian coordinate system. The cartesian system is defined
    * as x pointing "outwards" in the beam direction, and z pointing upwards.
    * To get a consistent right-handed system with correct transforms we use the following conventions
    * - horizontal angle [theta] = -longitude
    * - vertical angle [phi] = 90 - latitude
    *
    * @param channel channel number
    * @return horizontal angle in radians [theta]
    */
   public static double getHorizontalAngle(int channel, float[] steeringRxY) {
      return Math.toRadians(-steeringRxY[channel - 1]);
   }

   /**
    * The vertical angle of a channel.
    * Simrad uses Longitude and Latitude for defining beam angles, while
    * we use a spherical and a cartesian coordinate system. The cartesian system is defined
    * as x pointing "outwards" in the beam direction, and z pointing upwards.
    * To get a consistent right-handed system with correct transforms we use the following conventions
    * - horizontal angle [theta] = -longitude
    * - vertical angle [phi] = 90 - latitude
    *
    * @param channel channel number
    * @return vertical angle in radians. Defined as angle wrt to z-axis. Corresponds to phi in a spherical coordinate system.
    */
   public static double getVerticalAngle(int channel, float[] steeringRxX) {
      return Math.toRadians(90 - steeringRxX[channel - 1]);
   }

   public static final class BeamArrays {
      public final float[] steeringRxX;
      public final float[] steeringRxY;
      public final float[] frequencyTx;
      public final float[] frequencyRx;

      private final GeometryUtils.TrigAngle[] steeringRxXAngles;
      private final GeometryUtils.TrigAngle[] steeringRxYAngles;

      private BeamArrays(float[] steeringRxX, float[] steeringRxY, float[] frequencyRx, float[] frequencyTx) {
         this.steeringRxX = steeringRxX;
         this.steeringRxY = steeringRxY;
         this.frequencyRx = frequencyRx;
         this.frequencyTx = frequencyTx;
         steeringRxXAngles = calculateTrigAngles(steeringRxX.length, true, steeringRxX, steeringRxY);
         steeringRxYAngles = calculateTrigAngles(steeringRxY.length, false, steeringRxX, steeringRxY);
      }

      public static BeamArrays create(Element beamArraysElement) throws DatagramFormatException {
         float[] steeringRxX = Utils.EMPTY_FLOAT_ARRAY;
         float[] steeringRxY = Utils.EMPTY_FLOAT_ARRAY;
         float[] frequencyRx = Utils.EMPTY_FLOAT_ARRAY;
         float[] frequencyTx = Utils.EMPTY_FLOAT_ARRAY;
         if (beamArraysElement.elements(PARAMETER).isEmpty()) {
            for (Attribute attribute : beamArraysElement.attributes()) {
               String name = attribute.getName();
               switch (name) {
                  case "SteeringRxX" -> steeringRxX = parseFloatArray(beamArraysElement, name);
                  case "SteeringRxY" -> steeringRxY = parseFloatArray(beamArraysElement, name);
                  case "FrequencyTx" -> frequencyTx = parseFloatArray(beamArraysElement, name);
                  case "FrequencyRx" -> frequencyRx = parseFloatArray(beamArraysElement, name);
                  default -> {
                  }
               }
            }
         } else {
            for (Element element : beamArraysElement.elements(PARAMETER)) {
               String name = attributeValue(element, NAME);
               switch (name) {
                  case "SteeringRxX" -> steeringRxX = parseFloatArray(element);
                  case "SteeringRxY" -> steeringRxY = parseFloatArray(element);
                  case "FrequencyTx" -> frequencyTx = parseFloatArray(element);
                  case "FrequencyRx" -> frequencyRx = parseFloatArray(element);
                  default -> {
                  }
               }
            }
         }
         return new BeamArrays(steeringRxX, steeringRxY, frequencyRx, frequencyTx);
      }

      public GeometryUtils.TrigAngle[] getSteeringRxXAngles() {
         return steeringRxXAngles;
      }

      public GeometryUtils.TrigAngle[] getSteeringRxYAngles() {
         return steeringRxYAngles;
      }
   }
}
