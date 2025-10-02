package no.imr.lsss.framework.extensions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingMapping;
import no.imr.tools.time.NTDate;
import no.marec.lsss.api.data.Ping;
import no.marec.lsss.api.data.PingConfiguration;
import no.marec.lsss.api.data.PingDataset;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.data.PingRange;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

final class PingDatasetImpl implements PingDataset {
   private final DataFileSet dataFileSet;

   PingDatasetImpl(DataFileSet dataFileSet) {
      this.dataFileSet = dataFileSet;
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return dataFileSet.getRawFileConfiguration();
   }

   @Override
   public PingRange getTotalPingRange() {
      return dataFileSet.getTotalRange();
   }

   @Override
   public @Nullable PingIndex getPingIndex(long pingNumber) {
      return dataFileSet.getPingIndexOrNull(pingNumber);
   }

   @Override
   public @Nullable PingIndex getContainingPingIndex(Instant instant) {
      return dataFileSet.getContainingPingIndex(PingMapping.ntDateToTimeValue(NTDate.instantToNTDate(instant)), PingMapping.TIME);
   }

   @Override
   public PingIndex getClosestPingIndex(Instant instant) {
      return dataFileSet.getClosestPingIndex(PingMapping.ntDateToTimeValue(NTDate.instantToNTDate(instant)), PingMapping.TIME);
   }

   @Override
   public Ping getPing(PingIndex pingIndex) {
      return dataFileSet.getPing(toLsssPingIndex(pingIndex));
   }

   private no.imr.korona.data.ping.PingIndex toLsssPingIndex(PingIndex pingIndex) {
      return pingIndex instanceof no.imr.korona.data.ping.PingIndex lsssPingIndex
            ? lsssPingIndex
            : dataFileSet.getPingIndex(pingIndex.getPingNumber());
   }

   @Override
   public float physicalDepthToDepth(float physicalDepth) {
      return dataFileSet.getDataConfiguration().physicalDepthToDepth(physicalDepth);
   }
}
