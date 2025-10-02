package no.marec.lsss.api.data;

import no.marec.lsss.api.DoNotImplement;

import java.util.List;

/**
 * Configuration for the pings in a {@link PingDataset}.
 */
@DoNotImplement
public interface PingConfiguration {
   /**
    * {@return the configuration for all available channels}
    */
   List<? extends ChannelConfiguration> getChannelConfigurations();
}
