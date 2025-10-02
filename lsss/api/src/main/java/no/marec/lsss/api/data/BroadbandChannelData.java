package no.marec.lsss.api.data;

import no.marec.lsss.api.DoNotImplement;

/**
 * Broadband sample data.
 */
@DoNotImplement
public interface BroadbandChannelData extends ChannelData {
   /**
    * {@return the start frequency in Hz of the sweep}
    */
   float getStartFrequency();

   /**
    * {@return the stop frequency in Hz of the sweep}
    */
   float getStopFrequency();
}
