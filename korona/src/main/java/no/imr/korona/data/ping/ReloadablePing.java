package no.imr.korona.data.ping;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * A ping that can be reloaded.
 */
public final class ReloadablePing extends Ping {
   public static final AtomicLong RELOAD_COUNTER = new AtomicLong();

   private final ReloadablePingSource reloadablePingSource;
   private final PingIndex pingIndex;
   private volatile WeakReference<@Nullable PingData> pingDataReference;

   public ReloadablePing(ReloadablePingSource reloadablePingSource, PingIndex pingIndex) {
      this.reloadablePingSource = reloadablePingSource;
      this.pingIndex = pingIndex;
      pingDataReference = new WeakReference<>(null);
   }

   public ReloadablePing(ReloadablePingSource reloadablePingSource, Ping ping) {
      this.reloadablePingSource = reloadablePingSource;
      pingIndex = ping.getPingIndex();
      pingDataReference = new WeakReference<>(ping.getPingData());
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return reloadablePingSource.getPingConfiguration();
   }

   @Override
   public PingIndex getPingIndex() {
      return pingIndex;
   }

   @Override
   public Bot0Datagram getBot0Datagram() {
      return reloadablePingSource.getBot0Datagram(pingIndex);
   }

   @Override
   public PingData getPingData() {
      PingData pingData = pingDataReference.get();
      if (pingData != null) {
         return pingData;
      }
      return loadPingData();
   }

   private synchronized PingData loadPingData() {
      PingData existingPingData = pingDataReference.get();
      if (existingPingData != null) {
         return existingPingData;
      }
      if (reloadablePingSource.isDataLoadingCancelled()) {
         return new PingData(getPingConfiguration());
      }
      RELOAD_COUNTER.incrementAndGet();
      PingData newPingData;
      try {
         newPingData = reloadablePingSource.loadPingData(pingIndex, new AsyncHandle());
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error loading ping " + pingIndex.getInstant() + " from " + getPingConfiguration().getRawFileConfiguration().getDataFile(), e);
         return new PingData(getPingConfiguration());
      }
      pingDataReference = new WeakReference<>(newPingData);
      reloadablePingSource.onLoadPingData(this, newPingData);
      return newPingData;
   }

   public void clear() {
      pingDataReference = new WeakReference<>(null);
   }

   @Override
   public @Nullable PingData getAvailablePingData() {
      return pingDataReference.get();
   }
}
