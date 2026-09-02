package no.imr.korona.data.ping.items.channel;

import no.imr.korona.computation.broadband.EK80Parameters;
import no.imr.korona.data.datagrams.Raw0Datagram;
import no.imr.korona.data.datagrams.Raw3Datagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.formats.ek60.calibration.BroadbandFunction;
import no.imr.tools.Utils;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.MathUtils;
import org.apache.commons.numbers.complex.Complex;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.time.Instant;
import java.util.function.Consumer;

public abstract class ComplexChannelData extends ChannelData {
   private static final float[][] EMPTY_ARRAY = new float[0][];

   private float[][] real = EMPTY_ARRAY;
   private float[][] imag = EMPTY_ARRAY;
   private float slope;

   ComplexChannelData(Instant instant) {
      super(instant);
   }

   ComplexChannelData(ChannelData channelData) {
      super(channelData);
   }

   ComplexChannelData(Raw0Datagram raw0Datagram) {
      super(raw0Datagram);
   }

   public void setData(float[][] real, float[][] imag, float slope) {
      setCount(real[0].length);
      this.real = real;
      this.imag = imag;
      this.slope = slope;
      clearDerivedData();
   }

   abstract void clearDerivedData();

   public float[][] getReal() {
      return real;
   }

   public float[][] getImag() {
      return imag;
   }

   public float getSlope() {
      return slope;
   }

   public int getSectorCount() {
      return real.length;
   }

   public double getPrxFactor(double frequency) {
      double rwbtrx = getTransducer().getEK80rwbtrx();
      double ztde = getTransducerImpedance(frequency);
      return (getSectorCount() / 8.0) * MathUtils.sq((rwbtrx + ztde) / rwbtrx) / ztde;
   }

   public double getTransducerImpedance(double frequency) {
      BroadbandFunction broadbandTransducerImpedance = getCalibration().broadbandTransducerImpedance.orElse(null);
      if (broadbandTransducerImpedance != null) {
         return broadbandTransducerImpedance.getValue(frequency);
      } else {
         return getUncalibratedTransducerImpedance(frequency);
      }
   }

   public static double getUncalibratedTransducerImpedance(double frequency) {
      return EK80Parameters.ztrd;
   }

   public Xml0Datagram toXml0Parameter(int pulseForm, Consumer<Element> typeSpecificAttributes) {
      Element rootElement = DocumentHelper.createElement("Parameter");
      Element channelElement = rootElement.addElement("Channel")
            .addAttribute("ChannelID", getTransducer().getChannelId())
            .addAttribute("ChannelMode", Short.toString(getTransmitMode()))
            .addAttribute("PulseForm", Integer.toString(pulseForm))
            .addAttribute("PulseDuration", Utils.toString(getPulseDuration()))
            .addAttribute("SampleInterval", Utils.toString(getSampleInterval()))
            .addAttribute("TransmitPower", Utils.toString(getTransmitPower()))
            .addAttribute("Slope", Utils.toString(slope));
      typeSpecificAttributes.accept(channelElement);

      // These are removed from XML0/Parameter in recent versions:
      channelElement
            .addAttribute("TransducerDepth", Float.toString(getTransducerDepth()))
            .addAttribute("BandWidth", Float.toString(getBandWidth()));

      return new Xml0Datagram(getInstant(), DocumentHelper.createDocument(rootElement));
   }

   public Raw3Datagram toRaw3Datagram() {
      Raw3Datagram raw3Datagram = new Raw3Datagram(getInstant());
      raw3Datagram.channelId = getTransducer().getChannelId();
      raw3Datagram.dataType = (short) (getSectorCount() << 8 | PowerData.DATA_TYPE_COMPLEX_FLOAT_32);
      // raw3Datagram.spare = 0;
      raw3Datagram.offset = getOffset();
      raw3Datagram.count = getCount();
      raw3Datagram.real = real;
      raw3Datagram.imag = imag;
      return raw3Datagram;
   }

