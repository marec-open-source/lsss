package no.imr.deepvision.lsss.engine;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.deepvision.lsss.engine.data.DeepVisionDataAdministrator;
import no.imr.deepvision.lsss.engine.data.DeepVisionFileInfo;
import no.imr.deepvision.lsss.engine.data.DeepVisionFrameInfo;
import no.imr.deepvision.lsss.engine.data.DeepVisionSelectedFrame;
import no.imr.deepvision.lsss.engine.data.Direction;
import no.imr.tools.swing.ImageComparer;
import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;

public final class ImageDiffCache {
   private final LoadingCache<DeepVisionFileInfo, CacheData> cache = CacheBuilder.newBuilder()
         .weakKeys()
         .build(CacheLoader.from(CacheData::new));
   private final Cache<ImageKey, BufferedImage> images = CacheBuilder.newBuilder()
         .maximumSize(10)
         .expireAfterAccess(10, TimeUnit.SECONDS)
         .build();
   private final ImageComparer imageComparer = new ImageComparer(100, 100);

   ImageDiffCache(DeepVisionDataAdministrator dataAdministrator) {
      dataAdministrator.getChangeManager().addListener(() -> {
         cache.invalidateAll();
         images.invalidateAll();
      });
   }

   public OptionalDouble getImageDiffIfPresent(DeepVisionFrameInfo frameInfo) {
      CacheData cacheData = cache.getUnchecked(frameInfo.deepVisionFileInfo());
      float diff = cacheData.diffs[frameInfo.frameIndex()];
      if (diff < 0) {
         return OptionalDouble.empty();
      }
      return OptionalDouble.of(diff);
   }

   public float getImageDiff(DeepVisionFrameInfo frameInfo) {
      CacheData cacheData = cache.getUnchecked(frameInfo.deepVisionFileInfo());
      float diff = cacheData.diffs[frameInfo.frameIndex()];
      if (diff < 0) {
         diff = computeDiff(frameInfo);
         cacheData.diffs[frameInfo.frameIndex()] = diff;
      }
      return diff;
   }

   private float computeDiff(DeepVisionFrameInfo frameInfo) {
      DeepVisionFrameInfo prevFrameInfo = DeepVisionSelectedFrame.nextFrameInfo(frameInfo, -1, false);
      if (prevFrameInfo == null) {
         return Float.NaN;
      }
      BufferedImage image = readImage(frameInfo);
      if (image == null) {
         return Float.NaN;
      }
      BufferedImage nextImage = readImage(prevFrameInfo);
      if (nextImage == null) {
         return Float.NaN;
      }
      synchronized (imageComparer) {
         return imageComparer.diff(image, nextImage);
      }
   }

   private @Nullable BufferedImage readImage(DeepVisionFrameInfo frameInfo) {
      ImageKey key = new ImageKey(frameInfo.deepVisionFileInfo().getFile(), frameInfo.frameIndex());
      BufferedImage image = images.getIfPresent(key);
      if (image != null) {
         return image;
      }
      try {
         image = frameInfo.deepVisionFileInfo().loadImage(frameInfo, Direction.LEFT);
         images.put(key, image);
         return image;
      } catch (IOException _) {
         return null;
      }
   }

   private record ImageKey(Path file, int index) {
   }

   private static final class CacheData {
      private final float[] diffs;

      private CacheData(DeepVisionFileInfo fileInfo) {
         diffs = new float[fileInfo.getDeepVisionFile().frames.frames.size()];
         Arrays.fill(diffs, -1);
      }
   }
}
