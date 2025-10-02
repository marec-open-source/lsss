package no.imr.korona.data;

import no.imr.korona.data.datagrams.BaseDatagram;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Iterator;

/**
 * A source for getting datagrams.
 */
@FunctionalInterface
public interface DatagramSource {
   /**
    * Returns the next datagram.
    *
    * @return the next datagram or {@code null} if no more datagrams are available
    * @throws IOException if some IO error occurs
    */
   @Nullable BaseDatagram nextDatagram() throws IOException;

   // Static factory methods:

   static DatagramSource concat(DatagramSource first, DatagramSource second) {
      return () -> {
         BaseDatagram datagram = first.nextDatagram();
         if (datagram != null) {
            return datagram;
         }
         return second.nextDatagram();
      };
   }

   static DatagramSource ofDatagrams(Iterable<? extends BaseDatagram> datagrams) {
      Iterator<? extends BaseDatagram> iterator = datagrams.iterator();
      return () -> iterator.hasNext() ? iterator.next() : null;
   }
}
