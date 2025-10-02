package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

/**
 * {@link DataFormatPlugin} for EK60 data.
 */
public final class EK60DataFormatPlugin extends DataFormatPlugin {
   public static final String RAW_SUFFIX = ".raw";
   public static final String IDX_SUFFIX = ".idx";
   public static final String BOT_SUFFIX = ".bot";
   static final String XYZ_SUFFIX = ".xyz";

   private static final List<String> MAIN_SUFFIXES = List.of(RAW_SUFFIX);
   private static final List<String> CAN_OPEN_SUFFIXES = List.of(RAW_SUFFIX, IDX_SUFFIX, BOT_SUFFIX);

   private final DatagramTypeManager datagramTypeManager;

   public EK60DataFormatPlugin(Name name, DatagramTypeManager datagramTypeManager) {
      super(name);

      this.datagramTypeManager = datagramTypeManager;
   }

   @Override
   public String getDescription() {
      return "EK60 raw file";
   }

   @Override
   public List<String> getMainSuffixes() {
      return MAIN_SUFFIXES;
   }

   @Override
   public List<String> getCanOpenSuffixes() {
      return CAN_OPEN_SUFFIXES;
   }

   @Override
   public SegmentHandle createSegmentHandle(Path file) {
      return new EK60SegmentHandle(new EK60FileSet(file), datagramTypeManager);
   }

   @Override
   public List<SegmentHandle> createSegmentHandles(Set<Path> files, AsyncHandle asyncHandle) {
      NavigableSet<String> xyzFiles = XyzUtils.toXyzFiles(files);

      Set<Path> usedFiles = HashSet.newHashSet(files.size());
      List<SegmentHandle> segmentHandles = new ArrayList<>();
      for (Path file : files) {
         if (usedFiles.contains(file)) {
            continue;
         }
         if (asyncHandle.isCancelled()) {
            return List.of();
         }
         String path = file.toString();
         if (path.endsWith(RAW_SUFFIX)) {
            SegmentHandle segmentHandle = new EK60SegmentHandle(new EK60FileSet(file, xyzFiles), datagramTypeManager);
            segmentHandles.add(segmentHandle);
            usedFiles.addAll(segmentHandle.getFiles());
         }
      }
      return segmentHandles;
   }
}
