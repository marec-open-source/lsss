package no.imr.korona.data.datagrams;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Noise Quantification Parameters.
 */
public final class Nqp0Datagram extends DatagramPingItem implements PerChannelDatagram {
   public static final DatagramType TYPE = DatagramType.simple("NQP0", Nqp0Datagram::new);

   private short channel;
   private final float average; // N_E
   private final float upperLimit; // N_H
   private final float quality; // a number between 0 and 100

   /**
    * Constructor.
    *
    * @param instant    time
    * @param channel    channel no
    * @param average    average noise
    * @param upperLimit upper limit noise
    * @param quality    quality of data
    */
   public Nqp0Datagram(Instant instant, short channel,
                       float average, float upperLimit,
                       float quality) {
      super(instant);

      this.channel = channel;
      this.average = average;
      this.upperLimit = upperLimit;
      this.quality = quality;
   }

   public Nqp0Datagram(Instant instant, ByteBuffer byteBuffer) {
      super(instant);

      channel = byteBuffer.getShort();
      average = byteBuffer.getFloat();
      upperLimit = byteBuffer.getFloat();
      quality = byteBuffer.getFloat();
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putShort(channel);
      byteBuffer.putFloat(average);
      byteBuffer.putFloat(upperLimit);
      byteBuffer.putFloat(quality);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public int getChannel() {
      return channel;
   }

   @Override
   public void setChannel(int channel) {
      this.channel = (short) channel;
   }

   /**
    * {@return N<sub>E</sub>}
    */
   public float getAverage() {
      return average;
   }

   /**
    * {@return N<sub>H</sub>}
    */
   public float getUpperLimit() {
      return upperLimit;
   }

   /**
    * {@return the overall quality of the noise estimate}
    */
   public float getQuality() {
      return quality;
   }

   @Override
   public String toStringExtra() {
      return "channel: " + channel
            + ", ne: " + average
            + ", nh: " + upperLimit
            + ", quality: " + quality;
   }
}
