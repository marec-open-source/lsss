package no.imr.korona.data.formats.ek60.calibration;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.ping.items.configuration.TransducerNameAndSerialNumber;

import java.util.Optional;

public final class ChannelCalibrationBuilder {
   public Optional<String> id = Optional.empty();
   public Optional<Integer> channel = Optional.empty();
   public Optional<TransducerNameAndSerialNumber> nameAndSerialNumber = Optional.empty();
   public Optional<Integer> kHz = Optional.empty();
   public Optional<Float> gain = Optional.empty();
   public Optional<Float> equivalentBeamAngle = Optional.empty();
   public Optional<Float> beamWidthAlongship = Optional.empty();
   public Optional<Float> beamWidthAthwartship = Optional.empty();
   public Optional<Float> angleOffsetAlongship = Optional.empty();
   public Optional<Float> angleOffsetAthwartship = Optional.empty();
   public ImmutableMap<Float, Float> saCorrections = ImmutableMap.of();

   public Optional<Float> absorptionCoefficient = Optional.empty();
   public Optional<Float> soundVelocity = Optional.empty();

   public Optional<BroadbandFunction> broadbandGain = Optional.empty();
   public Optional<BroadbandFunction> broadbandTransducerImpedance = Optional.empty();
   public Optional<BroadbandFunction> broadbandEquivalentBeamAngle = Optional.empty();
   public Optional<BroadbandFunction> broadbandAngleOffsetAlongship = Optional.empty();
   public Optional<BroadbandFunction> broadbandAngleOffsetAthwartship = Optional.empty();
   public Optional<BroadbandFunction> broadbandBeamWidthAlongship = Optional.empty();
   public Optional<BroadbandFunction> broadbandBeamWidthAthwartship = Optional.empty();

   public Optional<Float> profosRo = Optional.empty();
   public Optional<Float> profosTauEff = Optional.empty();
   public Optional<Float> profosTau = Optional.empty();
   public Optional<Float> profosFrequency = Optional.empty();
   public Optional<Integer> profosBeamWithMode = Optional.empty();

   public ChannelCalibrationBuilder() {
   }

   public ChannelCalibration build() {
      return new ChannelCalibration(this);
   }
}
