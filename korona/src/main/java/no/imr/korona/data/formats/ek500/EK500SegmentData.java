package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.Min;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.misc.HtmlStringBuilder;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Reads pings from EK500 data.
 */
public final class EK500SegmentData extends SegmentData {
   private final PingConfiguration pingConfiguration;

   private final List<PingFile> pingFiles = new ArrayList<>();
   private final List<EK500PingIndex> pingIndices;
   private final List<Bot0Datagram> bot0Datagrams;

   EK500SegmentData(EK500FileSet ek500FileSet, NoticeHandler noticeHandler) throws IOException {
      ek500FileSet.exceptionIfNoData();

      EK500Settings ek500Settings = EK500Settings.createFromReferenceLocation(ek500FileSet.getMainFile());

      Instant smallestInstant = Instant.MAX;
      List<InfoRecord> infoRecords = new ArrayList<>();
      for (EK500FileSet.FrequencyFileSet frequencyFileSet : ek500FileSet.getFrequencyFileSets()) {
         InfoRecord infoRecord = new InfoRecord(frequencyFileSet.getInfoFile());
         if (infoRecord.getFrequency() == 0) {
            noticeHandler.addNotice("Ignored frequency 0 Hz: " + frequencyFileSet.baseName());
            continue;
         }
         short channel = (short) (pingFiles.size() + 1);
         PingFile pingFile = new PingFile(ek500Settings, frequencyFileSet, channel, infoRecord, noticeHandler);
         pingFiles.add(pingFile);
         infoRecords.add(infoRecord);

         List<IndexRecord> indexRecords = pingFile.getIndexRecords();
         Instant startInstant = indexRecords.getFirst().getInstant();
         Instant stopInstant = indexRecords.getLast().getInstant();
         infoRecord.setStartInstant(startInstant);
         infoRecord.setStopInstant(stopInstant);
         smallestInstant = Min.of(smallestInstant, startInstant);
      }

      RawFileConfiguration rawFileConfiguration = EK500DatagramFactory.createRawFileConfiguration(smallestInstant, ek500Settings, ek500FileSet, infoRecords);
      pingConfiguration = new PingConfiguration(rawFileConfiguration);
      pingIndices = createPingIndices();
      bot0Datagrams = createBot0Datagrams();
   }

   public List<PingFile> getPingFiles() {
      return pingFiles;
   }

   public void loadWorkRecords(Path workDir) throws IOException {
      for (PingFile pingFile : pingFiles) {
         pingFile.loadWorkRecord(workDir);
      }
   }

   private List<PingFile> getPingFilesForCreatingPingIndices() {
      PingFile referencePingFile = pingFiles.stream()
            .filter(pingFile -> Math.abs(pingFile.getInfoRecord().frequency - EK500FileSet.MAIN_FREQUENCY) < 1_000)
            .findFirst()
            .orElse(null);
      if (referencePingFile != null) {
         return List.of(referencePingFile);
      }

      List<PingFile> largestSyncedPingFileList = List.of();
      List<List<PingFile>> syncedPingFileLists = new ArrayList<>();
      for (PingFile pingFile : pingFiles) {
         List<PingFile> matchingSyncedPingFileList = null;

         for (List<PingFile> syncedPingFileList : syncedPingFileLists) {
            if (EK500Utils.isSynced(pingFile.getIndexRecords(), syncedPingFileList.getFirst().getIndexRecords())) {
               matchingSyncedPingFileList = syncedPingFileList;
               break;
            }
         }

         if (matchingSyncedPingFileList == null) {
            matchingSyncedPingFileList = new ArrayList<>();
            syncedPingFileLists.add(matchingSyncedPingFileList);
         }

         matchingSyncedPingFileList.add(pingFile);
         if (matchingSyncedPingFileList.size() > largestSyncedPingFileList.size()) {
            largestSyncedPingFileList = matchingSyncedPingFileList;
         }
      }
      return largestSyncedPingFileList;
   }

