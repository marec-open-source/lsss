package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.DatagramSource;
import no.imr.korona.data.datagrams.Idx0Datagram;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * A source for getting {@link Idx0Datagram}s.
 */
interface IdxSource extends DatagramSource {
   @Override
   @Nullable Idx0Datagram nextDatagram() throws IOException;
}
