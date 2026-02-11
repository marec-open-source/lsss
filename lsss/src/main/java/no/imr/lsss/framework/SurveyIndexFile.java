package no.imr.lsss.framework;

import no.imr.lsss.LSSS;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public final class SurveyIndexFile {
   public static final String NAME = "SurveyIndex.txt";

   private final Path dir;
   private final Set<Path> surveyFiles = new HashSet<>();
   private int changeCount;

   public SurveyIndexFile(Path dir) throws IOException {
      this.dir = dir;
      Path indexFile = getIndexFile();
      if (!Files.exists(indexFile)) {
         return;
      }
      try (BufferedReader in = Files.newBufferedReader(indexFile, Utils.UTF_8)) {
         in.lines()
               .filter(line -> !line.startsWith("#") && !line.isEmpty())
               .map(line -> dir.resolve(line).normalize())
               .forEach(surveyFiles::add);
      }
   }

   public Path getIndexFile() {
      return dir.resolve(NAME);
   }

   public Set<Path> getSurveyFiles() {
      return surveyFiles;
   }

   public void add(Path file) {
      if (surveyFiles.add(file.normalize())) {
         changeCount++;
      }
   }

   public void remove(Path file) {
      if (surveyFiles.remove(file.normalize())) {
         changeCount++;
      }
   }

   public int getChangeCount() {
      return changeCount;
   }

   public void saveIfChanged() throws IOException {
      if (changeCount == 0) {
         return;
      }
      Path indexFile = getIndexFile();
      if (surveyFiles.isEmpty() && !Files.exists(indexFile)) {
         return;
      }
      FileUtils.createDirectories(dir);
      try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(indexFile, Utils.UTF_8))) {
         out.println("# This file is updated automatically by LSSS.");
         out.println("# Last updated " + Instant.now().truncatedTo(ChronoUnit.SECONDS) + " by LSSS " + LSSS.VERSION);
         out.println("#");
         surveyFiles.stream()
               .map(dir::relativize)
               .sorted()
               .forEach(out::println);
      }
      changeCount = 0;
   }

   public static List<Path> getRoots() {
      List<Path> roots = new ArrayList<>();
      try {
         roots.addAll(FileUtils.listExistingRoots());
         roots.addAll(FileUtils.listFiles(Path.of("/media")));
         roots.addAll(FileUtils.listFiles(Path.of("/media/" + Utils.getUserName())));
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
      return roots;
   }

   public static @Nullable Path findMostSpecificRootForFile(Path file, Collection<Path> roots) {
      Path mostSpecificRoot = null;
      for (Path root : roots) {
         if (FileUtils.isInDir(file, root) && (mostSpecificRoot == null || FileUtils.isInDir(root, mostSpecificRoot))) {
            mostSpecificRoot = root;
         }
      }
      return mostSpecificRoot;
   }

   static void addSurveyFile(Path file) {
      if (FileUtils.isInDir(file, Utils.getTmpDir())) {
         return;
      }
      try {
         Path root = findMostSpecificRootForFile(file, getRoots());
         if (root == null) {
            return;
         }
         Path relativePath = root.relativize(file);
         Path lsssDataDir;
         if (relativePath.getNameCount() > 1 && relativePath.getName(0).toString().startsWith(SurveyManager.LSSS_DATA_DIR_NAME)) {
            lsssDataDir = root.resolve(relativePath.getName(0));
         } else {
            lsssDataDir = root.resolve(SurveyManager.LSSS_DATA_DIR_NAME);
         }
         if (!Files.exists(lsssDataDir)) {
            return;
         }
         SurveyIndexFile surveyIndexFile = new SurveyIndexFile(lsssDataDir);
         surveyIndexFile.add(file);
         surveyIndexFile.saveIfChanged();
      } catch (IOException _) {
         // Ignore here.
      }
   }
}
