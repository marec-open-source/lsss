package no.imr.korona.data;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.korona.plugins.DataFormatService;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.plugins.BaseService;
import no.imr.tools.swing.SuffixFileFilter;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Locates all instances of {@link DataFormatPlugin}.
 */
public final class DataFormatManager {
   private final DatagramTypeManager datagramTypeManager;
   private final List<DataFormatPlugin> dataFormatPlugins;

   public DataFormatManager() {
      datagramTypeManager = new DatagramTypeManager();
      dataFormatPlugins = BaseService.getUsableServices(DataFormatService.class)
            .map(dataFormatService -> dataFormatService.createPlugin(datagramTypeManager))
            .toList();
   }

   public DatagramTypeManager getDatagramTypeManager() {
      return datagramTypeManager;
   }

   public @Nullable SegmentHandle createSegmentHandle(Path file) {
      for (DataFormatPlugin dataFormatPlugin : dataFormatPlugins) {
         try {
            SegmentHandle segmentHandle = dataFormatPlugin.createSegmentHandle(file);
            if (segmentHandle != null) {
               return segmentHandle;
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error with data format " + dataFormatPlugin.getDescription(), e);
         }
      }
      return null;
   }

   public List<SegmentHandle> createSegmentHandlesInDirectory(Path dir) {
      return createSegmentHandlesInDirectory(dir, new AsyncHandle());
   }

   public List<SegmentHandle> createSegmentHandlesInDirectory(Path dir, AsyncHandle asyncHandle) {
      List<Path> files;
      try {
         files = FileUtils.listFiles(dir, asyncHandle);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error listing files in " + dir, e);
         return List.of();
      }
      return createSegmentHandles(files, asyncHandle);
   }

   public List<SegmentHandle> createSegmentHandles(Collection<Path> files, AsyncHandle asyncHandle) {
      List<SegmentHandle> allSegmentHandles = new ArrayList<>();
      Set<Path> fileSet = new HashSet<>(files);
      for (DataFormatPlugin dataFormatPlugin : dataFormatPlugins) {
         try {
            for (SegmentHandle segmentHandle : dataFormatPlugin.createSegmentHandles(fileSet, asyncHandle)) {
               segmentHandle.getFiles().forEach(fileSet::remove);
               allSegmentHandles.add(segmentHandle);
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error with data format " + dataFormatPlugin.getDescription(), e);
         }
      }
      allSegmentHandles.sort(null);
      return allSegmentHandles;
   }

   /**
    * Creates a list of segment handles. If the input argument is a directory, also data files
    * in subdirectories will be included.
    *
    * @param dir         the directory
    * @param asyncHandle async handle
    * @return a list of handles to the files in the directory and subdirectories
    * @throws IOException if some IO error occurs
    */
   public List<SegmentHandle> createSegmentHandlesInDirectoryRecursively(Path dir, AsyncHandle asyncHandle) throws IOException {
      List<FileInfo> fileInfos = FileUtils.listFilesWithAttributes(dir, asyncHandle);

      List<SegmentHandle> segmentHandles = new ArrayList<>();

      for (FileInfo fileInfo : fileInfos) {
         if (fileInfo.isDirectory()) {
            List<SegmentHandle> filesInDir = createSegmentHandlesInDirectoryRecursively(fileInfo.file(), asyncHandle);
            segmentHandles.addAll(filesInDir);
         }
      }

      Set<Path> files = fileInfos.stream()
            .map(FileInfo::file)
            .collect(Collectors.toSet());
      segmentHandles.addAll(createSegmentHandles(files, asyncHandle));
      return segmentHandles;
   }

   public List<FileFilter> getFileFilters() {
      List<String> allSuffixes = dataFormatPlugins.stream()
            .flatMap(dataFormatPlugin -> dataFormatPlugin.getMainSuffixes().stream())
            .toList();
      List<FileFilter> fileFilters = new ArrayList<>();
      fileFilters.add(new SuffixFileFilter("All Data Files", allSuffixes));
      dataFormatPlugins.stream()
            .map(dataFormatPlugin -> new SuffixFileFilter(dataFormatPlugin.getDescription(), dataFormatPlugin.getMainSuffixes()))
            .forEach(fileFilters::add);
      return fileFilters;
   }

   public void installFileFilters(JFileChooser fileChooser) {
      List<FileFilter> fileFilters = getFileFilters();
      for (FileFilter fileFilter : fileFilters) {
         fileChooser.addChoosableFileFilter(fileFilter);
      }
      fileChooser.setFileFilter(fileFilters.getFirst());
   }
}