   private List<EK500PingIndex> createPingIndices() {
      NavigableMap<Instant, EK500PingIndex> instantToEK500PingIndex = new TreeMap<>();
      for (PingFile pingFile : getPingFilesForCreatingPingIndices()) {
         for (IndexRecord indexRecord : pingFile.getIndexRecords()) {
            Instant instant = indexRecord.getInstant();
            if (!instantToEK500PingIndex.containsKey(instant)) {
               instantToEK500PingIndex.put(instant, new EK500PingIndex(indexRecord, pingFiles.size()));
            }
         }
      }

      for (PingFile pingFile : pingFiles) {
         for (IndexRecord indexRecord : pingFile.getIndexRecords()) {
            Map.Entry<Instant, EK500PingIndex> floorEntry = instantToEK500PingIndex.floorEntry(indexRecord.getInstant());
            if (floorEntry != null) {
               EK500PingIndex ek500PingIndex = floorEntry.getValue();
               ek500PingIndex.getIndexRecords()[pingFile.getChannel() - 1] = indexRecord;
            }
         }
      }

      return new ArrayList<>(instantToEK500PingIndex.values());
   }

   private List<Bot0Datagram> createBot0Datagrams() {
      List<Bot0Datagram> result = new ArrayList<>(pingIndices.size());
      for (int i = 0; i < pingIndices.size(); i++) {
         EK500PingIndex ek500PingIndex = pingIndices.get(i);
         ek500PingIndex.setPingNumber(i);
         result.add(createBot0Datagram(ek500PingIndex));
      }
      return result;
   }

   private Bot0Datagram createBot0Datagram(EK500PingIndex ek500PingIndex) {
      Bot0Datagram bot0Datagram = new Bot0Datagram(ek500PingIndex.getInstant(), pingFiles.size());
      @Nullable IndexRecord[] indexRecords = ek500PingIndex.getIndexRecords();
      for (int i = 0; i < indexRecords.length; i++) {
         IndexRecord indexRecord = indexRecords[i];
         if (indexRecord != null) {
            bot0Datagram.getChannelDepths()[i] = indexRecord.bottomDepth;
         }
      }
      return bot0Datagram;
   }

   @Override
   public void close() throws IOException {
      for (PingFile pingFile : pingFiles) {
         pingFile.close();
      }
   }

   @Override
   public List<EK500PingIndex> getPingIndices() {
      return pingIndices;
   }

   @Override
   public @Nullable WrapAround getWrapAround() {
      return null;
   }

   @Override
   public String getInfo() {
      HtmlStringBuilder sb = HtmlStringBuilder.withoutInitialHtmlTag()
            .html("<table border=1>"
                  + "<tr>"
                  + "<th>File</th>"
                  + "<th>SAPelagic</th>"
                  + "<th>PstPelagic</th>"
                  + "</tr>");
      for (PingFile pingFile : pingFiles) {
         sb.html("<tr><td>").text(pingFile.getFrequencyFileSet().baseName()).html("</td>");
         InfoRecord infoRecord = pingFile.getInfoRecord();
         if (infoRecord.isBEI()) {
            sb.html("<td>").text(Utils.format("%.1f", infoRecord.saPelagic)).html("</td>");
            sb.html("<td>").text(Utils.format("%.1f", infoRecord.pstPelagic)).html("</td>");
         } else {
            sb.html("<td>Not BEI</td>");
            sb.html("<td>Not BEI</td>");
         }
         sb.html("</tr>");
      }
      sb.html("</table>");
      return sb.toString();
   }

   @Override
   public List<Bot0Datagram> getBot0Datagrams() {
      return bot0Datagrams;
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException {
      int iPing = (int) (pingIndex.getPingNumber() - pingIndices.getFirst().getPingNumber());
      EK500PingIndex ek500PingIndex = pingIndices.get(iPing);

      PingData pingData = new PingData(pingConfiguration);

      if (iPing % EK500Settings.NMEA_GENERATION_INTERVAL == 0) {
         int i0 = Math.max(0, iPing - 5);
         int i1 = Math.min(pingIndices.size() - 1, iPing + 5);
         double knots = KoronaUtils.getKnots(pingIndices.get(i0), pingIndices.get(i1));
         double kmh = KoronaUtils.knotsToKilometerPerHour(knots);
         String nmea = "$GPVTG,000,T,000,M," + knots + ",N," + kmh + ",K";
         pingData.add(new NmeaPingItem(pingIndex.getInstant(), nmea));
      }

      for (PingFile pingFile : pingFiles) {
         if (asyncHandle.isCancelled()) {
            break;
         }

         PowerData powerData = pingFile.createRaw(ek500PingIndex, pingConfiguration.getRawFileConfiguration());
         if (powerData != null) {
            pingData.add(powerData);
         }
      }

      return pingData;
   }
}
