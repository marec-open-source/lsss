package no.imr.korona.computation.expression;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.Utils;
import no.imr.tools.compile.CompileException;

import java.util.ArrayList;
import java.util.List;

final class ExpressionModuleComputation extends ConcurrentPingModuleComputation {
   private final List<ExpressionApplier> expressionAppliers = new ArrayList<>();
   private final int resultChannel;

   ExpressionModuleComputation(ExpressionModule module, ComputationContext computationContext, PingSource pingSource) throws ModuleConfigurationException {
      super(module, computationContext, pingSource);

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      RawFileConfiguration oldRawFileConfiguration = pingConfiguration.getRawFileConfiguration();
      if (oldRawFileConfiguration.getTransducers().isEmpty()) {
         throw new ModuleConfigurationException(module, "No channels available");
      }

      int[] kHz = new int[oldRawFileConfiguration.getTransducerCount()];
      for (int channel = 1; channel <= kHz.length; channel++) {
         RawFileTransducer transducer = oldRawFileConfiguration.getTransducers().get(channel - 1);
         kHz[channel - 1] = Utils.hzToKHz(transducer.getFrequency());
      }

      resultChannel = oldRawFileConfiguration.getTransducerCount() + 1;

      for (String expression : module.expressions.getValue()) {
         try {
            ExpressionApplier expressionApplier = new ExpressionApplier(expression, kHz, resultChannel);
            if (expressionApplier.getUnavailableVariables().isEmpty()) {
               expressionAppliers.add(expressionApplier);
            }
         } catch (CompileException e) {
            // The parameter itself assures that the expression is compilable.
            throw new ShouldNotHappenException(e);
         }
      }

      if (expressionAppliers.isEmpty()) {
         throw new ModuleConfigurationException(module,
               module.expressions.getValue().isEmpty()
                     ? "No expressions specified"
                     : "All expressions have unavailable variables");
      }

      PingConfiguration newPingConfiguration = pingConfiguration.createCopy();
      RawFileConfiguration newRawFileConfiguration = newPingConfiguration.getRawFileConfiguration();
      newRawFileConfiguration.newChannel(newRawFileConfiguration.getTransducers().get(expressionAppliers.getFirst().getChannelToCopyFrom() - 1));
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
      for (ExpressionApplier expressionApplier : expressionAppliers) {
         PowerData powerData = expressionApplier.apply(ping);
         if (powerData != null) {
            RawFileTransducer transducer = getPingConfiguration().getRawFileConfiguration().getTransducers().get(resultChannel - 1);
            powerData.setFrequency(transducer.getFrequency());
            ping.add(powerData);
            return;
         }
      }
   }
}
