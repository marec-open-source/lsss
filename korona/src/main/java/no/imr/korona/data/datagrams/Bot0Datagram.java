package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Bottom depths per channel.
 */
public class Bot0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("BOT0", Bot0Datagram::new);

   private final double[] channelDepths; // varying size [m]

   public Bot0Datagram(long ntDate, int transducerCount) {
      super(ntDate);

      channelDepths = new double[transducerCount];
   }

   public Bot0Datagram(long ntDate, double[] channelDepths) {
      super(ntDate);

      this.channelDepths = channelDepths;
   }

   public Bot0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

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

   public double[] getChannelDepths() {
      return channelDepths;
   }

   @Override
   public Bot0Datagram makeCopy() {
      return new Bot0Datagram(getNTDate(), channelDepths.clone());
   }

   public Bot0Datagram copyWithAddedChannel(int channelToCopy) {
      double[] depths = Arrays.copyOf(channelDepths, channelDepths.length + 1);
      depths[channelDepths.length] = channelDepths[channelToCopy - 1];
      return new Bot0Datagram(getNTDate(), depths);
   }

   public Bot0Datagram copyWithRemovedChannel(int channelToRemove) {
      double[] depths = new double[channelDepths.length - 1];
      System.arraycopy(channelDepths, 0, depths, 0, channelToRemove - 1);
      System.arraycopy(channelDepths, channelToRemove, depths, channelToRemove - 1, depths.length - (channelToRemove - 1));
      return new Bot0Datagram(getNTDate(), depths);
   }
}
