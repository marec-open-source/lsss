package no.imr.deepvision.lsss.engine.data;

import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionEvent;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.listening.ChangeManager;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class DeepVisionSelectedFrame {
   private final InterpretationSettings interpretationSettings;
   private @Nullable DeepVisionFrameInfo selectedFrame;
   private final ChangeManager changeManager = new ChangeManager();

   public DeepVisionSelectedFrame(LSSS lsss, DeepVisionDataAdministrator dataAdministrator) {
      interpretationSettings = lsss.getInterpretationSettings();
      dataAdministrator.getChangeManager().addListener(fileInfos -> {
         setSelectedFrame(findFirstFrameInfo(fileInfos));
      });
   }

   private static @Nullable DeepVisionFrameInfo findFirstFrameInfo(List<DeepVisionFileInfo> fileInfos) {
      if (fileInfos.isEmpty()) {
         return null;
      }
      return new DeepVisionFrameInfo(fileInfos.getFirst(), 0);
   }

   public @Nullable DeepVisionFrameInfo getSelectedFrame() {
      return selectedFrame;
   }

   public void setFrameOffset(int offset, boolean onlyActiveFrames) {
      if (selectedFrame == null) {
         return;
      }
      DeepVisionFrameInfo frameInfo = nextFrameInfo(selectedFrame, offset, onlyActiveFrames);
      if (frameInfo != null) {
         setSelectedFrame(frameInfo);
      }
   }

   public static @Nullable DeepVisionFrameInfo nextFrameInfo(DeepVisionFrameInfo selectedFrame, int offset, boolean onlyActiveFrames) {
      int newFrameIndex = Math.clamp(selectedFrame.frameIndex() + offset, 0, selectedFrame.deepVisionFileInfo().getDeepVisionFile().frames.frames.size() - 1);
      if (newFrameIndex == selectedFrame.frameIndex()) {
         return null;
      }
      DeepVisionFrameInfo frameInfo = new DeepVisionFrameInfo(selectedFrame.deepVisionFileInfo(), newFrameIndex);
      if (onlyActiveFrames && !frameInfo.isActive()) {
         return nextActiveFrameInfo(selectedFrame, offset);
      }
      return frameInfo;
   }

   private static @Nullable DeepVisionFrameInfo nextActiveFrameInfo(DeepVisionFrameInfo selectedFrame, int direction) {
      int step = direction > 0 ? 1 : -1;
      for (int i = selectedFrame.frameIndex() + step; direction > 0 ? i < selectedFrame.deepVisionFileInfo().getDeepVisionFile().frames.frames.size() : i >= 0; i += step) {
         DeepVisionFrameInfo deepVisionFrameInfo = new DeepVisionFrameInfo(selectedFrame.deepVisionFileInfo(), i);
         if (deepVisionFrameInfo.isActive()) {
            return deepVisionFrameInfo;
         }
      }
      return null;
   }

   public void setSelectedFrame(@Nullable DeepVisionFrameInfo selectedFrame) {
      this.selectedFrame = selectedFrame;
      changeManager.notifyListeners();
      interpretationSettings.sendEvent("deepVisionFrame", createDeepVisionEvent(selectedFrame));
   }

   private static DeepVisionEvent createDeepVisionEvent(@Nullable DeepVisionFrameInfo frameInfo) {
      DeepVisionEvent deepVisionEvent = new DeepVisionEvent();
      if (frameInfo == null) {
         return deepVisionEvent;
      }
      deepVisionEvent.deepVisionFile = frameInfo.deepVisionFileInfo().getFile().toString();
      deepVisionEvent.imageFolder = frameInfo.frame().folder;
      deepVisionEvent.time = frameInfo.frame().time;
      return deepVisionEvent;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }
}
