package no.imr.korona.data.datagrams.subdatagrams.graphical;

import no.imr.korona.data.formats.ek60.io.ByteBufferReader;

public record GraphicalObjectType(
      int id,
      ByteBufferReader<GraphicalObject> reader
) {
   @Override
   public String toString() {
      return Integer.toString(id);
   }
}
