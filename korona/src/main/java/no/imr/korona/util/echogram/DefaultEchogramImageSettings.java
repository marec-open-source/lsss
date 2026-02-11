package no.imr.korona.util.echogram;

import no.imr.korona.data.ChannelSelector;
import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.datamanager.DefaultDataConfiguration;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.viewer.coloring.PingToColor;

public final class DefaultEchogramImageSettings implements EchogramImageSettings {
   private final DefaultEchogramPingSettings defaultEchogramPingSettings;
   private final DefaultEchogramZSettings defaultEchogramZSettings = new DefaultEchogramZSettings();
   private final DefaultDataConfiguration defaultDataConfiguration = new DefaultDataConfiguration();
   private PingToColor pingToColor;
   private ChannelSelector channelSelector;

   public DefaultEchogramImageSettings(PingToColor pingToColor, ChannelSelector channelSelector, PingContainer pingContainer, PingMapping pingMapping) {
      this.pingToColor = pingToColor;
      this.channelSelector = channelSelector;
      defaultEchogramPingSettings = new DefaultEchogramPingSettings(pingContainer, pingMapping);
   }

   @Override
   public PingToColor getPingToColor() {
      return pingToColor;
   }

   public void setPingToColor(PingToColor pingToColor) {
      this.pingToColor = pingToColor;
   }

   @Override
   public int getChannel(RawFileConfiguration rawFileConfiguration) {
      return channelSelector.getChannel(rawFileConfiguration);
   }

   public void setChannelSelector(ChannelSelector channelSelector) {
      this.channelSelector = channelSelector;
   }

   @Override
   public DefaultEchogramPingSettings getPingSettings() {
      return defaultEchogramPingSettings;
   }

   @Override
   public DefaultEchogramZSettings getZSettings() {
      return defaultEchogramZSettings;
   }

   @Override
   public DataConfiguration getDataConfiguration() {
      return defaultDataConfiguration;
   }
}
