package no.imr.deepvision.lsss.engine.data;

import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFrame;

public record DeepVisionFrameInfo(
      DeepVisionFileInfo deepVisionFileInfo,
      DeepVisionFrame frame,
      int frameIndex
) {
   public DeepVisionFrameInfo(DeepVisionFileInfo deepVisionFileInfo, int frameIndex) {
      this(
            deepVisionFileInfo,
            deepVisionFileInfo.getDeepVisionFile().frames.frames.get(frameIndex),
            frameIndex
      );
   }

   public boolean isActive() {
      return frame.active;
   }
}
