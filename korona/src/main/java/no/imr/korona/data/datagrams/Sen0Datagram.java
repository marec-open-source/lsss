package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.tools.Utils;
import no.imr.tools.time.NTDate;

import java.nio.ByteBuffer;

/**
 * Sensor datagram.
 */
public final class Sen0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("SEN0", Sen0Datagram::new);

   public final long receivedNTDate;
   public final String protocol;
   public final String portName;
   public final byte[] message;

   public Sen0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      receivedNTDate = byteBuffer.getLong();
      protocol = ByteBufferUtils.readCString(byteBuffer, 32);
      portName = ByteBufferUtils.readCString(byteBuffer, 32);
      int messageLength = ByteBufferUtils.readCount(byteBuffer, 1);
      message = new byte[messageLength];
      byteBuffer.get(message);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putLong(receivedNTDate);
      ByteBufferUtils.writeCString(byteBuffer, protocol, 32);
      ByteBufferUtils.writeCString(byteBuffer, portName, 32);
      byteBuffer.putInt(message.length);
      byteBuffer.put(message);
   }

   @Override
   public String toStringExtra() {
      return "received: " + NTDate.ntDateToInstant(receivedNTDate)
            + ", protocol: " + protocol
            + ", portName: " + portName
            + ", message: \"" + messageAsString() + '"';
   }

   public String messageAsString() {
      return new String(message, Utils.ISO_8859_1).trim();
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      pingConversion.addPingItem(this);
      if (protocol.equalsIgnoreCase("nmea")) {
         pingConversion.addPingItem(new NmeaPingItem(getNTDate(), messageAsString(), false));
      }
   }
}
