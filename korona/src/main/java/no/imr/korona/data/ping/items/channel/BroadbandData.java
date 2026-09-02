package no.imr.korona.data.ping.items.channel;

import no.imr.korona.computation.broadband.BroadbandToAngles;
import no.imr.korona.computation.broadband.BroadbandToSvAtCenterFrequency;
import no.imr.korona.computation.broadband.PulseCompression;
import no.imr.korona.computation.broadband.PulseCompressionCache;
import no.imr.korona.computation.broadband.PulseCompressionConfig;
import no.imr.korona.computation.broadband.PulseCompressionFilterChain;
import no.imr.korona.computation.broadband.pulsecompressionfilter.PulseCompressionFilterConfig;
import no.imr.korona.computation.broadband.pulsecompressionfilter.PulseCompressionFilterUtils;
import no.imr.korona.computation.broadband.transferfunction.IdentityTransferFunction;
import no.imr.korona.computation.broadband.transferfunction.NotchFilterCache;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Raw0Datagram;
import no.imr.korona.data.datagrams.Raw3Datagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.formats.ek60.calibration.BroadbandFunction;
import no.imr.korona.data.formats.ek60.calibration.ChannelCalibration;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.configuration.BeamType;
import no.imr.korona.data.ping.items.configuration.PulseForm;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.util.absorption.Absorption;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.ComplexArrayUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlParse;
import no.imr.tools.xml.XmlParseException;
import no.marec.lsss.api.data.BroadbandChannelData;
import org.apache.commons.numbers.complex.Complex;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * Channel with broadband data.
 */
public final class BroadbandData extends ComplexChannelData implements BroadbandChannelData {
   public static final String BROADBAND_DATA_TYPE_NAME = "Broadband data";

   private final int pulseForm;
   private float endFrequency;
   private int offsetCorrection;
   private PulseCompression pulseCompression = PulseCompression.EMPTY;
   private @Nullable ComplexArray averagePulseCompressedSignal;

   private @Nullable PowerData powerData;
   private @Nullable AngleData angleData;

   public BroadbandData(Instant instant, int pulseForm) {
      super(instant);

      this.pulseForm = pulseForm;
   }

   public BroadbandData(BroadbandData broadbandData) {
      super(broadbandData);

      pulseForm = broadbandData.pulseForm;
      endFrequency = broadbandData.endFrequency;
      offsetCorrection = broadbandData.offsetCorrection;
   }

   public BroadbandData(Raw0Datagram raw0Datagram, PingConversion pingConversion) {
      super(raw0Datagram);

      pulseForm = PulseForm.BROADBAND_LINEAR_UP;
      endFrequency = getFrequency() + getPulseDuration() * raw0Datagram.sweep;

      String channelId = pingConversion.getPingConfiguration().getRawFileConfiguration().getTransducers().get(getChannel() - 1).getChannelId();
      Element channelParameter = pingConversion.getIdToChannelParameter().get(channelId);
      if (channelParameter == null) {
         Log.global.log(Log.SILENT_WARNING, "No XML0/Parameter/Channel for " + channelId + " " + pingConversion.getDateAndFileString());
         return;
      }
      float slope;
      try {
         slope = XmlParse.floatAttribute(channelParameter, "Slope");
      } catch (XmlParseException e) {
         Log.global.log(Log.SILENT_WARNING, "Error with XML0/Parameter/Channel for " + channelId + " " + pingConversion.getDateAndFileString() + ": " + e);
         slope = 0;
      }

      if (raw0Datagram.real == null || raw0Datagram.imag == null) {
         throw new IllegalArgumentException();
      }
      setData(raw0Datagram.real, raw0Datagram.imag, slope);
   }

   @Override
   public String getDataTypeName() {
      return BROADBAND_DATA_TYPE_NAME;
   }

   @Override
   public PowerData getPowerData() {
      PowerData powerData = this.powerData;
      if (powerData == null) {
         powerData = new PowerData(this, (float) pulseCompression.getTauEff());
         RawFileTransducer transducerCopy = getTransducer().makeCopy();
         float centerFrequency = getCenterFrequency();
         powerData.setFrequency(centerFrequency);
         initTransducer(transducerCopy, centerFrequency);
         transducerCopy.calibrate(ChannelCalibration.EMPTY);
         powerData.setTransducer(transducerCopy);
         computeSvAtCenterFrequency(powerData.getSv());
         if (angleData != null) {
            powerData.setAngleData(angleData);
         }
         powerData.setReadOnly(this);
         this.powerData = powerData;
      }
      return powerData;
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      Xml0Datagram xml0Datagram = toXml0Parameter(pulseForm, element -> {
         element
               .addAttribute("FrequencyStart", Utils.toString(getStartFrequency()))
               .addAttribute("FrequencyEnd", Utils.toString(getEndFrequency()));
      });
      return List.of(xml0Datagram, toRaw3Datagram());
   }

