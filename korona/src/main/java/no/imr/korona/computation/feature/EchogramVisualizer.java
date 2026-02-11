package no.imr.korona.computation.feature;

import no.imr.korona.color.Colormaps;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.datagrams.Pid0Datagram;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.viewer.DepthRangeChooser;
import no.imr.korona.viewer.DepthRangeMode;
import no.imr.korona.viewer.EchogramColorPanel;
import no.imr.korona.viewer.EchogramPanel;
import no.imr.korona.viewer.SvColorPanel;
import no.imr.korona.viewer.coloring.ColorConverter;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.raw.TscVariable;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Visualizes the EchogramWindow.
 */
public final class EchogramVisualizer {
   private abstract static class RectangleSelectionMouseListener extends MouseAdapter {
      private @Nullable Point2D point;
      private @Nullable Rectangle2D rectangle;

      private RectangleSelectionMouseListener() {
      }

      protected abstract void selectedRectangle(Rectangle2D rectangle, MouseEvent e);

      @Override
      public void mousePressed(MouseEvent e) {
         point = e.getPoint();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         if (rectangle != null) {
            selectedRectangle(rectangle, e);
         }
         point = null;
         rectangle = null;
         ((Component) e.getSource()).repaint();
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         if (point == null) {
            return;
         }
         Graphics2D g = (Graphics2D) ((Component) e.getSource()).getGraphics();
         g.setXORMode(Color.GRAY);
         if (rectangle != null) {
            g.draw(rectangle);
         } else {
            rectangle = new Rectangle2D.Double();
         }
         rectangle.setFrameFromDiagonal(point, e.getPoint());
         g.draw(rectangle);
         g.dispose();
      }
   }

   private final class ChannelPanel {
      private final class MarkMouseListener extends RectangleSelectionMouseListener {
         private MarkMouseListener() {
         }

