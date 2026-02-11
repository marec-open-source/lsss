package no.imr.deepvision.lsss.modules.image;

import no.imr.deepvision.lsss.engine.DeepVisionEngine;
import no.imr.deepvision.lsss.engine.ImageDiffCache;
import no.imr.deepvision.lsss.engine.data.DeepVisionFrameInfo;
import no.imr.deepvision.lsss.engine.data.DeepVisionSelectedFrame;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;
import no.imr.tools.time.Stopwatch;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerListModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

final class DeepVisionImageView extends BaseViewModule.BaseView {
   private final DeepVisionImageViewModule module;
   private final DeepVisionSelectedFrame deepVisionSelectedFrame;
   private final ImagePanel imagePanel;
   private final JLabel label = new JLabel("Empty");
   private final JPanel panel = new JPanel(new BorderLayout());
   private final List<PlayButton> playButtons;
   private final JSpinner imagesPerSecondSpinner = new JSpinner(new SpinnerListModel(List.<Number>of(
         0.01f, 0.02f, 0.03f, 0.06f, 0.12f, 0.25f, 0.33f, 0.5f, 0.67f, 1,
         1.5f, 2, 3, 4, 6, 10, 15, 20, 25, 50, 60, 75, 100, 150, 200, 250, 300, 400, 500, 600, 750, 999
   )));
   private int playDelay;
   private @Nullable Timer playTimer;

   DeepVisionImageView(DeepVisionImageViewModule module) {
      super(module);

      this.module = module;
      DeepVisionEngine deepVisionEngine = module.getDeepVisionEngine();
      deepVisionSelectedFrame = deepVisionEngine.getDeepVisionSelectedFrame();

      imagePanel = new ImagePanel();

      GuiListeners.coalescingLater(this::updateVisibility).addToAndNotify(
            module.showLeftImage,
            module.showRightImage
      );

      GuiListeners.coalescingLater(this::update).addToAndNotify(
            module.showOnlyActiveImages,
            deepVisionSelectedFrame.getChangeManager()
      );

      Insets margin = new Insets(0, 0, 0, 0);

      PlayButton playBackwards = new PlayButton(MiscIcons.PLAY_BACKWARD, -1);
      PlayButton playForwards = new PlayButton(MiscIcons.PLAY, 1);
      playBackwards.button.setMargin(margin);
      playForwards.button.setMargin(margin);
      playBackwards.button.setToolTipText("Play backward");
      playForwards.button.setToolTipText("Play forward");
      playButtons = List.of(playBackwards, playForwards);

      NavigationButton navigatePrev = new NavigationButton(MiscIcons.STEP_BACK, -1);
      NavigationButton navigateNext = new NavigationButton(MiscIcons.STEP_FORWARD, 1);
      navigatePrev.button.setToolTipText("<html>Step backward to previous image.<br>Use Ctrl + Click to search.");
      navigateNext.button.setToolTipText("<html>Step forward to next image.<br>Use Ctrl + Click to search.");
      navigatePrev.button.setMargin(margin);
      navigateNext.button.setMargin(margin);

      imagesPerSecondSpinner.setToolTipText("Images per second");
      imagesPerSecondSpinner.setPreferredSize(new Dimension(42, 24));
      imagesPerSecondSpinner.setValue(10);
      imagesPerSecondSpinner.addChangeListener(_ -> updatePlayDelay());
      updatePlayDelay();

      JPanel navigationButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
      navigationButtons.setBackground(Color.WHITE);
      navigationButtons.add(playBackwards.button);
      navigationButtons.add(playForwards.button);
      navigationButtons.add(imagesPerSecondSpinner);
      navigationButtons.add(navigatePrev.button);
      navigationButtons.add(navigateNext.button);

      panel.add(label, BorderLayout.NORTH);
      panel.add(imagePanel.getComponent());
      panel.add(navigationButtons, BorderLayout.SOUTH);
      panel.setBackground(Color.WHITE);
      panel.addMouseWheelListener(e -> gotoFrame(e.getWheelRotation()));
   }

   private void updatePlayDelay() {
      double imgPerSec = ((Number) imagesPerSecondSpinner.getValue()).doubleValue();
      playDelay = (int) Math.round(1000 / imgPerSec);
      if (playTimer != null) {
         playTimer.setDelay(playDelay);
      }
   }

   private void startPlay(int step, int initialDelay) {
      stopPlay();
      playTimer = new Timer(playDelay, _ -> gotoFrame(step));
      playTimer.setInitialDelay(initialDelay);
      playTimer.start();
   }

   private void stopPlay() {
      if (playTimer != null) {
         playTimer.stop();
         playTimer = null;
      }
   }

   private void gotoFrame(int step) {
      deepVisionSelectedFrame.setFrameOffset(step, module.showOnlyActiveImages.getBooleanValue());
   }

