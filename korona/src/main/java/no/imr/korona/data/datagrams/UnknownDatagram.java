package no.imr.korona.data.datagrams;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.nio.ByteBuffer;

/**
 * Class for unknown datagrams.
 * All datagrams not recognized by the system are represented by this datagram.
 */
public final class UnknownDatagram extends DatagramPingItem {
   private static final LoadingCache<Integer, DatagramType> TYPE_CACHE = CacheBuilder.newBuilder()
         .weakKeys()
         .build(CacheLoader.from(intCode -> {
            return DatagramType.simple(intCode, (ntDate, byteBuffer) -> {
               return new UnknownDatagram(ntDate, type(intCode), byteBuffer);
            });
         }));

   private final DatagramType datagramType;
   private final byte[] contents;

   public UnknownDatagram(long ntDate, DatagramType datagramType, ByteBuffer byteBuffer) {
      super(ntDate);

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
