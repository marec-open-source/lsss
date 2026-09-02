package no.imr.korona.data.datagrams.subdatagrams.graphical;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.TableOfContentsPingItem;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

public final class GraphicalInfoTocSubDatagram extends BaseSubDatagram implements TableOfContentsPingItem {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.GRAPHICAL_INFO_TOC,
         "Graphical info toc", GraphicalInfoTocSubDatagram::new);

   private final List<Instant> instants;

   public GraphicalInfoTocSubDatagram(Instant instant, List<Instant> instants) {
      super(instant);

      this.instants = instants;
   }

   private GraphicalInfoTocSubDatagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      instants = ByteBufferUtils.readInstantsAsNTDates(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeInstantsAsNTDates(byteBuffer, instants);
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }

   @Override
   public List<Instant> getInstants() {
      return instants;
   }
}
