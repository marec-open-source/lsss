package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.KoronaUtils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.function.Function;

public final class Raw1Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("RAW1", Raw1Datagram::new);

   public static final short BEAM_MODE_OMNI = 0;
   public static final short BEAM_MODE_VERTICAL = 2;

   public static Function<Raw1Datagram, ? extends PingItem> toPingItem = Function.identity();

   public short channel; // 1,2,3...
   public byte dataType; // Data type: Bit0 = Power, Bit1 = Angle, Bit2 = ComplexFloat16, Bit 3 = ComplexFloat32, Bit 8-10: # of Complex per Samples
   public byte complexPerSample;
   public float gainTx; // Tx Gain [dB] fTransducerDepth; // [m]
   public float frequency; // [Hz]
   public float transmitPower; // [W]
   public float pulseLength; // [s]
   public float bandWidth; // [Hz]
   public float sampleInterval; // [s]
   public float soundVelocity; // [m/s]
   public float absorptionCoefficient; // [dB/m]
   public float heave; // [m]
   public float roll; // [deg]
   public float pitch; // [deg]
   public float temperature; // [C]
   public float heading; // [deg]
   public short transmitMode; // 0=Active, 1=Passive, 2=Test, -1=Unknown
   public short pulseForm; // 0=CW, 1=LFM, 2=HFM
   public float beamTilt; // fDirX;
   public float beamBearing; // fDirY;
   public float notUsed; // fDirZ;
   public float gainRx; // fGain; // [dB]
   public float saCorrection; // [dB]
   public float equivalentBeamAngle; // [dB]
   public float beamWidthHorizontalRx; // fBeamWidthAlongship; // [degree]
   public float beamWidthVerticalRx; // fBeamWidthAthwartship; // [degree]
   public float angleSensitivityAlongship;
   public float angleSensitivityAthwartship;
   public float angleOffsetAlongship; // [degree]
   public float angleOffsetAthwartship; // [degree]
   public byte[] spare = new byte[2]; // future use?
   public short noiseFilter; // 0=Off, 1=Weak, 2=Medium, 3=Strong
   public short beamWidthMode; // 0=Normal, 1=Narrow, 2=Wide
   public short beamMode; // 0 = Omni 1 = Vertical      (Seems to be wrong: Data uses 2 = Vertical)
   public float beamWidthHorizontalTx; // 360 deg for omni 7.5 degrees for Vertical
   public float beamWidthVerticalTx; // 7.5 deg for omni 180 degrees for Vertical
   public int offset; // first sample
   public int count; // no. of samples
   public float[] data;

   public Raw1Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      channel = byteBuffer.getShort();
      dataType = byteBuffer.get();
      complexPerSample = byteBuffer.get();
      if (complexPerSample <= 0) {
         throw new DatagramFormatException("complexPerSample: " + complexPerSample);
      }
      gainTx = byteBuffer.getFloat();
      frequency = byteBuffer.getFloat();
      transmitPower = byteBuffer.getFloat();
      pulseLength = byteBuffer.getFloat();
      bandWidth = byteBuffer.getFloat();
      sampleInterval = byteBuffer.getFloat();
      soundVelocity = byteBuffer.getFloat();
      absorptionCoefficient = byteBuffer.getFloat();
      heave = byteBuffer.getFloat();
      roll = byteBuffer.getFloat();
      pitch = byteBuffer.getFloat();
      temperature = byteBuffer.getFloat();
      heading = byteBuffer.getFloat();
      transmitMode = byteBuffer.getShort();
      pulseForm = byteBuffer.getShort();
      beamTilt = byteBuffer.getFloat();
      beamBearing = byteBuffer.getFloat();
      notUsed = byteBuffer.getFloat();
      gainRx = byteBuffer.getFloat();
      saCorrection = byteBuffer.getFloat();
      equivalentBeamAngle = byteBuffer.getFloat();
      beamWidthHorizontalRx = byteBuffer.getFloat();
      beamWidthVerticalRx = byteBuffer.getFloat();
      angleSensitivityAlongship = byteBuffer.getFloat();
      angleSensitivityAthwartship = byteBuffer.getFloat();
      angleOffsetAlongship = byteBuffer.getFloat();
      angleOffsetAthwartship = byteBuffer.getFloat();
      byteBuffer.get(spare);
      noiseFilter = byteBuffer.getShort();
      beamWidthMode = byteBuffer.getShort();
      beamMode = byteBuffer.getShort();
      beamWidthHorizontalTx = byteBuffer.getFloat();
      beamWidthVerticalTx = byteBuffer.getFloat();
      offset = byteBuffer.getInt();
      count = ByteBufferUtils.readCount(byteBuffer, 8 * complexPerSample);
      data = ByteBufferUtils.readFloatArray(byteBuffer, count * complexPerSample * 2);

      switch (dataType & 0b1111) {
         case PowerData.DATA_TYPE_COMPLEX_FLOAT_32 -> {
            // ok
         }
         default -> {
            throw new DatagramFormatException("Unhandled data type: " + dataType);
         }
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putShort(channel);
      byteBuffer.put(dataType);
      byteBuffer.put(complexPerSample);
      byteBuffer.putFloat(gainTx);
      byteBuffer.putFloat(frequency);
      byteBuffer.putFloat(transmitPower);
      byteBuffer.putFloat(pulseLength);
      byteBuffer.putFloat(bandWidth);
      byteBuffer.putFloat(sampleInterval);
      byteBuffer.putFloat(soundVelocity);
      byteBuffer.putFloat(absorptionCoefficient);
      byteBuffer.putFloat(heave);
      byteBuffer.putFloat(roll);
      byteBuffer.putFloat(pitch);
      byteBuffer.putFloat(temperature);
      byteBuffer.putFloat(heading);
      byteBuffer.putShort(transmitMode);
      byteBuffer.putShort(pulseForm);
      byteBuffer.putFloat(beamTilt);
      byteBuffer.putFloat(beamBearing);
      byteBuffer.putFloat(notUsed);
      byteBuffer.putFloat(gainRx);
      byteBuffer.putFloat(saCorrection);
      byteBuffer.putFloat(equivalentBeamAngle);
      byteBuffer.putFloat(beamWidthHorizontalRx);
      byteBuffer.putFloat(beamWidthVerticalRx);
      byteBuffer.putFloat(angleSensitivityAlongship);
      byteBuffer.putFloat(angleSensitivityAthwartship);
      byteBuffer.putFloat(angleOffsetAlongship);
      byteBuffer.putFloat(angleOffsetAthwartship);
      byteBuffer.put(spare);
      byteBuffer.putShort(noiseFilter);
      byteBuffer.putShort(beamWidthMode);
      byteBuffer.putShort(beamMode);
      byteBuffer.putFloat(beamWidthHorizontalTx);
      byteBuffer.putFloat(beamWidthVerticalTx);
      byteBuffer.putInt(offset);
      byteBuffer.putInt(count);
      ByteBufferUtils.writeFloatArray(byteBuffer, data);
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      pingConversion.addPingItem(toPingItem.apply(this));
   }

   @Override
   public boolean isSampleDatagram() {
      return true;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public String toStringExtra() {
      return "channel: " + String.format("%2d", channel)
            + ", frequency: " + String.format("%3d", KoronaUtils.hzToKHz(frequency)) + " kHz"
            + ", sampleCount: " + count;
   }

   //--

   public float getSampleDistance() {
      return sampleInterval * soundVelocity * 0.5f;
   }

   public float[] computePower() {
      return switch (dataType & 0b1111) {
         case PowerData.DATA_TYPE_COMPLEX_FLOAT_32 -> complexToPower();
         default -> throw new IllegalStateException("Data type: " + dataType);
      };
   }

   private float[] complexToPower() {
      float[] power = new float[count];
      int dataIndex = 0;
      for (int i = 0; i < count; i++) {
         double sumRe = 0;
         double sumIm = 0;
         for (int j = 0; j < complexPerSample; j++) {
            double re = data[dataIndex++];
            double im = data[dataIndex++];
            sumRe += re;
            sumIm += im;
         }
         power[i] = (float) (10 * Math.log10(sumRe * sumRe + sumIm * sumIm));
      }
      return power;
   }
}
