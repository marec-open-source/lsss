package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

final class XyzUtils {
   private XyzUtils() {
   }

   static NavigableSet<String> toXyzFiles(Set<Path> files) {
      return files.stream()
            .map(file -> file.getFileName().toString())
            .filter(fileName -> Utils.endsWithIgnoringCase(fileName, EK60DataFormatPlugin.XYZ_SUFFIX))
            .collect(Collectors.toCollection(TreeSet::new));
   }

   static List<Path> baseNameToXyzFiles(Path dir, String baseName, NavigableSet<String> xyzFiles) {
      List<Path> list = null;
      String xyzPrevious = baseName;
      while (true) {
         String xyzNext = xyzFiles.higher(xyzPrevious);
         if (xyzNext == null || !xyzNext.startsWith(baseName)) {
            break;
         }
         if (list == null) {
            list = new ArrayList<>();
         }
         list.add(dir.resolve(xyzNext));
         xyzPrevious = xyzNext;
      }
      return list != null ? List.copyOf(list) : List.of();
   }

   static void setDepths(List<Bot0Datagram> bot0Datagrams, RawFileConfiguration rawFileConfiguration, Map<String, List<XyzLine>> channelIdToXyzLines) {
      channelIdToXyzLines.forEach((channelId, xyzLines) -> {
         int channelIndex = rawFileConfiguration.channelIdToChannelIndex(channelId);
         if (channelIndex < 0) {
            return;
         }
         int botIndex = 0;
         int xyzIndex = 0;
         while (botIndex < bot0Datagrams.size() && xyzIndex < xyzLines.size()) {
            Bot0Datagram bot0Datagram = bot0Datagrams.get(botIndex);
            XyzLine xyzLine = xyzLines.get(xyzIndex);
            long botTime = bot0Datagram.getTimeInMillis();
            long xyzTime = xyzLine.timeInMillis;
            if (Math.abs(botTime - xyzTime) <= 10) {
               // Match within precision of xyz file, i.e., HHmmss.ff
               bot0Datagram.getChannelDepths()[channelIndex] = xyzLine.depth;
               botIndex++;
               xyzIndex++;
            } else {
               // Not a match => the earliest time steps forward
               if (xyzTime < botTime) {
                  xyzIndex++;
               } else {
                  botIndex++;
               }
            }
         }
      });
   }

   static int findChannelIndex(RawFileConfiguration rawFileConfiguration, Path xyzFile) {
      // xyzFileName = baseName + "-" + channelIdShort + ".xyz"
      String xyzFileName = xyzFile.getFileName().toString();
      String rawFileName = rawFileConfiguration.getDataFile().getFileName().toString();
      int offset = rawFileName.length() - 3; // 3 = ".raw" - "-"
      List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
      for (int i = 0; i < transducers.size(); i++) {
         RawFileTransducer transducer = transducers.get(i);
         RawFileTransducer.Xml0Info xml0Info = transducer.getXml0Info();
         if (xml0Info == null) {
            continue;
         }
         if (matches(xyzFileName, offset, xml0Info.getChannelIdShort())) {
            return i;
         }
      }
      return -1;
   }

   private static boolean matches(String xyzFileName, int offset, String channelIdShort) {
      if (xyzFileName.length() != offset + channelIdShort.length() + 4) {
         return false;
      }
      for (int i = 0; i < channelIdShort.length(); i++) {
         char c = channelIdShort.charAt(i);
         if (!Character.isLetterOrDigit(c)) {
            // Skip characters that are illegal in file names, such as ":".
            continue;
         }
         if (c != xyzFileName.charAt(offset + i)) {
            return false;
         }
      }
      return true;
   }

   static List<XyzLine> readXyzFile(Path xyzFile) throws IOException {
      int lineNumber = 0;
      boolean logError = true;
      List<XyzLine> xyzLines = new ArrayList<>();
      try (BufferedReader reader = Files.newBufferedReader(xyzFile, Utils.ISO_8859_1)) {
         while (true) {
            String line = reader.readLine();
            if (line == null) {
               return xyzLines;
            }
            lineNumber++;
            try {
               xyzLines.add(new XyzLine(line));
            } catch (Exception e) {
               if (logError) {
                  logError = false;
                  Log.global.warning("Error parsing line " + lineNumber + " in " + xyzFile + ": \"" + line + "\": " + e.getMessage());
               }
            }
         }
      }
   }
}
