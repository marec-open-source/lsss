package no.imr.korona.data.ping;

import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.util.DiscardedChannelData;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * The datagrams in a ping.
 */
public final class PingData {
   private final PingConfiguration pingConfiguration;
   private final List<PingItem> pingItems = new ArrayList<>();
   private final @Nullable ChannelData[] channelDatas;

   public PingData(PingConfiguration pingConfiguration) {
      this.pingConfiguration = pingConfiguration;
      int transducerCount = pingConfiguration.getRawFileConfiguration().getTransducerCount();
      channelDatas = new ChannelData[transducerCount];
   }

   @Override
   public String toString() {
      return "channelCount: " + channelDatas.length
            + ", pingItems: " + pingItems.size();
   }

   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   public List<PingItem> getPingItems() {
      return pingItems;
   }

   public @Nullable ChannelData[] getChannelDatas() {
      return channelDatas;
   }

   public @Nullable ChannelData getFirstAvailableChannelData() {
      for (ChannelData channelData : channelDatas) {
         if (channelData != null) {
            return channelData;
         }
      }
      return null;
   }

   public Stream<ChannelData> getNonNullChannelDatas() {
      return Arrays.stream(channelDatas)
            .filter(Objects::nonNull);
   }

   public <T extends ChannelData> Stream<T> getNonNullChannelDatas(Class<T> clazz) {
      return Arrays.stream(channelDatas)
            .gather(Utils.allOfType(clazz));
   }

   public <T extends PingItem> @Nullable T getPingItem(Class<T> clazz) {
      return Utils.getFirstOrNull(pingItems, clazz);
   }

   public <T extends PingItem> Stream<T> getPingItems(Class<T> clazz) {
      return Utils.getAllOfType(pingItems, clazz);
   }

   public void add(PingItem pingItem) {
      pingItems.add(pingItem);
      if (pingItem instanceof ChannelData channelData) {
         channelDatas[channelData.getChannel() - 1] = channelData;
      }
      pingItem.setPingConfiguration(pingConfiguration);
   }

   public void addAll(Collection<? extends PingItem> items) {
      items.forEach(this::add);
   }

   public void remove(PingItem pingItem) {
      boolean didRemove = pingItems.remove(pingItem);
      if (pingItem instanceof ChannelData channelData) {
         if (!didRemove) {
            channelData.throwExceptionIfReadOnly();
         }
         channelDatas[channelData.getChannel() - 1] = null;
      }
   }

   public void removeAll(Class<? extends PingItem> clazz) {
      if (ChannelData.class.isAssignableFrom(clazz)) {
         for (int i = 0; i < channelDatas.length; i++) {
            if (clazz.isInstance(channelDatas[i])) {
               channelDatas[i] = null;
            }
         }
      }
      pingItems.removeIf(clazz::isInstance);
   }

   public float getDataDepth() {
      float dataDepth = 0;
      boolean foundMissing = false;
      for (ChannelData channelData : channelDatas) {
         if (channelData != null) {
            dataDepth = Math.max(dataDepth, channelData.getMaxDepth());
         } else {
            foundMissing = true;
         }
      }
      if (foundMissing) {
         for (PingItem pingItem : pingItems) {
            if (pingItem instanceof DiscardedChannelData discardedChannelData) {
               dataDepth = Math.max(dataDepth, discardedChannelData.getChannelData().getMaxDepth());
            }
         }
      }
      return dataDepth;
   }

   public float getDataRange() {
      float dataRange = 0;
      boolean foundMissing = false;
      for (ChannelData channelData : channelDatas) {
         if (channelData != null) {
            dataRange = Math.max(dataRange, channelData.getMaxRange());
         } else {
            foundMissing = true;
         }
      }
      if (foundMissing) {
         for (PingItem pingItem : pingItems) {
            if (pingItem instanceof DiscardedChannelData discardedChannelData) {
               dataRange = Math.max(dataRange, discardedChannelData.getChannelData().getMaxRange());
            }
         }
      }
      return dataRange;
   }
}
