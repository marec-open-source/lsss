package no.imr.korona.data.datagrams.subdatagrams.range;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.time.Instant;

public final class BottomRangesSubDatagram extends BaseSubDatagram {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.BOTTOM_RANGES,
         "Bottom ranges", BottomRangesSubDatagram::new);

   private final float[] channelRanges;

   public BottomRangesSubDatagram(Instant instant, float[] channelRanges) {
      super(instant);

      this.channelRanges = channelRanges;
   }

   private BottomRangesSubDatagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

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