   private List<DeepVisionFrameInfo> candidateFrameInfos(int step) {
      DeepVisionFrameInfo frameInfo = deepVisionSelectedFrame.getSelectedFrame();
      if (frameInfo == null) {
         return List.of();
      }
      List<DeepVisionFrameInfo> frameInfos = new ArrayList<>();
      while (true) {
         frameInfo = DeepVisionSelectedFrame.nextFrameInfo(frameInfo, step, module.showOnlyActiveImages.getBooleanValue());
         if (frameInfo == null) {
            return frameInfos;
         }
         frameInfos.add(frameInfo);
      }
   }

   private void gotoInterestingFrame(int step) {
      List<DeepVisionFrameInfo> frameInfos = candidateFrameInfos(step);
      if (frameInfos.isEmpty()) {
         return;
      }
      ProgressView progressView = new ProgressView("Searching for left image with significant changes...", frameInfos.size())
            .useSecondaryLabel()
            .showRemainingTime();
      new WorkerDialog(getComponent(), progressView.getComponent())
            .start(asyncHandle -> {
               ImageDiffCache imageDiffCache = module.getDeepVisionEngine().getImageDiffCache();
               Stopwatch stopwatch = Stopwatch.createStarted();
               List<Future<@Nullable Float>> futures = frameInfos.stream()
                     .map(info -> Exec.LONG_RUNNING_THREAD_POOL.submit(() -> {
                        if (asyncHandle.isCancelled()) {
                           return null;
                        }
                        return imageDiffCache.getImageDiff(info);
                     }))
                     .toList();
               for (int i = 0; i < frameInfos.size(); i++) {
                  Float diff = Utils.awaitFuture(futures.get(i));
                  if (diff == null) {
                     return;
                  }
                  float thresholdDiff = 3;
                  if (diff >= thresholdDiff) {
                     int j = step < 0 && i + 1 < frameInfos.size() ? i + 1 : i;
                     deepVisionSelectedFrame.setSelectedFrame(frameInfos.get(j));
                     futures.forEach(future -> future.cancel(true));
                     return;
                  }
                  if (stopwatch.seconds() > 2) {
                     stopwatch.restart();
                     deepVisionSelectedFrame.setSelectedFrame(frameInfos.get(i));
                  }
                  progressView.setMainProgress(i + 1, Utils.format("Image diff: %.2f < " + Utils.toString(thresholdDiff), diff));
               }
               // Found nothing.
               deepVisionSelectedFrame.setSelectedFrame(frameInfos.getLast());
            });
   }

   private void updateVisibility() {
      imagePanel.setShowLeftImage(module.showLeftImage.getBooleanValue());
      imagePanel.setShowRightImage(module.showRightImage.getBooleanValue());
   }

   private void update() {
      DeepVisionFrameInfo frameInfo = deepVisionSelectedFrame.getSelectedFrame();
      if (frameInfo == null) {
         label.setText("Empty");
      } else {
         label.setText(frameInfo.deepVisionFileInfo().getFileName(frameInfo));
      }
      imagePanel.updateImages(frameInfo, !module.showOnlyActiveImages.getBooleanValue());
   }

   @Override
   public JComponent getComponent() {
      return panel;
   }

   private final class NavigationButton extends MouseAdapter {
      private final int step;
      private final JButton button;

      private NavigationButton(SvgIcon icon, int step) {
         button = icon.on(new JButton());
         this.step = step;
         button.addMouseListener(this);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
         if (e.isControlDown()) {
            gotoInterestingFrame(step);
         } else {
            gotoFrame(step);
         }
      }

      @Override
      public void mousePressed(MouseEvent e) {
         for (PlayButton playButton : playButtons) {
            playButton.setPlaying(false);
         }
         startPlay(step, 200);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         stopPlay();
      }
   }

   private final class PlayButton {
      private final SvgIcon playIcon;
      private final int step;
      private final JToggleButton button;
      private boolean playing;

      private PlayButton(SvgIcon playIcon, int step) {
         this.playIcon = playIcon;
         this.step = step;
         button = playIcon.on(new JToggleButton());
         button.addActionListener(e -> {
            if ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0) {
               for (PlayButton playButton : playButtons) {
                  playButton.setPlaying(false);
               }
               gotoInterestingFrame(step);
               button.setSelected(false);
            } else {
               setPlaying(!playing);
            }
         });
      }

      private void setPlaying(boolean playing) {
         if (this.playing == playing) {
            return;
         }
         this.playing = playing;
         button.setSelected(playing);
         if (playing) {
            for (PlayButton playButton : playButtons) {
               if (playButton != this) {
                  playButton.setPlaying(false);
               }
            }
            MiscIcons.PAUSE.on(button);
            startPlay(step, 0);
         } else {
            playIcon.on(button);
            stopPlay();
         }
      }
   }
}