         @Override
         protected void selectedRectangle(Rectangle2D rectangle, MouseEvent e) {
            Rectangle2D r = echogramPanelToEchogramWindow(rectangle);
            boolean setMarked = true;
            if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
               setMarked = false;
            } else if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
               echogramWindow.clearMarking();
            }
            echogramWindow.mark(r, setMarked);
            changeOccurred();
         }
      }

      private Rectangle2D echogramPanelToEchogramWindow(Rectangle2D rectangle) {
         double xFactor = (double) echogramWindow.getWidth() / echogramColorPanel.getEchogramPanel().getWidth();
         double yFactor = (double) echogramWindow.getHeight() / echogramColorPanel.getEchogramPanel().getHeight();
         return new Rectangle2D.Double(
               xFactor * rectangle.getX(), yFactor * rectangle.getY(),
               xFactor * rectangle.getWidth(), yFactor * rectangle.getHeight());
      }

      private final EchogramColorPanel echogramColorPanel;
      private final EchogramWindow echogramWindow;
      private final int channel;
      private final Ping[] pings;
      private final @Nullable PowerData[] rawOrigs;
      private final @Nullable PowerData[] rawCopies;

      private ChannelPanel(ColorConverterContainer converterContainer,
                           DepthRangeChooser depthRangeChooser,
                           EchogramWindow echogramWindow, int channel) {
         echogramColorPanel = new EchogramColorPanel(converterContainer, depthRangeChooser, echogramWindow.getPingConfiguration(), channel);
         this.echogramWindow = echogramWindow;
         this.channel = channel;
         pings = new Ping[echogramWindow.getWidth()];
         rawOrigs = new PowerData[echogramWindow.getWidth()];
         rawCopies = new PowerData[echogramWindow.getWidth()];

         init();

         echogramColorPanel.getEchogramPanel().setUsePrivateColor(true);
         echogramWindow.setThresholdForChannel(channel, getVariable().getSettings().getEffectiveRange());

         echogramColorPanel.getSvColorPanel().getConverterContainer().getChangeManager().addListener(() -> {
            echogramWindow.setThresholdForChannel(channel, getVariable().getSettings().getEffectiveRange());
            changeOccurred();
         });

         MarkMouseListener mouseInputListener = new MarkMouseListener();
         echogramColorPanel.getEchogramPanel().getComponent().addMouseListener(mouseInputListener);
         echogramColorPanel.getEchogramPanel().getComponent().addMouseMotionListener(mouseInputListener);
      }

      private void init() {
         PingConfiguration pingConfiguration = echogramWindow.getPingConfiguration();

         for (int i = 0; i < echogramWindow.getWidth(); i++) {
            Ping originalPing = echogramWindow.getPings().get(i);
            PowerData rawOrig = originalPing.getPowerData(channel);
            if (rawOrig != null) {
               PowerData rawCopy = rawOrig.makeCopyWithAnglesOnly();
               System.arraycopy(rawOrig.getSv(), 0, rawCopy.getSv(), 0, rawOrig.getCount());
               rawOrigs[i] = rawOrig;
               rawCopies[i] = rawCopy;
            }

            Ping ping = new DefaultPing(pingConfiguration, originalPing.getPingIndex(), originalPing.getBot0Datagram());
            Stream.of(rawCopies[i],
                        originalPing.getPingItem(Dep0Datagram.class),
                        originalPing.getPingItem(Cad0Datagram.class),
                        originalPing.getPingItem(Pid0Datagram.class))
                  .filter(Objects::nonNull)
                  .forEach(ping::add);
            pings[i] = ping;
         }

         echogramColorPanel.getSvColorPanel().setConfigurationItems(pingConfiguration.getConfigurationItems(), configFileSettings);
      }

      private void update() {
         ColorConverterContainer converterContainer = echogramColorPanel.getSvColorPanel().getConverterContainer();
         ContinuousVariable variable = getVariable();
         if (converterContainer.getColorConverter().getContinuousVariable() != variable) {
            ColorConverter colorConverter = new SingleValueColorConverter(variable, Colormaps.COMBINED);
            converterContainer.setColorConverter(colorConverter);
         }

         echogramColorPanel.getEchogramPanel().resetDraw();

         for (int i = 0; i < pings.length; i++) {
            PowerData rawOrig = rawOrigs[i];
            PowerData rawCopy = rawCopies[i];
            if (rawOrig != null && rawCopy != null) {
               System.arraycopy(rawOrig.getSv(), 0, rawCopy.getSv(), 0, rawOrig.getCount());

               int beginJ = rawCopy.depthToClampedSampleIndex(echogramWindow.getMinDepth());
               int endJ = rawCopy.depthToClampedSampleIndex(echogramWindow.getMaxDepth());

               for (int j = beginJ; j < endJ; j++) {
                  float depth = rawCopy.getSampleDepth(j);
                  if (!echogramWindow.isActive(i, echogramWindow.depthToIndex(depth))) {
                     rawCopy.getSv()[j] = 0;
                  }
               }
               rawCopy.setSv(rawCopy.getSv());
            }
         }

         addAllPings();
         drawMarking();
      }

      private void addAllPings() {
         int nPings = echogramWindow.getWidth();
         int nPixels = getPixelCount();
         float x = (float) nPings / (nPixels - 1);
         for (int i = 0; i < nPixels; i++) {
            int iPing = Math.min(nPings - 1, (int) Math.floor(i * x));
            echogramColorPanel.getEchogramPanel().addPing(pings[iPing]);
         }
      }

      private void drawMarking() {
         BufferedImage bi = echogramColorPanel.getEchogramPanel().getDrawImage();
         Graphics2D g = bi.createGraphics();
         float xFactor = (float) bi.getWidth() / echogramWindow.getWidth();
         float yFactor = (float) bi.getHeight() / echogramWindow.getHeight();
         int w = (int) Math.ceil(xFactor);
         int h = (int) Math.ceil(yFactor);
         g.setColor(Color.RED);
         for (int i = 0; i < echogramWindow.getWidth(); i++) {
            for (int j = 0; j < echogramWindow.getHeight(); j++) {
               if (echogramWindow.isActive(i, j) && echogramWindow.isMarked(i, j)) {
                  g.fillRect((int) Math.floor(i * xFactor), (int) Math.floor(j * yFactor), w, h);
               }
            }
         }
         g.dispose();
      }

      private int getPixelCount() {
         return echogramColorPanel.getEchogramPanel().getWidth();
      }

      private ContinuousVariable getVariable() {
         ColorConverterContainer converterContainer = echogramColorPanel.getSvColorPanel().getConverterContainer();
         return switch (echogramWindow.getConfigurator().getCategoryType()) {
            case Aggregation -> converterContainer.getSV();
            case Track -> converterContainer.getContinuousVariable(TscVariable.class);
         };
      }
   }

   private final EchogramWindow echogramWindow;
   private final ConfigFileSettings configFileSettings;
   private final ChannelPanel[] channelPanels;
   private final JPanel panel = new JPanel(new BorderLayout());
   private final JPanel echogramsPanel = new JPanel(new BorderLayout());
   private final JRadioButton maskAnd = new JRadioButton("and", true);
   private final JRadioButton maskOr = new JRadioButton("or", false);

   private final ChangeManager changeManager = new ChangeManager();

   public EchogramVisualizer(EchogramWindow echogramWindow, ConfigFileSettings configFileSettings) {
      this.echogramWindow = echogramWindow;
      this.configFileSettings = configFileSettings;
      RawFileConfiguration rawFileConfiguration = echogramWindow.getConfigurator().getRawFileConfiguration();
      channelPanels = new ChannelPanel[rawFileConfiguration != null ? rawFileConfiguration.getTransducerCount() : 0];
      makePanel();

      panel.addComponentListener(new ComponentAdapter() {
         private final Timer timer = new Timer(100, _ -> changeOccurred());

         {
            timer.setRepeats(false);
         }

         @Override
         public void componentResized(ComponentEvent e) {
            timer.restart();
         }
      });
   }

   private void updateEchogramColorPanels() {
      for (ChannelPanel channelPanel : channelPanels) {
         channelPanel.update();
      }
   }

   private void makeImageX() {
      int width = echogramsPanel.getWidth();
      int height = echogramsPanel.getHeight();
      Log.global.fine("rendering echogram window off screen: " + width + "x" + height);
      BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = bi.createGraphics();
      echogramsPanel.paint(g); // todo: this line causes program to slow down dramatically...
      g.dispose();
      echogramWindow.setImage(bi);
   }

   private void makeImage() {
      int width = echogramsPanel.getWidth();
      int height = echogramsPanel.getHeight();
      Log.global.fine("rendering echogram window off screen: " + width + "x" + height);
      BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = bi.createGraphics();
      //echogramsPanel.paint(g); // todo: this line causes program to slow down dramatically...
      for (int i = 0, x = 0; i < channelPanels.length; i++) {
         ChannelPanel channelPanel = channelPanels[i];

         SvColorPanel svColorPanel = channelPanel.echogramColorPanel.getSvColorPanel();
         EchogramPanel echogramPanel = channelPanel.echogramColorPanel.getEchogramPanel();

         Graphics2D gSv = bi.getSubimage(x, 0, svColorPanel.getWidth(), svColorPanel.getHeight()).createGraphics();
         svColorPanel.getComponent().paintComponents(gSv);
         gSv.dispose();

         x += svColorPanel.getWidth();

         Graphics2D gEcho = bi.getSubimage(x, 0, echogramPanel.getWidth(), echogramPanel.getHeight()).createGraphics();
         gEcho.drawImage(echogramPanel.getDrawImage(), null, 0, 0);
         echogramPanel.drawTexts(gEcho);
         gEcho.dispose();

         x += echogramPanel.getWidth();
      }
      g.dispose();
      echogramWindow.setImage(bi);

      /*
      JDialog dialog = new JDialog((JFrame) null, false);
      dialog.getContentPane().add(new JLabel(new ImageIcon(bi)));
      dialog.pack();
      dialog.setVisible(true);
      */
   }

   private void changeOccurred() {
      changeManager.notifyListeners();
   }

   private void doRedraw() {
      SwingUtilities.invokeLater(() -> {
         Log.global.fine("doRedraw");
         updateEchogramColorPanels();
         panel.repaint();
         SwingUtilities.invokeLater(this::makeImage);
      });
   }

   /**
    * Returns the ChangeManager for this EchogramWindow.
    * Listeners are notified when a change in this EchogramWindows has occurred,
    * and a call {@link #redraw} is required.
    *
    * @return the ChangeManager
    */
   public ChangeManager getChangeManager() {
      return changeManager;
   }

   /**
    * Redraws the contents of this EchogramWindow.
    *
    * @return the component which contain the rendering of this EchogramWindow
    */
   public JComponent redraw() {
      doRedraw();
      return panel;
   }

   private void makePanel() {
      JPanel buttonPanel = new JPanel();
      buttonPanel.setBackground(Color.WHITE);
      panel.add(buttonPanel, BorderLayout.NORTH);

      buttonPanel.add(new JLabel("Masking logic:   "));

      buttonPanel.add(maskAnd);
      maskAnd.setBackground(Color.WHITE);
      maskAnd.addActionListener(_ -> {
         echogramWindow.setAndMasking(true);
         changeOccurred();
      });

      buttonPanel.add(maskOr);
      maskOr.setBackground(Color.WHITE);
      maskOr.addActionListener(_ -> {
         echogramWindow.setAndMasking(false);
         changeOccurred();
      });

      GuiUtils.createButtonGroup(maskAnd, maskOr);

      buttonPanel.add(Box.createHorizontalGlue());

      Box echogramContainer = Box.createHorizontalBox();
      echogramsPanel.add(echogramContainer);
      panel.add(echogramsPanel);

      for (int i = 0; i < channelPanels.length; i++) {
         DepthRangeChooser depthRangeChooser = new DepthRangeChooser();
         depthRangeChooser.setDepthRangeMode(DepthRangeMode.MANUAL);
         float minDepth = echogramWindow.getDepth(0);
         depthRangeChooser.setMinFixedDepth(minDepth);
         float maxDepth = echogramWindow.getDepth(echogramWindow.getHeight());
         depthRangeChooser.setMaxFixedDepth(maxDepth);

         ColorConverterContainer converterContainer = new ColorConverterContainer();

         ChannelPanel channelPanel = new ChannelPanel(
               converterContainer,
               depthRangeChooser,
               echogramWindow, i + 1);

         channelPanel.echogramColorPanel.setColorPanelVisible(true);

         echogramContainer.add(channelPanel.echogramColorPanel.getComponent());
         channelPanels[i] = channelPanel;
      }
   }
}
