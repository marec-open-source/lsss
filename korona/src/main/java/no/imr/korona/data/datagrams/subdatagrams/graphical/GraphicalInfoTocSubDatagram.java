package no.imr.korona.data.datagrams.subdatagrams.graphical;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.TableOfContentsPingItem;

import java.nio.ByteBuffer;

public final class GraphicalInfoTocSubDatagram extends BaseSubDatagram implements TableOfContentsPingItem {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.GRAPHICAL_INFO_TOC,
         "Graphical info toc", GraphicalInfoTocSubDatagram::new);

   private final long[] ntDates;

   public GraphicalInfoTocSubDatagram(long ntDate, long[] ntDates) {
      super(ntDate);

      this.ntDates = ntDates;
   }

   private GraphicalInfoTocSubDatagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      ntDates = ByteBufferUtils.readCountAndLongArray(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCountAndLongArray(byteBuffer, ntDates);
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }

   @Override
   public long[] getNTDates() {
      return ntDates;
   }
}
