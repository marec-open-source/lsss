package no.imr.lsss.framework.config.survey.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import no.imr.korona.data.DataException;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.formats.ek60.EK60FileSet;
import no.imr.korona.data.formats.ek60.EK60SegmentHandle;
import no.imr.korona.data.formats.ek60.MissingIdxFileException;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.track.SegmentInfoCache;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.modules.echogram.overlays.AccumulatedSaOverlay;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.LastModifiedAndSize;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Data model for the file table.
 */
public final class DataFileTableModel extends AbstractTableModel {
   private static final Interner<ImmutableMap<Integer, String>> CHANNEL_TO_DATA_TYPE_NAME = Interners.newWeakInterner();

   abstract static sealed class BaseRow {
      private BaseRow() {
      }

      abstract @Nullable Object getValueAt(int columnIndex);
   }

   final class TimeRow extends BaseRow {
      private final TimeGroup timeGroup;
      private boolean expanded;
      private List<FileRow> fileRows = new ArrayList<>();

      private TimeRow(TimeGroup timeGroup, boolean expanded) {
         this.timeGroup = timeGroup;
         this.expanded = expanded;
      }

      TimeGroup getTimeGroup() {
         return timeGroup;
      }

      boolean isUnknownTime() {
         return timeGroup.instant().equals(Instant.MAX);
      }

      boolean isExpanded() {
         return expanded;
      }

      void setExpanded(boolean expanded) {
         this.expanded = expanded;
         fireTableDataChanged();
      }

      List<FileRow> getFileRows() {
         return fileRows;
      }

      @Override
      @Nullable Object getValueAt(int columnIndex) {
         return switch (columnIndex) {
            case KORONA_COLUMN -> null;
            case STATUS_COLUMN -> this;
            case FILE_NAME_COLUMN -> this;
            case PINGS_COLUMN -> getPingRangeStream().mapToLong(PingRange::getPingCount).sum();
            case DURATION_COLUMN -> Utils.getDurationString(getPingRangeStream().mapToLong(PingRange::getMilliseconds).sum());
            case DISTANCE_COLUMN -> Utils.format("%.1f", getPingRangeStream().mapToDouble(PingRange::getVesselDistance).sum());
            case SA_COLUMN -> {
               double sa = fileRows.stream()
                     .filter(r -> r.getSegmentInfo() != null)
                     .mapToDouble(FileRow::getSa)
                     .sum();
               yield AccumulatedSaOverlay.toMinimalString((float) sa);
            }
            default -> throw new IllegalArgumentException(Integer.toString(columnIndex));
         };
      }

      private Stream<PingRange> getPingRangeStream() {
         return fileRows.stream()
               .map(FileRow::getSegmentInfo)
               .filter(Objects::nonNull)
               .map(SegmentInfo::pingRange);
      }
   }

   public final class FileRow extends BaseRow {
      private final SegmentHandle rawSegmentHandle;
      private final long rawLastModified;
      private final long rawTotalFileSize;
      private @Nullable ImmutableMap<Integer, String> rawDataTypeNames;
      private @Nullable DataFile rawDataFile;
      private @Nullable String rawDataExceptionMessage;
      private @Nullable String rawIncompatibilityWithSelection;

      private @Nullable SegmentInfo segmentInfo;
      private float sa = -1;

      private @Nullable SegmentHandle koronaSegmentHandle;
      private long koronaLastModified;
      private long koronaTotalFileSize;
      private @Nullable ImmutableMap<Integer, String> koronaDataTypeNames;
      private @Nullable DataFile koronaDataFile;
      private @Nullable String koronaDataExceptionMessage;
      private @Nullable String koronaIncompatibilityWithSelection;
      private ImmutableList<String> koronaConflictingFiles = ImmutableList.of();

      private FileRow(SegmentHandle segmentHandle, LastModifiedAndSize lastModifiedAndSize) {
         rawSegmentHandle = segmentHandle;
         rawLastModified = lastModifiedAndSize.lastModified();
         rawTotalFileSize = lastModifiedAndSize.size();
      }

      @Override
      public String toString() {
         return rawSegmentHandle.toString();
      }

      public SegmentHandle getRawSegmentHandle() {
         return rawSegmentHandle;
      }

