package no.imr.korona.data.datagrams.subdatagrams.ts;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.PerChannelDatagram;
import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

public final class TsDatagram extends BaseSubDatagram implements PerChannelDatagram {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.TS,
         "TS", TsDatagram::new);

   private int channel;
   private final List<TsDatagramDetection> detections;

   public TsDatagram(Instant instant, int channel, List<TsDatagramDetection> detections) {
      super(instant);

      this.channel = channel;
      this.detections = detections;
   }

   private TsDatagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      channel = byteBuffer.getInt();
      detections = ByteBufferUtils.readCountAndList(byteBuffer, TsDatagramDetection.BYTE_SIZE, TsDatagramDetection::new);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt(channel);
      ByteBufferUtils.writeCountAndList(byteBuffer, detections, TsDatagramDetection::write);
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }

   @Override
   public int getChannel() {
      return channel;
   }

   @Override
   public void setChannel(int channel) {
      this.channel = channel;
   }

   public List<TsDatagramDetection> getDetections() {
      return detections;
   }
}
