package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.tools.time.DateTimeMillis;
import no.imr.tools.time.NTDate;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class EK500Utils {
   private EK500Utils() {
   }

   static long dateTimeToNTDate(int date, int time) {
      return NTDate.timeInMillisToNTDate(DateTimeMillis.toMillis(date, time * 1000));
   }

   static int ntDateToDate(long ntDate) {
      return new DateTimeMillis(NTDate.ntDateToTimeInMillis(ntDate)).getDate();
   }

   static int ntDateToTime(long ntDate) {
      return new DateTimeMillis(NTDate.ntDateToTimeInMillis(ntDate)).getTime() / 1000;
   }

   static List<TimeRecord> readTimeRecords(Path file, InfoRecord infoRecord) throws IOException {
      try (FileChannel fileChannel = FileChannel.open(file)) {
         ByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
               .order(infoRecord.getByteOrder());

         int n = (int) (fileChannel.size() / TimeRecord.SIZE_ON_FILE);
         List<TimeRecord> timeRecords = new ArrayList<>(n);
         for (int i = 0; i < n; i++) {
            timeRecords.add(new TimeRecord(byteBuffer));
         }

         return timeRecords;
      }
   }

   static List<IndexRecord> readIndexRecords(Path file, InfoRecord infoRecord) throws IOException {
      try (FileChannel fileChannel = FileChannel.open(file)) {
         ByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
               .order(infoRecord.getByteOrder());

         int n = (int) (fileChannel.size() / IndexRecord.SIZE_ON_FILE);
         List<IndexRecord> indexRecords = new ArrayList<>(n);
         for (int i = 0; i < n; i++) {
            indexRecords.add(new IndexRecord(byteBuffer));
         }

         return indexRecords;
      }
   }

   static boolean isSynced(List<IndexRecord> listA, List<IndexRecord> listB) {
      int equalCount = 0;
      int iA = 0;
      int iB = 0;
      while (iA < listA.size() && iB < listB.size()) {
         long ntDateA = listA.get(iA).getNTDate();
         long ntDateB = listB.get(iB).getNTDate();
         if (ntDateA == ntDateB) {
            equalCount++;
            iA++;
            iB++;
         } else {
            if (ntDateA < ntDateB) {
               iA++;
            } else {
               iB++;
            }
         }
      }

      return equalCount > 0.8 * Math.max(listA.size(), listB.size());
   }

   static SegmentInfo createSegmentInfo(EK500FileSet ek500FileSet) throws IOException {
      ek500FileSet.exceptionIfNoData();

      List<InfoRecord> infoRecords = new ArrayList<>();
      for (EK500FileSet.FrequencyFileSet frequencyFileSet : ek500FileSet.getFrequencyFileSets()) {
         infoRecords.add(new InfoRecord(frequencyFileSet.getInfoFile()));
      }

      long pingCount = Files.size(ek500FileSet.getFrequencyFileSets().getFirst().getPingFile()) / IndexRecord.SIZE_ON_FILE;

      EK500Settings ek500Settings = EK500Settings.createFromReferenceLocation(ek500FileSet.getMainFile());

      InfoRecord infoRecord = infoRecords.getFirst();
      PingIndex begin = new DefaultPingIndex(infoRecord.getStartNTDate(), 0, infoRecord.startDistance, null);
      PingIndex end = new DefaultPingIndex(infoRecord.getStopNTDate(), pingCount, infoRecord.stopDistance, null);
      RawFileConfiguration rawFileConfiguration = EK500DatagramFactory.createRawFileConfiguration(begin.getNTDate(), ek500Settings, ek500FileSet, infoRecords);

      return new SegmentInfo(rawFileConfiguration, PingRange.of(begin, end));
   }
}
