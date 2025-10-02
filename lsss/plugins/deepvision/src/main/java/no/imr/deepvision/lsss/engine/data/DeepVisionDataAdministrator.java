package no.imr.deepvision.lsss.engine.data;

import no.imr.deepvision.lsss.config.DeepVisionDataConf;
import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFile;
import no.imr.deepvision.lsss.engine.data.pojo.LsssDeepVisionFile;
import no.imr.deepvision.lsss.engine.mapping.DeepVisionMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.LSSS;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.range.Range;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.xml.JaxbUtils;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class DeepVisionDataAdministrator {
   private final LSSS lsss;
   private final DeepVisionDataConf deepVisionDataConf;
   private List<DeepVisionFileInfo> deepVisionFiles = List.of();
   private Map<DeepVisionFileInfo, LsssDeepVisionFileInfo> deepVisionToLsssFileInfo = Map.of();
   private final ArgChangeManager<List<DeepVisionFileInfo>> changeManager = new ArgChangeManager<>();

   public DeepVisionDataAdministrator(LSSS lsss, DeepVisionDataConf deepVisionDataConf) {
      this.lsss = lsss;
      this.deepVisionDataConf = deepVisionDataConf;
      lsss.getInterpretationSettings().createCoalescingListener(this::reload).addTo(
            deepVisionDataConf.baseDir,
            lsss.getInterpretationSettings().getReloadChangeManager());
   }

   public List<DeepVisionFileInfo> getAllFiles() {
      return deepVisionFiles;
   }

   public ArgChangeManager<List<DeepVisionFileInfo>> getChangeManager() {
      return changeManager;
   }

   private void setDeepVisionFiles(List<DeepVisionFileInfo> deepVisionFiles, List<LsssDeepVisionFileInfo> lsssDeepVisionFiles) {
      this.deepVisionFiles.forEach(DeepVisionFileInfo::close);
      this.deepVisionFiles = deepVisionFiles;
      deepVisionToLsssFileInfo = makeLsssFileInfoMap(deepVisionFiles, lsssDeepVisionFiles);
      changeManager.notifyListeners(deepVisionFiles);
   }

   private void reload() {
      Path dir = deepVisionDataConf.baseDir.getFile();
      if (dir == null) {
         setDeepVisionFiles(List.of(), List.of());
         return;
      }
      new WorkerDialog(lsss::getReferenceComponent, "Loading for Deep Vision files from\n" + dir)
            .setModalDialog(false)
            .start(asyncHandle -> {
               LoadFilesResult result = loadDeepVisionFiles(dir, asyncHandle);
               setDeepVisionFiles(result.deepVisionFiles, result.lsssDeepVisionFiles);
            });
   }

   private static LoadFilesResult loadDeepVisionFiles(Path directory, AsyncHandle asyncHandle) {
      List<DeepVisionFileInfo> dvFiles = new ArrayList<>();
      List<LsssDeepVisionFileInfo> lsssDvFiles = new ArrayList<>();
      try {
         Files.walkFileTree(directory, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 2, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
               if (asyncHandle.isCancelled()) {
                  return FileVisitResult.TERMINATE;
               }
               if (attrs.isRegularFile() && file.toString().endsWith(".xml")) {
                  try {
                     switch (XmlUtils.readRootElementName(file)) {
                        case DeepVisionFile.XML_ROOT_ELEMENT_NAME -> {
                           DeepVisionFile dvFile = JaxbUtils.read(DeepVisionFile.class, file);
                           dvFiles.add(new DeepVisionFileInfo(dvFile, file));
                        }
                        case LsssDeepVisionFile.XML_ROOT_ELEMENT_NAME -> {
                           LsssDeepVisionFile lsssDvFile = JaxbUtils.read(LsssDeepVisionFile.class, file);
                           lsssDvFiles.add(new LsssDeepVisionFileInfo(lsssDvFile, file));
                        }
                        default -> {
                        }
                     }
                  } catch (IOException e) {
                     Log.global.log(Level.WARNING, "Error reading " + file, e);
                  }
               }
               return FileVisitResult.CONTINUE;
            }
         });
      } catch (IOException e) {
         if (!Files.isDirectory(directory)) {
            return new LoadFilesResult(List.of(), List.of());
         }
         Log.global.log(Level.WARNING, "Error traversing " + directory, e);
      }
      return new LoadFilesResult(List.copyOf(dvFiles), List.copyOf(lsssDvFiles));
   }

   public List<DeepVisionFileInfo> getIntersectingFiles(PingRange pingRange, DeepVisionMapping deepVisionMapping) {
      if (pingRange.isEmpty()) {
         return List.of();
      }
      Range<Long> rangeInMillis = pingRange.toMillisRange();
      return deepVisionFiles.stream()
            .filter(fileInfo -> deepVisionMapping.deepVisionTimeRangeToLsssTimeRange(fileInfo.getTimeRangeMillis(), fileInfo).intersects(rangeInMillis))
            .toList();
   }

   public @Nullable DeepVisionFileInfo lsssTimeToFileInfo(long lsssTime, DeepVisionMapping deepVisionMapping) {
      return deepVisionFiles.stream()
            .filter(info -> {
               long deepVisionTime = deepVisionMapping.lsssTimeToDeepVisionTime(lsssTime, info);
               return info.getTimeRangeMillis().contains(deepVisionTime);
            })
            .findFirst()
            .orElse(null);
   }

   private static Map<DeepVisionFileInfo, LsssDeepVisionFileInfo> makeLsssFileInfoMap(List<DeepVisionFileInfo> deepVisionFiles,
                                                                                      List<LsssDeepVisionFileInfo> lsssDeepVisionFiles) {
      Map<DeepVisionFileInfo, LsssDeepVisionFileInfo> lsssDvFiles = new HashMap<>();
      // Only one deepVision file and one lsss-deepVision file allowed in the same folder.
      for (LsssDeepVisionFileInfo lsssDeepVisionFileInfo : lsssDeepVisionFiles) {
         Path lsssDeepVisionFile = lsssDeepVisionFileInfo.file();
         Path dir = lsssDeepVisionFile.getParent();
         DeepVisionFileInfo deepVisionFileInfo = deepVisionFiles.stream()
               .filter(info -> info.getFile().getParent().equals(dir))
               .findFirst()
               .orElse(null);
         if (deepVisionFileInfo != null) {
            lsssDvFiles.put(deepVisionFileInfo, lsssDeepVisionFileInfo);
         } else {
            Log.global.log(Level.WARNING, "Found no matching Deep Vision file for " + lsssDeepVisionFile);
         }
      }
      return Map.copyOf(lsssDvFiles);
   }

   public @Nullable LsssDeepVisionFileInfo getLsssDeepVisionFileInfo(DeepVisionFileInfo deepVisionFileInfo) {
      return deepVisionToLsssFileInfo.get(deepVisionFileInfo);
   }

   private record LoadFilesResult(
         List<DeepVisionFileInfo> deepVisionFiles,
         List<LsssDeepVisionFileInfo> lsssDeepVisionFiles
   ) {
   }
}
