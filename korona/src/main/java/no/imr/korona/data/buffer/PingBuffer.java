package no.imr.korona.data.buffer;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.tools.listening.ArgChangeManager;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Base class for ping buffers.
 */
public abstract class PingBuffer implements PingContainer, Consumer<Ping> {
   private final ArgChangeManager<Ping> changeManager = new ArgChangeManager<>();
   private @Nullable Ping lastPing;

   protected PingBuffer() {
   }

   public ArgChangeManager<Ping> getChangeManager() {
      return changeManager;
   }

   public @Nullable Ping getLastPing() {
      return lastPing;
   }

   public @Nullable PingIndex getLastPingIndex() {
      Ping lastPing = this.lastPing;
      return lastPing != null ? lastPing.getPingIndex() : null;
   }

   @Override
   public void accept(Ping ping) {
      newPing(ping);
   }

   public void newPing(Ping ping) {
      lastPing = ping;
      changeManager.notifyListeners(ping);
   }

   public PingIndex getFirstPingIndex() {
      return getTotalRange().begin();
   }

   public @Nullable PingIndex getPingIndex(PingIndex reference, int offset) {
      return getPingIndexOrNull(reference.getPingNumber() + offset);
   }

   public abstract @Nullable Ping getPing(double value, PingMapping pingMapping);

   public @Nullable Ping getPing(PingIndex reference, int offset) {
      return getPing(reference.getPingNumber() + offset, PingMapping.NUMBER);
   }

   public Ping getPing(PingIndex pingIndex) {
      Ping ping = getPing(pingIndex.getPingNumber(), PingMapping.NUMBER);
      assert ping != null : pingIndex;
      return ping;
   }

   public @Nullable Ping getPing(double value, PingMapping pingMapping, boolean adjustCache) {
      return getPing(value, pingMapping);
   }

   public @Nullable Ping getPing(PingIndex reference, int offset, boolean adjustCache) {
      return getPing(reference.getPingNumber() + offset, PingMapping.NUMBER, adjustCache);
   }

   public boolean isReady(PingIndex pingIndex) {
      return true;
   }
}
