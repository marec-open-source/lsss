package no.imr.korona.data.ping.items.channel;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Raw0Datagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.TvgArray;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.math.MathUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlParse;
import no.imr.tools.xml.XmlParseException;
import no.marec.lsss.api.data.SvChannelData;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Channel data with power, and possibly angles.
 */
public final class PowerData extends ChannelData implements SvChannelData {
   public static final int DATA_TYPE_POWER = 1;
   public static final int DATA_TYPE_ANGLES = 2;
   // public static final int DATA_TYPE_COMPLEX_FLOAT_16 = 4;
   public static final int DATA_TYPE_COMPLEX_FLOAT_32 = 8;

   private static final double LOG10_2_BY_256 = Math.log10(2) / 256;
   public static final float POWER2DB = (float) (10 * LOG10_2_BY_256);

   /**
    * The power value that is chosen to represent s<sub>V</sub> = 0.
    */
   public static final short EK60_SHORT_POWER_NULL = -20000;

   private static final float MIN_SHORT_POWER_AS_LINEAR_POWER = shortPowerToLinearPower(EK60_SHORT_POWER_NULL);
   private static final float MAX_SHORT_POWER_AS_LINEAR_POWER = shortPowerToLinearPower(Short.MAX_VALUE);

   /**
    * The value 4 &pi; 1852<sup>2</sup>.
    */
   public static final float IMR_CONSTANT = (float) (4 * Math.PI * 1852.0 * 1852.0);

   /**
    * Conversion from short power P read from the raw file, to linear power p times {@link #IMR_CONSTANT}.
    * <pre>
    * 10*log(p) = P * 10*log(2)/256
    * p = 10 ^ (P * log(2) / 256)
    * table entry = p * IMR_CONSTANT
    * </pre>
    */
   private static final float[] SHORT_POWER_TO_LINEAR_POWER_TIMES_IMR_CONSTANT = new float[0x10000];

