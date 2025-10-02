package no.imr.korona.data.datagrams.subdatagrams.range;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;

public final class BottomRangesSubDatagram extends BaseSubDatagram {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.BOTTOM_RANGES,
         "Bottom ranges", BottomRangesSubDatagram::new);

   private final float[] channelRanges;

   public BottomRangesSubDatagram(long ntDate, float[] channelRanges) {
      super(ntDate);

      this.channelRanges = channelRanges;
   }

   private BottomRangesSubDatagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      channelRanges = ByteBufferUtils.readCountAndFloatArray(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCountAndFloatArray(byteBuffer, channelRanges);
   }

   public float[] getChannelRanges() {
      return channelRanges;
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }
}
