package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.data.DataFileTableModel;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.time.NTDate;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

public final class ExtendedSurveyLine {
   private final LSSS lsss;
   private PingRange totalPingRange = PingRange.EMPTY_RANGE;
   private List<PingIndex> extendedPingIndices = List.of();
   private final ChangeManager changeManager = new ChangeManager();

   ExtendedSurveyLine(LSSS lsss) {
      this.lsss = lsss;
   }

   public PingRange getTotalPingRange() {
      return totalPingRange;
   }

   public List<PingIndex> getExtendedPingIndices() {
      return extendedPingIndices;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public void reset() {
      setExtendedPingIndices(List.of());
   }

   public PingIndex getClosestPingIndex(double value, PingMapping pingMapping) {
      DataFileSet dataFileSet = lsss.getDataManager().getDataFileSet();
      if (value < pingMapping.valueOf(dataFileSet.getTotalRange().begin()) && !extendedPingIndices.isEmpty()) {
         return DataUtils.getClosestPingIndex(extendedPingIndices, value, pingMapping);
      }
      return dataFileSet.getClosestPingIndex(value, pingMapping);
   }

   public @Nullable Point2D getTangent(PingIndex pingIndex) {
      DataFileSet dataFileSet = lsss.getDataManager().getDataFileSet();
      if (pingIndex.getPingNumber() < dataFileSet.getTotalRange().begin().getPingNumber() && !extendedPingIndices.isEmpty()) {
         int i = (int) (pingIndex.getPingNumber() - extendedPingIndices.getFirst().getPingNumber());
         int iBegin = Math.max(i - 10, 0);
         int iEnd = Math.min(i + 10, extendedPingIndices.size());
         return DataUtils.getTangent(extendedPingIndices.subList(iBegin, iEnd), null);
      }
      return DataUtils.getTangent(dataFileSet, null, pingIndex);
   }

   private void setExtendedPingIndices(List<PingIndex> extendedPingIndices) {
      this.extendedPingIndices = extendedPingIndices;
      PingRange dataFileSetPingRange = lsss.getDataManager().getDataFileSet().getTotalRange();
      if (extendedPingIndices.isEmpty()) {
         totalPingRange = dataFileSetPingRange;
      } else {
         totalPingRange = PingRange.of(extendedPingIndices.getFirst(), dataFileSetPingRange.end());
      }
      changeManager.notifyListeners();
   }

   public void addHours(float hours, ProgressHandler progressHandler, AsyncHandle asyncHandle) {
      PingIndex currentlyOldest;
      if (extendedPingIndices.isEmpty()) {
         currentlyOldest = lsss.getDataManager().getDataFileSet().getTotalRange().begin();
      } else {
         currentlyOldest = extendedPingIndices.getFirst();
      }
      long targetEnd = currentlyOldest.getNTDate();
      long targetBegin = (long) (targetEnd - (hours * 3600 * NTDate.UNITS_PER_SECOND));

      List<DataFileTableModel.FileRow> rows = lsss.getConfigurationManager().getDataConf().getFileRows().stream()
            .filter(row -> {
               SegmentInfo segmentInfo = row.getSegmentInfo();
               if (segmentInfo == null) {
                  return false;
               }
               PingRange pingRange = segmentInfo.pingRange();
               return !pingRange.isEmpty()
                     && pingRange.begin().getNTDate() < targetEnd
                     && pingRange.end().getNTDate() > targetBegin;
            })
            .sorted(Comparator.comparing(DataFileTableModel.FileRow::getInstant).reversed()) // Newest first.
            .toList();
      Listener progressListener = progressHandler.asCountingListener(rows.size());

      List<List<? extends PingIndex>> pingIndexLists = new ArrayList<>(); // Newest first.
      pingIndexLists.add(extendedPingIndices);
      for (DataFileTableModel.FileRow row : rows) {
         progressListener.listen();
         try {
            List<? extends PingIndex> rowPingIndices = row.getRawSegmentHandle().loadPingIndexes(NoticeHandler.ignore(), asyncHandle);
            if (asyncHandle.isCancelled()) {
               break;
            }
            pingIndexLists.add(rowPingIndices);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error loading ping indices for " + row.getRawSegmentHandle(), e);
         }
      }

      Collections.reverse(pingIndexLists); // Oldest first.

      List<PingIndex> pingIndices = pingIndexLists.stream()
            .<PingIndex>flatMap(List::stream)
            .toList();

      long pingNumber = lsss.getDataManager().getDataFileSet().getTotalRange().begin().getPingNumber() - 1;
      for (int i = pingIndices.size() - 1; i >= 0; i--, pingNumber--) {
         PingIndex pingIndex = pingIndices.get(i);
         pingIndex.setPingNumber(pingNumber);
      }

      setExtendedPingIndices(pingIndices);
   }
}
