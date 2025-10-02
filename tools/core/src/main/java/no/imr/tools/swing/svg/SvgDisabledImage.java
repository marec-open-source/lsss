package no.imr.tools.swing.svg;

import org.jspecify.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.MultiResolutionImage;
import java.util.List;

final class SvgDisabledImage extends Image implements MultiResolutionImage {
   private final SvgImage svgImage;
   private @Nullable Image disabledImage;

   SvgDisabledImage(SvgImage svgImage) {
      this.svgImage = svgImage;
   }

   @Override
   public int getWidth(ImageObserver observer) {
      return svgImage.getWidth();
   }

   @Override
   public int getHeight(ImageObserver observer) {
      return svgImage.getHeight();
   }

   @Override
   public ImageProducer getSource() {
      return getResolutionVariant(svgImage.getWidth(), svgImage.getHeight()).getSource();
   }

   @Override
   public Graphics getGraphics() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Object getProperty(String name, ImageObserver observer) {
      return UndefinedProperty;
   }

   @Override
   public Image getResolutionVariant(double destImageWidth, double destImageHeight) {
      Image disabledImage = this.disabledImage;
      if (disabledImage != null
            && disabledImage.getWidth(null) == SvgImage.toImageSize(destImageWidth)
            && disabledImage.getHeight(null) == SvgImage.toImageSize(destImageHeight)) {
         return disabledImage;
      }
      Image image = svgImage.getResolutionVariant(destImageWidth, destImageHeight);
      Icon disabledIcon = UIManager.getLookAndFeel().getDisabledIcon(null, new ImageIcon(image));
      disabledImage = disabledIcon instanceof ImageIcon disabledImageIcon
            ? disabledImageIcon.getImage()
            : image;
      this.disabledImage = disabledImage;
      return disabledImage;
   }

   @Override
   public List<Image> getResolutionVariants() {
      return List.of(getResolutionVariant(svgImage.getWidth(), svgImage.getHeight()));
   }
}
