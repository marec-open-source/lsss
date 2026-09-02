package no.imr.korona.data.datagrams;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.nio.ByteBuffer;
import java.time.Instant;

/**
 * Class for unknown datagrams.
 * All datagrams not recognized by the system are represented by this datagram.
 */
public final class UnknownDatagram extends DatagramPingItem {
   private static final LoadingCache<Integer, DatagramType> TYPE_CACHE = CacheBuilder.newBuilder()
         .weakValues()
         .build(CacheLoader.from(intCode -> {
            return DatagramType.simple(intCode, (instant, byteBuffer) -> {
               return new UnknownDatagram(instant, type(intCode), byteBuffer);
            });
         }));

   private final DatagramType datagramType;
   private final byte[] contents;

   public UnknownDatagram(Instant instant, DatagramType datagramType, ByteBuffer byteBuffer) {
      super(instant);

      this.datagramType = datagramType;
      contents = new byte[byteBuffer.remaining()];
      byteBuffer.get(contents);
   }

   public static DatagramType type(int intCode) {
      return TYPE_CACHE.getUnchecked(intCode);
   }

   @Override
   public String toStringExtra() {
      return "Unknown datagram: " + contents.length + " bytes";
   }

   @Override
   public DatagramType getDatagramType() {
      return datagramType;
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.put(contents);
   }
}
