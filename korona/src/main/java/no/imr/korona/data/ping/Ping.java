package no.imr.korona.data.ping;

import no.imr.korona.data.datagrams.BaseDepDatagram;
import no.imr.korona.data.datagrams.BaseDepPerChannelDatagram;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.datagrams.PerChannelDatagram;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.Utils;
import no.marec.lsss.api.data.SvChannelData;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * A ping.
 */
public abstract class Ping implements PingMappingArgument, Comparable<Ping>, no.marec.lsss.api.data.Ping {
   protected Ping() {
   }

   public abstract PingConfiguration getPingConfiguration();

   @Override
   public abstract PingIndex getPingIndex();

   public abstract Bot0Datagram getBot0Datagram();

   public abstract PingData getPingData();

   public abstract @Nullable PingData getAvailablePingData();

   @Override
   public long getPingNumber() {
      return getPingIndex().getPingNumber();
   }

   @Override
   public double getVesselDistance() {
      return getPingIndex().getVesselDistance();
   }

   @Override
   public Instant getInstant() {
      return getPingIndex().getInstant();
   }

   public void add(PingItem pingItem) {
      getPingData().add(pingItem);
   }

   public void addAll(Collection<? extends PingItem> pingItems) {
      getPingData().addAll(pingItems);
   }

   public void remove(PingItem pingItem) {
      getPingData().remove(pingItem);
   }

   public void removeAll(Class<? extends PingItem> clazz) {
      getPingData().removeAll(clazz);
   }

   public List<PingItem> getPingItems() {
      return getPingData().getPingItems();
   }

   public <T extends PingItem> @Nullable T getPingItem(Class<T> clazz) {
      return getPingData().getPingItem(clazz);
   }

   public <T extends PingItem> Stream<T> getPingItems(Class<T> clazz) {
      return getPingData().getPingItems(clazz);
   }

   public @Nullable PowerData getPowerData(int channel) {
      ChannelData channelData = getChannelData(channel);
      return channelData != null ? channelData.getPowerData() : null;
   }

   @Override
   public @Nullable SvChannelData getSvChannelData(int channel) {
      return getPowerData(channel);
   }

   @Override
   public @Nullable BroadbandData getBroadbandChannelData(int channel) {
      return getBroadbandData(channel);
   }

   public @Nullable BroadbandData getBroadbandData(int channel) {
      ChannelData channelData = getChannelData(channel);
      return channelData instanceof BroadbandData broadbandData ? broadbandData : null;
   }

   public @Nullable ChannelData getChannelData(int channel) {
      return Utils.getOrNull(getChannelDatas(), channel - 1);
   }

   public @Nullable ChannelData[] getChannelDatas() {
      return getPingData().getChannelDatas();
   }

   public Stream<ChannelData> getNonNullChannelDatas() {
      return getPingData().getNonNullChannelDatas();
   }

   public @Nullable ChannelData getFirstAvailableChannelData() {
      return getPingData().getFirstAvailableChannelData();
   }

   public @Nullable PowerData getFirstAvailablePowerData() {
      ChannelData channelData = getFirstAvailableChannelData();
      return channelData != null ? channelData.getPowerData() : null;
   }

   public Stream<PowerData> getNonNullPowerDatas() {
      return getPingData().getNonNullChannelDatas()
            .map(ChannelData::getPowerData);
   }

   public <T extends ChannelData> Stream<T> getNonNullChannelDatas(Class<T> clazz) {
      return getPingData().getNonNullChannelDatas(clazz);
   }

   public RawFileConfiguration getRawFileConfiguration() {
      return getPingConfiguration().getRawFileConfiguration();
   }

   @Override
   public String toString() {
      return "Ping " + getPingIndex().getPingNumber();
   }

   @Override
   public int compareTo(Ping other) {
      return getPingIndex().compareTo(other.getPingIndex());
   }

   public @Nullable Dep0Datagram getDep0Datagram() {
      return getPingItem(Dep0Datagram.class);
   }

   public <T extends PingItem & PerChannelDatagram> @Nullable T getPerChannelDatagram(Class<T> clazz, int channel) {
      return Utils.getAllOfType(getPingItems(), clazz)
            .filter(perChannelDatagram -> perChannelDatagram.getChannel() == channel)
            .findFirst()
            .orElse(null);
   }

   /**
    * Returns a depth datagram for a given channel.
    * Channel-specific datagrams, {@link BaseDepPerChannelDatagram}, are searched for first.
    * If not found, a {@link Dep0Datagram} is returned if it exists.
    *
    * @param channel the channel number (not index)
    * @return a dep datagram
    */
   public @Nullable BaseDepDatagram getBaseDepDatagram(int channel) {
      BaseDepPerChannelDatagram depPerChannel = getPerChannelDatagram(BaseDepPerChannelDatagram.class, channel);
      if (depPerChannel != null) {
         return depPerChannel;
      }
      return getDep0Datagram();
   }
}
