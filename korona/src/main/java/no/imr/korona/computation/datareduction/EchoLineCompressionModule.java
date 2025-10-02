package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.datagrams.subdatagrams.echoline.EchoLineData;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class EchoLineCompressionModule extends ConcurrentPingModule {
   public final FloatParameter threshold = new FloatParameter(
         new Name("Threshold"),
         -82, Unit.DB,
         "Only samples above the threshold will be kept");

   public EchoLineCompressionModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            threshold
      );
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new EchoLineCompressionModuleComputation(this, computationContext, pingSource);
   }

   private static final class EchoLineCompressionModuleComputation extends ConcurrentPingModuleComputation {
      private final float svThreshold;

      private EchoLineCompressionModuleComputation(EchoLineCompressionModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         svThreshold = PowerData.logSvToSv(module.threshold.getFloatValue());
      }

      @Override
      protected void processPing(Ping ping) {
         ping.getNonNullChannelDatas().forEach(channelData -> {
            EchoLineData echoLineData = toEchoLineData(channelData.getPowerData());
            ping.remove(channelData);
            ping.add(echoLineData);
         });
      }

      private EchoLineData toEchoLineData(PowerData powerData) {
         List<EchoLineData.EchoLine> echoLines = new ArrayList<>();
         float[] sv = powerData.getSv();
         AngleData angleData = powerData.getAngleData();
         float[] electricalAngles = angleData != null ? angleData.getElectricalAngles() : null;
         for (int i = 0; i < sv.length; i++) {
            if (sv[i] >= svThreshold) {
               int iEnd = i + 1;
               while (iEnd < sv.length && sv[iEnd] >= svThreshold) {
                  iEnd++;
               }
               echoLines.add(new EchoLineData.EchoLine(
                     i,
                     Arrays.copyOfRange(sv, i, iEnd),
                     electricalAngles != null ? Arrays.copyOfRange(electricalAngles, 2 * i, 2 * iEnd) : null
               ));
               i = iEnd;
            }
         }
         return new EchoLineData(powerData, echoLines);
      }
   }
}
