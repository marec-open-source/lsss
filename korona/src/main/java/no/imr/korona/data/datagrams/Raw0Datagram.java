package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Channel data with power, and possibly angles.
 */
public final class Raw0Datagram extends BaseDatagram implements PerChannelDatagram {
   public static final DatagramType TYPE = DatagramType.simple("RAW0", Raw0Datagram::new);

   public static final int DATA_TYPE_POWER = 1;
   public static final int DATA_TYPE_ANGLES = 2;
   // public static final int DATA_TYPE_COMPLEX_FLOAT_16 = 4;
   public static final int DATA_TYPE_COMPLEX_FLOAT_32 = 8;

   public short channel; // Channel number
   public short mode; // Power = 1, Angles = 2, BBT complex = 8
   public float transducerDepth; // [m]
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
   public float temperature; // [°C]

   // These are 12 bytes
   public float heading;  // The heading of the ship [deg]    // RK 2010.11.19
   public short transmitMode;  // The transmit mode: 0=Active, 1=Passive, 2=Test, -1=Unknown  // RK 2010.11.19
   public short spare;  // Nothing - just to fill up
   public float sweep;  // The frequency sweep rate [Hz/s]  RK 2010.11.19, WIDEBAND WBT_COMPLEX

   public int offset; // Offset of first sample
   public int count; // Number of samples

   public short @Nullable [] power;
   public byte @Nullable [] angles;

   public float @Nullable [][] real;
   public float @Nullable [][] imag;

   public Raw0Datagram(long ntDate) {
      super(ntDate);
   }

   public Raw0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      readConfig(byteBuffer);
      readData(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      writeConfig(byteBuffer);
      writeData(byteBuffer);
   }

   public void readConfig(ByteBuffer byteBuffer) throws DatagramFormatException {
      channel = byteBuffer.getShort();
      mode = byteBuffer.getShort();
      if ((mode & DATA_TYPE_POWER) == 0 && (mode & DATA_TYPE_COMPLEX_FLOAT_32) == 0) {
         throw new DatagramFormatException("Mode has neither power nor complex: " + mode);
      }
      transducerDepth = byteBuffer.getFloat();
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
      spare = byteBuffer.getShort();
      sweep = byteBuffer.getFloat();
      offset = byteBuffer.getInt();
      count = byteBuffer.getInt(); // Cannot use ByteBufferUtils.readCount since per sample might be 0
      if (count < 0 || count > MAX_DATAGRAM_SIZE / 2) {
         throw new DatagramFormatException("count: " + count);
      }
   }

   public void writeConfig(ByteBuffer byteBuffer) {
      byteBuffer.putShort(channel);
      byteBuffer.putShort(mode);
      byteBuffer.putFloat(transducerDepth);
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
      byteBuffer.putShort(spare);
      byteBuffer.putFloat(sweep);
      byteBuffer.putInt(offset);
      byteBuffer.putInt(count);
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      if (power != null) {
         pingConversion.addPingItem(new PowerData(this, pingConversion));
      } else if (real != null && imag != null) {
         pingConversion.addPingItem(new BroadbandData(this, pingConversion));
      }
   }

   private void readData(ByteBuffer byteBuffer) throws DatagramFormatException {
      if ((mode & DATA_TYPE_POWER) != 0) {
         power = ByteBufferUtils.readShortArray(byteBuffer, count);
      }
      if ((mode & DATA_TYPE_ANGLES) != 0) {
         angles = ByteBufferUtils.readByteArray(byteBuffer, count * 2);
      }
      if ((mode & DATA_TYPE_COMPLEX_FLOAT_32) != 0) {
         readComplexFloat32(byteBuffer);
      }
   }

   private void writeData(ByteBuffer byteBuffer) {
      if ((mode & DATA_TYPE_POWER) != 0) {
         if (power == null) {
            throw new IllegalStateException();
         }
         ByteBufferUtils.writeShortArray(byteBuffer, power);
      }
      if ((mode & DATA_TYPE_ANGLES) != 0) {
         if (angles == null) {
            throw new IllegalStateException();
         }
         byteBuffer.put(angles);
      }
      if ((mode & DATA_TYPE_COMPLEX_FLOAT_32) != 0) {
         writeComplexFloat32(byteBuffer);
      }
   }

   private void readComplexFloat32(ByteBuffer byteBuffer) throws DatagramFormatException {
      int complexPerSample = (mode & 0b111_0000_0000) >> 8;
      if (complexPerSample == 0) {
         throw new DatagramFormatException("complexPerSample: 0");
      }
      ByteBufferUtils.checkCount(count, byteBuffer, 8 * complexPerSample);

      real = new float[complexPerSample][count];
      imag = new float[complexPerSample][count];

      FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
      for (int i = 0; i < count; i++) {
         for (int j = 0; j < complexPerSample; j++) {
            real[j][i] = floatBuffer.get();
            imag[j][i] = floatBuffer.get();
         }
      }
      byteBuffer.position(byteBuffer.position() + 4 * floatBuffer.position());
   }

   private void writeComplexFloat32(ByteBuffer byteBuffer) {
      if (real == null || imag == null) {
         throw new IllegalStateException();
      }
      FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
      for (int i = 0; i < count; i++) {
         for (int j = 0; j < real.length; j++) {
            floatBuffer.put(real[j][i]);
            floatBuffer.put(imag[j][i]);
         }
      }
      byteBuffer.position(byteBuffer.position() + 4 * floatBuffer.position());
   }

   @Override
   public String toStringExtra() {
      return "channel: " + channel
            + ", frequency: " + Utils.hzToKHz(frequency) + " kHz";
   }

   @Override
   public boolean isSampleDatagram() {
      return true;
   }

   @Override
   public int getChannel() {
      return channel;
   }

   @Override
   public void setChannel(int channel) {
      this.channel = (short) channel;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }
}