   public ComplexArray computeAverageComplexValues() {
      int count = getCount();
      ComplexArray averageValues = ComplexArray.ofLength(count);
      int sectorCount = getSectorCount();
      for (int i = 0; i < count; i++) {
         double re = 0;
         double im = 0;
         for (int sector = 0; sector < sectorCount; sector++) {
            re += real[sector][i];
            im += imag[sector][i];
         }
         averageValues.set(i, re / sectorCount, im / sectorCount);
      }

      return averageValues;
   }

   @Override
   public void removeAngles() {
      throwExceptionIfReadOnly();
      int sectorCount = getSectorCount();
      if (sectorCount == 1) {
         return;
      }
      double sqrtSectorCount = Math.sqrt(sectorCount);
      int count = getCount();
      float[] newReal = new float[count];
      float[] newImag = new float[count];
      for (int i = 0; i < count; i++) {
         double realSum = 0;
         double imagSum = 0;
         for (int sector = 0; sector < sectorCount; sector++) {
            realSum += real[sector][i];
            imagSum += imag[sector][i];
         }
         newReal[i] = (float) (realSum / sqrtSectorCount);
         newImag[i] = (float) (imagSum / sqrtSectorCount);
      }
      setData(new float[][]{newReal}, new float[][]{newImag}, slope);
   }

   @Override
   public void reduceData(int beginSampleIndex, int endSampleIndex) {
      throwExceptionIfReadOnly();
      int sectorCount = getSectorCount();
      int count = endSampleIndex - beginSampleIndex;
      float[][] newReal = new float[sectorCount][count];
      float[][] newImag = new float[sectorCount][count];
      for (int sectorIndex = 0; sectorIndex < sectorCount; sectorIndex++) {
         System.arraycopy(real[sectorIndex], beginSampleIndex, newReal[sectorIndex], 0, count);
         System.arraycopy(imag[sectorIndex], beginSampleIndex, newImag[sectorIndex], 0, count);
      }
      setOffset(getOffset() + beginSampleIndex);
      setData(newReal, newImag, slope);
   }

   ComplexArray calculateComplexImpedanceForSector(int sectorIndex, float aSlope) {
      int count = (int) Math.ceil(getPulseDuration() / getSampleInterval());
      int first = (int) Math.floor(Math.min(3 * aSlope, 0.5) * count);
      int length = (int) Math.floor((1 - Math.min(3 * aSlope, 0.5)) * count) - first + 1;

      float[] sectorReal = real[sectorIndex];
      float[] sectorImag = imag[sectorIndex];

      ComplexArray impedance = ComplexArray.ofLength(length);
      for (int i = 0; i < length; i++) {
         int realIntBits = Float.floatToIntBits(sectorReal[i + first]);
         int imagIntBits = Float.floatToIntBits(sectorImag[i + first]);

         float realCurrent = Float.intBitsToFloat(realIntBits << 16);
         float realUnbiasedVoltage = Float.intBitsToFloat(realIntBits & 0xffff0000);

         float imagCurrent = Float.intBitsToFloat(imagIntBits << 16);
         float imagUnbiasedVoltage = Float.intBitsToFloat(imagIntBits & 0xffff0000);

         // a + ib   ac + bd + i(bc - ad)
         // ------ = ---------------------
         // c + id         c^2 + d^2
         float nominator = realCurrent * realCurrent + imagCurrent * imagCurrent;

         float realImpedance = (realUnbiasedVoltage * realCurrent + imagUnbiasedVoltage * imagCurrent) / nominator;
         float imagImpedance = (imagUnbiasedVoltage * realCurrent - realUnbiasedVoltage * imagCurrent) / nominator;

         impedance.set(i, realImpedance, imagImpedance);
      }
      return impedance;
   }

   public abstract Complex getTransducerImpedanceForSector(int sectorIndex);
}
