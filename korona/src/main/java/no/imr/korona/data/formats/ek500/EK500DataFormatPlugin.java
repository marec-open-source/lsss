package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.plugins.DataFormatPlugin;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;

/**
 * {@link DataFormatPlugin} for EK500 data.
 */
public final class EK500DataFormatPlugin extends DataFormatPlugin {
   static final String INFO_SUFFIX = "-Info";
   static final String PING_SUFFIX = "-Ping";
   static final String TIME_SUFFIX = "-Time";
   static final String DATA_SUFFIX = "-Data";
   static final String WORK_SUFFIX = "-Work";
   static final String SNAP_SUFFIX = "-Snap";

   public EK500DataFormatPlugin(Name name) {
      super(name, "EK500 raw file", List.of(INFO_SUFFIX));
   }

   @Override
   public @Nullable EK500SegmentHandle createSegmentHandle(Path file) throws IOException {
      EK500File ref = toEK500File(file);
      if (ref == null) {
         return null;
      }
      List<EK500File> matchingFiles = new ArrayList<>();
      for (Path f : FileUtils.listFiles(file.getParent())) {
         EK500File ek500File = toEK500File(f);
         if (ek500File != null && ek500File.nss.equals(ref.nss) && ek500File.isNear(ref.time)) {
            matchingFiles.add(ek500File);
         }
      }
      EK500FileSet ek500FileSet = toEK500FileSet(matchingFiles);
      return new EK500SegmentHandle(ek500FileSet);
   }

   @Override
   public List<SegmentHandle> createSegmentHandles(Set<Path> files, AsyncHandle asyncHandle) {
      Map<String, NavigableMap<Instant, List<EK500File>>> nssToTimeToMatchingFiles = new HashMap<>();

      for (Path file : files) {
         EK500File ek500File = toEK500File(file);
         if (ek500File == null) {
            continue;
         }
         NavigableMap<Instant, List<EK500File>> timeToMatchingFiles = nssToTimeToMatchingFiles.computeIfAbsent(ek500File.nss, _ -> new TreeMap<>());

         Map.Entry<Instant, List<EK500File>> floorEntry = timeToMatchingFiles.floorEntry(ek500File.time);
         if (floorEntry != null && ek500File.isNear(floorEntry.getKey())) {
            floorEntry.getValue().add(ek500File);
            continue;
         }

         Map.Entry<Instant, List<EK500File>> ceilingEntry = timeToMatchingFiles.ceilingEntry(ek500File.time);
         if (ceilingEntry != null && ek500File.isNear(ceilingEntry.getKey())) {
            ceilingEntry.getValue().add(ek500File);
            continue;
         }

         timeToMatchingFiles.computeIfAbsent(ek500File.time, _ -> new ArrayList<>()).add(ek500File);
      }

      return nssToTimeToMatchingFiles.values().stream()
            .flatMap(map -> map.values().stream())
            .map(EK500DataFormatPlugin::toEK500FileSet)
            .<SegmentHandle>map(EK500SegmentHandle::new)
            .toList();
   }

   private static @Nullable EK500File toEK500File(Path file) {
      Matcher matcher = EK500FileSet.PATTERN.matcher(file.getFileName().toString());
      if (!matcher.matches()) {
         return null;
      }
      String nss = matcher.group(1);
      int frequency = Integer.parseInt(matcher.group(2));
      String dateTime = matcher.group(4);
      Instant instant = EK500FileSet.DATE_TIME_FORMATTER.parse(dateTime, Instant::from);
      return new EK500File(file, nss, dateTime, frequency, instant);
   }

   private static EK500FileSet toEK500FileSet(List<EK500File> ek500Files) {
      EK500File main = ek500Files.stream()
            .min(Comparator.comparingInt(ek500File -> Math.abs(ek500File.frequency - EK500FileSet.MAIN_FREQUENCY)))
            .orElseThrow();
      List<Path> files = ek500Files.stream()
            .map(EK500File::file)
            .toList();
      return new EK500FileSet(main.nss, main.dateTime, files);
   }

   private record EK500File(
         Path file,
         String nss,
         String dateTime,
         int frequency,
         Instant time
   ) {
      private boolean isNear(Instant otherTime) {
         return Math.abs(time.toEpochMilli() - otherTime.toEpochMilli()) <= EK500FileSet.MAX_DIFF_MILLIS;
      }
   }
}
