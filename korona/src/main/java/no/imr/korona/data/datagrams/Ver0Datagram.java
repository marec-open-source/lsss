package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;

/**
 * Version information.
 */
public final class Ver0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("VER0", Ver0Datagram::new);

   public final String fileVersion;
   public final String softwareVersion;
   public final String versionInfo;
   public final String productName;

   public Ver0Datagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate);

      fileVersion = ByteBufferUtils.readCString(byteBuffer, 32);
      softwareVersion = ByteBufferUtils.readCString(byteBuffer, 32);
      versionInfo = ByteBufferUtils.readCString(byteBuffer, 64);
      productName = ByteBufferUtils.readCString(byteBuffer, 64);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCString(byteBuffer, fileVersion, 32);
      ByteBufferUtils.writeCString(byteBuffer, softwareVersion, 32);
      ByteBufferUtils.writeCString(byteBuffer, versionInfo, 64);
      ByteBufferUtils.writeCString(byteBuffer, productName, 64);
   }

   @Override
   public String toStringExtra() {
      return "fileVersion: " + fileVersion
            + ", softwareVersion: " + softwareVersion
            + ", versionInfo: " + versionInfo
            + ", productName: " + productName;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }
}