   public void initTransducer(RawFileTransducer transducer, float frequency) {
      transducer.setFrequency(frequency);
      transducer.setGainAndGainTable((float) getGain(frequency));
      int i = transducer.pulseDurationToIndex(getPulseDuration());
      if (i < 0) {
         // If not found then overwrite next larger entry
         i = Math.clamp(-(i + 1), 0, transducer.getPulseDurationTable().length - 1);
      }
      transducer.getPulseDurationTable()[i] = getPulseDuration();
      transducer.setEquivalentBeamAngle((float) getPsi(frequency));
      transducer.setBeamWidthAlongship((float) getAlongBeamWidth(frequency));
      transducer.setBeamWidthAthwartship((float) getAthwartBeamWidth(frequency));
      transducer.setAngleOffsetAlongship((float) getAlongAngleOffset(frequency));
      transducer.setAngleOffsetAthwartship((float) getAthwartAngleOffset(frequency));
      transducer.setAngleSensitivityAlongship(getAngleSensitivityAlongship());
      transducer.setAngleSensitivityAthwartship(getAngleSensitivityAthwartship());
   }

   @Override
   public Raw3Datagram toRaw3Datagram() {
      Raw3Datagram raw3Datagram = super.toRaw3Datagram();
      raw3Datagram.offset -= offsetCorrection;
      return raw3Datagram;
   }

   public Raw0Datagram toRaw0Datagram() {
      Raw0Datagram raw0Datagram = new Raw0Datagram(getInstant());
      setRaw0DatagramParameters(raw0Datagram);
      raw0Datagram.offset -= offsetCorrection;
      raw0Datagram.mode = (short) (getSectorCount() << 8 | Raw0Datagram.DATA_TYPE_COMPLEX_FLOAT_32);
      raw0Datagram.sweep = getSweep();
      raw0Datagram.real = getReal();
      raw0Datagram.imag = getImag();
      return raw0Datagram;
   }

   public BroadbandData makeCopyWithNoData() {
      return new BroadbandData(this);
   }

   @Override
   public BroadbandData makeCopy() {
      BroadbandData copy = makeCopyWithNoData();
      copy.setData(Utils.copy(getReal()), Utils.copy(getImag()), getSlope());
      return copy;
   }

   public BroadbandData makeCopyOfDepthRange(FloatRange depthRange) {
      depthRange = getDepthRange().intersection(depthRange);
      BroadbandData copy = makeCopyWithNoData();
      copy.setRange(depthRange.min(), depthRange.max());
      int from = copy.getOffset() - getOffset();
      int sectorCount = getSectorCount();
      float[][] real = new float[sectorCount][copy.getCount()];
      float[][] imag = new float[sectorCount][copy.getCount()];
      for (int sector = 0; sector < sectorCount; sector++) {
         System.arraycopy(getReal()[sector], from, real[sector], 0, copy.getCount());
         System.arraycopy(getImag()[sector], from, imag[sector], 0, copy.getCount());
      }
      copy.setData(real, imag, getSlope());
      return copy;
   }

   @Override
   protected void onSetTransducer() {
      initPulseCompression();
   }

   private void initPulseCompression() {
      pulseCompression = PulseCompressionCache.getPulseCompression(new PulseCompressionConfig(this,
            findEffectiveFilterChain(),
            new IdentityTransferFunction(),
            NotchFilterCache.getFilteredTransmitSignal(getPingConfiguration().getRawFileConfiguration().getBroadbandNotchFilterConfigs(), getFrequencyRange())));

      setOffset(getOffset() - offsetCorrection);
      offsetCorrection = -ComplexArrayUtils.maxIndex(pulseCompression.getAutoCorrelationTransmitSignal());
      setOffset(getOffset() + offsetCorrection);
      clearDerivedData();
   }

   private PulseCompressionFilterChain findEffectiveFilterChain() {
      PulseCompressionFilterChain originalFilterChain = getTransducer().getPulseCompressionFilterChain();
      List<PulseCompressionFilterConfig> filterConfigs = getPingConfiguration().getRawFileConfiguration().getPulseCompressionFilterMap()
            .getOrDefault(getTransducer().getChannelId(), List.of())
            .stream()
            .filter(filterConfig -> filterConfig.isValid(getSlope(), getPulseDuration(), getPulseForm()))
            .toList();
      return PulseCompressionFilterUtils.possiblyReplaceFilterChain(originalFilterChain, filterConfigs);
   }

