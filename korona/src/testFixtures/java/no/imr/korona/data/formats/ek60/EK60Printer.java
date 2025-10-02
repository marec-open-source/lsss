package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.datagrams.Nme0Datagram;
import no.imr.korona.data.datagrams.PerChannelDatagram;
import no.imr.korona.data.datagrams.Raw0Datagram;
import no.imr.korona.data.datagrams.UnknownDatagram;
import no.imr.korona.data.formats.DataFormatPrinter;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.korona.data.formats.ek60.io.RandomAccessDatagramReader;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.data.util.Nmea;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;
import no.imr.tools.time.NTDate;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("PMD.SystemPrintln")
final class EK60Printer extends DataFormatPrinter<EK60DataFormatPlugin> {
   private static final String[] HEADER = {"IDX counter", "Ping number", "        IDX NTDate", "   Datagram NTDate", "Raw offset", "Type"};
   private static final DateTimeFormatter TIME_FORMATTER = Utils.createUTCDateTimeFormatter("yyyy.MM.dd HH:mm:ss.SSS");
   private static final DatagramTypeManager DATAGRAM_TYPE_MANAGER = new DatagramTypeManager();

   private final Set<String> unknownDatagramTypes = new TreeSet<>();

   private EK60Printer() {
      super(new EK60DataFormatPlugin(new Name("EK60"), DATAGRAM_TYPE_MANAGER));
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

      int iIdx = 0;
      int iBot = 0;
      Idx0Datagram idx0Datagram = allIdxDatagrams.get(iIdx);
      Bot0Datagram bot0Datagram = null;
      long lastPingNumber = idx0Datagram.getPingNumber() - 1;
      long lastRawTime = 0;
      double lastDistance = 0;

      while (true) {
         long posBefore = datagramReader.getPosition();
         BaseDatagram datagram = datagramReader.nextDatagram();
         if (datagram == null) {
            break;
         }
         if (lastRawTime == 0 && !datagram.isSampleDatagram()) {
            System.out.print(TIME_FORMATTER.format(Instant.ofEpochMilli(NTDate.ntDateToTimeInMillis(datagram.getNTDate()))) + ' ' + datagram.getDatagramType().getAsciiQuad() + ' ');
            print(datagram);
            System.out.println();
            continue;
         }
         if (idx0Datagram.getPingNumber() != lastPingNumber) {
            System.out.println();
            print((Object[]) HEADER);
            System.out.print(TIME_FORMATTER.format(idx0Datagram.getInstant()));
            System.out.println();
            print(iIdx, idx0Datagram.getPingNumber(), idx0Datagram.getNTDate(), idx0Datagram.getNTDate(), idx0Datagram.getFileOffset(), idx0Datagram.getDatagramType().getAsciiQuad());
            System.out.print(idx0Datagram.getVesselDistance() + ", " + idx0Datagram.getGeographicalPosition());
            lastPingNumber = idx0Datagram.getPingNumber();

            if (idx0Datagram.getVesselDistance() < lastDistance) {
               System.out.print(" ************** <<< distance");
            }
            lastDistance = idx0Datagram.getVesselDistance();
            System.out.println();

            bot0Datagram = iBot + 1 < allBotDatagrams.size() ? allBotDatagrams.get(iBot++) : null;
            if (bot0Datagram != null) {
               print(iIdx, idx0Datagram.getPingNumber(), idx0Datagram.getNTDate(), bot0Datagram.getNTDate(), "", bot0Datagram.getDatagramType().getAsciiQuad());
               System.out.print(Arrays.toString(bot0Datagram.getChannelDepths()));
               System.out.println();
            }
         }
         print(iIdx, idx0Datagram.getPingNumber(), idx0Datagram.getNTDate(), datagram.getNTDate(), posBefore, datagram.getDatagramType().getAsciiQuad());
         print(datagram);
         if (datagram.isSampleDatagram()) {
            if (bot0Datagram != null && datagram instanceof PerChannelDatagram perChannelDatagram) {
               System.out.print(", " + bot0Datagram.getChannelDepths()[perChannelDatagram.getChannel() - 1] + " m");
            }
            if (datagram.getNTDate() < lastRawTime) {
               System.out.print(" ************** <<< time");
            }
            lastRawTime = datagram.getNTDate();
         }
         System.out.println();
         if (iIdx + 1 < allIdxDatagrams.size() && datagramReader.getPosition() >= allIdxDatagrams.get(iIdx + 1).getFileOffset()) {
            iIdx++;
            idx0Datagram = allIdxDatagrams.get(iIdx);
         }
      }
   }

   private static void print(Object... values) {
      for (int i = 0; i < values.length; i++) {
         String s = values[i].toString();
         int n = i < HEADER.length ? HEADER[i].length() : s.length();
         System.out.print(Utils.format("%" + n + 's', s));
         System.out.print(" | ");
      }
   }

   private static void printRawOnly(RandomAccessDatagramReader datagramReader) throws IOException {
      while (true) {
         BaseDatagram datagram = datagramReader.nextDatagram();
         if (datagram == null) {
            break;
         }
         System.out.print(TIME_FORMATTER.format(Instant.ofEpochMilli(NTDate.ntDateToTimeInMillis(datagram.getNTDate()))) + ' ' + datagram.getDatagramType().getAsciiQuad() + ' ');
         print(datagram);
         System.out.println();
      }
   }

   private static void print(BaseDatagram datagram) {
      switch (datagram) {
         case Raw0Datagram raw0 -> printRaw(raw0);
         case Nme0Datagram nme0 -> printNmea(nme0);
         case Cac0Datagram cac0 -> printCac(cac0);
         default -> System.out.print(datagram);
      }
   }

   private static void printRaw(Raw0Datagram raw0) {
      System.out.print("Channel " + raw0.channel + ", " + String.format("%3d", Utils.hzToKHz(raw0.frequency)) + " kHz, " + raw0.count + " samples");
   }

   private static void printNmea(Nme0Datagram nme0) {
      Nmea nmea = Nmea.of(nme0.getNmea());
      System.out.print('"' + nme0.getNmea() + "\" " + nmea.getMeterPerSec() + ' ' + nmea.getGeographicalPosition());
   }

   private static void printCac(Cac0Datagram cac0) {
      System.out.print(cac0.getCategories());
   }

   public static void main(String[] args) {
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
