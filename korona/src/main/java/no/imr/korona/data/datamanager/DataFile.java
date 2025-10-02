package no.imr.korona.data.datamanager;

import no.imr.korona.data.DataException;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.ExtrapolatedPingIndex;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.ReloadablePing;
import no.imr.korona.data.ping.ReloadablePingSource;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.Pair;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;

/**
 * A data file.
 */
public final class DataFile implements ReloadablePingSource, Comparable<DataFile> {
   private final SegmentHandle segmentHandle;
   private final SegmentData segmentData;
   private final DataFileSet dataFileSet;
   private final PingRange pingRange;
   private final List<ReloadablePing> reloadablePings;
   private final PingIndex extrapolatedPingIndex;
   private final float[] coordinatedBottom;
   private final List<String> notices = new ArrayList<>();

   DataFile(DataFileSet dataFileSet, SegmentHandle segmentHandle, FileOpenRequest fileOpenRequest) throws IOException {
      this.dataFileSet = dataFileSet;
      this.segmentHandle = segmentHandle;
      SegmentData segmentData = segmentHandle.createSegmentData(this::addNotice, fileOpenRequest.getAsyncHandle());
      try {
         segmentData = fileOpenRequest.getOnTheFlyProcessing().process(segmentData, segmentHandle);
         this.segmentData = segmentData;
         getRawFileConfiguration().getNotices().forEach(this::addNotice);
         if (getRawFileConfiguration().getTransducerCount() == 0) {
            throw new DataException("No channels");
         }
         if (getPingIndices().isEmpty()) {
            throw new DataException("No pings");
         }

         checkDataIntegrity();

         extrapolatedPingIndex = ExtrapolatedPingIndex.create(getPingIndices());
         pingRange = PingRange.of(getPingIndices().getFirst(), extrapolatedPingIndex);

         reloadablePings = getPingIndices().stream()
               .map(pingIndex -> new ReloadablePing(this, pingIndex))
               .toList();

         coordinatedBottom = computeCoordinatedBottom(getDataConfiguration(), getRawFileConfiguration(), getBot0Datagrams());
      } catch (Exception e) {
         try {
            segmentData.close();
         } catch (IOException suppressed) {
            e.addSuppressed(suppressed);
         }
         throw e;
      }
   }

   private static float[] computeCoordinatedBottom(DataConfiguration dataConfiguration, RawFileConfiguration rawFileConfiguration,
                                                   List<Bot0Datagram> bot0Datagrams) {
      List<Integer> channels = dataConfiguration.getChannelsForBottom(rawFileConfiguration);
      int preferredChannel = dataConfiguration.getPreferredChannelForBottom(rawFileConfiguration);
      float minimumDepthThresholdFactor = dataConfiguration.getMinimumDepthThresholdFactor();
      float[] coordinatedBottom = new float[bot0Datagrams.size()];
      for (int i = 0; i < coordinatedBottom.length; i++) {
         Bot0Datagram bot0Datagram = bot0Datagrams.get(i);
         double[] channelDepths = bot0Datagram.getChannelDepths();
         double maxDepth = Double.NEGATIVE_INFINITY;
         for (int channel : channels) {
            maxDepth = Math.max(maxDepth, channelDepths[channel - 1]);
         }
         double minMinDepth = minimumDepthThresholdFactor * maxDepth; // The shallowest acceptable depth.
         if (preferredChannel > 0 && channelDepths[preferredChannel - 1] >= minMinDepth) {
            coordinatedBottom[i] = (float) channelDepths[preferredChannel - 1];
         } else {
            double minDepth = maxDepth;
            for (int channel : channels) {
               double depth = channelDepths[channel - 1];
               if (depth >= minMinDepth) {
                  minDepth = Math.min(minDepth, depth);
               }
            }
            coordinatedBottom[i] = Utils.avoidInfinity((float) minDepth);
         }
      }
      return coordinatedBottom;
   }

