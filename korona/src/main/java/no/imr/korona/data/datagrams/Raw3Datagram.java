package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.channel.Raw3ToChannelData;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.time.Instant;

public final class Raw3Datagram extends BaseDatagram {
   public static final DatagramType TYPE = DatagramType.simple("RAW3", Raw3Datagram::new);

   public String channelId;
   public short dataType;
   public short spare;
   public int offset;
   public int count;

   public short @Nullable [] power;
   public byte @Nullable [] angles;

   public float @Nullable [][] real;
   public float @Nullable [][] imag;

   public Raw3Datagram(Instant instant) {
      super(instant);

      channelId = "";
   }

   public Raw3Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      channelId = ByteBufferUtils.readCString(byteBuffer, 128);
      dataType = byteBuffer.getShort();
      spare = byteBuffer.getShort();
      offset = byteBuffer.getInt();
      count = byteBuffer.getInt();

      switch (dataType & 0b1111) {
         case PowerData.DATA_TYPE_POWER -> {
            ByteBufferUtils.checkCount(count, byteBuffer, 2);
            power = ByteBufferUtils.readShortArray(byteBuffer, count);
            if (count % 2 != 0 && byteBuffer.remaining() == 2) {
               // The datagram has been padded to make size a multiple of 4. See issue #1500.
               byteBuffer.position(byteBuffer.position() + 2);
            }
         }
         case PowerData.DATA_TYPE_POWER | PowerData.DATA_TYPE_ANGLES -> {
            ByteBufferUtils.checkCount(count, byteBuffer, 2 + 2);
            power = ByteBufferUtils.readShortArray(byteBuffer, count);
            angles = ByteBufferUtils.readByteArray(byteBuffer, count * 2);
         }
         case PowerData.DATA_TYPE_COMPLEX_FLOAT_32 -> {
            readComplexFloat32(byteBuffer);
         }
         default -> {
            throw new DatagramFormatException("Unhandled data type: " + dataType);
         }
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCString(byteBuffer, channelId, 128);
      byteBuffer.putShort(dataType);
      byteBuffer.putShort(spare);
      byteBuffer.putInt(offset);
      byteBuffer.putInt(count);

      switch (dataType & 0b1111) {
         case PowerData.DATA_TYPE_POWER -> {
            if (power == null) {
               throw new IllegalStateException();
            }
            ByteBufferUtils.writeShortArray(byteBuffer, power);
         }
         case PowerData.DATA_TYPE_POWER | PowerData.DATA_TYPE_ANGLES -> {
            if (power == null || angles == null) {
               throw new IllegalStateException();
            }
            ByteBufferUtils.writeShortArray(byteBuffer, power);
            byteBuffer.put(angles);
         }
         case PowerData.DATA_TYPE_COMPLEX_FLOAT_32 -> {
            writeComplexFloat32(byteBuffer);
         }
         default -> {
            throw new IllegalStateException("Unhandled data type: " + dataType);
         }
      }
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      ChannelData channelData = Raw3ToChannelData.toChannelData(this, pingConversion);
      if (channelData == null) {
         return;
      }
      pingConversion.addPingItem(channelData);
   }

   private void readComplexFloat32(ByteBuffer byteBuffer) throws DatagramFormatException {
      int complexPerSample = (dataType & 0b111_0000_0000) >> 8;
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
      return channelId
            + ", dataType: " + dataType
            + ", offset: " + offset
            + ", count: " + count;
   }

   @Override
   public boolean isSampleDatagram() {
      return true;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }
}