   @Override
   void clearDerivedData() {
      averagePulseCompressedSignal = null;
      powerData = null;
      angleData = getTransducer().getBeamType() != BeamType.SINGLE ? new AngleData(this::computeAngles) : null;
   }

   public PulseCompression getPulseCompression() {
      return pulseCompression;
   }

   public ComplexArray getAveragePulseCompressedSignal() {
      ComplexArray averagePulseCompressedSignal = this.averagePulseCompressedSignal;
      if (averagePulseCompressedSignal == null) {
         averagePulseCompressedSignal = pulseCompression.computePulseCompressedSignal(computeAverageComplexValues());
         this.averagePulseCompressedSignal = averagePulseCompressedSignal;
      }
      return averagePulseCompressedSignal;
   }

   @Override
   public float getTvgRangeCorrection() {
      // Use 0 TVG range correction since `offsetCorrection` has already shifted the data towards the transducer. See #1415.
      return 0;
   }

   private void computeSvAtCenterFrequency(float[] sv) {
      BroadbandToSvAtCenterFrequency.computeSvAtCenterFrequency(this, sv);
   }

   private float[] computeAngles() {
      return new BroadbandToAngles(this).computeElectricalAngles(pulseCompression, 1);
   }

   public double getGain(double frequency) {
      BroadbandFunction broadbandGain = getCalibration().broadbandGain.orElse(null);
      if (broadbandGain != null) {
         return broadbandGain.getValue(frequency);
      } else {
         return getUncalibratedGain(frequency);
      }
   }

   public double getUncalibratedGain(double frequency) {
      RawFileTransducer transducer = getTransducer();
      double gainCorrection = 10 * Math.log10(frequency / transducer.getFrequency());
      int i = transducer.pulseDurationToClosestIndex(getPulseDuration());
      return transducer.getGainTable()[i] + gainCorrection;
   }

   public double getPsi(double frequency) {
      BroadbandFunction broadbandEquivalentBeamAngle = getCalibration().broadbandEquivalentBeamAngle.orElse(null);
      if (broadbandEquivalentBeamAngle != null) {
         return broadbandEquivalentBeamAngle.getValue(frequency);
      } else {
         return getUncalibratedPsi(frequency);
      }
   }

   public double getUncalibratedPsi(double frequency) {
      RawFileTransducer transducer = getTransducer();
      double psiCorrection = 20 * Math.log10(transducer.getFrequency() / frequency);
      return transducer.getEquivalentBeamAngle() + psiCorrection;
   }

   public float getDirectivityCorrection(int sampleIndex, float frequency) {
      if (angleData == null) {
         return 0;
      }
      return (float) getDirectivityCorrection(getMechanicalAlongAngle(sampleIndex, frequency), getMechanicalAthwartAngle(sampleIndex, frequency), frequency);
   }

   private double getDirectivityCorrection(double alongAngle, double athwartAngle, double frequency) {
      return AngleData.getDirectivityCorrection(alongAngle, athwartAngle,
            getAlongBeamWidth(frequency), getAthwartBeamWidth(frequency));
   }

   public float getMechanicalAlongAngle(int sampleIndex, float frequency) {
      if (angleData == null) {
         return 0;
      }
      return AngleData.electricalToMechanicalAngle(angleData.getElectricalAlongAngle(sampleIndex),
            getAngleSensitivityAlongship(),
            (float) getAlongAngleOffset(frequency));
   }

   public float getMechanicalAthwartAngle(int sampleIndex, float frequency) {
      if (angleData == null) {
         return 0;
      }
      return AngleData.electricalToMechanicalAngle(angleData.getElectricalAthwartAngle(sampleIndex),
            getAngleSensitivityAthwartship(),
            (float) getAthwartAngleOffset(frequency));
   }

   private float getAngleSensitivityAthwartship() {
      RawFileTransducer transducer = getTransducer();
      return transducer.getAngleSensitivityAthwartship() * getCenterFrequency() / transducer.getFrequency();
   }

   private float getAngleSensitivityAlongship() {
      RawFileTransducer transducer = getTransducer();
      return transducer.getAngleSensitivityAlongship() * getCenterFrequency() / transducer.getFrequency();
   }

   public double getAlongAngleOffset(double frequency) {
      BroadbandFunction broadbandAlongAngleOffset = getCalibration().broadbandAngleOffsetAlongship.orElse(null);
      if (broadbandAlongAngleOffset != null) {
         return broadbandAlongAngleOffset.getValue(frequency);
      } else {
         return getUncalibratedAlongAngleOffset();
      }
   }

