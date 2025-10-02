package no.marec.lsss.api.data;

import no.marec.lsss.api.DoNotImplement;

/**
 * Configuration for a channel.
 */
@DoNotImplement
public interface ChannelConfiguration {
   /**
    * {@return the ID of this channel}
    */
   String getChannelId();

   /**
    * {@return the nominal frequency in Hz of this channel}
    */
   float getNominalFrequency();
}
