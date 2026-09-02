package no.imr.korona.cli.commands;

import no.imr.korona.Korona;
import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.data.metadata.MetadataExtractor;
import no.imr.korona.data.metadata.pojo.Metadata;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.web.WebUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

final class ExtractMetadataCommandJob extends CliCommandJob {
   private final Path dir;
   private final @Nullable Path output;

   ExtractMetadataCommandJob(Path dir, @Nullable Path output) {
      this.dir = dir;
      this.output = output;
   }

   @Override
   public String getContentType() {
      return WebUtils.TEXT_XML;
   }

   @Override
   public void run(InputStream in, PrintStream out) throws IOException {
      Metadata metadata = MetadataExtractor.extract(dir, new Korona(), new AsyncHandle());
      byte[] bytes = JsonUtils.PRETTY_PRINTER.writeValueAsBytes(metadata);
      if (output == null) {
         out.println(new String(bytes, Utils.UTF_8));
      } else {
         Path file = dir.resolve(output);
         Log.global.info("Writing metadata to " + file);
         FileUtils.replaceFileSafely(file, bytes);
      }
   }
}