   private void checkDataIntegrity() throws DataException {
      int pingNumberErrorCounter = 0;
      int vesselDistanceErrorCounter = 0;
      List<? extends PingIndex> pingIndices = getPingIndices();
      for (int i = 1; i < pingIndices.size(); i++) {
         PingIndex previousPingIndex = pingIndices.get(i - 1);
         PingIndex pingIndex = pingIndices.get(i);

         if (pingIndex.getNTDate() <= previousPingIndex.getNTDate()) {
            throw new DataException("Non-increasing ping time: " + previousPingIndex.getInstant() +
                  " is followed by " + pingIndex.getInstant());
         }

         if (pingIndex.getPingNumber() != previousPingIndex.getPingNumber() + 1) {
            pingIndex.setPingNumber(previousPingIndex.getPingNumber() + 1);
            pingNumberErrorCounter++;
         }
         if (Math.abs(KoronaUtils.getKnots(previousPingIndex, pingIndex)) > KoronaUtils.MAX_KNOTS) {
            vesselDistanceErrorCounter++;
         }
         if (pingIndex.getVesselDistance() < previousPingIndex.getVesselDistance()) {
            pingIndex.setVesselDistance(previousPingIndex.getVesselDistance());
         }
      }

      if (pingNumberErrorCounter > 0) {
         addNotice("Corrected " + pingNumberErrorCounter + " ping numbering errors");
      }
      if (vesselDistanceErrorCounter > 0) {
         addNotice("Vessel distance inconsistent (detected vessel speed more than 100 knots " + vesselDistanceErrorCounter + " times)");
      }
   }

   void shiftPingNumber(long pingNumberShift) {
      for (PingIndex pingIndex : getPingIndices()) {
         pingIndex.setPingNumber(pingIndex.getPingNumber() + pingNumberShift);
      }
      extrapolatedPingIndex.setPingNumber(extrapolatedPingIndex.getPingNumber() + pingNumberShift);
      addNotice("Ping numbering shifted with " + pingNumberShift);
   }

   void shiftVesselDistance(double vesselDistanceShift, boolean addNotice) {
      for (PingIndex pingIndex : getPingIndices()) {
         pingIndex.setVesselDistance(pingIndex.getVesselDistance() + vesselDistanceShift);
      }
      extrapolatedPingIndex.setVesselDistance(extrapolatedPingIndex.getVesselDistance() + vesselDistanceShift);
      if (addNotice) {
         addNotice("Vessel distance shifted with " + vesselDistanceShift + " nmi");
      }
   }

   private void addNotice(String notice) {
      notices.add(notice);
      Log.global.finer(segmentHandle.getDisplayName() + ": " + notice);
   }

   public List<String> getNotices() {
      return notices;
   }

   public SegmentHandle getSegmentHandle() {
      return segmentHandle;
   }

   public SegmentData getSegmentData() {
      return segmentData;
   }

   void close() {
      try {
         segmentData.close();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error closing file " + segmentHandle.getDisplayName(), e);
      }
   }

   public RawFileConfiguration getRawFileConfiguration() {
      return segmentData.getRawFileConfiguration();
   }

   public List<? extends PingIndex> getPingIndices() {
      return segmentData.getPingIndices();
   }

   public @Nullable WrapAround getWrapAround() {
      return segmentData.getWrapAround();
   }

