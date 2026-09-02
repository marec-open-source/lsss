package no.marec.tools.help.internal;

import no.imr.tools.Utils;
import no.imr.tools.help.HelpSystemInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.test.JUnitUtils;
import no.marec.tools.help.server.WebHelpDisplayer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class HelpServerMain {
   private HelpServerMain() {
   }

   static void main() throws IOException {
      start(false);
   }

   public static void start(boolean launchBrowser) throws IOException {
      List<String> helpDirs = findAllHelpDirs().stream()
            .map(HelpServerMain::fileToResource)
            .toList();

      // proxy.conf.json redirects '/api/*' to 'http://localhost:8080/help/app/x.y.z'
      WebHelpDisplayer webHelpDisplayer = new WebHelpDisplayer(helpDirs, List.of(), new HelpSystemInfo(8080, "app", "x.y.z"));

      if (launchBrowser) {
         GuiUtils.desktopBrowse(URI.create(webHelpDisplayer.getUrl()), null);
      }

      Log.global.info("Server started at " + webHelpDisplayer.getUrl());
      Log.global.info("Press enter to stop...");
      Utils.waitForEnter();

      webHelpDisplayer.close();
   }

   public static List<Path> findAllHelpDirs() throws IOException {
      List<Path> helpDirs = new ArrayList<>();

      Files.walkFileTree(LoggingManager.getTopInstallationDir(), new SimpleFileVisitor<>() {
         @Override
         public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            String dirName = dir.getFileName().toString();
            if (JUnitUtils.IGNORE_DIR_NAMES.contains(dirName)) {
               return FileVisitResult.SKIP_SUBTREE;
            }
            if (dirName.equals("help")) {
               Path helpSetJson = dir.resolve("build/helpSet.json");
               if (Files.exists(helpSetJson)) {
                  helpDirs.add(dir);
                  return FileVisitResult.SKIP_SUBTREE;
               }
            }
            return FileVisitResult.CONTINUE;
         }
      });

      helpDirs.sort(null);
      String info = helpDirs.stream()
            .map(Path::toString)
            .collect(Collectors.joining("\n", "Help dirs (" + helpDirs.size() + "):\n", ""));
      Log.global.info(info);
      return helpDirs;
   }

   private static String fileToResource(Path file) {
      String path = FileUtils.toSlashSeparatorChar(file.toString());
      String srcMainResources = "/src/main/resources/";
      int i = path.indexOf(srcMainResources);
      if (i < 0) {
         throw new IllegalArgumentException(file + " does not contain '" + srcMainResources + "'");
      }
      return path.substring(i + srcMainResources.length());
   }
}