      public @Nullable SegmentHandle getSegmentHandle(DataType dataType) {
         return dataType == DataType.RAW ? rawSegmentHandle : koronaSegmentHandle;
      }

      long getLastModified(DataType dataType) {
         return dataType == DataType.RAW ? rawLastModified : koronaLastModified;
      }

      long getTotalFileSize(DataType dataType) {
         return dataType == DataType.RAW ? rawTotalFileSize : koronaTotalFileSize;
      }

      ImmutableMap<Integer, String> getDataTypes(DataType dataType) {
         ImmutableMap<Integer, String> dataTypes = dataType == DataType.RAW ? rawDataTypeNames : koronaDataTypeNames;
         if (dataTypes != null) {
            return dataTypes;
         }
         SegmentHandle segmentHandle = getSegmentHandle(dataType);
         if (segmentHandle == null) {
            return ImmutableMap.of();
         }
         dataTypes = CHANNEL_TO_DATA_TYPE_NAME.intern(readDataTypes(segmentHandle));
         if (dataType == DataType.RAW) {
            rawDataTypeNames = dataTypes;
         } else {
            koronaDataTypeNames = dataTypes;
         }
         return dataTypes;
      }

      private static ImmutableMap<Integer, String> readDataTypes(SegmentHandle segmentHandle) {
         try (PingReader pingReader = segmentHandle.createPingReader()) {
            Ping ping = pingReader.nextPing(new AsyncHandle());
            if (ping != null) {
               return ping.getNonNullChannelDatas()
                     .collect(ImmutableMap.toImmutableMap(ChannelData::getChannel, ChannelData::getDataTypeName));
            } else {
               return ImmutableMap.of();
            }
         } catch (UnsupportedOperationException | IOException _) {
            return ImmutableMap.of();
         }
      }

      public @Nullable SegmentInfo getSegmentInfo() {
         return segmentInfo;
      }

      public Instant getInstant() {
         return segmentInfo != null && !segmentInfo.pingRange().isEmpty()
               ? segmentInfo.pingRange().begin().getInstant()
               : Instant.MAX;
      }

      @Nullable
      String getDataExceptionMessage(DataType dataType) {
         return dataType == DataType.RAW ? rawDataExceptionMessage : koronaDataExceptionMessage;
      }

      @Nullable
      String getIncompatibilityWithSelection(DataType dataType) {
         return dataType == DataType.RAW ? rawIncompatibilityWithSelection : koronaIncompatibilityWithSelection;
      }

      ImmutableList<String> getKoronaConflictingFiles() {
         return koronaConflictingFiles;
      }

      @Nullable
      DataFile getDataFile(DataType dataType) {
         return dataType == DataType.RAW ? rawDataFile : koronaDataFile;
      }

      private void initSegmentInfo(@Nullable SegmentInfo cachedSegmentInfo) {
         if (cachedSegmentInfo != null) {
            segmentInfo = cachedSegmentInfo;
            return;
         }
         try {
            segmentInfo = rawSegmentHandle.createSegmentInfo();
         } catch (MissingIdxFileException _) {
            dataConf.foundMissingIdx();
         } catch (DataException _) {
            // Ignore at this point. Will be registered later if in selection.
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Could not read " + rawSegmentHandle, e);
         }
      }

      private void setDataFile(DataType dataType, @Nullable DataFile dataFile) {
         if (dataType == DataType.RAW) {
            rawDataFile = dataFile;
         } else {
            koronaDataFile = dataFile;
         }
         checkCompatibility();
      }

      private void checkCompatibility() {
         if (rawDataFile == null || koronaDataFile == null) {
            return;
         }

         if (!koronaDataFile.getPingRange().equals(rawDataFile.getPingRange())) {
            koronaDataExceptionMessage = "Wrong ping range: " + koronaDataFile.getPingRange();
         } else {
            String incompatibility = rawDataFile.getPingConfiguration().getIncompatibility(koronaDataFile.getPingConfiguration());
            if (incompatibility != null) {
               koronaIncompatibilityWithSelection = incompatibility;
            }
         }
      }

      private void setDataExceptionMessage(DataType dataType, String dataExceptionMessage) {
         if (dataType == DataType.RAW) {
            rawDataExceptionMessage = dataExceptionMessage;
         } else {
            koronaDataExceptionMessage = dataExceptionMessage;
         }
      }

