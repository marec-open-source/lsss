package no.imr.korona.util.echogram;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.DiscardedChannelData;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Creates an echogram image.
 */
public final class EchogramImage {
   private final EchogramImageSettings settings;
   private final BufferedImage image;
   private final boolean[] pixelColumnHasData;
   private final int[] colorBuf;
   private int minPixelColumnUpToDate;

   public EchogramImage(EchogramImageSettings settings, BufferedImage image) {
      this.settings = settings;
      this.image = image;
      pixelColumnHasData = new boolean[image.getWidth()];
      colorBuf = new int[image.getHeight()];
      clearImage();
   }

   public EchogramImage(EchogramImageSettings settings) {
      this(settings, new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
   }

   public EchogramImage(EchogramImageSettings settings, GraphicsConfiguration graphicsConfiguration, int width, int height) {
      this(settings, graphicsConfiguration.createCompatibleImage(width, height));
   }

   public boolean processPings(List<Ping> pings) {
      int lastX = -1;
      FloatRange zRange = settings.getZSettings().getZoomedZRange();
      boolean seabedMounted = settings.getDataConfiguration().isSeabedMounted();
      int scansize = seabedMounted ? -1 : 1;
      int offset = seabedMounted ? colorBuf.length - 1 : 0;

      PingRange totalRange = settings.getPingSettings().getPingContainer().getTotalRange();
      int beginDrawableX = Math.max(settings.getPingSettings().pingIndexToXIndex(totalRange.begin()), 0);
      int endDrawableX = Math.min(settings.getPingSettings().pingIndexToXIndex(totalRange.end()), pixelColumnHasData.length);

      for (int i = pings.size() - 1; i >= 0; i--) {
         Ping ping = pings.get(i);

         int x = Math.clamp(settings.getPingSettings().pingIndexToXIndex(ping.getPingIndex()), 0, pixelColumnHasData.length - 1);
         if (!pixelColumnHasData[x]) {
            int channel = settings.getChannel(ping.getRawFileConfiguration());
            PowerData powerData = ping.getPowerData(channel);
            if (powerData != null) {
               FloatRange depthRange = settings.getZSettings().getDepthTransform().zToDepth(zRange, ping.getPingIndex());
               settings.getPingToColor().convertToColor(ping, channel, colorBuf, depthRange);
            } else {
               DiscardedChannelData discardedChannelData = ping.getPerChannelDatagram(DiscardedChannelData.class, channel);
               FloatRange depthRange = discardedChannelData != null ? discardedChannelData.getChannelData().getDepthRange() : FloatRange.EMPTY_RANGE;
               int yMin = Math.clamp(settings.getZSettings().depthToYIndex(depthRange.min(), ping.getPingIndex()), 0, colorBuf.length);
               int yMax = Math.clamp(settings.getZSettings().depthToYIndex(depthRange.max(), ping.getPingIndex()), 0, colorBuf.length);
               if (yMin > yMax) { // Upside down
                  yMin = colorBuf.length - yMin;
                  yMax = colorBuf.length - yMax;
               }
               Arrays.fill(colorBuf, 0, yMin, Color.BLACK.getRGB());
               Arrays.fill(colorBuf, yMin, yMax, Color.WHITE.getRGB());
               Arrays.fill(colorBuf, yMax, colorBuf.length, Color.BLACK.getRGB());
            }

            for (int ix = x; ix < pixelColumnHasData.length; ix++) {
               if (pixelColumnHasData[ix]) {
                  break;
               }
               if (ix == endDrawableX) {
                  Arrays.fill(colorBuf, 0, colorBuf.length, Color.BLACK.getRGB());
               }
               image.setRGB(ix, 0, 1, colorBuf.length, colorBuf, offset, scansize);
            }

            lastX = x;
            pixelColumnHasData[x] = true;
         }
      }

      if (lastX >= 0 && lastX < minPixelColumnUpToDate) {
         for (int ix = lastX - 1; ix >= 0; ix--) {
            if (ix == beginDrawableX - 1) {
               Arrays.fill(colorBuf, 0, colorBuf.length, Color.BLACK.getRGB());
            }
            image.setRGB(ix, 0, 1, colorBuf.length, colorBuf, offset, scansize);
         }

         minPixelColumnUpToDate = lastX;
      }

      return lastX >= 0;
   }

   public void draw(Graphics2D g2d) {
      g2d.drawImage(image, null, 0, 0);
   }

   public void clearData() {
      Arrays.fill(pixelColumnHasData, false);
      minPixelColumnUpToDate = Integer.MAX_VALUE;
   }

   public void clearImage() {
      GuiUtils.clearImage(image);
   }
}
