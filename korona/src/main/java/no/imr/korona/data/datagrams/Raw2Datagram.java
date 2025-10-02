package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.time.NTDate;

import java.nio.ByteBuffer;

/**
 * Sample byte stream.
 */
public final class Raw2Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = new DatagramType.Simple("RAW2", Raw2Datagram::new);

   public final int ipAddress;
   public final short port;
   public final short padding;
   public final int messageByteSize;
   public final BeamData message;

   public Raw2Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      ipAddress = byteBuffer.getInt();
      port = byteBuffer.getShort();
      padding = byteBuffer.getShort();
      messageByteSize = ByteBufferUtils.readByteSize(byteBuffer);
      message = new BeamData(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt(ipAddress);
      byteBuffer.putShort(port);
      byteBuffer.putShort(padding);
      byteBuffer.putInt(messageByteSize);
      message.write(byteBuffer);
   }

   @Override
   public String toStringExtra() {
      return "ipAddress: " + ipAddress
            + ", port: " + port
            + ", message: " + message;
   }

   @Override
   public boolean isSampleDatagram() {
      return true;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   public static final class BeamData {
      public final int byteSize;
      public final String id;
      public final long ntDate;
      public final int pingNumber;
      public final int datagramNumber; // MSB=1 => last dg in ping (appropriate for Main Beam only)
      public final int sampleIndexStart; // sample start index for this dg
      public final int sampleCount; // no. of samples in this dg
      public final float heave; // [m]
      public final float roll; // [deg]
      public final float pitch; // [deg]
      public final float yaw; // [deg]
      public final int beamIndexStart; // index of first beam in datagram
      public final short spare; // unused
      public final int beamCount;

      public final float[] data;

      public BeamData(ByteBuffer byteBuffer) throws DatagramFormatException {
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         id = ByteBufferUtils.readCString(byteBuffer, 4);
         ntDate = byteBuffer.getLong();
         pingNumber = byteBuffer.getInt();
         datagramNumber = byteBuffer.getInt();
         sampleIndexStart = byteBuffer.getInt();
         sampleCount = byteBuffer.getInt();
         heave = byteBuffer.getFloat();
         roll = byteBuffer.getFloat();
         pitch = byteBuffer.getFloat();
         yaw = byteBuffer.getFloat();
         beamIndexStart = 0xffff & byteBuffer.getShort();
         spare = byteBuffer.getShort();
         beamCount = byteBuffer.getInt();

         int complexCount = sampleCount * beamCount;
         if (sampleCount < 0 || beamCount < 0 || complexCount < 0 || complexCount > byteBuffer.remaining() / 8) {
            throw new DatagramFormatException(sampleCount + ", " + beamCount);
         }

         data = ByteBufferUtils.readFloatArray(byteBuffer, sampleCount * beamCount * 2);
      }

      public void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         ByteBufferUtils.writeCString(byteBuffer, id, 4);
         byteBuffer.putLong(ntDate);
         byteBuffer.putInt(pingNumber);
         byteBuffer.putInt(datagramNumber);
         byteBuffer.putInt(sampleIndexStart);
         byteBuffer.putInt(sampleCount);
         byteBuffer.putFloat(heave);
         byteBuffer.putFloat(roll);
         byteBuffer.putFloat(pitch);
         byteBuffer.putFloat(yaw);
         byteBuffer.putShort((short) beamIndexStart);
         byteBuffer.putShort(spare);
         byteBuffer.putInt(beamCount);
         ByteBufferUtils.writeFloatArray(byteBuffer, data);
      }

      @Override
      public String toString() {
         return "BeamData{" +
               "byteSize=" + byteSize +
               ", id='" + id + '\'' +
               ", ntDate=" + NTDate.ntDateToInstant(ntDate) +
               ", pingNumber=" + pingNumber +
               ", datagramNumber=" + datagramNumber +
               ", sampleIndexStart=" + sampleIndexStart +
               ", sampleCount=" + sampleCount +
               ", heave=" + heave +
               ", roll=" + roll +
               ", pitch=" + pitch +
               ", yaw=" + yaw +
               ", beamIndexStart=" + beamIndexStart +
               ", spare=" + spare +
               ", beamCount=" + beamCount +
               '}';
      }
   }
}
