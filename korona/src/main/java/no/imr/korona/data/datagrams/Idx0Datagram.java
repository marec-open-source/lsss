package no.imr.korona.data.datagrams;

import no.imr.korona.data.ping.PingIndex;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Index datagram.
 */
public final class Idx0Datagram extends DatagramPingItem implements PingIndex {
   public static final DatagramType TYPE = new DatagramType.Simple("IDX0", Idx0Datagram::new);

   private long pingNumber;        // NB: ping number is stored as unsigned int on file
   private double vesselDistance;  // [Nm]
   private @Nullable GeoPoint geographicalPosition;
   private long fileOffset;

   private static final int SIZE_ON_FILE = ENVELOPE_AND_HEADER_SIZE + (4 + 8 + 8 + 8 + 8); // NB: ping number is stored as unsigned int on file

   /**
    * Returns the size in bytes of an Idx0Datagram on file.
    *
    * @return the size in bytes of an Idx0Datagram on file
    */
   public static int getSize() {
      return SIZE_ON_FILE;
   }

   private Idx0Datagram(long ntDate) {
      super(ntDate);
   }

   public Idx0Datagram(long ntDate, long pingNumber, double vesselDistance, @Nullable GeoPoint geographicalPosition, long fileOffset) {
      this(ntDate);

      this.pingNumber = pingNumber;
      this.vesselDistance = vesselDistance;
      this.geographicalPosition = geographicalPosition;
      this.fileOffset = fileOffset;
   }

   public Idx0Datagram(PingIndex pingIndex, long fileOffset) {
      this(pingIndex.getNTDate(), pingIndex.getPingNumber(), pingIndex.getVesselDistance(),
            pingIndex.getGeographicalPosition(), fileOffset);
   }

   public Idx0Datagram(Idx0Datagram idx0Datagram) {
      this(idx0Datagram, idx0Datagram.getFileOffset());
   }

   public Idx0Datagram(long ntDate, ByteBuffer byteBuffer) {
      this(ntDate);

      pingNumber = 0xffffffffL & byteBuffer.getInt(); // NB: ping number is stored as unsigned int on file
      vesselDistance = byteBuffer.getDouble();
      double latitude = byteBuffer.getDouble();  // 0.0 = no value
      double longitude = byteBuffer.getDouble(); // 0.0 = no value
      if (latitude != 0 && longitude != 0) {
         geographicalPosition = new GeoPoint(longitude, latitude);
      }
      fileOffset = byteBuffer.getLong();
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt((int) pingNumber); // NB: ping number is stored as unsigned int on file
      byteBuffer.putDouble(vesselDistance);
      byteBuffer.putDouble(geographicalPosition != null ? geographicalPosition.getY() : 0);
      byteBuffer.putDouble(geographicalPosition != null ? geographicalPosition.getX() : 0);
      byteBuffer.putLong(fileOffset);
   }

   public void copyFrom(Idx0Datagram idx0Datagram) {
      setNTDate(idx0Datagram.getNTDate());
      pingNumber = idx0Datagram.pingNumber;
      vesselDistance = idx0Datagram.vesselDistance;
      geographicalPosition = idx0Datagram.geographicalPosition;
      fileOffset = idx0Datagram.fileOffset;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public long getPingNumber() {
      return pingNumber;
   }

   @Override
   public void setPingNumber(long pingNumber) {
      this.pingNumber = pingNumber;
   }

   @Override
   public double getVesselDistance() {
      return vesselDistance;
   }

   @Override
   public void setVesselDistance(double vesselDistance) {
      this.vesselDistance = vesselDistance;
   }

   @Override
   public @Nullable GeoPoint getGeographicalPosition() {
      return geographicalPosition;
   }

   @Override
   public void setGeographicalPosition(@Nullable GeoPoint geographicalPosition) {
      this.geographicalPosition = geographicalPosition;
   }

   @Override
   public long getTimeInMillis() {
      // Needed since inheriting default implementations from two interfaces
      return super.getTimeInMillis();
   }

   @Override
   public Instant getInstant() {
      // Needed since inheriting default implementations from two interfaces
      return super.getInstant();
   }

   /**
    * Returns the file offset in the corresponding raw file.
    *
    * @return the raw file offset
    */
   public long getFileOffset() {
      return fileOffset;
   }

   /**
    * Tests for equality using ping number only.
    *
    * @param obj another PingIndex
    * @return {@code true} if the ping numbers are equal
    */
   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PingIndex that
            && pingNumber == that.getPingNumber();
   }

   @Override
   public int hashCode() {
      return Long.hashCode(pingNumber);
   }

   @Override
   public String toStringExtra() {
      return "pingNumber: " + pingNumber
            + ", vesselDistance: " + vesselDistance
            + ", longitude: " + (geographicalPosition != null ? geographicalPosition.getLongitude() : 0)
            + ", latitude: " + (geographicalPosition != null ? geographicalPosition.getLatitude() : 0)
            + ", fileOffset: " + fileOffset;
   }
}