   public List<Bot0Datagram> getBot0Datagrams() {
      return segmentData.getBot0Datagrams();
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException {
      return segmentData.loadPingData(pingIndex, asyncHandle);
   }

   @Override
   public boolean isDataLoadingCancelled() {
      return getDataConfiguration().isDataLoadingCancelled();
   }

   @Override
   public void onLoadPingData(Ping ping, PingData pingData) {
      dataFileSet.pingLoaded(new Pair<>(ping, pingData));
   }

   /**
    * Returns the PingRange defined by this DataFile.
    * The upper limit for the range, which is not included, is at a location found
    * by extrapolating linearly one beyond the last ping.
    *
    * @return the PingRange defined by this DataFile
    */
   public PingRange getPingRange() {
      return pingRange;
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return segmentData.getPingConfiguration();
   }

   double computeMaxDepth(double currentMaxDepth) {
      int indexForMaxBottomDepth = -1;
      double maxBottomDepth = 0;
      List<Bot0Datagram> bot0Datagrams = getBot0Datagrams();
      List<Integer> channels = getDataConfiguration().getChannelsForBottom(getRawFileConfiguration());

      for (int i = 0; i < bot0Datagrams.size(); i++) {
         Bot0Datagram bot0Datagram = bot0Datagrams.get(i);
         for (int channel : channels) {
            double depth = bot0Datagram.getChannelDepths()[channel - 1];
            if (depth > maxBottomDepth) {
               maxBottomDepth = depth;
               indexForMaxBottomDepth = i;
            }
         }
      }

      if (maxBottomDepth > 0) {
         // Has bottom.
         if (maxBottomDepth <= currentMaxDepth) {
            // No need to check data depth.
            return 0;
         }
         // Check if bottom is below data depth.
         float dataDepth = getDataDepth(getPingIndices().get(indexForMaxBottomDepth));
         return Math.min(maxBottomDepth, dataDepth);
      } else {
         // No bottom.
         List<? extends PingIndex> pingIndices = getPingIndices();

         // Always check the last ping in each file:
         float dataDepth = getDataDepth(pingIndices.getLast());

         // Also check the first ping in the first data file:
         if (this == dataFileSet.getDataFiles().getFirst()) {
            dataDepth = Math.max(dataDepth, getDataDepth(pingIndices.getFirst()));
         }

         // Check at least a minimum number of pings in total:
         int n = 50 / dataFileSet.getDataFiles().size();
         for (int i = 0; i < n; i++) {
            long j = (long) pingIndices.size() * (i + 1) / (n + 1);
            dataDepth = Math.max(dataDepth, getDataDepth(pingIndices.get((int) j)));
         }

         if (dataDepth == 0) {
            // Try middle ping.
            dataDepth = getDataDepth(pingIndices.get(pingIndices.size() / 2));
         }
         return dataDepth;
      }
   }

   private float getDataDepth(PingIndex pingIndex) {
      return getPing(pingIndex).getPingData().getDataDepth();
   }

   private DataConfiguration getDataConfiguration() {
      return dataFileSet.getDataConfiguration();
   }

   private int pingNumberToIndex(long pingNumber) {
      return (int) (pingNumber - pingRange.begin().getPingNumber());
   }

   public PingIndex pingNumberToPingIndex(long pingNumber) {
      int i = pingNumberToIndex(pingNumber);
      return getPingIndices().get(i);
   }

   public float getCoordinatedDepth(PingIndex pingIndex) {
      int i = pingNumberToIndex(pingIndex.getPingNumber());
      return coordinatedBottom[i];
   }

   @Override
   public Bot0Datagram getBot0Datagram(PingIndex pingIndex) {
      int i = pingNumberToIndex(pingIndex.getPingNumber());
      return getBot0Datagrams().get(i);
   }

   public PingIndex getClosestPingIndex(double value, PingMapping pingMapping) {
      return DataUtils.getClosestPingIndex(getPingIndices(), value, pingMapping);
   }

   public @Nullable PingIndex getContainingPingIndex(double value, PingMapping pingMapping) {
      return DataUtils.getContainingPingIndex(getPingIndices(), pingRange.end(), value, pingMapping);
   }

   public Ping getPing(PingIndex pingIndex) {
      return getReloadablePing(pingIndex);
   }

   public Ping getLastPing() {
      return getPing(getPingIndices().getLast());
   }

   private ReloadablePing getReloadablePing(PingIndex pingIndex) {
      int i = pingNumberToIndex(pingIndex.getPingNumber());
      return reloadablePings.get(i);
   }

   public void getAvailablePings(PingRange pingRange, BiConsumer<Ping, PingData> pingHandler) {
      int begin = Math.max(pingNumberToIndex(pingRange.begin().getPingNumber()), 0);
      int end = Math.min(pingNumberToIndex(pingRange.end().getPingNumber()), reloadablePings.size());
      for (int i = begin; i < end; i++) {
         ReloadablePing ping = reloadablePings.get(i);
         PingData pingData = ping.getAvailablePingData();
         if (pingData != null) {
            pingHandler.accept(ping, pingData);
         }
      }
   }

   void discardLoadedData() {
      reloadablePings.forEach(ReloadablePing::clear);
   }

   @Override
   public int compareTo(DataFile dataFile) {
      return pingRange.begin().compareTo(dataFile.pingRange.begin());
   }

   @Override
   public String toString() {
      return segmentHandle.getDisplayName();
   }
}
