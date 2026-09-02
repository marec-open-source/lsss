package no.imr.korona.data.formats.ek60.io;

import java.io.OutputStream;
import java.nio.channels.Channels;

public final class OutputStreamDatagramWriter extends ChannelDatagramWriter {
   public OutputStreamDatagramWriter(OutputStream outputStream) {
      super(Channels.newChannel(outputStream));
   }
}
