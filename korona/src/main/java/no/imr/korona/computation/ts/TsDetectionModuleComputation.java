package no.imr.korona.computation.ts;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.datagrams.subdatagrams.ts.TsDatagram;
import no.imr.korona.data.datagrams.subdatagrams.ts.TsDatagramDetection;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.util.ts.TSDetection;
import no.imr.korona.util.ts.TSDetector;
import no.imr.tools.xml.XmlUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

final class TsDetectionModuleComputation extends ConcurrentPingModuleComputation {
   private final TSDetector tsDetector;
   private final float[] minRange;
   private final float[] maxRange;

   TsDetectionModuleComputation(TsDetectionModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      tsDetector = module.createTSDetector();
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      List<RawFileTransducer> transducers = pingConfiguration.getRawFileConfiguration().getTransducers();
      minRange = new float[transducers.size()];
      maxRange = new float[transducers.size()];
      Path file = module.getOptionalConfigFile(TransducerRangesFileService.NAME);
      if (file != null) {
         TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(file));
         for (int i = 0; i < transducers.size(); i++) {
            RawFileTransducer transducer = transducers.get(i);
            minRange[i] = transducerParameterManager.getBlindZone(transducer.getKHz()).orElse(0f);
            maxRange[i] = transducerParameterManager.getRange(transducer.getKHz()).orElse(Float.POSITIVE_INFINITY);
         }
      } else {
         Arrays.fill(minRange, 0);
         Arrays.fill(maxRange, Float.POSITIVE_INFINITY);
      }
   }

   @Override
   protected void processPing(Ping ping) {
      ping.getNonNullChannelDatas().forEach(channelData -> {
         processChannel(ping, channelData);
      });
   }

   private void processChannel(Ping ping, ChannelData channelData) {
      int channel = channelData.getChannel();
      float endRange = Math.min(maxRange[channel - 1], channelData.getMaxRange());
      float bottomRange = channelData.depthToRange((float) ping.getBot0Datagram().getChannelDepths()[channel - 1]);
      if (bottomRange > 0) {
         endRange = Math.min(endRange, bottomRange);
      }
      int iBegin = Math.clamp((int) Math.ceil(channelData.rangeToSampleIndexAsFloat(minRange[channel - 1])), 0, channelData.getCount());
      int iEnd = Math.clamp((int) Math.floor(channelData.rangeToSampleIndexAsFloat(endRange)), 0, channelData.getCount());
      if (iEnd <= iBegin) {
         return;
      }
      List<TSDetection> tsDetections = tsDetector.getAcceptedTsDetections(ping, channelData, iBegin, iEnd);
      if (tsDetections.isEmpty()) {
         return;
      }
      List<TsDatagramDetection> detections = tsDetections.stream()
            .map(tsDataCandidate -> new TsDatagramDetection(channelData, tsDataCandidate))
            .toList();
      ping.add(new TsDatagram(channelData.getNTDate(), channel, detections));
   }
}
