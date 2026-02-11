package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.datagrams.DatagramTypeManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.channels.Channels;

public final class InputStreamDatagramReader extends ChannelDatagramReader {
   private final InputStream inputStream;

   public InputStreamDatagramReader(InputStream inputStream, DatagramTypeManager datagramTypeManager) {
      super(Channels.newChannel(inputStream), datagramTypeManager);

      this.inputStream = inputStream;
   }

   @Override
   protected void skipBytesFromChannel(int bytesToSkip) throws IOException {
      try {
         inputStream.skipNBytes(bytesToSkip);
      } catch (EOFException _) {
         throw new BufferUnderflowException();
      }
   }
}
