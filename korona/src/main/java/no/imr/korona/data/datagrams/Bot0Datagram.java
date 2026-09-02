package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;

/**
 * Bottom depths per channel.
 */
public class Bot0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("BOT0", Bot0Datagram::new);

   private final double[] channelDepths; // varying size [m]

   public Bot0Datagram(Instant instant, int transducerCount) {
      super(instant);

      channelDepths = new double[transducerCount];
   }

   public Bot0Datagram(Instant instant, double[] channelDepths) {
      super(instant);

      this.channelDepths = channelDepths;
   }

   public Bot0Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      channelDepths = ByteBufferUtils.readCountAndDoubleArray(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCountAndDoubleArray(byteBuffer, channelDepths);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public String toStringExtra() {
      return "channelDepths: " + Arrays.toString(channelDepths);
   }

   public double[] getChannelDepths() {
      return channelDepths;
   }

   @Override
   public Bot0Datagram makeCopy() {
      return new Bot0Datagram(getInstant(), channelDepths.clone());
   }

   public Bot0Datagram copyWithAddedChannel(int channelToCopy) {
      double[] depths = Arrays.copyOf(channelDepths, channelDepths.length + 1);
      depths[channelDepths.length] = channelDepths[channelToCopy - 1];
      return new Bot0Datagram(getInstant(), depths);
   }

   public Bot0Datagram copyWithRemovedChannel(int channelToRemove) {
      double[] depths = new double[channelDepths.length - 1];
      System.arraycopy(channelDepths, 0, depths, 0, channelToRemove - 1);
      System.arraycopy(channelDepths, channelToRemove, depths, channelToRemove - 1, depths.length - (channelToRemove - 1));
      return new Bot0Datagram(getInstant(), depths);
   }
}
