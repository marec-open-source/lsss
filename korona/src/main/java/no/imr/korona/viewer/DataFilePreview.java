package no.imr.korona.viewer;

import no.imr.korona.color.Colormaps;
import no.imr.korona.data.ChannelSelector;
import no.imr.korona.data.DataFormatManager;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DefaultDataConfiguration;
import no.imr.korona.data.datamanager.FileOpenRequest;
import no.imr.korona.data.datamanager.PingLoadingStrategy;
import no.imr.korona.data.datamanager.PingSampler;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.util.echogram.DefaultEchogramImageSettings;
import no.imr.korona.util.echogram.EchogramImage;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.korona.viewer.variables.raw.SvVariable;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.Listeners;
import no.imr.tools.swing.GuiText;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.nio.file.Path;
import java.util.List;

/**
 * Preview of a data file.
 */
public final class DataFilePreview {
   private static final int WIDTH = 200;

   private final JComponent component = new JComponent() {
      @Override
      protected void paintComponent(Graphics g) {
         draw((Graphics2D) g);
      }
   };
   private final DataFormatManager dataFormatManager = new DataFormatManager();
   private final DefaultDataConfiguration dataConfiguration = new DefaultDataConfiguration();
   private final DataManager dataManager = new DataManager(dataConfiguration);
   private final SingleValueColorConverter colorConverter = new SingleValueColorConverter(new SvVariable(), Colormaps.COMBINED);
   private final DefaultEchogramImageSettings echogramImageSettings = new DefaultEchogramImageSettings(colorConverter,
         ChannelSelector.channelOne(), dataManager.getDataFileSet(), PingMapping.NUMBER);
   private EchogramImage echogramImage = new EchogramImage(echogramImageSettings);
   private final Listener fileLoader = Listeners.coalescingInExecutor(Exec.CACHED_THREAD_POOL, this::load);
   private @Nullable String text;
   private @Nullable Path file;

   public DataFilePreview() {
      component.setPreferredSize(new Dimension(WIDTH, 1));
      component.setBorder(BorderFactory.createEtchedBorder());
      component.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            int width = component.getWidth();
            int height = component.getHeight();
            echogramImage = new EchogramImage(echogramImageSettings, component.getGraphicsConfiguration(), width, height);
            echogramImageSettings.getPingSettings().setWidth(width);
            loadFile();
         }
      });
   }

   public JComponent getComponent() {
      return component;
   }

   public void setFile(@Nullable Path file) {
      this.file = file;
      loadFile();
   }

   private void draw(Graphics2D g) {
      if (text != null) {
         g.setColor(Color.WHITE);
         g.fillRect(0, 0, component.getWidth(), component.getHeight());
         GuiText.draw(g, text, Color.BLACK, component.getWidth() / 2f, component.getHeight() / 2f,
               GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.CENTER, null);
      } else {
         echogramImage.draw(g);
      }
   }

   private void setText(@Nullable String text) {
      this.text = text;
      component.repaint();
   }

   private void loadFile() {
      if (echogramImageSettings.getPingSettings().getWidth() == 0) {
         return;
      }
      setText("Loading...");
      fileLoader.listen();
   }

   private void load() {
      Path file = this.file;
      if (file == null) {
         setText("");
         return;
      }

      SegmentHandle segmentHandle = dataFormatManager.createSegmentHandle(file);
      if (segmentHandle == null) {
         setText("Cannot load file");
         return;
      }

      FileOpenRequest fileOpenRequest = new FileOpenRequest(List.of(segmentHandle));
      dataManager.asyncOpenFiles(fileOpenRequest);
      fileOpenRequest.getAsyncHandle().waitUntilFinished();
      if (dataManager.getDataFileSet().isEmpty()) {
         setText("No data");
         return;
      }

      echogramImageSettings.getPingSettings().setPingContainer(dataManager.getDataFileSet());
      echogramImageSettings.getPingSettings().zoomOut();
      echogramImageSettings.getZSettings().setMaxZ(0, 1.05f * dataManager.getDataFileSet().getMaxDepth());
      echogramImageSettings.getZSettings().zoomOut();

      echogramImage.clearData();
      PingSampler pingSampler = new PingSampler(dataManager, echogramImageSettings.getPingSettings());
      pingSampler.getNewPingsChangeManager().addListener(pings -> {
         echogramImage.processPings(pings);
         setText(null);
      });
      pingSampler.requestPings(PingLoadingStrategy.LONGEST_GAP_LEFT_TO_RIGHT);
      pingSampler.waitForPingRequest();
      dataManager.closeAllFiles();
   }

   public static void install(JFileChooser fileChooser) {
      DataFilePreview preview = new DataFilePreview();
      fileChooser.setAccessory(preview.getComponent());
      fileChooser.addPropertyChangeListener(evt -> {
         switch (evt.getPropertyName()) {
            case JFileChooser.DIRECTORY_CHANGED_PROPERTY,
                 JFileChooser.SELECTED_FILE_CHANGED_PROPERTY -> {
               preview.setFile(FileUtils.toPath(fileChooser.getSelectedFile()));
            }
            default -> {
            }
         }
      });
      preview.setFile(FileUtils.toPath(fileChooser.getSelectedFile()));
   }
}
