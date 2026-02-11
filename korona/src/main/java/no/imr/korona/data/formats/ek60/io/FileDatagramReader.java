package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramTypeManager;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * Reads datagrams from a file.
 */
public class FileDatagramReader extends ChannelDatagramReader implements RandomAccessDatagramReader {
   private final Path file;
   private final FileChannel fileChannel;

   /**
    * Creates a new FileDatagramReader.
    *
    * @param file                the file to read from
    * @param datagramTypeManager datagram type manager
    * @throws IOException if some IO error occurs
    */
   public FileDatagramReader(Path file, DatagramTypeManager datagramTypeManager) throws IOException {
      FileChannel fileChannel = FileChannel.open(file);

      super(fileChannel, datagramTypeManager);

      this.file = file;
      this.fileChannel = fileChannel;
   }

   @Override
   public String toString() {
      return file.toString();
   }

   public Path getFile() {
      return file;
   }

   public FileChannel getFileChannel() {
      return fileChannel;
   }

   @Override
   public long getSize() throws IOException {
      return fileChannel.size();
   }

   @Override
   public long getPosition() throws IOException {
      return fileChannel.position() - getReadBuffer().remaining();
   }

   @Override
   public void setPosition(long position) throws IOException {
      fileChannel.position(position);
      discardBufferContents();
   }

   @Override
   protected void skipBytesFromChannel(int bytesToSkip) throws IOException {
      long p = fileChannel.position() + bytesToSkip;
      if (p > fileChannel.size()) {
         throw new BufferUnderflowException();
      }
      fileChannel.position(p);
   }
}
