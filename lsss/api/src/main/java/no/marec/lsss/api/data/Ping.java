package no.marec.lsss.api.data;

import no.marec.lsss.api.DoNotImplement;
import org.jspecify.annotations.Nullable;

/**
 * A ping with sample data on the different channels.
 */
@DoNotImplement
public interface Ping {
   /**
    * {@return the ping index for this ping}
    */
   PingIndex getPingIndex();

   /**
    * Returns the {@link SvChannelData} on the specified channel.
    * <p>
    * Other types of {@link ChannelData} will be converted to {@link SvChannelData}.
    *
    * @param channel a channel number, starting at 1
    * @return the {@link SvChannelData}, or {@code null} if not available
    */
   @Nullable SvChannelData getSvChannelData(int channel);

   /**
    * Returns the {@link BroadbandChannelData} on the specified channel, if available.
    *
    * @param channel a channel number, starting at 1
    * @return the {@link BroadbandChannelData}, or {@code null} if not available
    */
   @Nullable BroadbandChannelData getBroadbandChannelData(int channel);
}
