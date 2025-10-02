package no.imr.korona.cli.commands;

import no.imr.korona.Korona;
import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.cli.CliException;
import no.imr.korona.cli.CliUtils;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingReader;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.concurrent.AsyncHandle;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class PrintPingsCommandJob extends CliCommandJob {
   private final List<Path> files;
   private final Korona korona = new Korona();

   PrintPingsCommandJob(List<Path> files) {
      this.files = files;
      if (files.isEmpty()) {
         throw new CliException("Please specify one or more files or directories");
      }
   }

   @Override
   public void run(InputStream in, PrintStream out) {
      for (Path file : files) {
         if (Files.isDirectory(file)) {
            printDir(out, file);
         } else {
            printFile(out, file);
         }
      }
   }

   private void printDir(PrintStream out, Path dir) {
      List<SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandlesInDirectory(dir);
      for (SegmentHandle segmentHandle : segmentHandles) {
         print(out, segmentHandle);
      }
   }

   private void printFile(PrintStream out, Path file) {
      SegmentHandle segmentHandle = korona.getDataFormatManager().createSegmentHandle(file);
      if (segmentHandle == null) {
         throw new CliException("Unknown data format: " + file);
      }
      print(out, segmentHandle);
   }

   private static void print(PrintStream out, SegmentHandle segmentHandle) {
      out.println(CliUtils.FILE_SEPARATOR);
      out.println(segmentHandle.getMainFile());
      try (PingReader pingReader = segmentHandle.createPingReader()) {
         pingReader.getPingConfiguration().getConfigurationItems().forEach(pingItem -> print(out, pingItem));
         while (true) {
            Ping ping = pingReader.nextPing(new AsyncHandle());
            if (ping == null) {
               break;
            }
            out.println("--------------");
            ping.getPingItems().forEach(pingItem -> print(out, pingItem));
         }
      } catch (IOException e) {
         throw new CliException("Error reading " + segmentHandle.getMainFile(), e);
      }
   }

   private static void print(PrintStream out, PingItem pingItem) {
      for (BaseDatagram baseDatagram : pingItem.toDatagrams()) {
         out.println(baseDatagram.getNTDate() + " " + baseDatagram);
      }
   }
}
