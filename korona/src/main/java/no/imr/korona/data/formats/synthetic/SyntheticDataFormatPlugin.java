package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.parameter.Name;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@link DataFormatPlugin} for EK60 data.
 */
public final class SyntheticDataFormatPlugin extends DataFormatPlugin {
   public static final String LSSS_SS_SUFFIX = ".lsss-ss";

   private static final List<String> MAIN_SUFFIXES = List.of(LSSS_SS_SUFFIX);

   SyntheticDataFormatPlugin(Name name) {
      super(name);
   }

   @Override
   public String getDescription() {
      return "LSSS synthetic survey";
   }

   @Override
   public List<String> getMainSuffixes() {
      return MAIN_SUFFIXES;
   }

   @Override
   public List<String> getCanOpenSuffixes() {
      return MAIN_SUFFIXES;
   }

   @Override
   public SegmentHandle createSegmentHandle(Path file) throws IOException {
      return new SyntheticSegmentHandle(new SyntheticDataFile(file));
   }

   @Override
   public List<SegmentHandle> createSegmentHandles(Set<Path> files, AsyncHandle asyncHandle) throws IOException {
      List<SegmentHandle> segmentHandles = new ArrayList<>();
      for (Path file : files) {
         if (asyncHandle.isCancelled()) {
            return List.of();
         }
         if (file.toString().endsWith(LSSS_SS_SUFFIX)) {
            segmentHandles.add(createSegmentHandle(file));
         }
      }
      return segmentHandles;
   }
}
