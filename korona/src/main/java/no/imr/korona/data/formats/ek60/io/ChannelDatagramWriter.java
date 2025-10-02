package no.imr.korona.data.formats.ek60.io;

import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Writes datagrams to a {@link WritableByteChannel}.
 */
public abstract class ChannelDatagramWriter extends BaseDatagramWriter {
   private final WritableByteChannel channel;

   protected ChannelDatagramWriter(WritableByteChannel channel) {
      this.channel = channel;
   }

   @Override
   public void writeBuffer(ByteBuffer byteBuffer) throws IOException {
      FileUtils.write(channel, byteBuffer);
   }

   public boolean isOpen() {
      return channel.isOpen();
   }

   @Override
   public void close() throws IOException {
      channel.close();
   }
}
