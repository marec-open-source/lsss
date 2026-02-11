package no.imr.korona.viewer;

import no.imr.korona.computation.feature.CategoryVisualizer;
import no.imr.korona.computation.feature.EchogramWindow;
import no.imr.korona.data.KoronaRegion;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.data.datagrams.Cat0Datagram;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.datagrams.RegionBorderDatagram;
import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datagrams.TBR0Datagram;
import no.imr.korona.data.datagrams.TNF0Datagram;
import no.imr.korona.data.datagrams.subdatagrams.ts.TsDatagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.tools.Utils;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.IntRange;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Handles display of echogram.
 */
public final class EchogramPanel {
   private static final List<Color> REGION_COLORS = List.of(Color.RED, Color.GREEN, Color.BLUE);

   private final EchogramPanelComponent component = new EchogramPanelComponent();

   private int lastDepthY;
   private int lastMaxDepthY;

   private final Map<Float, boolean[]> lastRegionInteriors = new HashMap<>();
   private final Map<Float, Color> thresholdToRegionColor = new HashMap<>();
   private final Map<Integer, TBR0Datagram> lastTrackDepths = new HashMap<>();
   private final Map<Integer, FloatRange> combinedTrackDepths = new HashMap<>();

   private BufferedImage drawImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
   private VolatileImage backImage = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleVolatileImage(1, 1);

   private ColorConverterContainer colorConverterContainer;
   private final ColorConverterContainer publicColorConverterContainer;
   private final ColorConverterContainer privateColorConverterContainer;

   private final EchogramColorPanel echogramColorPanel;

   private PingInfo[] pingInfos = new PingInfo[1];

   private int lastAddedPing;
   private int lastDisplayPixel;
   private int lastCopyPixel;
   private int pixelsToCopy;

   private final DepthRangeChooser depthRangeChooser;

   /**
    * Color for one pixel.
    */
   private int[] rgbArray = new int[1];

   private String kHzText = "";
   private String channelIdText = "";
   private final int channel;

   private boolean showText = true;
   private boolean showDepthMarkers = true;
   private boolean showRegions = false;
   private boolean showTracks = false;
   private boolean showTargets = false;

   private final List<Consumer<JPopupMenu>> popupMenuExtenders = new ArrayList<>();

