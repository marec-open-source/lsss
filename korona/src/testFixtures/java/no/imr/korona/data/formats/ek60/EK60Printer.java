package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.datagrams.UnknownDatagram;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.korona.data.formats.ek60.io.RandomAccessDatagramReader;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.viewer.DataFilePreview;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.test.BaseFileMain;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("PMD.SystemPrintln")
final class EK60Printer extends BaseFileMain {
   private static final String[] PING_HEADER = {
         "IDX counter",
         "Ping number",
         "                 Datagram time",
         "Raw offset",
         "Type"
   };
   private static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder()
         .appendInstant(9)
         .toFormatter(Locale.ROOT);
   private static final DatagramTypeManager DATAGRAM_TYPE_MANAGER = new DatagramTypeManager();

   private final EK60DataFormatPlugin dataFormatPlugin = new EK60DataFormatPlugin(new Name("EK60"), DATAGRAM_TYPE_MANAGER);
   private final Set<String> unknownDatagramTypes = new TreeSet<>();

   private EK60Printer() {
   }

   @Override
   protected void customizeFileChooser(JFileChooser fileChooser) {
      fileChooser.setFileFilter(new SuffixFileFilter(dataFormatPlugin.getDescription(), dataFormatPlugin.getMainSuffixes()));
      DataFilePreview.install(fileChooser);
   }

   @Override
   protected void execute(Path file) throws IOException {
      EK60FileSet ek60FileSet = new EK60FileSet(file);
      try (MyFileDatagramReader datagramReader = new MyFileDatagramReader(ek60FileSet.getRaw(), DATAGRAM_TYPE_MANAGER)) {
         if (Files.exists(ek60FileSet.getIdx())) {
            printAllFiles(ek60FileSet, datagramReader);
         } else {
            printRawOnly(datagramReader);
         }

         if (datagramReader.getBytesSkipped() > 0) {
            System.out.println("datagramReader.getBytesSkipped() = " + datagramReader.getBytesSkipped());
         }

         if (!unknownDatagramTypes.isEmpty()) {
            System.out.println("Unknown datagrams: " + unknownDatagramTypes);
         }
      }
   }

   private void printAllFiles(EK60FileSet ek60FileSet, RandomAccessDatagramReader datagramReader) throws IOException {
      List<Idx0Datagram> allIdxDatagrams = DataUtils.getAll(new MyFileDatagramReader(ek60FileSet.getIdx(), DATAGRAM_TYPE_MANAGER), Idx0Datagram.class);
      List<Bot0Datagram> allBotDatagrams = Files.exists(ek60FileSet.getBot())
            ? DataUtils.getAll(new MyFileDatagramReader(ek60FileSet.getBot(), DATAGRAM_TYPE_MANAGER), Bot0Datagram.class)
            : List.of();

      int idx0Index = 0;
      Idx0Datagram idx0Datagram = allIdxDatagrams.get(idx0Index);
      long lastHeaderIdx0Index = -1;
      boolean beforeFirstSampleDatagram = true;

      while (true) {
         long posBefore = datagramReader.getPosition();
         BaseDatagram datagram = datagramReader.nextDatagram();
         if (datagram == null) {
            break;
         }
         if (beforeFirstSampleDatagram) {
            if (!datagram.isSampleDatagram()) {
               printDatagram(datagram, posBefore);
               continue;
            }
            beforeFirstSampleDatagram = false;
         }
         if (idx0Index != lastHeaderIdx0Index) {
            lastHeaderIdx0Index = idx0Index;
            System.out.println();
            printPingValues((Object[]) PING_HEADER);
            printPingDatagram(idx0Index, idx0Datagram, idx0Datagram, String.valueOf(idx0Datagram.getFileOffset()));

            if (idx0Index < allBotDatagrams.size()) {
               printPingDatagram(idx0Index, idx0Datagram, allBotDatagrams.get(idx0Index), "");
            }
         }
         printPingDatagram(idx0Index, idx0Datagram, datagram, String.valueOf(posBefore));
         while (idx0Index + 1 < allIdxDatagrams.size() && datagramReader.getPosition() >= allIdxDatagrams.get(idx0Index + 1).getFileOffset()) {
            idx0Index++;
            idx0Datagram = allIdxDatagrams.get(idx0Index);
         }
      }
   }

   private static void printDatagram(BaseDatagram datagram, long rawFilePos) {
      System.out.println("%10d".formatted(rawFilePos)
            + " | " + TIME_FORMATTER.format(datagram.getInstant())
            + " | " + datagram.getDatagramType().getAsciiQuad()
            + " | " + datagram.toStringExtra()
      );
   }

   private static void printPingDatagram(int iIdx, Idx0Datagram idx0Datagram, BaseDatagram datagram, String rawFilePos) {
      printPingValues(
            iIdx,
            idx0Datagram.getPingNumber(),
            TIME_FORMATTER.format(datagram.getInstant()),
            rawFilePos,
            datagram.getDatagramType().getAsciiQuad(),
            datagram.toStringExtra()
      );
   }

   private static void printPingValues(Object... values) {
      for (int i = 0; i < values.length; i++) {
         String s = values[i].toString();
         if (i < PING_HEADER.length) {
            System.out.print(" ".repeat(Math.max(0, PING_HEADER[i].length() - s.length())));
         }
         System.out.print(s);
         if (i < PING_HEADER.length) {
            System.out.print(" | ");
         }
      }
      System.out.println();
   }

   private static void printRawOnly(RandomAccessDatagramReader datagramReader) throws IOException {
      while (true) {
         long posBefore = datagramReader.getPosition();
         BaseDatagram datagram = datagramReader.nextDatagram();
         if (datagram == null) {
            break;
         }
         printDatagram(datagram, posBefore);
      }
   }

   static void main() {
      new EK60Printer().start();
   }

   private final class MyFileDatagramReader extends FileDatagramReader {
      private MyFileDatagramReader(Path file, DatagramTypeManager datagramTypeManager) throws IOException {
         super(file, datagramTypeManager);
      }

      @Override
      public @Nullable BaseDatagram nextDatagram(AsyncHandle asyncHandle) throws IOException {
         BaseDatagram datagram = super.nextDatagram(asyncHandle);
         if (datagram instanceof UnknownDatagram) {
            unknownDatagramTypes.add(datagram.getDatagramType().getAsciiQuad());
         }
         return datagram;
      }
   }
}
