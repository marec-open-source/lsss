package no.imr.korona.data.util;

import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.computation.display.PlayboxModule;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentDataPingReader;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Does on the fly processing.
 */
public final class ProcessingSegmentData extends SegmentData {
   private final SegmentData segmentData;
   private final PingConfiguration pingConfiguration;
   private final ModuleContainerComputation containerComputation;
   private final List<ConcurrentPingModuleComputation> moduleComputations;
   private final List<Bot0Datagram> bot0Datagrams;

   public ProcessingSegmentData(SegmentHandle segmentHandle, SegmentData segmentData, ModuleContainer moduleContainer) throws IOException {
      this.segmentData = segmentData;
      containerComputation = moduleContainer.createComputation(new SegmentDataPingReader(segmentData, segmentHandle.getMainFile()));
      pingConfiguration = containerComputation.getPingConfiguration();

      moduleComputations = Utils.getAllOfType(containerComputation.getModuleComputations(), ConcurrentPingModuleComputation.class)
            .toList();
      moduleComputations.forEach(ConcurrentPingModuleComputation::prepareForConcurrentProcessing);

      List<Bot0Datagram> inputBot0Datagrams = segmentData.getBot0Datagrams();
      if (inputBot0Datagrams.getFirst() != convertBot0(inputBot0Datagrams.getFirst())) {
         bot0Datagrams = inputBot0Datagrams.stream()
               .map(this::convertBot0)
               .toList();
      } else {
         bot0Datagrams = inputBot0Datagrams;
      }
   }

   public static List<ConcurrentPingModule> getApplicableModules(ModuleContainer moduleContainer) {
      return moduleContainer.getModules().stream()
            .map(module -> {
               if (module.active.getBooleanValue()) {
                  if (module instanceof ConcurrentPingModule concurrentPingModule) {
                     return concurrentPingModule;
                  }
                  if (!(module instanceof PlayboxModule)) {
                     Log.global.warning("Cannot use module '" + module.getDisplayName() + "' in on the fly processing");
                  }
               }
               return null;
            })
            .filter(Objects::nonNull)
            .toList();
   }

   private Bot0Datagram convertBot0(Bot0Datagram bot0Datagram) {
      for (ConcurrentPingModuleComputation computation : moduleComputations) {
         bot0Datagram = computation.convertBot0(bot0Datagram);
      }
      return bot0Datagram;
   }

   public List<ConcurrentPingModuleComputation> getModuleComputations() {
      return moduleComputations;
   }

   @Override
   public List<? extends PingIndex> getPingIndices() {
      return segmentData.getPingIndices();
   }

   @Override
   public @Nullable WrapAround getWrapAround() {
      return segmentData.getWrapAround();
   }

   @Override
   public @Nullable String getInfo() {
      return segmentData.getInfo();
   }

   @Override
   public List<Bot0Datagram> getBot0Datagrams() {
      return bot0Datagrams;
   }

   @Override
   public void close() throws IOException {
      segmentData.close();
      containerComputation.close();
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException {
      PingData pingData = segmentData.loadPingData(pingIndex, asyncHandle);
      Ping ping = new DefaultPing(pingIndex, segmentData.getBot0Datagram(pingIndex), pingData);
      for (ConcurrentPingModuleComputation computation : moduleComputations) {
         ping = computation.convertAndProcess(ping);
      }
      return ping.getPingData();
   }

   @Override
   public void onLoadPingData(Ping ping, PingData pingData) {
      segmentData.onLoadPingData(ping, pingData);
   }
}
