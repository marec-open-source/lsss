package no.imr.korona.data.formats.ek60.io;

import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Writes datagrams to a file.
 */
public final class FileDatagramWriter extends ChannelDatagramWriter {
   private final Path file;
   private final FileChannel fileChannel;

   public FileDatagramWriter(Path file) throws IOException {
      FileChannel fileChannel = FileUtils.openWritableChannel(file);

      super(fileChannel);

      this.file = file;
      this.fileChannel = fileChannel;
   }

   public void flush() throws IOException {
      if (fileChannel.isOpen()) {
         fileChannel.force(false);
      }
   }

   public Path getFile() {
      return file;
   }

   public FileChannel getFileChannel() {
      return fileChannel;
   }
}
