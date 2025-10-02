package no.imr.korona.viewer;

import no.imr.korona.data.ChannelSelector;
import no.imr.korona.data.buffer.CyclicPingList;
import no.imr.korona.data.buffer.PingBuffer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.viewer.coloring.PingToColor;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.Repaintable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * A scrollable echogram image showing sv values or categories.
 */
public final class EchogramImage {
   private final Repaintable repaintable;
   private ChannelSelector channelSelector;
   private FloatRange depthRange;
   private final PingToColor pingToColor;

   private final BufferedImage image;
   private final int[] pingRGB;
   private final CyclicPingList cyclicPingList;
   private final SerialExecutor serialExecutor = new SerialExecutor(Exec.FORK_JOIN_POOL);

   public EchogramImage(Repaintable repaintable, PingBuffer pingBuffer, ChannelSelector channelSelector, FloatRange depthRange, PingToColor pingToColor) {
      this(repaintable, pingBuffer, channelSelector, depthRange, pingToColor,
            GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration(), 1, 1);
   }

   public EchogramImage(Repaintable repaintable, PingBuffer pingBuffer, ChannelSelector channelSelector, FloatRange depthRange, PingToColor pingToColor,
                        GraphicsConfiguration deviceConfiguration, int width, int height) {
      this.repaintable = repaintable;
      this.channelSelector = channelSelector;
      this.depthRange = depthRange;
      this.pingToColor = pingToColor;

      // Negative might occur for instance when display resolution is changed.
      width = Math.max(1, width);
      height = Math.max(1, height);

      cyclicPingList = new CyclicPingList(pingBuffer, width) {
         @Override
         protected void updated(int begin, int count) {
            draw(begin, count);
         }
      };

      image = deviceConfiguration.createCompatibleImage(width, height);
      pingRGB = new int[height];
   }

   public void dispose() {
      image.flush();
   }

   public void setEndPingIndex(PingIndex endPingIndex) {
      cyclicPingList.setEndPing(endPingIndex);
   }

   private void draw(int begin, int count) {
      serialExecutor.execute(() -> doDraw(begin, count));
   }

   public void redraw() {
      serialExecutor.discardWaitingJobs();
      draw(0, cyclicPingList.getSize());
   }

   public void paint(Graphics2D g2D) {
      int w = image.getWidth();
      int h = image.getHeight();
      int x = cyclicPingList.getNext();
      g2D.drawImage(image, 0, 0, w - x, h, x, 0, w, h, null);
      g2D.drawImage(image, w - x, 0, w, h, 0, 0, x, h, null);
   }

   public void setChannel(ChannelSelector channelSelector) {
      this.channelSelector = channelSelector;
      redraw();
   }

   public void setDepthRange(FloatRange depthRange) {
      this.depthRange = depthRange;
      redraw();
   }

   private void doDraw(int begin, int count) {
      for (int i = 0; i < count; i++) {
         int index = cyclicPingList.index(begin + i);
         Ping ping = cyclicPingList.getPings().get(index);
         if (ping != null) {
            pingToColor.convertToColor(ping, channelSelector.getChannel(ping.getRawFileConfiguration()), pingRGB, depthRange);
         } else {
            Arrays.fill(pingRGB, Color.BLACK.getRGB());
         }
         image.setRGB(index, 0, 1, pingRGB.length, pingRGB, 0, 1);
      }
      repaintable.repaint();
   }
}
