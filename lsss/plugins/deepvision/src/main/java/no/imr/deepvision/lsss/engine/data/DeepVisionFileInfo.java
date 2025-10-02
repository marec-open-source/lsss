package no.imr.deepvision.lsss.engine.data;

import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFile;
import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFrame;
import no.imr.tools.logging.Log;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DeepVisionFileInfo {
   private final DeepVisionFile deepVisionFile;
   private final Range<Long> timeRangeMillis;
   private final Path file;
   private final String imageFileSuffix;
   private final boolean zipped;
   private final Map<Path, ZipFile> pathToZipFile;

   DeepVisionFileInfo(DeepVisionFile deepVisionFile, Path file) {
      this.deepVisionFile = deepVisionFile;
      timeRangeMillis = findTimeRangeMillis(deepVisionFile);
      this.file = file;

      String fileFormat = deepVisionFile.frames.fileformat;
      zipped = fileFormat.endsWith("-zip");
      if (zipped) {
         imageFileSuffix = fileFormat.substring(0, fileFormat.length() - 4);
         pathToZipFile = new ConcurrentHashMap<>();
      } else {
         imageFileSuffix = fileFormat;
         pathToZipFile = Map.of();
      }
   }

   private static Range<Long> findTimeRangeMillis(DeepVisionFile deepVisionFile) {
      List<DeepVisionFrame> frames = deepVisionFile.frames.frames;
      if (frames.isEmpty()) {
         return new DefaultRange<>(0L, 0L);
      }
      long begin = DeepVisionDataUtils.timeInMillis(frames.getFirst());
      long end = DeepVisionDataUtils.timeInMillis(frames.getLast());
      return new DefaultRange<>(begin, end);
   }

   public DeepVisionFile getDeepVisionFile() {
      return deepVisionFile;
   }

   public Range<Long> getTimeRangeMillis() {
      return timeRangeMillis;
   }

   public Path getFile() {
      return file;
   }

   public String getImageFileSuffix() {
      return imageFileSuffix;
   }

   public boolean isZipped() {
      return zipped;
   }

   public BufferedImage loadImage(DeepVisionFrameInfo frameInfo, Direction direction) throws IOException {
      Path fileContainer = getFileContainer(frameInfo, direction);
      if (zipped) {
         return loadImageFromZipFile(fileContainer, getFileName(frameInfo));
      } else {
         return ImageIO.read(fileContainer.toFile());
      }
   }

   public Path getFileContainer(DeepVisionFrameInfo frameInfo, Direction direction) {
      return getFileContainer(frameInfo.frame(), direction);
   }

   public Path getFileContainer(DeepVisionFrame frame, Direction direction) {
      Path dir = getImageDirectory(direction);
      if (zipped) {
         return dir.resolve(frame.folder + ".zip");
      } else {
         return dir.resolve(frame.folder).resolve(getFileName(frame));
      }
   }

   public Path getImageDirectory(Direction direction) {
      return file.resolveSibling(direction.dirName);
   }

   private BufferedImage loadImageFromZipFile(Path zipPath, String fileName) throws IOException {
      AtomicReference<IOException> ioExceptionReference = new AtomicReference<>();
      ZipFile zipFile = pathToZipFile.computeIfAbsent(zipPath, key -> {
         try {
            return new ZipFile(key.toFile());
         } catch (IOException e) {
            ioExceptionReference.set(e);
            return null;
         }
      });
      if (zipFile == null) {
         throw ioExceptionReference.get();
      }
      ZipEntry zipEntry = zipFile.getEntry(fileName);
      if (zipEntry == null) {
         throw new IOException(zipPath + " does not contain " + fileName);
      }
      try (InputStream in = zipFile.getInputStream(zipEntry)) {
         return ImageIO.read(in);
      }
   }

   public String getFileName(DeepVisionFrameInfo frameInfo) {
      return getFileName(frameInfo.frame());
   }

   public String getFileName(DeepVisionFrame frame) {
      return frame.time + "." + imageFileSuffix;
   }

   public void close() {
      pathToZipFile.values().forEach(zipFile -> {
         try {
            zipFile.close();
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error closing " + zipFile.getName(), e);
         }
      });
   }
}
