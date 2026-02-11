package no.imr.lsss.framework.export;

import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.parameter.Name;
import tools.jackson.databind.ObjectWriter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public abstract class StreamingExporter extends Exporter {
   protected StreamingExporter(FeaturePlugin plugin, Name name, String description) {
      super(plugin, name, description);
   }

   public boolean isJson() {
      return true;
   }

   public void exportToStream(AsyncHandle asyncHandle, ProgressHandler progressHandler, ExportFile exportFile) throws IOException {
      try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(exportFile.getFile()))) {
         exportToStream(asyncHandle, progressHandler, out, JsonUtils.PRETTY_PRINTER);
      }
   }

   public abstract void exportToStream(AsyncHandle asyncHandle, ProgressHandler progressHandler, OutputStream out, ObjectWriter objectWriter);
}
