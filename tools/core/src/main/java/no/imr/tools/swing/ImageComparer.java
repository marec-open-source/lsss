package no.imr.tools.swing;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public final class ImageComparer {
   private final BufferedImage aScaled;
   private final BufferedImage bScaled;

   public ImageComparer(int width, int height) {
      aScaled = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      bScaled = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
   }

   public float diff(BufferedImage a, BufferedImage b) {
      byte[] aBytes = draw(a, aScaled);
      byte[] bBytes = draw(b, bScaled);

      long sum = 0;
      for (int i = 0; i < aBytes.length; i++) {
         int aValue = aBytes[i] & 0xff;
         int bValue = bBytes[i] & 0xff;
         sum += Math.abs(aValue - bValue);
      }
      return (float) sum / aBytes.length;
   }

   private static byte[] draw(BufferedImage source, BufferedImage dest) {
      Graphics2D g = dest.createGraphics();
      g.drawImage(source, 0, 0, dest.getWidth(), dest.getHeight(), null);
      g.dispose();
      return ((DataBufferByte) dest.getRaster().getDataBuffer()).getData();
   }
}
