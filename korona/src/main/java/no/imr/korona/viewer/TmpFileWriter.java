package no.imr.korona.viewer;

import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.feature.EchogramWindow;
import no.imr.korona.data.formats.ek60.EK60DataFormatPlugin;
import no.imr.korona.data.formats.ek60.EK60Writer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.IntRange;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class TmpFileWriter implements PingSource {
   private static final Path DIR = Utils.getTmpDir().resolve("korona");
   private static final String FILE_NAME = "tmp" + EK60DataFormatPlugin.RAW_SUFFIX;

   private final PingSource pingSource;
   private final EK60Writer ek60Writer;

   TmpFileWriter(PingSource pingSource) throws IOException {
      this.pingSource = pingSource;
      ek60Writer = new EK60Writer(DIR, FILE_NAME, getPingConfiguration());
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingSource.getPingConfiguration();
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      Ping ping = pingSource.nextPing(asyncHandle);
      if (ping != null) {
         ek60Writer.write(ping);
      }
      return ping;
   }

   @Override
   public void close() throws IOException {
      ek60Writer.close();
   }

   EchogramWindow createEchogramWindow(ModuleContainerComputation computation, IntRange pingRange, FloatRange depthRange) throws IOException {
      ek60Writer.flush();

      Path tmpFile = DIR.resolve(FILE_NAME);
      SegmentHandle segmentHandle = computation.getComputationContext().getModuleContainer().getKorona().getDataFormatManager().createSegmentHandle(tmpFile);
      if (segmentHandle == null) {
         throw new IOException("Error opening " + tmpFile);
      }
      try (SegmentData segmentData = segmentHandle.createSegmentData(NoticeHandler.ignore(), new AsyncHandle())) {
         List<Ping> pings = new ArrayList<>();
         for (PingIndex pingIndex : segmentData.getPingIndices().subList(pingRange.begin(), pingRange.end())) {
            pings.add(segmentData.loadPing(pingIndex, new AsyncHandle()));
         }

         EchogramWindow echogramWindow = new EchogramWindow(new Configurator(computation.getComputationContext().getModuleContainer().getConfigFileSettings(),
               segmentData.getRawFileConfiguration()), pingRange.begin(), pings, depthRange);
         echogramWindow.setRawFile(computation.getComputationContext().getPingReader().getFile());
         echogramWindow.setConfigDocument(XmlUtils.toDocument(computation.getComputationContext().getModuleContainer().toXml()));
         return echogramWindow;
      }
   }
}
