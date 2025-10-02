package no.imr.deepvision.lsss.modules.image;

import no.imr.deepvision.lsss.engine.data.DeepVisionFrameInfo;
import no.imr.deepvision.lsss.engine.data.Direction;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

final class ImagePanel {
   private final JPanel panel = new JPanel(new GridBagLayout());
   private final ImageComponent leftImage = new ImageComponent(Direction.LEFT);
   private final ImageComponent rightImage = new ImageComponent(Direction.RIGHT);
   private final ImageLoader leftImageLoader = new ImageLoader(Direction.LEFT);
   private final ImageLoader rightImageLoader = new ImageLoader(Direction.RIGHT);
   private boolean showLeftImage = true;
   private boolean showRightImage = true;
   private boolean active;
   private @Nullable DeepVisionFrameInfo frameInfo;

   ImagePanel() {
      panel.setBackground(Color.WHITE);
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.weightx = 1;
      c.weighty = 1;
      c.gridx = 0;
      panel.add(leftImage.getComponent(), c);
      c.gridx = 1;
      panel.add(rightImage.getComponent(), c);
   }

   void updateImages(@Nullable DeepVisionFrameInfo frameInfo, boolean alwaysShowImage) {
      this.frameInfo = frameInfo;
      active = alwaysShowImage || (frameInfo != null && frameInfo.isActive());
      updateLeftImage();
      updateRightImage();
   }

   private void updateRightImage() {
      if (showRightImage) {
         rightImageLoader.load(frameInfo, active, rightImage);
      }
   }

   private void updateLeftImage() {
      if (showLeftImage) {
         leftImageLoader.load(frameInfo, active, leftImage);
      }
   }

   void setShowLeftImage(boolean showLeftImage) {
      this.showLeftImage = showLeftImage;
      leftImage.getComponent().setVisible(showLeftImage);
      updateLeftImage();
   }

   void setShowRightImage(boolean showRightImage) {
      this.showRightImage = showRightImage;
      rightImage.getComponent().setVisible(showRightImage);
      updateRightImage();
   }

   JComponent getComponent() {
      return panel;
   }
}