      private void setIncompatibilityWithSelection(DataType dataType, String incompatibilityWithSelection) {
         if (dataType == DataType.RAW) {
            rawIncompatibilityWithSelection = incompatibilityWithSelection;
         } else {
            koronaIncompatibilityWithSelection = incompatibilityWithSelection;
         }
      }

      private void updateSa() {
         sa = -1;
      }

      float getSa() {
         if (segmentInfo == null) {
            return 0;
         }
         if (sa < 0) {
            LSSS lsss = dataConf.getLSSS();
            float frequency = lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency.getFloatValue();
            sa = lsss.getInterpretationSummary().getInterpretedSa(segmentInfo.pingRange(), frequency);
         }
         return sa;
      }

      /**
       * Returns a distance string for this data file.
       * This format, e.g., "1234.5 (6.7)", is also used by InterpretationModule.
       *
       * @return a distance string
       */
      private String getDistanceString() {
         if (segmentInfo == null) {
            return "";
         }
         String vesselDistance = Utils.format("%.1f", segmentInfo.pingRange().getVesselDistance());
         if (vesselDistance.length() > maxVesselDistanceStringLength) {
            maxVesselDistanceStringLength = vesselDistance.length();
            updateAllRows();
         }
         return Utils.format("%.1f", segmentInfo.pingRange().begin().getVesselDistance()) +
               "\u2007".repeat(maxVesselDistanceStringLength - vesselDistance.length()) + // Figure space
               " (" + vesselDistance + ')';
      }

      @Override
      Object getValueAt(int columnIndex) {
         if (columnIndex > FILE_NAME_COLUMN) {
            if (segmentInfo == null) {
               return "";
            }
            if (segmentInfo.pingRange().isEmpty()) {
               return columnIndex == PINGS_COLUMN ? "No pings" : "";
            }
         }

         return switch (columnIndex) {
            case KORONA_COLUMN -> this;
            case STATUS_COLUMN -> this;
            case FILE_NAME_COLUMN -> this;
            case PINGS_COLUMN -> segmentInfo.pingRange();
            case DURATION_COLUMN -> segmentInfo.pingRange().getDurationString();
            case DISTANCE_COLUMN -> getDistanceString();
            case SA_COLUMN -> AccumulatedSaOverlay.toMinimalString(getSa());
            default -> throw new IllegalArgumentException(Integer.toString(columnIndex));
         };
      }
   }

   public static final int KORONA_COLUMN = 0;
   public static final int STATUS_COLUMN = 1;
   public static final int FILE_NAME_COLUMN = 2;
   public static final int PINGS_COLUMN = 3;
   public static final int DURATION_COLUMN = 4;
   public static final int DISTANCE_COLUMN = 5;
   public static final int SA_COLUMN = 6;

   private static final List<String> COLUMN_NAMES = List.of(
         "", "", "File", "Pings", "Duration", "Dist [nmi]", "<html>∑ s<sub>A</sub>"
   );

   private final DataConf dataConf;
   private AsyncHandle dataFileInfoAsyncHandle = new AsyncHandle();
   private SegmentHandlesAndAttributes.DirListingStatus rawDirListingStatus = SegmentHandlesAndAttributes.DirListingStatus.NOT_SPECIFIED;
   private List<TimeRow> timeRows = List.of();
   private List<FileRow> fileRows = List.of();
   private int maxVesselDistanceStringLength;
   private @Nullable DataFile firstRawDataFile;
   private boolean firingTableDataChanged;

   DataFileTableModel(DataConf dataConf) {
      this.dataConf = dataConf;
   }

   void setup() {
      dataConf.getRawDir().subscribe(_ -> update());
      SurveyDirectoryParameter processedDir = dataConf.getProcessedDir();
      if (processedDir != null) {
         processedDir.subscribe(_ -> {
            refreshKoronaColumn();
            dataConf.updateSelectedProcessedRows();
         });
      }
      dataConf.sortByTime.subscribe(_ -> {
         fileRows = fileRows.stream()
               .sorted(currentFileRowComparator())
               .toList();
         fireTableDataChanged();
      });
      dataConf.timeGrouping.subscribe(_ -> update());
   }

