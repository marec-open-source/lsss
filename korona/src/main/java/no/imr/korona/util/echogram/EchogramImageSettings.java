package no.imr.korona.util.echogram;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.viewer.coloring.PingToColor;

/**
 * Settings used bu {@link EchogramImage}.
 */
public interface EchogramImageSettings {
   PingToColor getPingToColor();

   int getChannel(RawFileConfiguration rawFileConfiguration);

   EchogramPingSettings getPingSettings();

   EchogramZSettings getZSettings();

   DataConfiguration getDataConfiguration();
}