   EchogramPanel(ColorConverterContainer publicColorConverterContainer, ColorConverterContainer privateColorConverterContainer,
                 EchogramColorPanel echogramColorPanel, DepthRangeChooser depthRangeChooser, PingConfiguration pingConfiguration, int channel) {
      this.publicColorConverterContainer = publicColorConverterContainer;
      this.privateColorConverterContainer = privateColorConverterContainer;
      colorConverterContainer = publicColorConverterContainer;
      this.echogramColorPanel = echogramColorPanel;
      this.depthRangeChooser = depthRangeChooser;
      this.channel = channel;
      setPingConfiguration(pingConfiguration);

      component.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            setNewSize((Graphics2D) component.getGraphics(), component.getWidth(), component.getHeight());
         }
      });
   }

   public JComponent getComponent() {
      return component;
   }

   public int getChannel() {
      return channel;
   }

   public void installDefaultListener(KoronaPlaybox koronaPlaybox) {
      MyListener listener = new MyListener(koronaPlaybox);
      component.addMouseListener(listener);
      component.addMouseMotionListener(listener);
   }

   public void addPopupMenuExtender(Consumer<JPopupMenu> popupMenuExtender) {
      popupMenuExtenders.add(popupMenuExtender);
   }

   public void setPingConfiguration(PingConfiguration pingConfiguration) {
      RawFileTransducer transducer = pingConfiguration.getRawFileConfiguration().getTransducers().get(channel - 1);
      kHzText = transducer.getKHz() + " kHz";
      channelIdText = transducer.getChannelId();
      resetDraw();
      lastRegionInteriors.clear();
      thresholdToRegionColor.clear();
      lastTrackDepths.clear();
      combinedTrackDepths.clear();

      GuiUtils.clearImage(drawImage);
      GuiUtils.clearImage(backImage);
   }

   private JPopupMenu makePopupMenu() {
      JPopupMenu popupMenu = new JPopupMenu();

      JMenuItem textMenuItem = MiscIcons.check(showText).on(popupMenu.add("Show texts"));
      textMenuItem.addActionListener(_ -> {
         showText = !showText;
         repaint();
      });

      JMenuItem showDepthMenuItem = MiscIcons.check(showDepthMarkers).on(popupMenu.add("Show depth markers"));
      showDepthMenuItem.addActionListener(_ -> showDepthMarkers = !showDepthMarkers);

      JMenuItem showRegionsMenuItem = MiscIcons.check(showRegions).on(popupMenu.add("Show regions"));
      showRegionsMenuItem.addActionListener(_ -> showRegions = !showRegions);

      JMenuItem showTracksMenuItem = MiscIcons.check(showTracks).on(popupMenu.add("Show tracks"));
      showTracksMenuItem.addActionListener(_ -> showTracks = !showTracks);

      JMenuItem showTargetsMenuItem = MiscIcons.check(showTargets).on(popupMenu.add("Show targets"));
      showTargetsMenuItem.addActionListener(_ -> showTargets = !showTargets);

      JMenuItem privateColorMenu = MiscIcons.check(getUsePrivateColor()).on(popupMenu.add("Use private colormap"));
      privateColorMenu.addActionListener(_ -> setUsePrivateColor(!getUsePrivateColor()));

      for (Consumer<JPopupMenu> popupMenuExtender : popupMenuExtenders) {
         popupMenuExtender.accept(popupMenu);
      }

      return popupMenu;
   }

   private final class MyListener extends MouseAdapter {
      private final KoronaPlaybox koronaPlaybox;

      private int x1;
      private int y1;
      private int x2;
      private int y2;

      private MyListener(KoronaPlaybox koronaPlaybox) {
         this.koronaPlaybox = koronaPlaybox;
      }

      @Override
      public void mousePressed(MouseEvent e) {
         if (SwingUtilities.isLeftMouseButton(e) && lastAddedPing != 0) {
            koronaPlaybox.stop();
            startDraw(e.getX(), e.getY());
         }

         maybeShowPopup(e);
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         if (SwingUtilities.isLeftMouseButton(e) && lastAddedPing != 0) {
            contDraw(e.getX(), e.getY());
         }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         if (SwingUtilities.isLeftMouseButton(e) && lastAddedPing != 0) {
            endDraw();
         }
         maybeShowPopup(e);
      }

      private void maybeShowPopup(MouseEvent e) {
         if (e.isPopupTrigger()) {
            makePopupMenu().show(e.getComponent(), e.getX(), e.getY());
         }
      }

      private void startDraw(int x, int y) {
         x1 = x2 = clampX(x);
         y1 = y2 = clampY(y);
      }

      private void contDraw(int x, int y) {
         // Erase
         draw(x1, y1, x2, y2);
         x2 = clampX(x);
         y2 = clampY(y);
         // Set
         draw(x1, y1, x2, y2);
      }

      private int clampX(int x) {
         int minX = Math.max(0, getWidth() - lastAddedPing);
         return Math.clamp(x, minX, getWidth() - 1);
      }

      private int clampY(int y) {
         return Math.clamp(y, 0, getHeight() - 1);
      }

      private void endDraw() {
         // Erase
         draw(x1, y1, x2, y2);

         // Do not draw to small
         int dx = Math.abs(x1 - x2);
         int dy = Math.abs(y1 - y2);
         if (dx * dy < 16) {
            JOptionPane.showMessageDialog(component, "Too few samples. Please draw a larger rectangle.", "Too little data", JOptionPane.ERROR_MESSAGE);
            return;
         }

         if (dx * dy > 50000) {
            JOptionPane.showMessageDialog(component, "Too many samples to handle", "Too much data", JOptionPane.ERROR_MESSAGE);
            return;
         }

         PingInfo pingInfo1 = getPingInfo(x1);
         PingInfo pingInfo2 = getPingInfo(x2);

         int ping1 = pingInfo1.pingIndex();
         int ping2 = pingInfo2.pingIndex();

         float depth1 = pingInfo1.getDepth(y1, getHeight());
         float depth2 = pingInfo2.getDepth(y2, getHeight());

         KoronaPlayboxProcessing processing = koronaPlaybox.getProcessing();
         if (processing == null) {
            JOptionPane.showMessageDialog(component, "No data", "No data", JOptionPane.ERROR_MESSAGE);
            return;
         }
         try {
            EchogramWindow echogramWindow = processing.tmpFileWriter().createEchogramWindow(processing.computation(),
                  new IntRange(Math.min(ping1, ping2), Math.max(ping1, ping2) + 1),
                  FloatRange.ofUnsorted(depth1, depth2));
            new CategoryVisualizer(echogramWindow, koronaPlaybox.getConfigFileSettings(), koronaPlaybox.getComponent(), Dialog.ModalityType.MODELESS);
         } catch (IOException e) {
            GuiUtils.showErrorDialog(component, "Error showing feature visualization", e);
         }
      }

      private void draw(int x1, int y1, int x2, int y2) {
         int x = Math.min(x1, x2);
         int w = Math.abs(x1 - x2);
         int y = Math.min(y1, y2);
         int h = Math.abs(y1 - y2);
         if (w != 0 || h != 0) {
            Graphics g = component.getGraphics();
            g.setXORMode(Color.WHITE);
            g.drawRect(x, y, w, h);
         }
      }
   }

   private void repaint() {
      component.repaint();
   }

   public int getWidth() {
      return component.getWidth();
   }

   public int getHeight() {
      return component.getHeight();
   }

   private final class EchogramPanelComponent extends JComponent {
      private EchogramPanelComponent() {
      }

      @Override
      protected void paintComponent(Graphics g) {
         privatePaint((Graphics2D) g);
      }
   }

   private void privatePaint(Graphics2D g) {
      drawBackImage(g);
      drawTexts(g);
   }

   /**
    * Set new size for panel.
    *
    * @param g      graphics
    * @param width  width
    * @param height height
    */
   private synchronized void setNewSize(Graphics2D g, int width, int height) {
      width = Math.max(1, width);
      height = Math.max(1, height);

      GraphicsConfiguration graphConf = g.getDeviceConfiguration();
      drawImage = graphConf.createCompatibleImage(width, height);
      backImage = graphConf.createCompatibleVolatileImage(width, height);

      pingInfos = new PingInfo[width];
      rgbArray = new int[height];

      resetDraw();
      repaint();
   }

   public void resetDraw() {
      lastDepthY = -1;
      lastMaxDepthY = -1;
      lastDisplayPixel = 0;
      lastCopyPixel = 0;
      pixelsToCopy = 0;
      lastAddedPing = 0;
      Arrays.fill(pingInfos, null);
   }

   private synchronized void copyToBackImage() {
      if (pixelsToCopy == 0) {
         return;
      }

      Graphics2D g = backImage.createGraphics();

      int w = drawImage.getWidth();
      int h = drawImage.getHeight();

      if (pixelsToCopy >= w) {
         g.drawImage(drawImage, null, 0, 0);
      } else {
         if (lastDisplayPixel > lastCopyPixel) {
            g.drawImage(drawImage, lastCopyPixel, 0, lastDisplayPixel, h, lastCopyPixel, 0, lastDisplayPixel, h, null);
         } else {
            g.drawImage(drawImage, lastCopyPixel, 0, w, h, lastCopyPixel, 0, w, h, null);
            g.drawImage(drawImage, 0, 0, lastDisplayPixel, h, 0, 0, lastDisplayPixel, h, null);
         }
      }

      g.dispose();

      lastCopyPixel = lastDisplayPixel;
      pixelsToCopy = 0;
   }

   private void restoreBackImage() {
      Graphics2D g = backImage.createGraphics();
      g.drawImage(drawImage, null, 0, 0);
      g.dispose();
   }

   private void drawBackImage(Graphics2D g) {
      copyToBackImage();

      do {
         int returnCode = backImage.validate(g.getDeviceConfiguration());
         if (returnCode == VolatileImage.IMAGE_RESTORED) {
            // Contents need to be restored
            restoreBackImage();
         } else if (returnCode == VolatileImage.IMAGE_INCOMPATIBLE) {
            // old vImg doesn't work with new GraphicsConfig; re-create it
            backImage = g.getDeviceConfiguration().createCompatibleVolatileImage(backImage.getWidth(), backImage.getHeight());
            restoreBackImage();
         }

         int w = backImage.getWidth();
         int h = backImage.getHeight();
         int x = lastCopyPixel;
         g.drawImage(backImage, 0, 0, w - x, h, x, 0, w, h, null);
         g.drawImage(backImage, w - x, 0, w, h, 0, 0, x, h, null);
      } while (backImage.contentsLost());
   }

   public synchronized void addPing(Ping ping) {
      FloatRange depthRange = depthRangeChooser.getViewDepthRange();
      colorConverterContainer.getColorConverter().convertToColor(ping, channel, rgbArray, depthRange);
      pingInfos[lastDisplayPixel] = new PingInfo(lastAddedPing, depthRange);

      if (!depthRange.isEmpty()) {
         drawRegions(ping, rgbArray, depthRange);
         drawTracks(ping, rgbArray, depthRange);
         drawTargets(ping, rgbArray, depthRange);

         drawDepth(ping.getPingItem(Dep0Datagram.class), rgbArray, depthRange);

         if (showDepthMarkers) {
            drawDepthMarker(rgbArray, depthRange);
         }
      }

      drawImage.setRGB(lastDisplayPixel, 0, 1, drawImage.getHeight(), rgbArray, 0, 1);

      lastDisplayPixel = (lastDisplayPixel + 1) % drawImage.getWidth();
      pixelsToCopy++;
      lastAddedPing++;
   }

   private PingInfo getPingInfo(int pixelX) {
      int i = (pixelX + lastDisplayPixel) % pingInfos.length;
      return pingInfos[i];
   }

   private void drawRegions(Ping ping, int[] colorBuf, FloatRange depthRange) {
      if (!showRegions) {
         lastRegionInteriors.clear();
         return;
      }

      int height = colorBuf.length;
      List<RegionBorderDatagram> regionBorderDatagrams = ping.getPingItems(RegionBorderDatagram.class)
            .sorted()
            .toList();
      for (int i = 0; i < regionBorderDatagrams.size(); i++) {
         RegionBorderDatagram regionBorderDatagram = regionBorderDatagrams.get(i);
         Float threshold = regionBorderDatagram.getThreshold();
         Color regionColor = REGION_COLORS.get(i % REGION_COLORS.size());
         thresholdToRegionColor.put(threshold, regionColor);
         boolean[] regionInterior = new boolean[height];
         for (RegionBorderDatagram.BorderInfo borderInfo : regionBorderDatagram.getBorderInfos()) {
            FloatRange borderDepthRange = FloatRange.of(borderInfo.startDepth(), borderInfo.endDepth());
            borderDepthRange = borderDepthRange.intersection(depthRange);
            if (!borderDepthRange.isEmpty()) {
               int y0 = Math.clamp(Math.round(height * depthRange.valueToFraction(borderDepthRange.min())), 0, height);
               int y1 = Math.clamp(Math.round(height * depthRange.valueToFraction(borderDepthRange.max())), 0, height);
               Arrays.fill(regionInterior, y0, y1, true);
               if (borderInfo.currentlyAccepted()) {
                  Arrays.fill(colorBuf, y0, y1, regionColor.getRGB());
               }
            }
         }

         boolean[] lastRegionInterior = lastRegionInteriors.get(threshold);
         if (lastRegionInterior != null && regionInterior.length == lastRegionInterior.length) {
            for (int y = 0; y < height; y++) {
               if (y > 0 && regionInterior[y - 1] ^ regionInterior[y]) {
                  // draw end points of current borders
                  colorBuf[regionInterior[y] ? y : y - 1] = regionColor.getRGB();
               }
               if (regionInterior[y] && !lastRegionInterior[y]) {
                  colorBuf[y] = regionColor.getRGB();
               }
               if (!regionInterior[y] && lastRegionInterior[y]) {
                  drawImage.setRGB(lastDisplayPixel, y, regionColor.getRGB());
               }
            }
         }
         lastRegionInteriors.put(threshold, regionInterior);
      }

      //Draw a dashed line indicating the end of school. If school categorization is run,
      //draw solid line with color of categorization result.
      ping.getPingItems(RegionInfoDatagram.class).forEach(regionInfoDatagram -> {
         if (regionInfoDatagram.isAccepted()) {
            Color color = thresholdToRegionColor.get(regionInfoDatagram.getThreshold());
            Color categoryColor = null;

            Cac0Datagram cac0Datagram = ping.getPingConfiguration().getConfigurationItem(Cac0Datagram.class);
            if (cac0Datagram != null) {
               Cas0Datagram cas0Datagram = KoronaRegion.findCas0Datagram(ping, regionInfoDatagram);
               if (cas0Datagram != null) {
                  categoryColor = findCategoryColor(cac0Datagram, cas0Datagram.getBestCategory());
               }
            }

            RegionInfoDatagram.BoundingBox boundingBox = regionInfoDatagram.getBoundingBox();
            FloatRange rectangleDepthRange = FloatRange.ofMinAndSize(boundingBox.y, boundingBox.height);
            rectangleDepthRange = rectangleDepthRange.intersection(depthRange);
            if (!rectangleDepthRange.isEmpty()) {
               int y0 = Math.clamp(Math.round(height * depthRange.valueToFraction(rectangleDepthRange.min())), 0, height);
               int y1 = Math.clamp(Math.round(height * depthRange.valueToFraction(rectangleDepthRange.max())), 0, height);

               if (categoryColor != null) {
                  Arrays.fill(colorBuf, y0, y1, categoryColor.getRGB());
               } else {
                  int y = y0;

                  while (y < y1) {
                     if (y + 8 < y1) {
                        Arrays.fill(colorBuf, y, y + 8, color.getRGB());
                        y += 10;
                     } else {
                        Arrays.fill(colorBuf, y, y1, color.getRGB());
                        y = y1;
                     }
                  }
               }
            }
         }
      });
   }

   private void drawTracks(Ping ping, int[] colorBuf, FloatRange depthRange) {
      if (!showTracks) {
         return;
      }

      ping.getPingItems(TBR0Datagram.class).forEach(tbr0Datagram -> {
         if (tbr0Datagram.getChannel() != channel) {
            return;
         }
         TBR0Datagram lastTbr0Datagram = lastTrackDepths.put(tbr0Datagram.getId(), tbr0Datagram);
         if (lastTbr0Datagram == null) {
            lastTbr0Datagram = tbr0Datagram;
         }
         combinedTrackDepths.merge(tbr0Datagram.getId(), tbr0Datagram.getDepthRange(), FloatRange::union);
         draw(colorBuf, depthRange, lastTbr0Datagram.getDepthRange().min(), tbr0Datagram.getDepthRange().min(), Color.DARK_GRAY);
         draw(colorBuf, depthRange, lastTbr0Datagram.getDepthRange().max(), tbr0Datagram.getDepthRange().max(), Color.DARK_GRAY);
         draw(colorBuf, depthRange, lastTbr0Datagram.getPeakDepth(), tbr0Datagram.getPeakDepth(), Color.BLACK);
      });

      Cac0Datagram cac0Datagram = ping.getPingConfiguration().getConfigurationItem(Cac0Datagram.class);
      if (cac0Datagram != null) {
         int height = colorBuf.length;
         ping.getPingItems(TNF0Datagram.class).forEach(tnf0Datagram -> {
            Cat0Datagram cat0Datagram = findCat0Datagram(ping, tnf0Datagram.getId());
            if (cat0Datagram != null) {
               Color categoryColor = findCategoryColor(cac0Datagram, cat0Datagram.getBestCategory());
               if (categoryColor != null) {
                  FloatRange trackDepthRange = combinedTrackDepths.remove(tnf0Datagram.getId());
                  if (trackDepthRange == null) {
                     return;
                  }
                  int y0 = Math.clamp(Math.round(height * depthRange.valueToFraction(trackDepthRange.min())), 0, height);
                  int y1 = Math.clamp(Math.round(height * depthRange.valueToFraction(trackDepthRange.max())), 0, height);
                  Arrays.fill(colorBuf, y0, y1, categoryColor.getRGB());
               }
            }
         });
      }
   }

   private static @Nullable Color findCategoryColor(Cac0Datagram cac0Datagram, byte categoryNumber) {
      for (Cac0Datagram.Category category : cac0Datagram.getCategories()) {
         if (category.getNumber() == categoryNumber) {
            return category.getColor();
         }
      }
      return null;
   }

   private static @Nullable Cat0Datagram findCat0Datagram(Ping ping, int id) {
      return ping.getPingItems(Cat0Datagram.class)
            .filter(cat0Datagram -> cat0Datagram.getRegionId() == id)
            .findFirst()
            .orElse(null);
   }

   private void drawTargets(Ping ping, int[] colorBuf, FloatRange depthRange) {
      if (!showTargets) {
         return;
      }
      ping.getPingItems(TsDatagram.class)
            .filter(tsDatagram -> tsDatagram.getChannel() == channel)
            .flatMap(tsDatagram -> tsDatagram.getDetections().stream())
            .forEach(detection -> {
               draw(colorBuf, depthRange, detection.peakDepth, Color.BLACK, 1);
            });
   }

   private static void draw(int[] colorBuf, FloatRange depthRange, float depth, Color color, float pixelRadius) {
      float i = depthRange.valueToFraction(depth) * colorBuf.length;
      int begin = Math.max(Math.round(i - pixelRadius), 0);
      int end = Math.min(Math.round(i + pixelRadius) + 1, colorBuf.length);
      if (end <= begin) {
         return;
      }
      Arrays.fill(colorBuf, begin, end, color.getRGB());
   }

   private static void draw(int[] colorBuf, FloatRange depthRange, float depth0, float depth1, Color color) {
      int i0 = Math.round(depthRange.valueToFraction(depth0) * colorBuf.length);
      int i1 = Math.round(depthRange.valueToFraction(depth1) * colorBuf.length);
      int iMin = Math.min(i0, i1);
      int iMax = Math.max(i0, i1);
      if (iMax < 0 || iMin >= colorBuf.length) {
         return;
      }
      Arrays.fill(colorBuf, Math.max(iMin, 0), Math.min(iMax + 1, colorBuf.length), color.getRGB());
   }

   private void drawDepth(@Nullable Dep0Datagram dep0Datagram, int[] colorBuf, FloatRange depthRange) {
      if (dep0Datagram == null) {
         lastDepthY = -1;
         lastMaxDepthY = -1;
      } else {
         lastDepthY = drawDepth(dep0Datagram.getMinimumDepth(), depthRange, colorBuf, lastDepthY);
         lastMaxDepthY = drawDepth(dep0Datagram.getDepth(), depthRange, colorBuf, lastMaxDepthY);
      }
   }

   private static int drawDepth(float depth, FloatRange depthRange, int[] colorBuf, int lastY) {
      int y = Math.round(colorBuf.length * depthRange.valueToFraction(depth));

      if (lastY != -1) {
         int begin = Math.clamp(Math.min(y, lastY), 0, colorBuf.length);
         int end = Math.clamp(Math.max(y, lastY) + 1, 0, colorBuf.length);
         Arrays.fill(colorBuf, begin, end, Color.BLACK.getRGB());
      } else {
         if (y >= 0 && y < colorBuf.length) {
            colorBuf[y] = Color.BLACK.getRGB();
         }
      }

      return y;
   }

   public void drawTexts(Graphics2D g) {
      if (!showText) {
         return;
      }

      // General texts
      int textHeight = g.getFontMetrics().getHeight();
      float x = 2;
      float y = 2;
      GuiText.draw(g, kHzText, Color.BLACK, x, y, GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.TOP, null);
      y += textHeight;
      GuiText.draw(g, channelIdText, Color.BLACK, x, y, GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.TOP, null);

      // Depth texts
      FloatRange depthRange = depthRangeChooser.getViewDepthRange();
      if (depthRange.isEmpty()) {
         return;
      }
      drawDepthText(g, depthRange);
   }

   private void drawDepthText(Graphics2D g, FloatRange depthRange) {
      int height = getHeight();
      int textHeight = component.getFontMetrics(component.getFont()).getHeight();
      int textCount = height / (textHeight * 3);
      if (textCount < 2) {
         return;
      }

      float delta = (float) NiceNumber.niceNumber(depthRange.getSize() / textCount, true);
      FloatRange shrunkRange = depthRange.shrinkToMultipleOf(delta);
      if (shrunkRange == FloatRange.EMPTY_RANGE) {
         return;
      }
      int count = Math.round(shrunkRange.getSize() / delta) + 1;

      String format = Utils.getPrecisionString(delta);
      int x = getWidth() - 4;
      for (int i = 0; i < count; i++) {
         float depth = shrunkRange.min() + i * delta;
         int y = Math.round(height * depthRange.valueToFraction(depth));
         String text = Utils.format(format, depth);
         GuiText.draw(g, text, Color.BLACK, x, y, GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.CENTER, null);
      }
   }

   private void drawDepthMarker(int[] colorBuf, FloatRange depthRange) {
      int height = colorBuf.length;
      int textHeight = component.getFontMetrics(component.getFont()).getHeight();
      int textCount = height / (textHeight * 3);
      if (textCount < 2) {
         return;
      }

      float delta = (float) NiceNumber.niceNumber(depthRange.getSize() / textCount, true);
      FloatRange shrunkRange = depthRange.shrinkToMultipleOf(delta);
      if (shrunkRange == FloatRange.EMPTY_RANGE) {
         return;
      }
      int count = Math.round(shrunkRange.getSize() / delta) + 1;

      for (int i = 0; i < count; i++) {
         float depth = shrunkRange.min() + i * delta;
         int y = Math.round(height * depthRange.valueToFraction(depth));
         if (y >= 0 && y < height) {
            colorBuf[y] = Color.BLACK.getRGB();
         }
      }
   }

   public boolean getUsePrivateColor() {
      return colorConverterContainer == privateColorConverterContainer;
   }

   public void setUsePrivateColor(boolean usePrivateColor) {
      echogramColorPanel.setColorPanelVisible(usePrivateColor);
      colorConverterContainer = usePrivateColor ? privateColorConverterContainer : publicColorConverterContainer;
   }

   public BufferedImage getDrawImage() {
      return drawImage;
   }

   /**
    * Class containing data about each ping.
    */
   private record PingInfo(int pingIndex, FloatRange depthRange) {
      @Override
      public String toString() {
         return pingIndex + " " + depthRange;
      }

      private float getDepth(int pixelY, int lastHeight) {
         float fraction = (float) pixelY / (float) lastHeight;
         return depthRange.fractionToValue(fraction);
      }
   }
}
