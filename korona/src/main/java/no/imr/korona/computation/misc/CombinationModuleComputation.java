package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.tools.logging.Log;
import no.imr.tools.math.MathUtils;
import org.jspecify.annotations.Nullable;

final class CombinationModuleComputation extends ConcurrentPingModuleComputation {
   private final CombinationModule module;
   private final int resultChannel;
   private final int firstOperandChannel;
   private final int secondOperandChannel;

   CombinationModuleComputation(CombinationModule module, ComputationContext computationContext, PingSource pingSource) throws ModuleConfigurationException {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      PingConfiguration newPingConfiguration = pingConfiguration.createCopy();
      RawFileConfiguration newRawFileConfiguration = newPingConfiguration.getRawFileConfiguration();

      firstOperandChannel = module.getChannel(module.firstOperandKHz, module.firstOperandChannel, newRawFileConfiguration);
      secondOperandChannel = module.getChannel(module.secondOperandKHz, module.secondOperandChannel, newRawFileConfiguration);

      newRawFileConfiguration.newChannel(newRawFileConfiguration.getTransducers().get(firstOperandChannel - 1));
      resultChannel = newRawFileConfiguration.getTransducerCount();
      setNewPingConfiguration(newPingConfiguration);
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      newPing.addAll(ping.getPingItems());
   }

   @Override
   public Bot0Datagram convertBot0(Bot0Datagram bot0Datagram) {
      return bot0Datagram.copyWithAddedChannel(1);
   }

   @Override
   protected void processPing(Ping ping) {
      PowerData powerData = doOperation(ping);
      if (powerData != null) {
         ping.add(powerData);
      }
   }

   private float[] getArray(PowerData powerData) {
      return module.logarithmicOperands.getBooleanValue() ? powerData.getLogSv() : powerData.getSv();
   }

   private @Nullable PowerData doOperation(Ping ping) {
      PowerData x = ping.getPowerData(firstOperandChannel);
      PowerData y = ping.getPowerData(secondOperandChannel);

      if (x == null || y == null) {
         PowerData notNull = x != null ? x : y;
         if (notNull == null) {
            return null;
         }
         PowerData result = notNull.makeCopyWithNoData();
         result.setChannel(resultChannel);
         result.setRange(notNull.getSampleDepth(0), notNull.getSampleDepth(0));
         getArray(result);
         return result;
      } else {
         PowerData result = x.makeCopyWithNoData();
         result.setChannel(resultChannel);

         float minDepth = Math.max(x.getMinDepth(), y.getMinDepth());
         float maxDepth = Math.min(x.getMaxDepth(), y.getMaxDepth());
         if (minDepth > maxDepth) {
            // Datagrams do not overlay vertically.
            result.setRange(x.getSampleDepth(0), x.getSampleDepth(0));
            getArray(result);
            return result;
         }
         result.setRange(minDepth, maxDepth);

         ResampledFloatArray xx = ResampledFloatArray.create(getArray(x), x, result);
         ResampledFloatArray yy = ResampledFloatArray.create(getArray(y), y, result);

         module.operation.getValue().eval(xx, yy, getArray(result));

         float[] data = module.logarithmicOperands.getBooleanValue() ? result.getLogSv() : result.getSv();

         if (MathUtils.avoidInfinities(data)) {
            Log.global.warning("CombinationModule: Result contains infinite values. Values will be clamped to min/max float value.");
         }

         return result;
      }
   }
}
