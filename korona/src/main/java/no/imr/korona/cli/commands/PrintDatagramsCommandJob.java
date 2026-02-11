package no.imr.korona.cli.commands;

import no.imr.korona.Korona;
import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.cli.CliException;
import no.imr.korona.cli.CliUtils;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.formats.ek60.io.BaseDatagramReader;
import no.imr.korona.data.formats.ek60.io.FileDatagramReader;
import no.imr.korona.data.formats.ek60.io.InputStreamDatagramReader;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class PrintDatagramsCommandJob extends CliCommandJob {
   private final long limit;
   private final Set<String> only;
   private final Set<String> skip;
   private final List<Path> files;
   private final Korona korona = new Korona();
   private long datagramCount;

   PrintDatagramsCommandJob(long limit, Set<String> only, Set<String> skip, List<Path> files) {
      this.limit = limit;
      this.only = only;
      this.skip = skip;
      this.files = files;
   }

   @Override
   public void run(InputStream in, PrintStream out) throws IOException {
      if (files.isEmpty()) {
         print(out, new InputStreamDatagramReader(in, korona.getDatagramTypeManager()));
         return;
      }
      for (Path file : files) {
         if (Files.isDirectory(file)) {
            printDir(out, file);
         } else {
            printFile(out, file);
         }
      }
   }

   private void printDir(PrintStream out, Path dir) throws IOException {
      for (FileInfo fileInfo : FileUtils.listFilesWithAttributes(dir, Predicate.not(FileInfo::isDirectory))) {
         out.println(CliUtils.FILE_SEPARATOR);
         printFile(out, fileInfo.file());
      }
   }

   private void printFile(PrintStream out, Path file) {
      out.println(file);
      try (BaseDatagramReader datagramReader = new FileDatagramReader(file, korona.getDatagramTypeManager())) {
         print(out, datagramReader);
      } catch (IOException e) {
         throw new CliException("Error with file: " + file, e);
      }
   }

   private void print(PrintStream out, BaseDatagramReader datagramReader) throws IOException {
      long totalBytesSkipped = 0;
      while (true) {
         if (datagramCount >= limit) {
            out.println("Stopped after " + datagramCount + " datagrams");
            break;
         }
         long startOffset = datagramReader.getTotalRead();
         BaseDatagram datagram = datagramReader.nextDatagram();
         long bytesSkipped = datagramReader.getBytesSkipped() - totalBytesSkipped;
         if (bytesSkipped > 0) {
            out.println("Skipped " + bytesSkipped + " bytes");
            totalBytesSkipped = datagramReader.getBytesSkipped();
         }
         if (datagram == null) {
            break;
         }
         long datagramOffset = startOffset - totalBytesSkipped;
         String datagramType = datagram.getDatagramType().getAsciiQuad();
         if (skip.contains(datagramType)) {
            continue;
         }
         if (!only.isEmpty() && !only.contains(datagramType)) {
            continue;
         }
         out.println(datagramOffset + " " + datagram.getNTDate() + " " + datagram);
         datagramCount++;
      }
      out.println("Skipped " + datagramReader.getBytesSkipped() + " bytes in total");
   }
}
