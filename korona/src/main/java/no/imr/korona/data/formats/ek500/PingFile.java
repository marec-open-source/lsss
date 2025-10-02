package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.DataException;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.NoticeHandler;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Data for one frequency.
 */
public final class PingFile {
   private final EK500FileSet.FrequencyFileSet frequencyFileSet;
   private final EK500TransducerSettings ek500TransducerSettings;
   private final short channel;
   private final InfoRecord infoRecord;
   private final List<IndexRecord> indexRecords;
   private @Nullable WorkRecord workRecord;

   private final FileChannel fileChannel;
   private final ByteBuffer byteBuffer;

   PingFile(EK500Settings ek500Settings, EK500FileSet.FrequencyFileSet frequencyFileSet, short channel, InfoRecord infoRecord, NoticeHandler noticeHandler) throws IOException {
      this.frequencyFileSet = frequencyFileSet;
      this.channel = channel;
      this.infoRecord = infoRecord;

      ek500TransducerSettings = ek500Settings.getEK500TransducerSettings(infoRecord.frequency, noticeHandler);

      indexRecords = EK500Utils.readIndexRecords(frequencyFileSet.getPingFile(), infoRecord);
      if (indexRecords.isEmpty()) {
         throw new DataException("No index records for " + frequencyFileSet.baseName());
      }
      if (ek500Settings.ignoreLastPing() && indexRecords.size() > 1) {
         IndexRecord lastIndexRecord = indexRecords.getLast();
         IndexRecord secondLastIndexRecord = indexRecords.get(indexRecords.size() - 2);
         lastIndexRecord.pelagicOffset = secondLastIndexRecord.pelagicOffset;
         lastIndexRecord.bottomOffset = secondLastIndexRecord.bottomOffset;
      }

      fileChannel = FileChannel.open(frequencyFileSet.getDataFile());

      int maxCount = 0;
      for (IndexRecord indexRecord : indexRecords) {
         maxCount = Math.max(maxCount, indexRecord.pelagicCount);
         maxCount = Math.max(maxCount, indexRecord.bottomCount);
      }
      byteBuffer = ByteBuffer.allocate(2 * maxCount)
            .order(infoRecord.getByteOrder());

      Path timeFile = frequencyFileSet.getTimeFile();
      if (Files.exists(timeFile)) {
         initTimes(EK500Utils.readTimeRecords(timeFile, infoRecord), noticeHandler);
      }
   }

   @Override
   public String toString() {
      return frequencyFileSet.getPingFile().getFileName().toString();
   }

   private void initTimes(List<TimeRecord> timeRecords, NoticeHandler noticeHandler) {
      if (timeRecords.size() != indexRecords.size()) {
         noticeHandler.addNotice("Wrong number of time records in " + frequencyFileSet.getTimeFile().getFileName());
      }

      long timeShiftNTDate = frequencyFileSet.getTimeShift().getTimeShiftNTDate(infoRecord);
      long timeFileCorrection = 0;
      int n = Math.min(indexRecords.size(), timeRecords.size());
      for (int i = 0; i < n; i++) {
         IndexRecord indexRecord = indexRecords.get(i);
         TimeRecord timeRecord = timeRecords.get(i);
         long pingFileTime = indexRecord.getNTDate();
         long timeFileTime = timeRecord.getNTDate(indexRecord);

         if (Math.abs((timeFileTime + timeFileCorrection - pingFileTime) - timeShiftNTDate) > EK500Settings.MAX_TIME_RECORD_JUMP_NT_DATE) {
            timeFileCorrection = timeShiftNTDate - (timeFileTime - pingFileTime);
            noticeHandler.addNotice("Found jump in time file " + frequencyFileSet.getTimeFile().getFileName());
         }
         indexRecord.setNTDate(timeFileTime + timeFileCorrection);
      }

      ensureNonDecreasingTime();
   }

   private void ensureNonDecreasingTime() {
      long t = 0;
      for (IndexRecord indexRecord : indexRecords) {
         long ntDate = indexRecord.getNTDate();
         if (ntDate < t) {
            indexRecord.setNTDate(t);
         } else {
            t = ntDate;
         }
      }
   }

   synchronized void close() throws IOException {
      fileChannel.close();
   }

   synchronized @Nullable PowerData createRaw(EK500PingIndex ek500PingIndex, RawFileConfiguration rawFileConfiguration) throws IOException {
      if (!fileChannel.isOpen()) {
         return null;
      }
      IndexRecord indexRecord = ek500PingIndex.getIndexRecords()[channel - 1];
      if (indexRecord != null) {
         return EK500DatagramFactory.createPowerData(ek500TransducerSettings, rawFileConfiguration, ek500PingIndex.getNTDate(), channel, indexRecord, fileChannel, byteBuffer);
      } else {
         return null;
      }
   }

   EK500FileSet.FrequencyFileSet getFrequencyFileSet() {
      return frequencyFileSet;
   }

   public InfoRecord getInfoRecord() {
      return infoRecord;
   }

   public short getChannel() {
      return channel;
   }

   List<IndexRecord> getIndexRecords() {
      return indexRecords;
   }

   int getIndexRecordCount() {
      return indexRecords.size();
   }

   public @Nullable WorkRecord getWorkRecord() {
      return workRecord;
   }

   void loadWorkRecord(Path workDir) throws IOException {
      Path workFile = frequencyFileSet.getExistingSnapOrWorkFile(workDir);
      if (workFile != null) {
         workRecord = new WorkRecord(workFile, infoRecord.getByteOrder());
      }
   }
}
