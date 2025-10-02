package no.imr.lsss.modules.echogram;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataFileSetPingReader;
import no.imr.korona.data.formats.ek60.EK60FileSet;
import no.imr.korona.data.formats.ek60.EK60Writer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.util.KoronaUtils;
import no.imr.lsss.framework.export.ExportFile;
import no.imr.lsss.framework.export.Exporter;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Exports an echogram covering the ping range specified by the parameters in EchogramExportSettings.
 */
public abstract class EchogramExporter extends Exporter {
   protected EchogramExporter(FeaturePlugin plugin, Name name, String description) {
      super(plugin, name, description);
   }

   protected void exportEchogram(DataFileSet dataFileSet, PingRange pingRange, AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      if (pingRange.isEmpty()) {
         return;
      }

      ExportFile rawFile = new ExportFile("EchosounderData", ".raw");
      ExportFile cdsFile = createCdsFileWrapper();
      initExportFiles(pingRange, rawFile, cdsFile);
      writeModuleSetup(cdsFile, dataFileSet, pingRange);

      Path file = rawFile.getFile();

      try (PingReader pingReader = new DataFileSetPingReader(dataFileSet, pingRange);
           EK60Writer ek60Writer = new EK60Writer(file.getParent(), file.getFileName().toString(), pingReader.getPingConfiguration())) {

         while (!asyncHandle.isCancelled()) {
            Ping ping = pingReader.nextPing(asyncHandle);
            if (ping == null) {
               break;
            }
            ping = filterPing(ping);
            ek60Writer.write(ping);
            progressHandler.setProgress(KoronaUtils.getFraction(pingRange, ping.getPingIndex()));
         }
      }

      if (asyncHandle.isCancelled()) {
         new EK60FileSet(rawFile.getFile()).delete();
      }
   }

   protected Ping filterPing(Ping ping) {
      return ping;
   }
}
