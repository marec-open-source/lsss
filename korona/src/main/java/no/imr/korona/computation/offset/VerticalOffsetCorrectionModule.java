package no.imr.korona.computation.offset;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ConfigFileSettingsException;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.tools.parameter.Name;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class VerticalOffsetCorrectionModule extends ConcurrentPingModule {
   public VerticalOffsetCorrectionModule() {
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(VerticalTransducerOffsetsFileService.NAME);
   }

   private TransducerParameterManager getTransducerParameterManager() throws IOException {
      Path file = getRequiredConfigFile(VerticalTransducerOffsetsFileService.NAME);
      Document document = XmlUtils.readDocument(file);
      String type = document.getRootElement().attributeValue(TransducerParameterManager.XML_TYPE);
      if (TransducerParameters.ParameterType.HORIZONTAL.toString().equals(type)) {
         throw new ConfigFileSettingsException(this, VerticalTransducerOffsetsFileService.NAME, file, "Wrong type: " + type);
      }
      return new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, document);
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new VerticalOffsetCorrectionModuleComputation(this, computationContext, pingSource);
   }

   private static final class VerticalOffsetCorrectionModuleComputation extends ConcurrentPingModuleComputation {
      private final Map<Integer, TransducerParameters> channelToOffsetMap;

      private VerticalOffsetCorrectionModuleComputation(VerticalOffsetCorrectionModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
         super(module, computationContext, pingSource);

         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         channelToOffsetMap = OffsetCorrectionUtils.makeChannelToOffsetMap(module.getTransducerParameterManager(), pingConfiguration);
      }

      @Override
      protected void processPing(Ping ping) {
         for (ChannelData channelData : ping.getChannelDatas()) {
            if (channelData == null) {
               continue;
            }
            TransducerParameters offsets = channelToOffsetMap.get(channelData.getChannel());
            if (offsets != null) {
               channelData.setTransducerDepth(channelData.getTransducerDepth() +
                     offsets.getDeltaZ0() + offsets.getDeltaZPulseDelay());
            }
         }
      }
   }
}
