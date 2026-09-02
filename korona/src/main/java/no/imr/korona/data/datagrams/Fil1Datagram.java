package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.math.ComplexArray;

import java.nio.ByteBuffer;
import java.time.Instant;

public final class Fil1Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("FIL1", Fil1Datagram::new);

   public short stage; // 1 = WBT filter, 2 = PC filter
   public short channel;
   public String channelId = ""; // 128
   public short decimationFactor;
   public ComplexArray coefficients = ComplexArray.EMPTY;

   public Fil1Datagram(Instant instant) {
      super(instant);
   }

   public Fil1Datagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant);

      stage = byteBuffer.getShort();
      channel = byteBuffer.getShort();
      channelId = ByteBufferUtils.readCString(byteBuffer, 128);
      int coefficientCount = 0xffff & byteBuffer.getShort();
      decimationFactor = byteBuffer.getShort();
      if (decimationFactor == 0) {
         decimationFactor = 6;
      }
      coefficients = ComplexArray.ofLength(coefficientCount);
      for (int i = 0; i < coefficientCount; i++) {
         float re = byteBuffer.getFloat();
         float im = byteBuffer.getFloat();
         coefficients.set(i, re, im);
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putShort(stage);
      byteBuffer.putShort(channel);
      ByteBufferUtils.writeCString(byteBuffer, channelId, 128);
      byteBuffer.putShort((short) coefficients.length());
      byteBuffer.putShort(decimationFactor);
      for (int i = 0; i < coefficients.length(); i++) {
         byteBuffer.putFloat((float) coefficients.re(i));
         byteBuffer.putFloat((float) coefficients.im(i));
      }
   }

   @Override
   public String toStringExtra() {
      return "stage: " + stage
            + ", channel: " + channel
            + ", " + channelId
            + ", decimationFactor: " + decimationFactor
            + ", coefficientCount: " + coefficients.length()
            + ", coefficients: " + coefficients.toComplexValuesString();
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }
}