   public double getUncalibratedAlongAngleOffset() {
      RawFileTransducer transducer = getTransducer();
      return transducer.getAngleOffsetAlongship();
   }

   public double getAthwartAngleOffset(double frequency) {
      BroadbandFunction broadbandAthwartAngleOffset = getCalibration().broadbandAngleOffsetAthwartship.orElse(null);
      if (broadbandAthwartAngleOffset != null) {
         return broadbandAthwartAngleOffset.getValue(frequency);
      } else {
         return getUncalibratedAthwartAngleOffset();
      }
   }

   public double getUncalibratedAthwartAngleOffset() {
      RawFileTransducer transducer = getTransducer();
      return transducer.getAngleOffsetAthwartship();
   }

   public double getAlongBeamWidth(double frequency) {
      BroadbandFunction broadbandAlongBeamWidth = getCalibration().broadbandBeamWidthAlongship.orElse(null);
      if (broadbandAlongBeamWidth != null) {
         return broadbandAlongBeamWidth.getValue(frequency);
      } else {
         return getUncalibratedAlongBeamWidth(frequency);
      }
   }

   public double getUncalibratedAlongBeamWidth(double frequency) {
      RawFileTransducer transducer = getTransducer();
      return getCorrectedBeamWidth(transducer.getBeamWidthAlongship(), frequency, transducer.getFrequency());
   }

   public double getAthwartBeamWidth(double frequency) {
      BroadbandFunction broadbandAthwartBeamWidth = getCalibration().broadbandBeamWidthAthwartship.orElse(null);
      if (broadbandAthwartBeamWidth != null) {
         return broadbandAthwartBeamWidth.getValue(frequency);
      } else {
         return getUncalibratedAthwartBeamWidth(frequency);
      }
   }

   public double getUncalibratedAthwartBeamWidth(double frequency) {
      RawFileTransducer transducer = getTransducer();
      return getCorrectedBeamWidth(transducer.getBeamWidthAthwartship(), frequency, transducer.getFrequency());
   }

   private static double getCorrectedBeamWidth(double beamWidth, double frequency, double nominalFrequency) {
      return 2 * Math.toDegrees(Math.asin(Math.sin(Math.toRadians(beamWidth) / 2) * nominalFrequency / frequency));
   }

   @Override
   public float getStopFrequency() {
      return getEndFrequency();
   }

   @Override
   public float getEndFrequency() {
      return endFrequency;
   }

   public void setEndFrequency(float endFrequency) {
      throwExceptionIfReadOnly();
      this.endFrequency = endFrequency;
   }

   public float getSweep() {
      return (getEndFrequency() - getStartFrequency()) / getPulseDuration();
   }

   @Override
   public @Nullable AngleData getAngleData() {
      return angleData;
   }

   @Override
   public float getTSU(int sampleIndex) {
      float sv = BroadbandToSvAtCenterFrequency.computeSvAtCenterFrequency(this, sampleIndex);
      float logSv = PowerData.svToLogSv(sv);
      float r = getTvgRange(sampleIndex);
      float logR = 20 * (float) Math.log10(r);
      // From PowerData: svToTsConstant = (float) (10 * Math.log10(c * tauEff * 0.5)) + psi + 2 * saCorr;
      float svToTsConstant = 10 * (float) Math.log10(getSoundVelocity() * pulseCompression.getTauEff() * 0.5)
            + (float) getPsi(getCenterFrequency());
      return logSv + logR + svToTsConstant;
   }

   public float getTSC(int sampleIndex, float frequency) {
      return getTSU(sampleIndex) + getDirectivityCorrection(sampleIndex, frequency);
   }

   public Absorption getAbsorption() {
      return getPingConfiguration().getRawFileConfiguration().getXml0Info().getAbsorption();
   }

   @Override
   public Complex getTransducerImpedanceForSector(int sectorIndex) {
      int iCenterFrequency = switch (pulseForm) {
         case PulseForm.BROADBAND_LINEAR_UP,
              PulseForm.BROADBAND_LINEAR_DOWN -> {
            float pulseSampleCount = getPulseDuration() / getSampleInterval();
            yield Math.round(pulseSampleCount / 2);
         }
         default -> throw new ShouldNotHappenException("Unknown pulse form: " + pulseForm);
      };
      return calculateComplexImpedanceForSector(sectorIndex, 0).toComplex(iCenterFrequency);
      // NOTE: aSlope in call of calculateComplexImpedance is INTENTIONALLY set to zero. Avoids having to compensate the center frequency index for windowing.
   }

   public int getPulseForm() {
      return pulseForm;
   }
}
