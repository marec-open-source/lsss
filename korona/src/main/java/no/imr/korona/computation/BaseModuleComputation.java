package no.imr.korona.computation;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingBuffering;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelDataReadOnlyException;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract sealed class BaseModuleComputation implements PingSource permits
      GeneralPingModuleComputation, SimplePingModuleComputation, ConcurrentPingModuleComputation {

   private final BaseModule module;
   private final ComputationContext computationContext;
   private final PingSource pingSource;
   private PingConfiguration outputPingConfiguration;

   private final List<Ping> peekedPingSourcePings = new ArrayList<>();

   private final PingBuffering pingBuffering = new PingBuffering();
   private AsyncHandle asyncHandle;

   BaseModuleComputation(BaseModule module, ComputationContext computationContext, PingSource pingSource) {
      this.module = module;
      this.computationContext = computationContext;
      this.pingSource = pingSource;
      outputPingConfiguration = pingSource.getPingConfiguration();
      asyncHandle = computationContext.getAsyncHandle();
   }

   public BaseModule getModule() {
      return module;
   }

   public ComputationContext getComputationContext() {
      return computationContext;
   }

   public PingSource getPingSource() {
      return pingSource;
   }

   public PingBuffering getPingBuffering() {
      return pingBuffering;
   }

   public AsyncHandle getAsyncHandle() {
      return asyncHandle;
   }

   protected final <T extends PingItem> T getRequiredConfigItem(Class<T> clazz, String errorMessage) throws ModuleConfigurationException {
      T item = pingSource.getPingConfiguration().getConfigurationItem(clazz);
      if (item != null) {
         return item;
      } else {
         throw new ModuleConfigurationException(module, errorMessage);
      }
   }

   public void setNewPingConfiguration(PingConfiguration newPingConfiguration) {
      outputPingConfiguration = newPingConfiguration;
   }

   final Ping convertPing(Ping ping) throws IOException {
      if (ping.getPingConfiguration() == outputPingConfiguration) {
         return ping;
      } else {
         Bot0Datagram bot0Datagram = convertBot0(ping);
         Ping newPing = new DefaultPing(outputPingConfiguration, ping.getPingIndex(), bot0Datagram);
         convertPing(ping, newPing);
         assert newPing.getRawFileConfiguration().getTransducerCount() == newPing.getBot0Datagram().getChannelDepths().length : this;
         return newPing;
      }
   }

   protected void convertPing(Ping ping, Ping newPing) throws IOException {
      throw new UnsupportedOperationException();
   }

   protected Bot0Datagram convertBot0(Ping ping) throws IOException {
      if (ping.getRawFileConfiguration().getTransducerCount() == outputPingConfiguration.getRawFileConfiguration().getTransducerCount()) {
         return ping.getBot0Datagram();
      }
      throw new UnsupportedOperationException();
   }

   @Override
   public void close() throws IOException {
   }

   @Override
   public final PingConfiguration getPingConfiguration() {
      return outputPingConfiguration;
   }

   /**
    * Should only be called from {@link ConcurrentPingModuleComputation#prepareForConcurrentProcessing()}.
    */
   void discardPeekedPingSourcePings() {
      peekedPingSourcePings.clear();
   }

   protected final @Nullable Ping peekPingSourcePing(int index) throws IOException {
      while (index >= peekedPingSourcePings.size()) {
         Ping ping = pingSource.nextPing(asyncHandle);
         if (ping == null) {
            return null;
         }
         peekedPingSourcePings.add(ping);
      }
      return peekedPingSourcePings.get(index);
   }

   private @Nullable Ping nextPingFromPingSource() throws IOException {
      Ping ping;
      if (!peekedPingSourcePings.isEmpty()) {
         ping = peekedPingSourcePings.removeFirst();
      } else {
         ping = pingSource.nextPing(asyncHandle);
      }
      pingBuffering.in(ping);
      return ping;
   }

   final @Nullable Ping nextConvertedInputPing() throws IOException {
      Ping ping = nextPingFromPingSource();
      if (ping == null) {
         return null;
      }
      return convertPing(ping);
   }

   @Override
   public final @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      this.asyncHandle = asyncHandle;
      if (asyncHandle.isCancelled()) {
         return null;
      }
      try {
         Ping ping = nextProcessedPing();
         pingBuffering.out(ping);
         return ping;
      } catch (ChannelDataReadOnlyException e) {
         throw new ModuleProcessingException(module, "Module \"" + module.getDisplayName() + "\" cannot be used on data type \"" + e.getChannelData().getReadOnlyBecauseOfDataType()
               + "\" on channel " + e.getChannelData().getChannel() + ".\nTry using the \"Complex to real\" module first!", e);
      }
   }

   abstract @Nullable Ping nextProcessedPing() throws IOException;
}
