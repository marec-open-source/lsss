package no.marec.api.korona;

import java.util.Collection;

/**
 * A ping consisting of {@link Channel}s.
 * <p>
 * All pings in the same file has the same number of channels.
 */
public interface Ping {
   /**
    * Return the channel for a given channel number.
    * <p>
    * Note: channels are numbered starting at 1.
    *
    * @param channelNumber the channel number
    * @return the channel
    */
   Channel getChannel(int channelNumber);

   /**
    * Returns all channels in this ping.
    *
    * @return all channels
    */
   Collection<Channel> getChannels();
}
