package no.imr.lsss.framework.export;

import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

public final class ExportFile {
   private final String baseName;
   private final String suffix;
   private @Nullable Path file;

   public ExportFile(String baseName, String suffix) {
      this.baseName = baseName;
      this.suffix = suffix;
   }

   void setInfix(Path directory, String prefix, String infix) {
      file = directory.resolve(prefix + baseName + infix + suffix);
   }

   public Path getFile() {
      if (file == null) {
         throw new IllegalStateException();
      }
      return file;
   }

   public PrintWriter newPrintWriter() throws IOException {
      return FileUtils.newPrintWriter(getFile(), Utils.nativeCharset());
   }
}
