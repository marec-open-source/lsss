package no.imr.korona.data.formats.ek60;

import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;

/**
 * The triplet of EK60 files: raw, idx and bot.
 */
public final class EK60FileSet {
   private final Path raw;
   private final Path idx;
   private final Path bot;
   private final List<Path> xyz;

   public EK60FileSet(Path file) {
      this(file, Collections.emptyNavigableSet());
   }

   public EK60FileSet(Path file, NavigableSet<String> xyzFiles) {
      Path dir = file.getParent();
      String baseName = FileUtils.baseName(file);
      raw = FileUtils.resolve(dir, baseName + EK60DataFormatPlugin.RAW_SUFFIX);
      idx = FileUtils.resolve(dir, baseName + EK60DataFormatPlugin.IDX_SUFFIX);
      bot = FileUtils.resolve(dir, baseName + EK60DataFormatPlugin.BOT_SUFFIX);
      xyz = dir != null ? XyzUtils.baseNameToXyzFiles(dir, baseName, xyzFiles) : List.of();
   }

   @Override
   public String toString() {
      return raw.toString();
   }

   public Path getRaw() {
      return raw;
   }

   public Path getIdx() {
      return idx;
   }

   public Path getBot() {
      return bot;
   }

   public List<Path> getXyz() {
      return xyz;
   }

   public List<Path> getFiles() {
      List<Path> files = new ArrayList<>(3 + xyz.size());
      files.add(raw);
      files.add(idx);
      files.add(bot);
      files.addAll(xyz);
      return files;
   }

   public void delete() throws IOException {
      Files.deleteIfExists(raw);
      Files.deleteIfExists(idx);
      Files.deleteIfExists(bot);
      for (Path xyzFile : xyz) {
         Files.deleteIfExists(xyzFile);
      }
   }
}