   static {
      for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
         SHORT_POWER_TO_LINEAR_POWER_TIMES_IMR_CONSTANT[i & 0xffff] = shortPowerToLinearPower(i) * IMR_CONSTANT;
      }
   }

   public static final float MISSING_EFFECTIVE_PULSE_DURATION = -1;
   private static final String XML0_PARAMETER_EFFECTIVE_PULSE_DURATION = "EffectivePulseDuration";

   private volatile float @Nullable [] nullableSv; // Linear Sv (non-logarithmic)
   private volatile float @Nullable [] nullableLogSv; // Logarithmic Sv (dB) = 10 * log10(s_v / IMR_CONSTANT)

   private @Nullable AngleData angleData;

   private float effectivePulseDuration = MISSING_EFFECTIVE_PULSE_DURATION;
   private float constant;
   private float svToTsConstant;

   public PowerData(Instant instant) {
      super(instant);
   }

   public PowerData(ChannelData channelData) {
      super(channelData);

      computeConstants();
   }

   public PowerData(ChannelData channelData, float effectivePulseDuration) {
      super(channelData);

      this.effectivePulseDuration = effectivePulseDuration;
      computeConstants();
   }

   private PowerData(PowerData powerData) {
      super(powerData);

      // Data not copied.
      nullableSv = null;
      nullableLogSv = null;
      angleData = null;

      effectivePulseDuration = powerData.effectivePulseDuration;
      constant = powerData.constant;
      svToTsConstant = powerData.svToTsConstant;
   }

   public PowerData(Raw0Datagram raw0Datagram, PingConversion pingConversion) {
      super(raw0Datagram);

      RawFileTransducer transducer = pingConversion.getPingConfiguration().getRawFileConfiguration().getTransducers().get(getChannel() - 1);
      effectivePulseDuration = findEffectivePulseDuration(pingConversion, transducer.getChannelId());

      setPingConfiguration(pingConversion.getPingConfiguration());

      if (raw0Datagram.power != null) {
         nullableSv = shortPowerToSv(raw0Datagram.power);
      }
      if (raw0Datagram.angles != null) {
         setElectricAngles(AngleData.bytesToElectricalAngles(raw0Datagram.angles, transducer));
      }
   }

   public static float findEffectivePulseDuration(PingConversion pingConversion, String channelId) {
      Element channelParameter = pingConversion.getIdToChannelParameter().get(channelId);
      if (channelParameter != null) {
         try {
            return XmlParse.floatAttribute(channelParameter, XML0_PARAMETER_EFFECTIVE_PULSE_DURATION, MISSING_EFFECTIVE_PULSE_DURATION);
         } catch (XmlParseException e) {
            Log.global.log(Log.SILENT_WARNING, "Error with XML0/Parameter/Channel for " + channelId + " " + pingConversion.getDateAndFileString() + ": " + e);
         }
      }
      return MISSING_EFFECTIVE_PULSE_DURATION;
   }

   @Override
   public String getDataTypeName() {
      return "Narrowband data (EK60-style, real data)";
   }

   private Raw0Datagram toRaw0Datagram() {
      Raw0Datagram raw0Datagram = new Raw0Datagram(getInstant());
      setRaw0DatagramParameters(raw0Datagram);
      raw0Datagram.power = computeShortPower();
      if (angleData != null) {
         raw0Datagram.angles = AngleData.electricalAnglesToBytes(angleData.getElectricalAngles(), getTransducer());
      } else {
         raw0Datagram.angles = null;
      }
      raw0Datagram.mode = Raw0Datagram.DATA_TYPE_POWER;
      if (raw0Datagram.angles != null) {
         raw0Datagram.mode |= Raw0Datagram.DATA_TYPE_ANGLES;
      }
      return raw0Datagram;
   }

   @Override
   public PowerData getPowerData() {
      return this;
   }

   @Override
   public List<BaseDatagram> toDatagrams() {
      if (effectivePulseDuration != MISSING_EFFECTIVE_PULSE_DURATION) {
         return List.of(toXml0ParameterDatagram(), toRaw0Datagram());
      } else {
         return List.of(toRaw0Datagram());
      }
   }

   public Xml0Datagram toXml0ParameterDatagram() {
      Element rootElement = DocumentHelper.createElement("Parameter");
      rootElement.addElement("Channel")
            .addAttribute("ChannelID", getTransducer().getChannelId())
            .addAttribute(XML0_PARAMETER_EFFECTIVE_PULSE_DURATION, Utils.toString(effectivePulseDuration));
      return new Xml0Datagram(getInstant(), DocumentHelper.createDocument(rootElement));
   }

   public PowerData makeCopyWithNoData() {
      return new PowerData(this);
   }

   public PowerData makeCopyWithAnglesOnly() {
      PowerData copy = makeCopyWithNoData();
      if (angleData != null) {
         copy.setElectricAngles(angleData.getElectricalAngles().clone());
      }
      return copy;
   }

   @Override
   public PowerData makeCopy() {
      PowerData copy = makeCopyWithNoData();
      copy.setSv(getSv().clone());
      if (angleData != null) {
         copy.setElectricAngles(angleData.getElectricalAngles().clone());
      }
      return copy;
   }

   /**
    * Creates a copy of this datagram.
    * Sample values and angles or broadband data are copied.
    *
    * @param depthRange the depth range to copy
    * @return the copy
    */
   public PowerData makeCopyOfDepthRange(FloatRange depthRange) {
      depthRange = getDepthRange().intersection(depthRange);
      PowerData copy = makeCopyWithNoData();
      copy.setRange(depthRange.min(), depthRange.max());
      int from = copy.getOffset() - getOffset();
      System.arraycopy(getSv(), from, copy.getSv(), 0, copy.getCount());
      if (angleData != null) {
         copy.setElectricAngles(Arrays.copyOfRange(angleData.getElectricalAngles(), 2 * from, 2 * (from + copy.getCount())));
      }
      return copy;
   }

   @Override
   public void setCount(int count) {
      if (getCount() == count) {
         // Same count => Keep all data.
         return;
      }
      // Different count => Remove all data.
      super.setCount(count);

      nullableSv = null;
      nullableLogSv = null;
      removeAngles();
   }

   private void computeConstants() {
      RawFileTransducer transducer = getTransducer();
      float tau = getPulseDuration();
      float tauEff = getEffectivePulseDuration();
      float G = transducer.getGainForPulseDuration(tau);
      float psi = transducer.getEquivalentBeamAngle();
      float pt = getTransmitPower();
      float c = getSoundVelocity();
      float f = getFrequency();
      float lambda = c / f;
      float saCorr = transducer.getSaCorrection(tau);

      constant = (float) (
            Math.pow(10, -(2 * (G + saCorr) + psi) / 10)
                  * (32 * Math.PI * Math.PI)
                  / (pt * lambda * lambda * c * tauEff));
      svToTsConstant = (float) (10 * Math.log10(c * tauEff * 0.5)) + psi + 2 * saCorr;
   }

   public float getEffectivePulseDuration() {
      return effectivePulseDuration != MISSING_EFFECTIVE_PULSE_DURATION ? effectivePulseDuration : getPulseDuration();
   }

   public void setEffectivePulseDuration(float effectivePulseDuration) {
      throwExceptionIfReadOnly();
      this.effectivePulseDuration = effectivePulseDuration;
   }

   @Override
   protected void onSetTransducer() {
      computeConstants();
   }

   public float shortPowerToNpi(short i) {
      return SHORT_POWER_TO_LINEAR_POWER_TIMES_IMR_CONSTANT[i & 0xffff] * constant;
   }

   float linearPowerToNoisePowerIndex(float linearPower) {
      return linearPower * IMR_CONSTANT * constant;
   }

   public short npiToShortPower(float npi) {
      float linearPower = npi / (constant * IMR_CONSTANT);
      return linearPowerToShortPower(linearPower);
   }

   static short linearPowerToShortPower(float linearPower) {
      if (linearPower <= MIN_SHORT_POWER_AS_LINEAR_POWER) {
         return EK60_SHORT_POWER_NULL;
      }
      if (linearPower >= MAX_SHORT_POWER_AS_LINEAR_POWER) {
         return Short.MAX_VALUE;
      }
      return (short) Math.round(Math.log10(linearPower) / LOG10_2_BY_256);
   }

   static float shortPowerToLinearPower(int shortPower) {
      return (float) Math.pow(10, shortPower * LOG10_2_BY_256);
   }

   public short[] computeShortPower() {
      TvgArray tvg = getTVGArray();
      float[] sv = getSv();
      short[] shortPower = new short[sv.length];
      for (int i = 0; i < shortPower.length; i++) {
         shortPower[i] = npiToShortPower(sv[i] / tvg.get(i));
      }
      return shortPower;
   }

   public void setSvFromShortPower(short[] shortPower) {
      throwExceptionIfReadOnly();
      setCount(shortPower.length);
      setSv(shortPowerToSv(shortPower));
   }

   private float[] shortPowerToSv(short[] shortPower) {
      float[] sv = new float[shortPower.length];
      TvgArray tvg = getTVGArray();
      for (int i = 0; i < sv.length; i++) {
         sv[i] = MathUtils.avoidInfinity(shortPowerToNpi(shortPower[i]) * tvg.get(i));
      }
      return sv;
   }

   public void setSv(float[] sv) {
      throwExceptionIfReadOnly();
      setCount(sv.length);
      nullableSv = sv;
      nullableLogSv = null;
   }

   public void setLogSv(float[] logSv) {
      throwExceptionIfReadOnly();
      setCount(logSv.length);
      nullableLogSv = logSv;
      nullableSv = null;
   }

   public float[] computeNoisePowerIndex() {
      TvgArray tvg = getTVGArray();
      float[] sv = getSv();
      float[] npi = new float[getCount()];
      for (int i = 0; i < npi.length; i++) {
         npi[i] = MathUtils.avoidInfinity(sv[i] / tvg.get(i));
      }
      return npi;
   }

   /**
    * Returns the noise power index threshold value for noise.
    * <p>
    * Noise power index &lt; noise threshold ⇒ noise
    *
    * @return noise threshold
    */
   public float getNoisePowerIndexNoiseThreshold() {
      return shortPowerToNpi((short) (EK60_SHORT_POWER_NULL + 10));
   }

   /**
    * Integrates s<sub>v</sub> vertically.
    *
    * @param depthRange the vertical range for the integral
    * @param svRange    only use values contained in this range
    * @return vertical integral of s<sub>v</sub>
    */
   public double getVerticalIntegralSv(FloatRange depthRange, FloatRange svRange) {
      float iBeginAsFloat = depthToClampedSampleIndexAsFloat(depthRange.min());
      float iEndAsFloat = depthToClampedSampleIndexAsFloat(depthRange.max());

      int iBegin = (int) Math.ceil(iBeginAsFloat);
      int iEnd = (int) Math.floor(iEndAsFloat);

      //    iBeginAsFloat                                  iEndAsFloat
      //          |  iBegin                            iEnd    |
      //          |    |                                |      |
      // ---|----------|----------|----------|----------|----------|---> Samples

      float[] svArray = getSv();
      if (iBegin > iEnd) {
         // => Begin and end are in the same sample.
         float sv = svArray[iEnd];
         if (svRange.contains(sv)) {
            return sv * (iEndAsFloat - iBeginAsFloat) * getSampleDistance();
         } else {
            return 0;
         }
      }

      double svSum = 0;
      if (iBegin > 0) {
         float sv = svArray[iBegin - 1];
         if (svRange.contains(sv)) {
            svSum += sv * (iBegin - iBeginAsFloat);
         }
      }
      for (int i = iBegin; i < iEnd; i++) {
         float sv = svArray[i];
         if (svRange.contains(sv)) {
            svSum += sv;
         }
      }
      if (iEnd < svArray.length) {
         float sv = svArray[iEnd];
         if (svRange.contains(sv)) {
            svSum += sv * (iEndAsFloat - iEnd);
         }
      }
      return svSum * getSampleDistance();
   }

   public double getVerticalIntegralNoise(FloatRange depthRange, FloatRange noiseRange) {
      float iBeginAsFloat = depthToClampedSampleIndexAsFloat(depthRange.min());
      float iEndAsFloat = depthToClampedSampleIndexAsFloat(depthRange.max());

      int iBegin = (int) Math.ceil(iBeginAsFloat);
      int iEnd = (int) Math.floor(iEndAsFloat);

      //    iBeginAsFloat                                  iEndAsFloat
      //          |  iBegin                            iEnd    |
      //          |    |                                |      |
      // ---|----------|----------|----------|----------|----------|---> Samples

      float[] svArray = getSv();
      TvgArray tvg = getTVGArray();
      if (iBegin > iEnd) {
         // => Begin and end are in the same sample.
         float npi = svArray[iEnd] / tvg.get(iEnd);
         if (noiseRange.contains(npi)) {
            return npi * (iEndAsFloat - iBeginAsFloat) * getSampleDistance();
         } else {
            return 0;
         }
      }

      double npiSum = 0;
      if (iBegin > 0) {
         float noise = svArray[iBegin - 1] / tvg.get(iBegin - 1);
         if (noiseRange.contains(noise)) {
            npiSum += noise * (iBegin - iBeginAsFloat);
         }
      }
      for (int i = iBegin; i < iEnd; i++) {
         float npi = svArray[i] / tvg.get(i);
         if (noiseRange.contains(npi)) {
            npiSum += npi;
         }
      }
      if (iEnd < svArray.length) {
         float noise = svArray[iEnd] / tvg.get(iEnd);
         if (noiseRange.contains(noise)) {
            npiSum += noise * (iEndAsFloat - iEnd);
         }
      }
      return npiSum * getSampleDistance();
   }

   /**
    * Get Sv linear (non-logarithmic).
    * <p>
    * Note: Handles asynchronous access.
    *
    * @return the linear Sv values
    */
   @Override
   public float[] getSv() {
      float[] sv = nullableSv;
      if (sv == null) {
         sv = new float[getCount()];
         float[] logSv = nullableLogSv;
         if (logSv != null) {
            for (int i = 0; i < sv.length; i++) {
               sv[i] = logSvToSv(logSv[i]);
            }
         }
         nullableSv = sv;
      }
      return sv;
   }

   /**
    * Get Sv (logarithmic, in dB).
    * <p>
    * Note: Handles asynchronous access.
    *
    * @return the logarithmic Sv values
    */
   public float[] getLogSv() {
      float[] logSv = nullableLogSv;
      if (logSv == null) {
         logSv = new float[getCount()];
         float[] sv = nullableSv;
         if (sv != null) {
            for (int i = 0; i < logSv.length; i++) {
               logSv[i] = svToLogSv(sv[i]);
            }
         } else {
            Arrays.fill(logSv, svToLogSv(0));
         }
         nullableLogSv = logSv;
      }
      return logSv;
   }

   @Override
   public float getTSU(int sampleIndex) {
      float logSv = getLogSv()[sampleIndex];
      float r = getTvgRange(sampleIndex);
      float logR = (float) (20 * Math.log10(r));
      return logSv + logR + svToTsConstant;
   }

   public float getLinearTSU(int sampleIndex) {
      return logSvToSv(getTSU(sampleIndex));
   }

   public float[] getLinearTSU() {
      float[] values = new float[getCount()];
      for (int i = 0; i < values.length; i++) {
         values[i] = getLinearTSU(i);
      }
      return values;
   }

   public float getTSC(int sampleIndex) {
      return getTSU(sampleIndex) + getDirectivityCorrection(sampleIndex);
   }

   public float getLinearTSC(int sampleIndex) {
      return logSvToSv(getTSC(sampleIndex));
   }

   public float[] getLinearTSC() {
      float[] values = new float[getCount()];
      for (int i = 0; i < values.length; i++) {
         values[i] = getLinearTSC(i);
      }
      return values;
   }

   /**
    * Converts logarithmic Sv value to linear Sv value.
    *
    * @param logSvValue the logarithmic Sv value
    * @return the linear Sv value
    */
   public static float logSvToSv(float logSvValue) {
      return MathUtils.avoidInfinity((float) (IMR_CONSTANT * Math.pow(10, logSvValue / 10)));
   }

   /**
    * Converts linear Sv value to logarithmic Sv value.
    *
    * @param svValue the linear Sv value
    * @return the logarithmic Sv value
    */
   public static float svToLogSv(float svValue) {
      return MathUtils.avoidInfinity((float) (10 * Math.log10(svValue / IMR_CONSTANT)));
   }

   public void setElectricAngles(float[] electricalAngles) {
      throwExceptionIfReadOnly();
      if (electricalAngles.length != 2 * getCount()) {
         throw new IllegalArgumentException("Wrong angle length: " + electricalAngles.length + ", count: " + getCount());
      }
      angleData = new AngleData(electricalAngles);
   }

   @Override
   public void removeAngles() {
      throwExceptionIfReadOnly();
      angleData = null;
   }

   @Override
   public void reduceData(int beginSampleIndex, int endSampleIndex) {
      throwExceptionIfReadOnly();
      float[] sv = getSv();
      AngleData originalAngleData = angleData;

      setOffset(getOffset() + beginSampleIndex);
      setSv(Arrays.copyOfRange(sv, beginSampleIndex, endSampleIndex));
      if (originalAngleData != null) {
         setElectricAngles(Arrays.copyOfRange(originalAngleData.getElectricalAngles(), 2 * beginSampleIndex, 2 * endSampleIndex));
      }
   }

   @Override
   public float getMechanicalAlongAngle(int sampleIndex) {
      if (angleData == null) {
         return 0;
      }
      return angleData.getMechanicalAlongAngle(sampleIndex, getTransducer());
   }

   @Override
   public float getMechanicalAthwartAngle(int sampleIndex) {
      if (angleData == null) {
         return 0;
      }
      return angleData.getMechanicalAthwartAngle(sampleIndex, getTransducer());
   }

   public float getDirectivityCorrection(int sampleIndex) {
      if (angleData == null) {
         return 0;
      }
      return angleData.getDirectivityCorrection(sampleIndex, getTransducer());
   }

   @Override
   public @Nullable AngleData getAngleData() {
      return angleData;
   }

   void setAngleData(AngleData angleData) {
      throwExceptionIfReadOnly();
      this.angleData = angleData;
   }
}
