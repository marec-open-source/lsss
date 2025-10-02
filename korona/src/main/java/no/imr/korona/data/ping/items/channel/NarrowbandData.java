package no.imr.korona.data.ping.items.channel;

import no.imr.korona.computation.broadband.BroadbandToAngles;
import no.imr.korona.computation.broadband.PulseCompression;
import no.imr.korona.computation.broadband.PulseCompressionFilterChain;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.ping.items.configuration.BeamType;
import no.imr.korona.data.ping.items.configuration.PulseForm;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.TvgArray;
import no.imr.tools.Utils;
import no.imr.tools.logging.LogOnce;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.ComplexArrayUtils;
import org.apache.commons.numbers.complex.Complex;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Channel with narrowband data.
 */
public final class NarrowbandData extends ComplexChannelData {
   private @Nullable PowerData powerData;

   public NarrowbandData(long ntDate) {
      super(ntDate);
   }

   public NarrowbandData(ChannelData channelData) {
      super(channelData);
   }

   @Override
   public String getDataTypeName() {
      return "Narrowband data (complex)";
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      Xml0Datagram xml0Datagram = toXml0Parameter(PulseForm.NARROWBAND, element -> element.addAttribute("Frequency", Utils.toString(getFrequency())));
      return List.of(xml0Datagram, toRaw3Datagram());
   }

   @Override
   public NarrowbandData makeCopy() {
      return makeCopyWithAllData();
   }

   @Override
   public PowerData getPowerData() {
      PowerData powerData = this.powerData;
      if (powerData == null) {
         powerData = new PowerData(this, (float) calculateEffectiveTau());
         computeSv(powerData);
         if (getTransducer().getBeamType() != BeamType.SINGLE) {
            powerData.setAngleData(new AngleData(this::computeAngles));
         }
         powerData.setReadOnly(this);
         this.powerData = powerData;
      }
      return powerData;
   }

   @Override
   void clearDerivedData() {
      powerData = null;
   }

   public NarrowbandData makeCopyWithNoData() {
      return new NarrowbandData(this);
   }

   public NarrowbandData makeCopyWithAllData() {
      NarrowbandData copy = makeCopyWithNoData();
      copy.setData(Utils.copy(getReal()), Utils.copy(getImag()), getSlope());
      return copy;
   }

   private double calculateEffectiveTau() {
      return calculateEffectiveTau(this, getTransducer(), getSlope());
   }

   static double calculateEffectiveTau(ChannelData channelData, RawFileTransducer transducer, float slope) {
      // Effective tau for CW data. See Gavin's note 10 August 2016, v1.1
      ComplexArray signal = getSentSignal(channelData, transducer, slope);
      PulseCompressionFilterChain filterChain = transducer.getPulseCompressionFilterChain();
      return PulseCompression.calculateEffectiveTau(signal, transducer.getEK80fs() / filterChain.getTotalDecimationFactor());
   }

   public ComplexArray getSentSignal() {
      return getSentSignal(this, getTransducer(), getSlope());
   }

   private static ComplexArray getSentSignal(ChannelData channelData, RawFileTransducer transducer, float slope) {
      PulseCompressionFilterChain filterChain = transducer.getPulseCompressionFilterChain();
      double[] fullSentSignal = PulseCompression.generateFullSentSignal(
            slope, channelData.getPulseDuration(),
            transducer.getEK80fs(),
            channelData.getFrequencyRange(), PulseForm.NARROWBAND);
      return filterChain.apply(fullSentSignal);
   }

   private void computeSv(PowerData powerData) {
      int sectorCount = getSectorCount();
      double prxFactor = getPrxFactor(powerData.getFrequency());
      TvgArray tvg = powerData.getTVGArray();
      float[] sv = powerData.getSv();
      for (int i = 0; i < sv.length; i++) {
         double re = 0;
         double im = 0;
         for (int sector = 0; sector < sectorCount; sector++) {
            re += getReal()[sector][i];
            im += getImag()[sector][i];
         }
         re /= sectorCount;
         im /= sectorCount;
         double linearPower = (re * re + im * im) * prxFactor;
         float npi = powerData.linearPowerToNoisePowerIndex((float) linearPower);
         sv[i] = npi * tvg.get(i);
      }
   }

   private float[] computeAngles() {
      int beamType = getTransducer().getBeamType();
      int count = getCount();
      float[] angles = new float[2 * count];
      for (int i = 0; i < count; i++) {
         BroadbandToAngles.DeltaPhi deltaPhi = switch (beamType) {
            case BeamType.SINGLE -> {
               yield BroadbandToAngles.DeltaPhi.ZERO;
            }
            case BeamType.REF, BeamType.REF_B, BeamType.SPLIT_2 -> {
               LogOnce.warning("Computation of angles for beam type " + beamType + " is not yet implemented", getPingConfiguration());
               yield BroadbandToAngles.DeltaPhi.ZERO;
            }
            case BeamType.SPLIT -> {
               yield BroadbandToAngles.computeDeltaPhiFromFourSectors(
                     Complex.ofCartesian(getReal()[0][i], getImag()[0][i]),
                     Complex.ofCartesian(getReal()[1][i], getImag()[1][i]),
                     Complex.ofCartesian(getReal()[2][i], getImag()[2][i]),
                     Complex.ofCartesian(getReal()[3][i], getImag()[3][i]));
            }
            case BeamType.SPLIT_3, BeamType.SPLIT_3_C, BeamType.SPLIT_3_CN, BeamType.SPLIT_3_CW -> {
               if (getSectorCount() > 3) {
                  yield BroadbandToAngles.calculateDeltaPhiFromThreeSectorsAndCenter(
                        Complex.ofCartesian(getReal()[0][i], getImag()[0][i]),
                        Complex.ofCartesian(getReal()[1][i], getImag()[1][i]),
                        Complex.ofCartesian(getReal()[2][i], getImag()[2][i]),
                        Complex.ofCartesian(getReal()[3][i], getImag()[3][i]));
               } else {
                  yield BroadbandToAngles.calculateDeltaPhiFromThreeSectors(
                        Complex.ofCartesian(getReal()[0][i], getImag()[0][i]),
                        Complex.ofCartesian(getReal()[1][i], getImag()[1][i]),
                        Complex.ofCartesian(getReal()[2][i], getImag()[2][i]));
               }
            }
            case BeamType.SPLIT_4_B -> {
               yield BroadbandToAngles.calculateDeltaPhiForBeamType97(
                     getReal()[0][i], getImag()[0][i],
                     getReal()[1][i], getImag()[1][i],
                     getReal()[2][i], getImag()[2][i],
                     getReal()[3][i], getImag()[3][i],
                     getTransducer());
            }
            default -> {
               LogOnce.warning("Unknown beam type " + beamType, getPingConfiguration());
               yield BroadbandToAngles.DeltaPhi.ZERO;
            }
         };

         angles[2 * i] = (float) BroadbandToAngles.deltaPhiToElectricalAngle(deltaPhi.athwart());
         angles[2 * i + 1] = (float) BroadbandToAngles.deltaPhiToElectricalAngle(deltaPhi.along());
      }
      return angles;
   }

   @Override
   public Complex getTransducerImpedanceForSector(int sectorIndex) {
      return ComplexArrayUtils.average(calculateComplexImpedanceForSector(sectorIndex, getSlope()));
   }
}
