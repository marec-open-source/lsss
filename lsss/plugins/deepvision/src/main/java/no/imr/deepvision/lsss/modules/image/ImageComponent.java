package no.imr.deepvision.lsss.modules.image;

import no.imr.deepvision.lsss.engine.data.DeepVisionFrameInfo;
import no.imr.deepvision.lsss.engine.data.Direction;
import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFramework;
import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFrameworkObject;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.UiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

final class ImageComponent {
   private final Direction direction;
   private final JComponent component = new JComponent() {
      @Override
      protected void paintComponent(Graphics g) {
         draw((Graphics2D) g);
      }
   };
   private ImageLoader.Result result = new ImageLoader.Result(null, null, null);
   private float imageAspect = 1;

   ImageComponent(Direction direction) {
      this.direction = direction;
   }

   JComponent getComponent() {
      return component;
   }

   void setImage(ImageLoader.Result result) {
      this.result = result;
      component.repaint();
   }

   private void draw(Graphics2D g) {
      List<String> message = result.message();
      if (message != null) {
         drawText(g, message);
         return;
      }
      BufferedImage image = result.image();
      if (image == null) {
         return;
      }
      imageAspect = (float) image.getWidth() / image.getHeight();
      float xScale = (float) component.getWidth() / image.getWidth();
      float yScale = (float) component.getHeight() / image.getHeight();
      float scale = Math.min(xScale, yScale);
      g.scale(scale, scale);
      g.drawImage(image, null, 0, 0);

      DeepVisionFrameInfo frameInfo = result.frameInfo();
      if (frameInfo != null) {
         for (DeepVisionFramework framework : frameInfo.frame().frameworks) {
            drawFramework(g, framework);
         }
      }
   }

   private void drawText(Graphics2D g, List<String> lines) {
      float componentAspect = (float) component.getWidth() / component.getHeight();
      float width = Math.min(1, imageAspect / componentAspect) * component.getWidth();
      float height = Math.min(1, componentAspect / imageAspect) * component.getHeight();
      g.setColor(Color.DARK_GRAY);
      g.fill(new Rectangle2D.Float(0, 0, width, height));
      g.setColor(ColorUtils.WHITE);
      int fontSize = 14;
      g.setFont(UiUtils.labelFont().deriveFont(Font.BOLD, fontSize));
      float y = (height - fontSize * (lines.size() - 1)) / 2;
      for (int i = 0; i < lines.size(); i++) {
         GuiText.draw(g, lines.get(i), 10, y + i * fontSize);
      }
   }

   private void drawFramework(Graphics2D g, DeepVisionFramework framework) {
      for (DeepVisionFrameworkObject object : framework.objects) {
         DeepVisionFrameworkObject.BoundingBox bb = boundingBox(object, direction);
         if (bb != null) {
            g.setColor(Color.GRAY);
            g.draw(new Rectangle2D.Float(bb.x0, bb.y0, bb.x1 - bb.x0, bb.y1 - bb.y0));
         }
      }
   }

   private static DeepVisionFrameworkObject.@Nullable BoundingBox boundingBox(DeepVisionFrameworkObject object, Direction direction) {
      return switch (direction) {
         case LEFT -> object.lbb;
         case RIGHT -> object.rbb;
      };
   }
}
