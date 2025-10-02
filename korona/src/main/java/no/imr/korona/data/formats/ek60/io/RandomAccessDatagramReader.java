package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.DatagramSource;

import java.io.Closeable;
import java.io.IOException;

/**
 * Datagram reader with random access.
 */
public interface RandomAccessDatagramReader extends DatagramSource, Closeable {
   long getSize() throws IOException;

   long getPosition() throws IOException;

   /**
    * Sets the position to start reading from.
    * <p>
    * NB: The buffer returned by {@link BaseDatagramReader#getReadBuffer()} might have to be cleared,
    * for example by calling {@link ChannelDatagramReader#discardBufferContents()}.
    *
    * @param position the new position
    * @throws IOException if some IO error occurs
    */
   void setPosition(long position) throws IOException;

   /**
    * Get datagrams from start again.
    *
    * @throws IOException if some IO error occurs
    */
   default void rewind() throws IOException {
      setPosition(0);
   }
}
