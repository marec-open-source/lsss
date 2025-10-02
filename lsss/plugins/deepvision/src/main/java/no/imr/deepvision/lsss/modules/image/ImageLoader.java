package no.imr.deepvision.lsss.modules.image;

import no.imr.deepvision.lsss.engine.data.DeepVisionFrameInfo;
import no.imr.deepvision.lsss.engine.data.Direction;
import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.swing.SwingDelayer;
import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

final class ImageLoader {
   private final Direction direction;
   private final CoalescingExecutor coalescingExecutor = new CoalescingExecutor(Exec.CACHED_THREAD_POOL);

   ImageLoader(Direction direction) {
      this.direction = direction;
   }

   void load(@Nullable DeepVisionFrameInfo frameInfo, boolean active, ImageComponent imageComponent) {
      coalescingExecutor.execute(() -> {
         Result result = loadImage(frameInfo, active);
         SwingDelayer.invokeLater(coalescingExecutor, () -> {
            imageComponent.setImage(result);
         });
      });
   }

   private Result loadImage(@Nullable DeepVisionFrameInfo frameInfo, boolean active) {
      if (frameInfo == null) {
         return new Result(null, null, List.of("No image"));
      }
      if (!active) {
         return new Result(frameInfo, null, List.of("Inactive image"));
      }
      try {
         BufferedImage image = frameInfo.deepVisionFileInfo().loadImage(frameInfo, direction);
         return new Result(frameInfo, image, null);
      } catch (IOException e) {
         return new Result(frameInfo, null, List.of(
               "Error loading frame " + frameInfo.frame().time + " from",
               "",
               frameInfo.deepVisionFileInfo().getFileContainer(frameInfo, direction).toString(),
               "",
               e.getClass().getName() + ":", e.getMessage()));
      }
   }

   record Result(@Nullable DeepVisionFrameInfo frameInfo, @Nullable BufferedImage image, @Nullable List<String> message) {
   }
}