   DataConf getDataConf() {
      return dataConf;
   }

   private Comparator<FileRow> currentFileRowComparator() {
      return dataConf.sortByTime.getBooleanValue() ? fileRowByTimeComparator() : fileRowByNameComparator();
   }

   private Comparator<FileRow> fileRowByNameComparator() {
      return Comparator.comparing(FileRow::getRawSegmentHandle);
   }

   private Comparator<FileRow> fileRowByTimeComparator() {
      return Comparator.comparing(FileRow::getInstant).thenComparing(fileRowByNameComparator());
   }

   void setAllExpanded(boolean expanded) {
      for (TimeRow timeRow : timeRows) {
         timeRow.expanded = expanded;
      }
      fireTableDataChanged();
   }

   List<FileRow> getFileRows() {
      return fileRows;
   }

   void update() {
      dataConf.cancelDataSetLoader();

      dataFileInfoAsyncHandle.cancel();
      dataFileInfoAsyncHandle.waitUntilFinished();
      dataFileInfoAsyncHandle = new AsyncHandle();

      maxVesselDistanceStringLength = 0;

      Map<TimeGroup, Boolean> timeGroupToExpanded = timeRows.stream().collect(Collectors.toMap(TimeRow::getTimeGroup, TimeRow::isExpanded));
      Function<TimeGroup, TimeRow> newTimeRow = timeGroup -> new TimeRow(timeGroup, timeGroupToExpanded.getOrDefault(timeGroup, false));

      SegmentHandlesAndAttributes segmentHandlesAndAttributes = createSegmentHandles(dataConf.getRawDir());
      rawDirListingStatus = segmentHandlesAndAttributes.status();
      fileRows = segmentHandlesAndAttributes.segmentHandles().stream()
            .map(segmentHandle -> new FileRow(segmentHandle, segmentHandle.getLastModifiedAndSize(segmentHandlesAndAttributes.attributes(), dataFileInfoAsyncHandle)))
            .toList();

      TimeRow unknownTimeRow = newTimeRow.apply(TimeGroup.MAX);
      unknownTimeRow.fileRows = fileRows;
      timeRows = List.of(unknownTimeRow);

      refreshKoronaColumn();

      SegmentInfoCache segmentInfoCache = dataConf.getLSSS().getSurveyManager().readSegmentInfoCache(dataConf.getRawDir().getFile(), segmentHandlesAndAttributes);

      Map<TimeGroup, TimeRow> timeGroupToTimeRow = new TreeMap<>();
      timeGroupToTimeRow.put(unknownTimeRow.getTimeGroup(), unknownTimeRow);
      Set<FileRow> unknownFileRows = new LinkedHashSet<>(fileRows);
      Queue<FileRow> updatedFileRows = new ConcurrentLinkedQueue<>();
      Runnable processUpdates = () -> {
         if (dataFileInfoAsyncHandle.isCancelled()) {
            return;
         }
         Set<TimeRow> toBeSorted = new HashSet<>();
         while (!updatedFileRows.isEmpty()) {
            FileRow fileRow = updatedFileRows.remove();
            TimeGroup timeGroup = dataConf.timeGrouping.getValue().toTimeGroup(fileRow.getInstant());
            TimeRow timeRow = timeGroupToTimeRow.computeIfAbsent(timeGroup, newTimeRow);
            if (timeRow.isUnknownTime()) {
               continue;
            }
            unknownFileRows.remove(fileRow);
            timeRow.fileRows.add(fileRow);
            toBeSorted.add(timeRow);
         }
         toBeSorted.forEach(timeRow -> timeRow.fileRows.sort(fileRowByTimeComparator()));
         unknownTimeRow.fileRows = new ArrayList<>(unknownFileRows);
         if (unknownFileRows.isEmpty()) {
            timeGroupToTimeRow.remove(unknownTimeRow.getTimeGroup());
         }
         timeRows = new ArrayList<>(timeGroupToTimeRow.values());
         List<FileRow> newFileRows = new ArrayList<>(fileRows);
         newFileRows.sort(currentFileRowComparator());
         fileRows = newFileRows;
         fireTableDataChanged();
      };
      Timer timer = new Timer(1000, _ -> processUpdates.run());
      timer.start();
      Queue<FileRow> fileRowsToInit = new ConcurrentLinkedQueue<>(fileRows);
      for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
         Exec.CACHED_THREAD_POOL.execute(dataFileInfoAsyncHandle.createManagedRunnable(() -> {
            while (true) {
               if (dataFileInfoAsyncHandle.isCancelled()) {
                  break;
               }
               FileRow fileRow = fileRowsToInit.poll();
               if (fileRow == null) {
                  break;
               }

               if (fileRow.rawSegmentHandle instanceof EK60SegmentHandle ek60SegmentHandle) {
                  EK60FileSet ek60FileSet = ek60SegmentHandle.getEK60FileSet();
                  if (!segmentHandlesAndAttributes.attributes().containsKey(ek60FileSet.getBot()) && !ek60FileSet.getXyz().isEmpty()) {
                     dataConf.foundMissingBotWithXyz();
                  }
               }
               SegmentInfo segmentInfo = segmentInfoCache != null ? segmentInfoCache.get(fileRow.rawSegmentHandle) : null;
               fileRow.initSegmentInfo(segmentInfo);
               if (segmentInfo == null && fileRow.segmentInfo != null && segmentInfoCache != null) {
                  segmentInfoCache.add(fileRow.rawSegmentHandle, fileRow.rawLastModified, fileRow.rawTotalFileSize, fileRow.segmentInfo);
               }

               fileRow.updateSa();

               updatedFileRows.add(fileRow);
            }
         }));
      }
      Exec.CACHED_THREAD_POOL.execute(() -> {
         dataFileInfoAsyncHandle.waitUntilFinished();
         if (segmentInfoCache != null) {
            segmentInfoCache.saveIfNeeded();
         }
         GuiUtils.invokeNowOrWait(() -> {
            timer.stop();
            processUpdates.run();
         });
      });

