package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.math.linalg.Vec3;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Physical configuration.
 */
public final class Phy0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("PHY0", Phy0Datagram::new);

   public final List<Platform> platforms;

   public Phy0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      platforms = ByteBufferUtils.readCountAndList(byteBuffer, 8, Phy0Datagram::readPlatform);
   }

   private static Platform readPlatform(ByteBuffer byteBuffer) throws DatagramFormatException {
      int size = ByteBufferUtils.readByteSize(byteBuffer);
      int type = byteBuffer.getInt();
      return switch (type) {
         case OwnShipPlatform.TYPE -> new OwnShipPlatform(byteBuffer);
         case SensorPlatform.TYPE -> new SensorPlatform(byteBuffer);
         default -> new UnknownPlatform(size, type, byteBuffer);
      };
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCountAndList(byteBuffer, platforms, Platform::write);
   }

   @Override
   public String toStringExtra() {
      return "platforms: " + platforms;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   public interface Platform {
      void write(ByteBuffer byteBuffer);
   }

   public static final class UnknownPlatform implements Platform {
      private final int size;
      private final int type;
      private final byte[] content;

      public UnknownPlatform(int size, int type, ByteBuffer byteBuffer) {
         this.size = size;
         this.type = type;
         content = new byte[size - 8]; // Subtract size and type
         byteBuffer.get(content);
      }

      @Override
      public void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(size);
         byteBuffer.putInt(type);
         byteBuffer.put(content);
      }

      @Override
      public String toString() {
         return "Unknown[" + size + ", " + type + ']';
      }
   }

   public static final class OwnShipPlatform implements Platform {
      public static final int TYPE = 0;

      public final Vec3 dimension; // x = Length, y = Width, z = Height
      public final Vec3 originOffsetFromCenter;

      public OwnShipPlatform(ByteBuffer byteBuffer) {
         dimension = ByteBufferUtils.readVec3(byteBuffer);
         originOffsetFromCenter = ByteBufferUtils.readVec3(byteBuffer);
      }

      @Override
      public void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(32);
         byteBuffer.putInt(TYPE);
         ByteBufferUtils.writeVec3(byteBuffer, dimension);
         ByteBufferUtils.writeVec3(byteBuffer, originOffsetFromCenter);
      }

      @Override
      public String toString() {
         return "OwnShipPlatform{" +
               "dimension=" + dimension +
               ", originOffsetFromCenter=" + originOffsetFromCenter +
               '}';
      }
   }

   public static final class SensorPlatform implements Platform {
      public static final int TYPE = 1;

      public final String name;
      public final String parentPlatform;
      public final Vec3 offset;
      public final Vec3 rotation;

      public SensorPlatform(ByteBuffer byteBuffer) {
         name = ByteBufferUtils.readCString(byteBuffer, 32);
         parentPlatform = ByteBufferUtils.readCString(byteBuffer, 32);
         offset = ByteBufferUtils.readVec3(byteBuffer);
         rotation = ByteBufferUtils.readVec3(byteBuffer);
      }

      @Override
      public void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(96);
         byteBuffer.putInt(TYPE);
         ByteBufferUtils.writeCString(byteBuffer, name, 32);
         ByteBufferUtils.writeCString(byteBuffer, parentPlatform, 32);
         ByteBufferUtils.writeVec3(byteBuffer, offset);
         ByteBufferUtils.writeVec3(byteBuffer, rotation);
      }

      @Override
      public String toString() {
         return "SensorPlatform{" +
               "name='" + name + '\'' +
               ", parentPlatform='" + parentPlatform + '\'' +
               ", offset=" + offset +
               ", rotation=" + rotation +
               '}';
      }
   }
}
