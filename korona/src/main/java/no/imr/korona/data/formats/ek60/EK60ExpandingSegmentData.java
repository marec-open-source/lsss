package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A segment expanding as data is being written.
 */
final class EK60ExpandingSegmentData extends EK60SegmentData {
   private final EK60PingReader pingReader;

   EK60ExpandingSegmentData(EK60FileSet ek60FileSet, EndOfInputHandler endOfInputHandler, DatagramTypeManager datagramTypeManager) throws IOException {
      super(new RawFile(ek60FileSet.getRaw(), endOfInputHandler, datagramTypeManager),
            Collections.synchronizedList(new ArrayList<>()), List.of(), null,
            Collections.synchronizedList(new ArrayList<>()));

      pingReader = EK60PingReader.create(ek60FileSet, endOfInputHandler, datagramTypeManager, true);
      pingReader.disallowPingIndexBuffering();
   }

   @Nullable PingIndex expand(AsyncHandle asyncHandle) throws IOException {
      Ping ping = pingReader.nextEmptyPing(asyncHandle);
      if (ping == null) {
         return null;
      }
      getPingIndices().add((Idx0Datagram) ping.getPingIndex());
      getBot0Datagrams().add(ping.getBot0Datagram());
      setFileEndOffset(pingReader.getEndOffset());
      return ping.getPingIndex();
   }

   @Override
   public void close() throws IOException {
      closePingReader();
      super.close();
   }

   void closePingReader() throws IOException {
      pingReader.close();
   }
}
