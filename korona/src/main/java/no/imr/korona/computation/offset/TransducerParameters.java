package no.imr.korona.computation.offset;

import no.imr.korona.data.FrequencyChannelSelector;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.List;

/**
 * Keeps configurable settings for a transducer as ParameterObjects.
 */
public final class TransducerParameters implements ParameterContainer, Comparable<TransducerParameters> {
   public final IntParameter frequency = new IntParameter(
         new Name("Frequency"),
         0, Unit.KHZ);

   public final FloatParameter alongCorrection = new FloatParameter(
         new Name("AlongCorrection", "Alongship coordinate"),
         0, Unit.METER);

   public final FloatParameter athwartCorrection = new FloatParameter(
         new Name("AthwartCorrection", "Athwartship coordinate"),
         0, Unit.METER);

   public final FloatParameter depthCorrection = new FloatParameter(
         new Name("DepthCorrection", "Depth coordinate"),
         0, Unit.METER);

   public final FloatParameter pulseDelayDepthCorrection = new FloatParameter(
         new Name("PulseDelayDepthCorrection", "Pulse delay depth correction"),
         0, Unit.METER);

   public final FloatParameter angle = new FloatParameter(
         new Name("Angle"),
         0, Unit.DEGREES);

   public final FloatParameter blindZone = new FloatParameter(
         new Name("BlindZone", "Blind zone"),
         5, Unit.METER, ValueConstraints.gte(0f));

   public final FloatParameter range = new FloatParameter(
         new Name("Range"),
         1000, Unit.METER, ValueConstraints.gte(0f));

   public enum ParameterType {
      HORIZONTAL, VERTICAL, RANGE, ALL
   }

   private FrequencyChannelSelector frequencyChannelSelector = new FrequencyChannelSelector(0);

   public TransducerParameters(ParameterType parameterType) {
      frequency.subscribe(kHz -> {
         frequencyChannelSelector = new FrequencyChannelSelector(kHz * 1000);
      });

      boolean horizontalVisible = parameterType == ParameterType.HORIZONTAL || parameterType == ParameterType.ALL;
      alongCorrection.setVisible(horizontalVisible);
      athwartCorrection.setVisible(horizontalVisible);

      boolean verticalVisible = parameterType == ParameterType.VERTICAL || parameterType == ParameterType.ALL;
      depthCorrection.setVisible(verticalVisible);
      pulseDelayDepthCorrection.setVisible(verticalVisible);

      angle.setVisible(parameterType == ParameterType.ALL);

      boolean rangeVisible = parameterType == ParameterType.RANGE || parameterType == ParameterType.ALL;
      blindZone.setVisible(rangeVisible);
      range.setVisible(rangeVisible);

      blindZone.subscribe(range::setAtLeastTo);
      range.subscribe(blindZone::setAtMostTo);
   }

   public TransducerParameters(ParameterType parameterType, Element element) {
      this(parameterType);

      Element parametersElement = element.element(ParameterCollection.XML_PARAMETERS);
      if (parametersElement != null) {
         new ParameterCollection(this).fromXml(parametersElement);
      }
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            frequency,
            alongCorrection,
            athwartCorrection,
            depthCorrection,
            pulseDelayDepthCorrection,
            angle,
            blindZone,
            range
      );
   }

   public FrequencyChannelSelector getChannelSelector() {
      return frequencyChannelSelector;
   }

   public Element toXml() {
      Element element = DocumentHelper.createElement(TransducerParameterManager.XML_TRANSDUCER);
      Element parametersElement = element.addElement(ParameterCollection.XML_PARAMETERS);
      for (BaseParameter<?> baseParameter : getParameters()) {
         if (baseParameter.isVisible()) {
            parametersElement.add(baseParameter.toXml());
         }
      }
      return element;
   }

   public int getKHz() {
      return frequency.getIntValue();
   }

   public void setKHz(int kHz) {
      frequency.setIntValue(kHz);
   }

   public float getDeltaX0() {
      return alongCorrection.getFloatValue();
   }

   public void setDeltaX0(float deltaX0) {
      alongCorrection.setFloatValue(deltaX0);
   }

   public float getDeltaY0() {
      return athwartCorrection.getFloatValue();
   }

   public void setDeltaY0(float deltaY0) {
      athwartCorrection.setFloatValue(deltaY0);
   }

   public float getDeltaZ0() {
      return depthCorrection.getFloatValue();
   }

   public void setDeltaZ0(float deltaZ0) {
      depthCorrection.setFloatValue(deltaZ0);
   }

   public float getDeltaZPulseDelay() {
      return pulseDelayDepthCorrection.getFloatValue();
   }

   public void setDeltaZPulseDelay(float pulseDelay) {
      pulseDelayDepthCorrection.setFloatValue(pulseDelay);
   }

   public float getAngle() {
      return angle.getFloatValue();
   }

   public void setAngle(float angle) {
      this.angle.setFloatValue(angle);
   }

   @Override
   public int compareTo(TransducerParameters that) {
      return Integer.compare(getKHz(), that.getKHz());
   }
}
