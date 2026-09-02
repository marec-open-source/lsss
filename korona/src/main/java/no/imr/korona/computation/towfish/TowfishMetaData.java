package no.imr.korona.computation.towfish;

import no.imr.korona.data.DatagramSource;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.Idx0Datagram;
import no.imr.korona.data.formats.ek60.IdxCorrectionFilter;
import no.imr.tools.Min;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

final class TowfishMetaData {
   static final float WATER_DENSITY = 1024;
   static final float GRAVITY_ACCEL = 9.81f;

   private final NavigableMap<Instant, Path> metaDataFileMap = new TreeMap<>();
   private final Set<Path> metaFilesRead = new HashSet<>();

   enum DistBehindFunction {
      PYTAGORAS {
         @Override
         float distBehind(float cableLength, float depth) {
            if (cableLength < depth) {
               return 0;
            }
            return (float) Math.sqrt(cableLength * cableLength - depth * depth);
         }
      },

      STRAIGHT_DOWN {
         @Override
         float distBehind(float cableLength, float depth) {
            return 0;
         }
      };

      abstract float distBehind(float cableLength, float depth);
   }

   static final class Function<A extends Comparable<A>, B> {
      private NavigableMap<A, B> timeFloatMap = new TreeMap<>();

      private Function() {
      }

      NavigableMap<A, B> getMap() {
         return timeFloatMap;
      }

      void removeEntriesOutsideRange(A startKey, A stopKey) {
         if (timeFloatMap.isEmpty()) {
            return;
         }
         //keeping only entries with key inside the inclusive range [startKey, stopKey].
         if (timeFloatMap.firstKey().compareTo(startKey) < 0 || timeFloatMap.lastKey().compareTo(stopKey) > 0) {
            timeFloatMap = new TreeMap<>(timeFloatMap.subMap(startKey, true, stopKey, true));
         }
      }

      B getBestValue(A parameterValue) {
         Map.Entry<A, B> floorEntry = timeFloatMap.floorEntry(parameterValue);
         if (floorEntry != null) {
            return floorEntry.getValue();
         }
         Map.Entry<A, B> firstEntry = timeFloatMap.firstEntry();
         if (firstEntry != null) {
            return firstEntry.getValue();
         }
         throw new IllegalStateException("Empty");
      }
   }

   private static final class VesselLogIdxSource implements DatagramSource {
      private final List<Instant> times;
      private final List<Float> vesselDistances;
      private int index = 0;

      private VesselLogIdxSource(Map<Instant, Float> timeFloatMap) {
         times = new ArrayList<>();
         times.addAll(timeFloatMap.keySet());
         vesselDistances = new ArrayList<>();
         vesselDistances.addAll(timeFloatMap.values());
      }

      @Override
      public @Nullable BaseDatagram nextDatagram() {
         if (index >= times.size()) {
            return null;
         }
         Idx0Datagram datagram = new Idx0Datagram(times.get(index), index, vesselDistances.get(index), new GeoPoint(0, 0), 0);
         index++;
         return datagram;
      }
   }

   private final Function<Instant, Float> depthData = new Function<>();
   private final Function<Instant, Float> vesselLogData = new Function<>();
   private final Function<Instant, Float> cableLengthData = new Function<>();
   private final Function<Float, Instant> towedVehicleSailedDistToTime = new Function<>();

   interface MetadataFileReader {
      void updateMetaDataFileMap(Collection<Path> metaDataFiles, NavigableMap<Instant, Path> metaDataFileMap);

      void parseFiles(Collection<Path> metaDataFiles, Map<Instant, Float> depthMap, Function<Instant, Float> vesselLogData, Function<Instant, Float> cableLengthData);
   }

   private final MetadataFileReader metadataFileReader;

   TowfishMetaData(MetadataFileReader metadataFileReader, List<Path> metaDataFiles) {
      this.metadataFileReader = metadataFileReader;
      metadataFileReader.updateMetaDataFileMap(metaDataFiles, metaDataFileMap);
   }

