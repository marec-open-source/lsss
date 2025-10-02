package no.imr.lsss.modules.map.overlays;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.listening.Listeners;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.web.Wms;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Displays a background map from a WMS server.
 */
public final class BackgroundMapOverlay extends BaseMapOverlay implements PojoDataContainer {
   private BufferedImage displayImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
   private final CoalescingExecutor mapDownloader = new CoalescingExecutor(Exec.CACHED_THREAD_POOL);
   private final Listener mapUpdater = Listeners.debouncing(() -> mapDownloader.execute(this::updateMap));

   public BackgroundMapOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getMapModule().getSizeChangeManager(), newCoalescingExecListener(this::resized));

      registry.add(mapUpdater, List.of(
            getMapModule().getGeographicalAreaChangeManager(),
            getConfigurationManager().getAppMiscConf().mapURL,
            getConfigurationManager().getAppMiscConf().mapLayers
      ));

      //---

      resized();
   }

   @Override
   protected void onDisable() {
      displayImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
   }

   @Override
   protected OverlayDisplayData recomputeDisplayData() {
      return new DisplayData();
   }

   @Override
   public PojoData getPojoData() {
      return PojoData.newBuilder(getPersistentName())
            .with("url", toGetMapUrl(new DisplayData()))
            .build();
   }

   private void resized() {
      int width = Math.max(1, getWidth());
      int height = Math.max(1, getHeight());
      displayImage = getOverlaidModule().getGraphicsConfiguration().createCompatibleImage(width, height);
      mapUpdater.listen();
   }

   private void updateMap() {
      DisplayData displayData = new DisplayData();
      BufferedImage downloadedImage = downloadMap(displayData);
      SwingUtilities.invokeLater(() -> {
         Graphics2D g2d = displayData.image.createGraphics();
         g2d.drawImage(downloadedImage, null, 0, 0);
         g2d.dispose();
         setDisplayData(displayData);
      });
   }

   private String toGetMapUrl(DisplayData displayData) {
      String baseUrl = getConfigurationManager().getAppMiscConf().mapURL.getValue();
      String layers = getConfigurationManager().getAppMiscConf().mapLayers.getValue();
      if (baseUrl.isEmpty() || layers.isEmpty()) {
         return "";
      }
      return Wms.toGetMapUrl(baseUrl, layers, displayData.image.getWidth(), displayData.image.getHeight(), displayData.geoRect);
   }

   private BufferedImage downloadMap(DisplayData displayData) {
      String url = toGetMapUrl(displayData);
      if (url.isEmpty()) {
         return createEmptyImage(displayData.image.getWidth(), displayData.image.getHeight());
      }
      return Wms.downloadMap(url, displayData.image.getWidth(), displayData.image.getHeight());
   }

   private static BufferedImage createEmptyImage(int width, int height) {
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      g.setColor(ColorUtils.BLACK);
      g.fillRect(0, 0, width, height);
      g.dispose();
      return image;
   }

   private final class DisplayData extends TransformedDisplayData {
      private final Rectangle2D geoRect = getMapModule().getGeoRect();
      private final BufferedImage image = displayImage;

      private DisplayData() {
      }

      @Override
      public void transformedDraw(Graphics2D g2d) {
         g2d.drawImage(image, null, 0, 0);
      }
   }
}
