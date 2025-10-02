package no.imr.korona.data.util.geometry.depth;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.PingIndex;

/**
 * The depth transform that transform the bottom for a given channel to a horizontal line at z = 0.
 * The relation between depth and z is:
 * <blockquote>{@code
 * z = depth - bottomDepth(PingIndex)
 * }</blockquote>
 */
public final class ChannelBottomDepthTransform extends BottomDepthTransform {
   private final DataManager dataManager;
   private final int channelIndex;

   /**
    * Creates a new ChannelBottomDepthTransform.
    *
    * @param dataManager the data manager
    * @param channel     the channel number
    */
   public ChannelBottomDepthTransform(DataManager dataManager, int channel) {
      super(dataManager.getDataConfiguration());

      this.dataManager = dataManager;
      channelIndex = channel - 1;
   }

   /**
    * Return the channel used by this depth transform.
    *
    * @return the channel number
    */
   public int getChannel() {
      return channelIndex + 1;
   }

   @Override
   protected float getReferenceDepth(PingIndex pingIndex) {
      Bot0Datagram bot0Datagram = dataManager.getDataFileSet().getBot0Datagram(pingIndex);
      return (float) bot0Datagram.getChannelDepths()[channelIndex];
   }
}
