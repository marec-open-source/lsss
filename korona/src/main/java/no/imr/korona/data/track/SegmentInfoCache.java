package no.imr.korona.data.track;

import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.io.LastModifiedAndSize;
import no.imr.tools.logging.Log;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class SegmentInfoCache {
   private static final int VERSION = 1;

   private final String dataDirId;
   private final Path cacheFile;
   private final Map<String, CacheItem> baseNameToCacheItem = new ConcurrentHashMap<>();
   private volatile boolean needSave;

   public SegmentInfoCache(List<SegmentHandle> segmentHandles, Map<Path, BasicFileAttributes> attributes, Path cacheDir, String dataDirId) {
      this.dataDirId = dataDirId;
      byte[] hash = Utils.getSha256().digest(dataDirId.getBytes(Utils.UTF_8));
      cacheFile = cacheDir.resolve(Base64.getUrlEncoder().encodeToString(hash) + ".cache");

      Map<String, SegmentHandle> baseNameToSegmentHandle = HashMap.newHashMap(segmentHandles.size());
      for (SegmentHandle segmentHandle : segmentHandles) {
         baseNameToSegmentHandle.put(segmentHandle.getBaseName(), segmentHandle);
      }

      try (DataInputStream in = new DataInputStream(FileUtils.newBufferedInputStream(cacheFile))) {
         if (in.readInt() != VERSION) {
            return;
         }
         if (!in.readUTF().equals(dataDirId)) {
            return;
         }
         int rawFileConfigurationInfoCount = in.readInt();
         List<RawFileConfigurationInfo> indexToRawFileConfigurationInfo = new ArrayList<>(rawFileConfigurationInfoCount);
         for (int i = 0; i < rawFileConfigurationInfoCount; i++) {
            indexToRawFileConfigurationInfo.add(readRawFileConfigurationInfo(in));
         }
         int cacheItemCount = in.readInt();
         for (int i = 0; i < cacheItemCount; i++) {
            CacheItem cacheItem = CacheItem.make(in, indexToRawFileConfigurationInfo, baseNameToSegmentHandle, attributes);
            if (cacheItem != null) {
               baseNameToCacheItem.put(cacheItem.baseName, cacheItem);
            }
         }
         if (in.read() >= 0) {
            // Not at end of file as expected => Discard data read.
            Log.global.log(Level.WARNING, "Unexpected data in " + cacheFile);
            baseNameToCacheItem.clear();
            needSave = true;
         } else {
            // Ok.
            needSave = baseNameToCacheItem.size() != cacheItemCount;
         }
      } catch (Exception e) {
         if (Files.exists(cacheFile)) {
            // Error while reading existing file => Discard data read.
            Log.global.log(Level.WARNING, "Error reading info about " + dataDirId + " from " + cacheFile, e);
            baseNameToCacheItem.clear();
            needSave = true;
         }
      }
   }

   private void save() throws IOException {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try (DataOutputStream out = new DataOutputStream(byteArrayOutputStream)) {
         out.writeInt(VERSION);
         out.writeUTF(dataDirId);

         Map<RawFileConfigurationInfo, Integer> rawFileConfigurationInfoToIndex = new LinkedHashMap<>();
         for (CacheItem cacheItem : baseNameToCacheItem.values()) {
            RawFileConfigurationInfo rawFileConfigurationInfo = cacheItem.segmentInfo.rawFileConfigurationInfo();
            rawFileConfigurationInfoToIndex.putIfAbsent(rawFileConfigurationInfo, rawFileConfigurationInfoToIndex.size());
         }
         out.writeInt(rawFileConfigurationInfoToIndex.size());
         for (RawFileConfigurationInfo rawFileConfigurationInfo : rawFileConfigurationInfoToIndex.keySet()) {
            writeRawFileConfigurationInfo(out, rawFileConfigurationInfo);
         }

         out.writeInt(baseNameToCacheItem.size());
         List<String> baseNames = new ArrayList<>(baseNameToCacheItem.keySet());
         baseNames.sort(null);
         for (String baseName : baseNames) {
            baseNameToCacheItem.get(baseName).write(out, rawFileConfigurationInfoToIndex);
         }
      }
      FileUtils.replaceFileSafely(cacheFile, byteArrayOutputStream.toByteArray());
   }

   public @Nullable SegmentInfo get(SegmentHandle segmentHandle) {
      CacheItem cacheItem = baseNameToCacheItem.get(segmentHandle.getBaseName());
      return cacheItem != null ? cacheItem.segmentInfo : null;
   }

   public void add(SegmentHandle segmentHandle, long lastModified, long size, SegmentInfo segmentInfo) {
      CacheItem cacheItem = new CacheItem(segmentHandle.getBaseName(), lastModified, size, segmentInfo);
      if (baseNameToCacheItem.putIfAbsent(cacheItem.baseName, cacheItem) == null) {
         needSave = true;
      }
   }

   public void saveIfNeeded() {
      if (!needSave) {
         return;
      }
      try {
         if (baseNameToCacheItem.isEmpty()) {
            Files.deleteIfExists(cacheFile);
         } else {
            save();
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error writing info about " + dataDirId + " to " + cacheFile, e);
      }
   }

   private static void writeRawFileConfigurationInfo(DataOutputStream out, RawFileConfigurationInfo rawFileConfigurationInfo) throws IOException {
      float[] frequencies = rawFileConfigurationInfo.frequencies;
      out.writeInt(frequencies.length);
      for (float frequency : frequencies) {
         out.writeFloat(frequency);
      }
   }

   private static RawFileConfigurationInfo readRawFileConfigurationInfo(DataInputStream in) throws IOException {
      float[] frequencies = new float[in.readInt()];
      for (int i = 0; i < frequencies.length; i++) {
         frequencies[i] = in.readFloat();
      }
      return RawFileConfigurationInfo.of(frequencies);
   }

   private static void writePingIndex(DataOutputStream out, PingIndex pingIndex) throws IOException {
      out.writeLong(pingIndex.getNTDate());
      out.writeLong(pingIndex.getPingNumber());
      out.writeDouble(pingIndex.getVesselDistance());
      GeoPoint geoPoint = pingIndex.getGeographicalPosition();
      out.writeDouble(geoPoint != null ? geoPoint.getLongitude() : Double.NaN);
      out.writeDouble(geoPoint != null ? geoPoint.getLatitude() : Double.NaN);
   }

   private static PingIndex readPingIndex(DataInputStream in) throws IOException {
      long ntDate = in.readLong();
      long pingNumber = in.readLong();
      double vesselDistance = in.readDouble();
      double longitude = in.readDouble();
      double latitude = in.readDouble();
      GeoPoint geoPoint = Double.isNaN(longitude) ? null : new GeoPoint(longitude, latitude);
      return new DefaultPingIndex(ntDate, pingNumber, vesselDistance, geoPoint);
   }

   private record CacheItem(String baseName, long lastModified, long size, SegmentInfo segmentInfo) {

      private static @Nullable CacheItem make(DataInputStream in,
                                              List<RawFileConfigurationInfo> indexToRawFileConfigurationInfo,
                                              Map<String, SegmentHandle> baseNameToSegmentHandle,
                                              Map<Path, BasicFileAttributes> attributes) throws IOException {
         String baseName = in.readUTF();
         long lastModified = in.readLong();
         long size = in.readLong();
         PingIndex begin = readPingIndex(in);
         PingIndex end = readPingIndex(in);
         RawFileConfigurationInfo rawFileConfigurationInfo = indexToRawFileConfigurationInfo.get(in.readInt());

         // Must read all data before possibly returning null.

         SegmentHandle segmentHandle = baseNameToSegmentHandle.get(baseName);
         if (segmentHandle == null) {
            return null;
         }
         LastModifiedAndSize lastModifiedAndSize = segmentHandle.getLastModifiedAndSize(attributes, new AsyncHandle());
         if (size != lastModifiedAndSize.size() || lastModified != lastModifiedAndSize.lastModified()) {
            return null;
         }
         SegmentInfo segmentInfo = new SegmentInfo(rawFileConfigurationInfo, PingRange.of(begin, end));
         return new CacheItem(baseName, lastModified, size, segmentInfo);
      }

      private void write(DataOutputStream out, Map<RawFileConfigurationInfo, Integer> rawFileConfigurationInfoToIndex) throws IOException {
         out.writeUTF(baseName);
         out.writeLong(lastModified);
         out.writeLong(size);
         writePingIndex(out, segmentInfo.pingRange().begin());
         writePingIndex(out, segmentInfo.pingRange().end());
         out.writeInt(rawFileConfigurationInfoToIndex.get(segmentInfo.rawFileConfigurationInfo()));
      }
   }
}