   void parseRelevantMetaDataFiles(Instant startTime, Instant stopTime, DistBehindFunction distBehindFunc) throws IOException {
      List<Path> relevantFiles = new ArrayList<>();
      // add the file closest in time before start time
      Instant lastTimeBeforeStart = metaDataFileMap.firstKey();
      long closest = Long.MAX_VALUE;
      for (Map.Entry<Instant, Path> longFileEntry : metaDataFileMap.entrySet()) {
         Instant key = longFileEntry.getKey();
         long timeDiff = key.until(startTime, ChronoUnit.MILLIS);
         if (timeDiff > 0 && timeDiff < closest) {
            closest = timeDiff;
            lastTimeBeforeStart = key;
         }
      }
      relevantFiles.add(metaDataFileMap.get(lastTimeBeforeStart));
      // Add files from and including best key until time is larger than stop time.
      Instant firstTimeAfterStop = Instant.MAX;
      for (Map.Entry<Instant, Path> longFileEntry : metaDataFileMap.entrySet()) {
         Instant key = longFileEntry.getKey();
         if (key.isAfter(lastTimeBeforeStart) && key.isBefore(stopTime)) {
            relevantFiles.add(longFileEntry.getValue());
         } else if (key.isAfter(stopTime)) {
            firstTimeAfterStop = Min.of(firstTimeAfterStop, key);
         }
      }
      Set<Path> unreadRelevantFiles = new HashSet<>(relevantFiles);
      unreadRelevantFiles.removeAll(metaFilesRead);
      metadataFileReader.parseFiles(unreadRelevantFiles,
            depthData.getMap(), vesselLogData, cableLengthData);
      deleteDataOutside(lastTimeBeforeStart, firstTimeAfterStop);
      correctVesselDistances();
      computeSailedDistToTimeMap(distBehindFunc);
      metaFilesRead.clear();
      metaFilesRead.addAll(relevantFiles);
   }

   private void deleteDataOutside(Instant startTime, Instant stopTime) {
      depthData.removeEntriesOutsideRange(startTime, stopTime);
      vesselLogData.removeEntriesOutsideRange(startTime, stopTime);
      cableLengthData.removeEntriesOutsideRange(startTime, stopTime);
   }

   private void correctVesselDistances() throws IOException {
      IdxCorrectionFilter idxCorrection = new IdxCorrectionFilter(new VesselLogIdxSource(vesselLogData.getMap()));
      Map<Instant, Float> correctedVesselLogMap = vesselLogData.getMap();
      Idx0Datagram idxDatagram = idxCorrection.nextDatagram();
      while (idxDatagram != null) {
         correctedVesselLogMap.put(idxDatagram.getInstant(), (float) idxDatagram.getVesselDistance());
         idxDatagram = idxCorrection.nextDatagram();
      }
   }

   void computeSailedDistToTimeMap(DistBehindFunction distBehindFunc) {
      Map<Float, Instant> sailedDistToTimeMap = towedVehicleSailedDistToTime.getMap();
      sailedDistToTimeMap.clear();
      for (Map.Entry<Instant, Float> floatEntry : vesselLogData.getMap().entrySet()) {
         float cableLength = cableLengthData.getBestValue(floatEntry.getKey());
         float depth = depthData.getBestValue(floatEntry.getKey());
         float distBehind = distBehindFunc.distBehind(cableLength, depth);
         distBehind /= 1852; //in nautical miles
         float towfishSailedDist = floatEntry.getValue() - distBehind;
         sailedDistToTimeMap.put(towfishSailedDist, floatEntry.getKey());
      }
   }

   float getDepth(Instant towfishTime) {
      return depthData.getBestValue(towfishTime);
   }

   Instant getCorrespondingTowfishTime(Instant shipTime) {
      float sailedDist = vesselLogData.getBestValue(shipTime);
      return towedVehicleSailedDistToTime.getBestValue(sailedDist);
   }

   float getShipVesselDistance(Instant towfishTime) {
      return vesselLogData.getBestValue(towfishTime);
   }
}
