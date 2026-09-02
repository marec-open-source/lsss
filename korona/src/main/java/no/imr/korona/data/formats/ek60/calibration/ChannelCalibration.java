package no.imr.korona.data.formats.ek60.calibration;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.ping.items.configuration.TransducerNameAndSerialNumber;
import no.imr.tools.Utils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ChannelCalibration {
   public static final ChannelCalibration EMPTY = new ChannelCalibration(new ChannelCalibrationBuilder());

   public final Optional<String> id;
   public final Optional<Integer> channel;
   public final Optional<TransducerNameAndSerialNumber> nameAndSerialNumber;
   public final Optional<Integer> kHz;
   public final Optional<Float> gain;
   public final Optional<Float> equivalentBeamAngle;
   public final Optional<Float> beamWidthAlongship;
   public final Optional<Float> beamWidthAthwartship;
   public final Optional<Float> angleOffsetAlongship;
   public final Optional<Float> angleOffsetAthwartship;
   public final ImmutableMap<Float, Float> saCorrections;

   public final Optional<Float> absorptionCoefficient;
   public final Optional<Float> soundVelocity;

   public final Optional<BroadbandFunction> broadbandGain;
   public final Optional<BroadbandFunction> broadbandTransducerImpedance;
   public final Optional<BroadbandFunction> broadbandEquivalentBeamAngle;
   public final Optional<BroadbandFunction> broadbandBeamWidthAlongship;
   public final Optional<BroadbandFunction> broadbandBeamWidthAthwartship;
   public final Optional<BroadbandFunction> broadbandAngleOffsetAlongship;
   public final Optional<BroadbandFunction> broadbandAngleOffsetAthwartship;

   public final Optional<Float> profosRo;
   public final Optional<Float> profosTauEff;
   public final Optional<Float> profosTau;
   public final Optional<Float> profosFrequency;
   public final Optional<Integer> profosBeamWidthMode;

   ChannelCalibration(Element element) {
      id = Optional.ofNullable(element.attributeValue(CalibrationXml.ID));
      channel = parseInt(element.attributeValue(CalibrationXml.CHANNEL));
      nameAndSerialNumber = Optional.empty();
      kHz = parseInt(element.attributeValue(CalibrationXml.KHZ));
      gain = parseFloat(element.attributeValue(CalibrationXml.GAIN));
      equivalentBeamAngle = parseFloat(element.attributeValue(CalibrationXml.EQUIVALENT_BEAM_ANGLE));
      beamWidthAlongship = parseFloat(element.attributeValue(CalibrationXml.BEAM_WIDTH_ALONGSHIP));
      beamWidthAthwartship = parseFloat(element.attributeValue(CalibrationXml.BEAM_WIDTH_ATHWARTSHIP));
      angleOffsetAlongship = parseFloat(element.attributeValue(CalibrationXml.ANGLE_OFFSET_ALONGSHIP));
      angleOffsetAthwartship = parseFloat(element.attributeValue(CalibrationXml.ANGLE_OFFSET_ATHWARTSHIP));
      saCorrections = parsePulseDurationMap(element.attributeValue(CalibrationXml.SA_CORRECTIONS));

      absorptionCoefficient = parseFloat(element.attributeValue(CalibrationXml.ABSORPTION_COEFFICIENT));
      soundVelocity = parseFloat(element.attributeValue(CalibrationXml.SOUND_VELOCITY));

      Element broadbandElement = element.element(CalibrationXml.BROADBAND);
      broadbandGain = parseBroadbandFunction(broadbandElement, CalibrationXml.BROADBAND_GAIN);
      broadbandTransducerImpedance = parseBroadbandFunction(broadbandElement, CalibrationXml.BROADBAND_TRANSDUCER_IMPEDANCE);
      broadbandEquivalentBeamAngle = parseBroadbandFunction(broadbandElement, CalibrationXml.BROADBAND_EQUIVALENT_BEAM_ANGLE);
      broadbandBeamWidthAlongship = parseBroadbandFunction(broadbandElement, CalibrationXml.BROADBAND_BEAM_WIDTH_ALONGSHIP);
      broadbandBeamWidthAthwartship = parseBroadbandFunction(broadbandElement, CalibrationXml.BROADBAND_BEAM_WIDTH_ATHWARTSHIP);
      broadbandAngleOffsetAlongship = parseBroadbandFunction(broadbandElement, CalibrationXml.BROADBAND_ANGLE_OFFSET_ALONGSHIP);
      broadbandAngleOffsetAthwartship = parseBroadbandFunction(broadbandElement, CalibrationXml.BROADBAND_ANGLE_OFFSET_ATHWARTSHIP);

      profosRo = parseFloat(element.attributeValue(CalibrationXml.PROFOS_RO));
      profosTauEff = parseFloat(element.attributeValue(CalibrationXml.PROFOS_TAU_EFF));
      profosTau = parseFloat(element.attributeValue(CalibrationXml.PROFOS_TAU));
      profosFrequency = parseFloat(element.attributeValue(CalibrationXml.PROFOS_FREQUENCY));
      profosBeamWidthMode = parseInt(element.attributeValue(CalibrationXml.PROFOS_BEAM_WIDTH_MODE));
   }

   ChannelCalibration(ChannelCalibrationBuilder builder) {
      id = builder.id;
      channel = builder.channel;
      nameAndSerialNumber = builder.nameAndSerialNumber;
      kHz = builder.kHz;
      gain = builder.gain;
      equivalentBeamAngle = builder.equivalentBeamAngle;
      beamWidthAlongship = builder.beamWidthAlongship;
      beamWidthAthwartship = builder.beamWidthAthwartship;
      angleOffsetAlongship = builder.angleOffsetAlongship;
      angleOffsetAthwartship = builder.angleOffsetAthwartship;
      saCorrections = builder.saCorrections;

      absorptionCoefficient = builder.absorptionCoefficient;
      soundVelocity = builder.soundVelocity;

      broadbandGain = builder.broadbandGain;
      broadbandTransducerImpedance = builder.broadbandTransducerImpedance;
      broadbandEquivalentBeamAngle = builder.broadbandEquivalentBeamAngle;
      broadbandAngleOffsetAlongship = builder.broadbandAngleOffsetAlongship;
      broadbandAngleOffsetAthwartship = builder.broadbandAngleOffsetAthwartship;
      broadbandBeamWidthAlongship = builder.broadbandBeamWidthAlongship;
      broadbandBeamWidthAthwartship = builder.broadbandBeamWidthAthwartship;

      profosRo = builder.profosRo;
      profosTauEff = builder.profosTauEff;
      profosTau = builder.profosTau;
      profosFrequency = builder.profosFrequency;
      profosBeamWidthMode = builder.profosBeamWidthMode;
   }

   private static ImmutableMap<Float, Float> parsePulseDurationMap(@Nullable String string) {
      if (string == null) {
         return ImmutableMap.of();
      }
      ImmutableMap.Builder<Float, Float> builder = ImmutableMap.builder();
      for (String entry : Splitter.on(',').omitEmptyStrings().trimResults().split(string)) {
         List<String> keyValue = Splitter.on(':').trimResults().splitToList(entry);
         if (keyValue.size() != 2) {
            throw new IllegalArgumentException("Invalid pulse duration map: " + keyValue);
         }
         float pulseDuration = Float.parseFloat(keyValue.get(0)) * 1e-6f; // microseconds to seconds
         float value = Float.parseFloat(keyValue.get(1));
         builder.put(pulseDuration, value);
      }
      return builder.build();
   }

   private static @Nullable String pulseDurationMapToString(ImmutableMap<Float, Float> map) {
      if (map.isEmpty()) {
         return null;
      }
      return map.entrySet().stream()
            .map(entry -> {
               float microseconds = entry.getKey() * 1e6f;
               return Utils.toString(microseconds) + ":" + Utils.toString(entry.getValue());
            })
            .collect(Collectors.joining(", "));
   }

   private static Optional<BroadbandFunction> parseBroadbandFunction(@Nullable Element broadbandElement, String name) {
      if (broadbandElement == null) {
         return Optional.empty();
      }
      Element functionElement = broadbandElement.element(name);
      if (functionElement == null) {
         return Optional.empty();
      }
      return Optional.of(BroadbandFunction.parse(functionElement, name));
   }

   private static Optional<Integer> parseInt(@Nullable String string) {
      if (string == null) {
         return Optional.empty();
      }
      return Optional.of(Integer.parseInt(string));
   }

   private static Optional<Float> parseFloat(@Nullable String string) {
      if (string == null) {
         return Optional.empty();
      }
      return Optional.of(Float.parseFloat(string));
   }

   void addXml(Element element) {
      addAttribute(element, CalibrationXml.ID, id);
      addAttribute(element, CalibrationXml.CHANNEL, channel);
      addAttribute(element, CalibrationXml.KHZ, kHz);
      addAttribute(element, CalibrationXml.GAIN, gain);
      addAttribute(element, CalibrationXml.EQUIVALENT_BEAM_ANGLE, equivalentBeamAngle);
      addAttribute(element, CalibrationXml.BEAM_WIDTH_ALONGSHIP, beamWidthAlongship);
      addAttribute(element, CalibrationXml.BEAM_WIDTH_ATHWARTSHIP, beamWidthAthwartship);
      addAttribute(element, CalibrationXml.ANGLE_OFFSET_ALONGSHIP, angleOffsetAlongship);
      addAttribute(element, CalibrationXml.ANGLE_OFFSET_ATHWARTSHIP, angleOffsetAthwartship);
      element.addAttribute(CalibrationXml.SA_CORRECTIONS, pulseDurationMapToString(saCorrections));

      addAttribute(element, CalibrationXml.ABSORPTION_COEFFICIENT, absorptionCoefficient);
      addAttribute(element, CalibrationXml.SOUND_VELOCITY, soundVelocity);

      Element broadbandElement = element.addElement(CalibrationXml.BROADBAND);
      addBroadbandFunction(broadbandElement, broadbandGain, CalibrationXml.BROADBAND_GAIN);
      addBroadbandFunction(broadbandElement, broadbandTransducerImpedance, CalibrationXml.BROADBAND_TRANSDUCER_IMPEDANCE);
      addBroadbandFunction(broadbandElement, broadbandEquivalentBeamAngle, CalibrationXml.BROADBAND_EQUIVALENT_BEAM_ANGLE);
      addBroadbandFunction(broadbandElement, broadbandBeamWidthAlongship, CalibrationXml.BROADBAND_BEAM_WIDTH_ALONGSHIP);
      addBroadbandFunction(broadbandElement, broadbandBeamWidthAthwartship, CalibrationXml.BROADBAND_BEAM_WIDTH_ATHWARTSHIP);
      addBroadbandFunction(broadbandElement, broadbandAngleOffsetAlongship, CalibrationXml.BROADBAND_ANGLE_OFFSET_ALONGSHIP);
      addBroadbandFunction(broadbandElement, broadbandAngleOffsetAthwartship, CalibrationXml.BROADBAND_ANGLE_OFFSET_ATHWARTSHIP);
      if (!broadbandElement.hasContent()) {
         broadbandElement.detach();
      }
   }

   private static void addAttribute(Element element, String name, Optional<?> value) {
      value.ifPresent(v -> {
         String string = v instanceof Float x ? Utils.toString(x) : v.toString();
         element.addAttribute(name, string);
      });
   }

   private static void addBroadbandFunction(Element element, Optional<BroadbandFunction> function, String name) {
      function.ifPresent(f -> f.addXml(element.addElement(name), name));
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ChannelCalibration that
            && id.equals(that.id)
            && channel.equals(that.channel)
            && nameAndSerialNumber.equals(that.nameAndSerialNumber)
            && kHz.equals(that.kHz)
            && gain.equals(that.gain)
            && equivalentBeamAngle.equals(that.equivalentBeamAngle)
            && beamWidthAlongship.equals(that.beamWidthAlongship)
            && beamWidthAthwartship.equals(that.beamWidthAthwartship)
            && angleOffsetAlongship.equals(that.angleOffsetAlongship)
            && angleOffsetAthwartship.equals(that.angleOffsetAthwartship)
            && saCorrections.equals(that.saCorrections)
            && absorptionCoefficient.equals(that.absorptionCoefficient)
            && soundVelocity.equals(that.soundVelocity)
            && broadbandGain.equals(that.broadbandGain)
            && broadbandTransducerImpedance.equals(that.broadbandTransducerImpedance)
            && broadbandEquivalentBeamAngle.equals(that.broadbandEquivalentBeamAngle)
            && broadbandBeamWidthAlongship.equals(that.broadbandBeamWidthAlongship)
            && broadbandBeamWidthAthwartship.equals(that.broadbandBeamWidthAthwartship)
            && broadbandAngleOffsetAlongship.equals(that.broadbandAngleOffsetAlongship)
            && broadbandAngleOffsetAthwartship.equals(that.broadbandAngleOffsetAthwartship)
            && profosRo.equals(that.profosRo)
            && profosTauEff.equals(that.profosTauEff)
            && profosTau.equals(that.profosTau)
            && profosFrequency.equals(that.profosFrequency)
            && profosBeamWidthMode.equals(that.profosBeamWidthMode);
   }

   @Override
   public int hashCode() {
      int result = id.hashCode();
      result = 31 * result + channel.hashCode();
      result = 31 * result + nameAndSerialNumber.hashCode();
      result = 31 * result + kHz.hashCode();
      result = 31 * result + gain.hashCode();
      result = 31 * result + equivalentBeamAngle.hashCode();
      result = 31 * result + beamWidthAlongship.hashCode();
      result = 31 * result + beamWidthAthwartship.hashCode();
      result = 31 * result + angleOffsetAlongship.hashCode();
      result = 31 * result + angleOffsetAthwartship.hashCode();
      result = 31 * result + saCorrections.hashCode();
      result = 31 * result + absorptionCoefficient.hashCode();
      result = 31 * result + soundVelocity.hashCode();
      result = 31 * result + broadbandGain.hashCode();
      result = 31 * result + broadbandTransducerImpedance.hashCode();
      result = 31 * result + broadbandEquivalentBeamAngle.hashCode();
      result = 31 * result + broadbandBeamWidthAlongship.hashCode();
      result = 31 * result + broadbandBeamWidthAthwartship.hashCode();
      result = 31 * result + broadbandAngleOffsetAlongship.hashCode();
      result = 31 * result + broadbandAngleOffsetAthwartship.hashCode();
      result = 31 * result + profosRo.hashCode();
      result = 31 * result + profosTauEff.hashCode();
      result = 31 * result + profosTau.hashCode();
      result = 31 * result + profosFrequency.hashCode();
      result = 31 * result + profosBeamWidthMode.hashCode();
      return result;
   }
}