      fireTableDataChanged();

      dataConf.dataFileTableModelUpdated();
   }

   private void refreshKoronaColumn() {
      SegmentHandlesAndAttributes segmentHandlesAndAttributes = createSegmentHandles(dataConf.getProcessedDir());

      List<FileRow> fileRowsByName = new ArrayList<>(fileRows);
      fileRowsByName.sort(fileRowByNameComparator());

      fileRowsByName.forEach(fileRow -> {
         fileRow.koronaSegmentHandle = null;
         fileRow.koronaLastModified = 0;
         fileRow.koronaTotalFileSize = 0;
         fileRow.koronaDataTypeNames = null;
      });

      int fileRowIndex = 0;
      for (SegmentHandle koronaSegmentHandle : segmentHandlesAndAttributes.segmentHandles()) {
         String koronaBaseName = koronaSegmentHandle.getBaseName();
         for (; fileRowIndex < fileRowsByName.size(); fileRowIndex++) {
            FileRow fileRow = fileRowsByName.get(fileRowIndex);
            String rawBaseName = fileRow.rawSegmentHandle.getBaseName();
            if (rawBaseName.compareTo(koronaBaseName) > 0) {
               break;
            }
            if (koronaBaseName.startsWith(rawBaseName)) {
               LastModifiedAndSize lastModifiedAndSize = koronaSegmentHandle.getLastModifiedAndSize(segmentHandlesAndAttributes.attributes(), dataFileInfoAsyncHandle);
               if (fileRow.koronaSegmentHandle != null) {
                  if (fileRow.koronaConflictingFiles.isEmpty()) {
                     fileRow.koronaConflictingFiles = ImmutableList.of(fileRow.koronaSegmentHandle.getDisplayName());
                  }
                  fileRow.koronaConflictingFiles = ImmutableUtils.add(fileRow.koronaConflictingFiles, koronaSegmentHandle.getDisplayName());
                  if (lastModifiedAndSize.lastModified() < fileRow.koronaLastModified) {
                     continue;
                  }
               }
               fileRow.koronaSegmentHandle = koronaSegmentHandle;
               fileRow.koronaLastModified = lastModifiedAndSize.lastModified();
               fileRow.koronaTotalFileSize = lastModifiedAndSize.size();
               break;
            }
         }
      }

      updateAllRows();
   }

   private SegmentHandlesAndAttributes createSegmentHandles(@Nullable FileParameter fileParameter) {
      if (fileParameter == null) {
         return new SegmentHandlesAndAttributes(SegmentHandlesAndAttributes.DirListingStatus.NOT_SPECIFIED);
      }
      return dataConf.createSegmentHandles(fileParameter.getFile());
   }

   private void updateFileRow(int fileRowIndex) {
      int i = fileRowIndexToTableRowIndex(fileRowIndex);
      fireTableRowsUpdated(i, i);
   }

   private void updateAllRows() {
      fireTableRowsUpdated(0, getRowCount() - 1);
   }

   boolean isFiringTableDataChanged() {
      return firingTableDataChanged;
   }

   @Override
   public void fireTableDataChanged() {
      firingTableDataChanged = true;
      super.fireTableDataChanged();
      firingTableDataChanged = false;
      dataConf.updateTableSelection();
   }

   void updateSa() {
      fileRows.forEach(FileRow::updateSa);
      updateAllRows();
   }

   int getFileRowIndex(DataType dataType, String baseName) {
      return getFileRowIndex(dataType, baseName, 0);
   }

   private int getFileRowIndex(DataType dataType, String baseName, int beginIndex) {
      for (int i = Math.max(0, beginIndex); i < fileRows.size(); i++) {
         FileRow fileRow = fileRows.get(i);
         SegmentHandle segmentHandle = fileRow.getSegmentHandle(dataType);
         if (segmentHandle != null && baseName.equals(segmentHandle.getBaseName())) {
            return i;
         }
      }
      return -1;
   }

   @Nullable
   SegmentHandle getSegmentHandle(DataType dataType, Path file) {
      return fileRows.stream()
            .map(fileRow -> fileRow.getSegmentHandle(dataType))
            .filter(segmentHandle -> segmentHandle != null && segmentHandle.getFiles().contains(file))
            .findFirst()
            .orElse(null);
   }

   int fileRowIndexToTableRowIndex(int fileRowIndex) {
      if (fileRowIndex < 0) {
         return -1;
      }
      if (!dataConf.sortByTime.getBooleanValue()) {
         return fileRowIndex;
      }

      int tableRowCounter = 0;
      int fileRowCounter = 0;
      for (TimeRow timeRow : timeRows) {
         int indexInDay = fileRowIndex - fileRowCounter;
         if (indexInDay < timeRow.fileRows.size()) {
            if (timeRow.isExpanded()) {
               tableRowCounter += 1 + indexInDay;
            }
            return tableRowCounter;
         }
         tableRowCounter += timeRow.isExpanded() ? 1 + timeRow.fileRows.size() : 1;
         fileRowCounter += timeRow.fileRows.size();
      }
      return -1;
   }

   int tableRowIndexToFileRowIndex(int tableRowIndex, boolean first) {
      if (tableRowIndex < 0) {
         return -1;
      }
      if (!dataConf.sortByTime.getBooleanValue()) {
         return tableRowIndex;
      }

      int tableRowCounter = 0;
      int fileRowCounter = 0;
      for (TimeRow timeRow : timeRows) {
         if (tableRowIndex == tableRowCounter) {
            if (first) {
               return fileRowCounter;
            } else {
               return timeRow.isExpanded() ? fileRowCounter : fileRowCounter + timeRow.fileRows.size() - 1;
            }
         }
         tableRowCounter++;
         if (timeRow.isExpanded()) {
            int indexInDay = tableRowIndex - tableRowCounter;
            if (indexInDay < timeRow.fileRows.size()) {
               return fileRowCounter + indexInDay;
            }
            tableRowCounter += timeRow.fileRows.size();
         }
         fileRowCounter += timeRow.fileRows.size();
      }
      return -1;
   }

   private void removeDataFiles(DataType dataType) {
      if (dataType == DataType.RAW) {
         firstRawDataFile = null;
      }
      fileRows.forEach(fileRow -> {
         if (dataType == DataType.RAW) {
            fileRow.rawDataFile = null;
            fileRow.rawDataExceptionMessage = null;
            fileRow.rawIncompatibilityWithSelection = null;
         } else {
            fileRow.koronaDataFile = null;
            fileRow.koronaDataExceptionMessage = null;
            fileRow.koronaIncompatibilityWithSelection = null;
         }
      });
      updateAllRows();
   }

   SegmentHandlesAndAttributes.DirListingStatus getRawDirListingStatus() {
      return rawDirListingStatus;
   }

   List<SegmentHandle> getSegmentHandles() {
      return fileRows.stream()
            .map(FileRow::getRawSegmentHandle)
            .toList();
   }

   @Nullable DataFile getFirstRawDataFile() {
      return firstRawDataFile;
   }

   @Override
   public int getRowCount() {
      if (dataConf.sortByTime.getBooleanValue()) {
         return timeRows.stream()
               .mapToInt(day -> day.expanded ? 1 + day.fileRows.size() : 1)
               .sum();
      } else {
         return fileRows.size();
      }
   }

   @Override
   public int getColumnCount() {
      return COLUMN_NAMES.size();
   }

   @Override
   public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
      return getRow(rowIndex).getValueAt(columnIndex);
   }

   BaseRow getRow(int rowIndex) {
      if (dataConf.sortByTime.getBooleanValue()) {
         int rowCounter = 0;
         for (TimeRow timeRow : timeRows) {
            if (rowIndex == rowCounter) {
               return timeRow;
            }
            rowCounter++;
            if (!timeRow.expanded) {
               continue;
            }
            int indexInDay = rowIndex - rowCounter;
            if (indexInDay < timeRow.fileRows.size()) {
               return timeRow.fileRows.get(indexInDay);
            }
            rowCounter += timeRow.fileRows.size();
         }
         throw new IllegalArgumentException(Integer.toString(rowIndex));
      } else {
         return fileRows.get(rowIndex);
      }
   }

   @Override
   public String getColumnName(int column) {
      return COLUMN_NAMES.get(column);
   }

   FileOpenRegistration newRegisterLater(DataType dataType) {
      return new FileOpenRegistration(dataType);
   }

   final class FileOpenRegistration {
      private final DataType dataType;
      private int lastFileRowIndex;

      private FileOpenRegistration(DataType dataType) {
         this.dataType = dataType;
         SwingUtilities.invokeLater(() -> removeDataFiles(dataType));
      }

      private int segmentHandleToFileRowIndex(SegmentHandle segmentHandle, boolean updateLastIndex) {
         int fileRowIndex = getFileRowIndex(dataType, segmentHandle.getBaseName(), lastFileRowIndex);
         if (updateLastIndex && fileRowIndex > 0) {
            lastFileRowIndex = fileRowIndex + 1;
         }
         return fileRowIndex;
      }

      int getLastFileRowIndex() {
         return lastFileRowIndex;
      }

      void dataException(SegmentHandle segmentHandle, DataException dataException) {
         SwingUtilities.invokeLater(() -> {
            int fileRowIndex = segmentHandleToFileRowIndex(segmentHandle, false);
            if (fileRowIndex != -1) {
               fileRows.get(fileRowIndex).setDataExceptionMessage(dataType, dataException.getMessage());
               updateFileRow(fileRowIndex);
            }
         });
      }

      void dataFileIncompatibility(SegmentHandle segmentHandle, String incompatibilityReasons) {
         SwingUtilities.invokeLater(() -> {
            int fileRowIndex = segmentHandleToFileRowIndex(segmentHandle, false);
            if (fileRowIndex != -1) {
               fileRows.get(fileRowIndex).setIncompatibilityWithSelection(dataType, incompatibilityReasons);
               updateFileRow(fileRowIndex);
            }
         });
      }

      void dataFile(SegmentHandle segmentHandle, @Nullable DataFile dataFile) {
         SwingUtilities.invokeLater(() -> {
            int fileRowIndex = segmentHandleToFileRowIndex(segmentHandle, true);
            if (fileRowIndex != -1) {
               FileRow fileRow = fileRows.get(fileRowIndex);
               fileRow.setDataFile(dataType, dataFile);
               updateFileRow(fileRowIndex);
               if (firstRawDataFile == null && dataType == DataType.RAW && dataFile != null) {
                  firstRawDataFile = dataFile;
                  updateAllRows();
               }
            }
         });
      }
   }
}
