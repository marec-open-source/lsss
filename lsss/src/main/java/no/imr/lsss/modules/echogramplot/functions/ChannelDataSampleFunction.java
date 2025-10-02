package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.util.ExportRounding;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public final class ChannelDataSampleFunction extends PingFunction {
   private static final ComputeFunction COMPUTE_FUNCTION_NAN = (ping, channel) -> Double.NaN;

   private final ComputeFunctionForChannelData computeFunctionForChannelData;
   private ComputeFunction computeFunction = COMPUTE_FUNCTION_NAN;

   private ChannelDataSampleFunction(Name name, Unit unit, ExportTransform exportTransform, ComputeFunctionForChannelData computeFunctionForChannelData) {
      super(name, unit, exportTransform, true);

      this.computeFunctionForChannelData = computeFunctionForChannelData;
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      return computeFunction.compute(ping, channel);
   }

   private double computeForZ(Ping ping, int channel, EchogramZSettings zSettings, float z) {
      ChannelData channelData = ping.getChannelData(channel);
      if (channelData == null) {
         return Double.NaN;
      }
      float depth = zSettings.zToDepth(z, ping.getPingIndex());
      int i = channelData.depthToContainingSampleIndex(depth);
      if (i < 0 || i >= channelData.getCount()) {
         return Double.NaN;
      }
      return computeFunctionForChannelData.compute(channelData, i);
   }

   @Override
   public void addListeners(LSSS lsss, ListenerRegistry listenerRegistry) {
      AtomicReference<@Nullable EchogramModule> echogramModuleReference = new AtomicReference<>();
      lsss.getModuleManager().getModules(EchogramModule.class)
            .forEach(echogramModule -> {
               listenerRegistry.add(echogramModule.mousePosition(), mousePosition -> {
                  echogramModuleReference.set(mousePosition.isPresent() ? echogramModule : null);
               });
            });
      listenerRegistry.add(lsss.getInterpretationSettings().mouseover().echogramPoint(), optionalEchogramPoint -> {
         EchogramPoint echogramPoint = optionalEchogramPoint.orElse(null);
         EchogramModule echogramModule = echogramModuleReference.get();
         if (echogramModule == null || echogramPoint == null) {
            computeFunction = COMPUTE_FUNCTION_NAN;
         } else {
            EchogramZSettings zSettings = echogramModule.getZSettings();
            float z = zSettings.depthToZ(echogramPoint.depth(), echogramPoint.pingIndex());
            computeFunction = (ping, channel) -> computeForZ(ping, channel, zSettings, z);
         }
         getChangeManager().notifyListeners();
      });
   }

   @FunctionalInterface
   private interface ComputeFunction {
      double compute(Ping ping, int channel);
   }

   @FunctionalInterface
   private interface ComputeFunctionForChannelData {
      double compute(ChannelData channelData, int sampleIndex);
   }

   public static ChannelDataSampleFunction sv() {
      return new ChannelDataSampleFunction(new Name("sv", "Sv"), Unit.DB, ExportRounding.db(),
            (channelData, i) -> channelData.getPowerData().getLogSv()[i]);
   }

   public static ChannelDataSampleFunction tsc() {
      return new ChannelDataSampleFunction(new Name("tsc", "TSC"), Unit.DB, ExportRounding.db(),
            (channelData, i) -> {
               PowerData powerData = channelData.getPowerData();
               return powerData.getAngleData() != null ? powerData.getTSC(i) : Double.NaN;
            });
   }

   public static ChannelDataSampleFunction tsu() {
      return new ChannelDataSampleFunction(new Name("tsu", "TSU"), Unit.DB, ExportRounding.db(),
            (channelData, i) -> channelData.getPowerData().getTSU(i));
   }
}
