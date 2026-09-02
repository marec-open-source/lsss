package no.imr.korona.data.datagrams.subdatagrams;

import java.nio.ByteBuffer;
import java.time.Instant;

public final class DataIncoherenceSubDatagram extends BaseSubDatagram {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.DATA_INCOHERENCE,
         "Data incoherence", DataIncoherenceSubDatagram::new);

   private final short channel;
   private final int kHz;
   private final float incoherenceValue;

   public DataIncoherenceSubDatagram(Instant instant, short channel, int kHz, float incoherenceValue) {
      super(instant);

      this.channel = channel;
      this.kHz = kHz;
      this.incoherenceValue = incoherenceValue;
   }

   public DataIncoherenceSubDatagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant);

      channel = byteBuffer.getShort();
      kHz = byteBuffer.getInt();
      incoherenceValue = byteBuffer.getFloat();
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putShort(channel);
      byteBuffer.putInt(kHz);
      byteBuffer.putFloat(incoherenceValue);
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }

   @Override
   public String toStringExtra() {
      return "channel: " + channel +
            ", kHz: " + kHz +
            ", incoherenceValue: " + incoherenceValue;
   }

   public short getChannel() {
      return channel;
   }

   public int getKHz() {
      return kHz;
   }

   public float getIncoherenceValue() {
      return incoherenceValue;
   }
}
