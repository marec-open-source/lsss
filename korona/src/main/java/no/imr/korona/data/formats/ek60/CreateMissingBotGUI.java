package no.imr.korona.data.formats.ek60;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.formats.ek60.io.FileDatagramWriter;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class CreateMissingBotGUI {
   private final List<EK60SegmentHandle> allSegmentHandles;
   private final List<Integer> missingBotIndexes = new ArrayList<>();
   private final ProgressView progressView;

   public CreateMissingBotGUI(Collection<? extends SegmentHandle> segmentHandles) {
      allSegmentHandles = Utils.getAllOfType(segmentHandles, EK60SegmentHandle.class).toList();
      for (int i = 0; i < allSegmentHandles.size(); i++) {
         EK60SegmentHandle ek60SegmentHandle = allSegmentHandles.get(i);
         EK60FileSet ek60FileSet = ek60SegmentHandle.getEK60FileSet();
         if (!Files.exists(ek60FileSet.getBot()) && !ek60FileSet.getXyz().isEmpty()) {
            missingBotIndexes.add(i);
         }
      }
      progressView = new ProgressView("Creating bot files...", missingBotIndexes.size())
            .useSecondaryProgress();
   }

   public void start(@Nullable JComponent referenceComponent) {
      new WorkerDialog(referenceComponent, progressView.getComponent())
            .start(this::createMissingBotFiles);
   }

   private void createMissingBotFiles(AsyncHandle asyncHandle) {
      LoadingCache<EK60SegmentHandle, XyzData> xyzDataCache = CacheBuilder.newBuilder()
            .maximumSize(2L * Runtime.getRuntime().availableProcessors())
            .build(CacheLoader.from(CreateMissingBotGUI::loadXyzData));

      missingBotIndexes.parallelStream()
            .forEach(i -> {
               if (asyncHandle.isCancelled()) {
                  return;
               }
               EK60SegmentHandle segmentHandle = allSegmentHandles.get(i);
               XyzData xyzData = xyzDataCache.getUnchecked(segmentHandle);
               XyzData nextXyzData = i + 1 < allSegmentHandles.size()
                     ? xyzDataCache.getUnchecked(allSegmentHandles.get(i + 1))
                     : new XyzData(new RawFileConfiguration(0), List.of(), Map.of());

               // The xyz lines for the last pings in a raw file can be in the xyz file for the next raw file.
               Map<String, List<XyzLine>> channelIdToXyzLines = new HashMap<>();
               xyzData.channelIdToXyzLines.forEach((channelId, xyzLines) -> {
                  List<XyzLine> nextXyzLines = nextXyzData.channelIdToXyzLines.getOrDefault(channelId, List.of());
                  channelIdToXyzLines.put(channelId, Utils.toList(xyzLines, nextXyzLines));
               });

               XyzUtils.setDepths(xyzData.bot0Datagrams, xyzData.rawFileConfiguration, channelIdToXyzLines);

               try {
                  writeBotFile(segmentHandle.getEK60FileSet().getBot(), xyzData.rawFileConfiguration, xyzData.bot0Datagrams);
               } catch (IOException e) {
                  Log.global.log(Level.WARNING, "Error writing " + segmentHandle.getEK60FileSet().getBot(), e);
               }

               progressView.incrementMainProgress("");
            });
   }

   private static XyzData loadXyzData(EK60SegmentHandle segmentHandle) {
      try (SegmentData segmentData = segmentHandle.createSegmentData(NoticeHandler.ignore(), new AsyncHandle())) {
         Map<String, List<XyzLine>> channelIdToXyzLines = new HashMap<>();
         for (Path xyzFile : segmentHandle.getEK60FileSet().getXyz()) {
            int channelIndex = XyzUtils.findChannelIndex(segmentData.getRawFileConfiguration(), xyzFile);
            if (channelIndex < 0) {
               continue;
            }
            String channelId = segmentData.getRawFileConfiguration().getTransducers().get(channelIndex).getChannelId();
            List<XyzLine> xyzLines = XyzUtils.readXyzFile(xyzFile);
            channelIdToXyzLines.put(channelId, xyzLines);
         }
         return new XyzData(segmentData.getRawFileConfiguration(), segmentData.getBot0Datagrams(), channelIdToXyzLines);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error using xyz-files for " + segmentHandle.getMainFile(), e);
         return new XyzData(new RawFileConfiguration(0), List.of(), Map.of());
      }
   }

   private static void writeBotFile(Path botFile, RawFileConfiguration rawFileConfiguration, List<Bot0Datagram> bot0Datagrams) throws IOException {
      try (FileDatagramWriter writer = new FileDatagramWriter(botFile)) {
         writer.writeDatagrams(rawFileConfiguration.toDatagrams());
         writer.writeDatagrams(bot0Datagrams);
      }
   }

   private record XyzData(
         RawFileConfiguration rawFileConfiguration,
         List<Bot0Datagram> bot0Datagrams,
         Map<String, List<XyzLine>> channelIdToXyzLines
   ) {
   }
}
