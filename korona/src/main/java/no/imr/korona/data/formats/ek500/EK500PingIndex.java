package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.ping.DefaultPingIndex;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

public final class EK500PingIndex extends DefaultPingIndex {
   private final @Nullable IndexRecord[] indexRecords;

   EK500PingIndex(IndexRecord indexRecord, int channelCount) {
      super(indexRecord.getInstant(), -1, indexRecord.distance, new GeoPoint(indexRecord.longitude, indexRecord.latitude));

      indexRecords = new IndexRecord[channelCount];
   }

   public @Nullable IndexRecord[] getIndexRecords() {
      return indexRecords;
   }
}
