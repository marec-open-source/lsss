package no.imr.korona.data.ping;

import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Configuration settings for a {@link Ping}.
 */
public final class PingConfiguration {
   private final RawFileConfiguration rawFileConfiguration;
   private final List<PingItem> configurationItems;

   public PingConfiguration(RawFileConfiguration rawFileConfiguration) {
      this.rawFileConfiguration = rawFileConfiguration;
      configurationItems = new ArrayList<>(1);
      configurationItems.add(rawFileConfiguration);
   }

   public PingConfiguration(List<PingItem> configurationItems) {
      rawFileConfiguration = (RawFileConfiguration) configurationItems.getFirst();
      this.configurationItems = configurationItems;
   }

   public static PingConfiguration newEmpty() {
      return new PingConfiguration(new RawFileConfiguration(Instant.EPOCH));
   }

   public RawFileConfiguration getRawFileConfiguration() {
      return rawFileConfiguration;
   }

   public List<PingItem> getConfigurationItems() {
      return configurationItems;
   }

   public List<PingItem> getOtherConfigurationItems() {
      return configurationItems.subList(1, configurationItems.size());
   }

   public <T extends PingItem> @Nullable T getConfigurationItem(Class<T> clazz) {
      return Utils.getFirstOrNull(configurationItems, clazz);
   }

   public <T extends PingItem> Stream<T> getConfigurationItems(Class<T> clazz) {
      return Utils.getAllOfType(configurationItems, clazz);
   }

   public PingConfiguration createCopy() {
      return createCopy(rawFileConfiguration.makeCopy());
   }

   public PingConfiguration createCopy(RawFileConfiguration newRawFileConfiguration) {
      List<PingItem> copy = new ArrayList<>(configurationItems);
      copy.set(0, newRawFileConfiguration);
      return new PingConfiguration(copy);
   }

   /**
    * Test if a ping configuration can be used in combination with another ping configuration.
    *
    * @param pingConfiguration another ping configuration
    * @return {@code null} if compatible, or a reason for why not
    */
   public @Nullable String getIncompatibility(PingConfiguration pingConfiguration) {
      StringBuilder sb = null;
      for (PingItem configurationItem : configurationItems) {
         if (configurationItem instanceof RawFileConfiguration && configurationItem != rawFileConfiguration) {
            // Other instances of RawFileConfiguration are not used and should not cause incompatibilities
            // This can happen if file offset in first ping is set to 0.
            continue;
         }
         String incompatibility = configurationItem.getIncompatibility(pingConfiguration);
         if (incompatibility != null) {
            if (sb == null) {
               sb = new StringBuilder();
            } else {
               sb.append(", ");
            }
            sb.append(incompatibility);
         }
      }
      return sb != null ? sb.toString() : null;
   }
}
