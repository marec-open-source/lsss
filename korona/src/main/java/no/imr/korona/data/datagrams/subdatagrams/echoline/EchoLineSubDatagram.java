package no.imr.korona.data.datagrams.subdatagrams.echoline;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.Raw0Datagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.korona.data.datagrams.subdatagrams.SubDatagram;
import no.imr.korona.data.formats.ek60.io.ByteBufferReader;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.List;

public final class EchoLineSubDatagram extends SubDatagram {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.ECHO_LINE,
         "Echo line data", EchoLineSubDatagram::new);

   private final Raw0Datagram raw0Datagram = new Raw0Datagram(0);
   private final List<PowerEchoLine> powerEchoLines;

   EchoLineSubDatagram(long ntDate, List<PowerEchoLine> powerEchoLines) {
      super(ntDate);

      this.powerEchoLines = powerEchoLines;
   }

   private EchoLineSubDatagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      raw0Datagram.readConfig(byteBuffer);
      boolean hasAngles = (raw0Datagram.mode & Raw0Datagram.DATA_TYPE_ANGLES) != 0;
      powerEchoLines = ByteBufferUtils.readCountAndList(byteBuffer, 8, PowerEchoLine.reader(hasAngles));
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      raw0Datagram.writeConfig(byteBuffer);
      ByteBufferUtils.writeCountAndList(byteBuffer, powerEchoLines, PowerEchoLine::write);
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      RawFileTransducer transducer = pingConversion.getPingConfiguration().getRawFileConfiguration().getTransducers().get(raw0Datagram.channel - 1);
      float effectivePulseDuration = PowerData.findEffectivePulseDuration(pingConversion, transducer.getChannelId());
      EchoLineData echoLineData = new EchoLineData(this, pingConversion.getPingConfiguration(), effectivePulseDuration);
      PowerData powerData = echoLineData.getPowerData();
      powerData.setReadWrite();
      pingConversion.addPingItem(powerData);
   }

   @Override
   public boolean isSampleDatagram() {
      return true;
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }

   Raw0Datagram getRaw0Datagram() {
      return raw0Datagram;
   }

   List<PowerEchoLine> getPowerEchoLines() {
      return powerEchoLines;
   }

   record PowerEchoLine(
         int startSample,
         short[] shortPower,
         byte @Nullable [] angles
   ) {
      private static ByteBufferReader<PowerEchoLine> reader(boolean hasAngles) {
         return byteBuffer -> {
            int startSample = byteBuffer.getInt();
            short[] shortPower = ByteBufferUtils.readCountAndShorArray(byteBuffer);
            byte[] angles = hasAngles ? ByteBufferUtils.readByteArray(byteBuffer, 2 * shortPower.length) : null;
            return new PowerEchoLine(startSample, shortPower, angles);
         };
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(startSample);
         ByteBufferUtils.writeCountAndShortArray(byteBuffer, shortPower);
         if (angles != null) {
            byteBuffer.put(angles);
         }
      }
   }
}
