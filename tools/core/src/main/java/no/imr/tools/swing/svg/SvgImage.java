package no.imr.tools.swing.svg;

import no.imr.tools.ResourceUtils;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.MultiResolutionImage;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

public final class SvgImage extends Image implements MultiResolutionImage {
   private final String resource;
   private int width;
   private int height;
   private @Nullable SvgContent svgContent;
   private @Nullable BufferedImage image;

   public SvgImage(String resource) {
      this.resource = resource;
   }

   public SvgImage(String resource, int size) {
      this.resource = resource;
      width = size;
      height = size;
   }

   public SvgImage(Path file, int size) {
      resource = file.toUri().toString();
      width = size;
      height = size;
   }

   SvgImage withSize(int size) {
      return new SvgImage(resource, size);
   }

   private void initSize() {
      if (width != 0) {
         return;
      }
      SvgContent svgContent = getSvgContent();
      width = (int) svgContent.getWidth();
      height = (int) svgContent.getHeight();
   }

   private SvgContent getSvgContent() {
      SvgContent svgContent = this.svgContent;
      if (svgContent == null) {
         svgContent = loadSvgContent(resource);
         this.svgContent = svgContent;
      }
      return svgContent;
   }

   private static SvgContent loadSvgContent(String resource) {
      if (resource.startsWith("file:") || resource.startsWith("jar:")) {
         try {
            return new SvgContent(Utils.toURL(resource));
         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Error loading svg from " + resource + ": " + e);
            return new SvgContent(ResourceUtils.getUrl("no/imr/tools/resources/images/icons/Empty.svg"));
         }
      }
      return new SvgContent(ResourceUtils.getUrl(resource));
   }

   public int getWidth() {
      initSize();
      return width;
   }

   @Override
   public int getWidth(@Nullable ImageObserver observer) {
      return getWidth();
   }

   public int getHeight() {
      initSize();
      return height;
   }

   @Override
   public int getHeight(@Nullable ImageObserver observer) {
      return getHeight();
   }

   @Override
   public ImageProducer getSource() {
      return getResolutionVariant(getWidth(), getHeight()).getSource();
   }

   @Override
   public Graphics getGraphics() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Object getProperty(String name, @Nullable ImageObserver observer) {
      return UndefinedProperty;
   }

   @Override
   public Image getResolutionVariant(double destImageWidth, double destImageHeight) {
      BufferedImage image = this.image;
      if (image != null
            && image.getWidth() == toImageSize(destImageWidth)
            && image.getHeight() == toImageSize(destImageHeight)) {
         return image;
      }
      image = getSvgContent().toImage(destImageWidth, destImageHeight);
      this.image = image;
      svgContent = null;
      return image;
   }

   @Override
   public List<Image> getResolutionVariants() {
      return List.of(getResolutionVariant(getWidth(), getHeight()));
   }

   static int toImageSize(double fractionalSize) {
      return Math.max(1, (int) Math.round(fractionalSize));
   }
}
