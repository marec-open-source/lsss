package no.imr.tools.web;

import com.google.common.net.UrlEscapers;
import no.imr.tools.Utils;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.UiUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class Wms {
   public static final String DEFAULT_WMS_URL = Utils.isTestRun() ? "" : "https://kart.hi.no/data/hi_basemap/wms?";
   public static final String DEFAULT_WMS_LAYERS = "basemap_2";
   private static final String VERSION = "1.0.0";

   private Wms() {
   }

   public static Document downloadCapabilities(String baseUrl) throws IOException {
      return XmlUtils.readDocument(Utils.toURL(baseUrl
            + "SERVICE=WMS"
            + "&VERSION=" + VERSION
            + "&REQUEST=GetCapabilities"));
   }

   public static BufferedImage downloadMap(String baseUrl, String layers, int width, int height, Rectangle2D geoRect) {
      String url = toGetMapUrl(baseUrl, layers, width, height, geoRect);
      return downloadMap(url, width, height);
   }

   public static BufferedImage downloadMap(String url, int width, int height) {
      try {
         return ImageIO.read(Utils.toURL(url));
      } catch (IOException e) {
         return createErrorImage(width, height,
               "Error downloading map",
               "",
               url,
               "",
               e.getClass().getName() + ":",
               e.getMessage());
      }
   }

   public static String toGetMapUrl(String baseUrl, String layers, int width, int height, Rectangle2D geoRect) {
      return baseUrl
            + "VERSION=" + VERSION
            + "&REQUEST=GetMap"
            + "&SRS=EPSG:4326"
            + "&WIDTH=" + width
            + "&HEIGHT=" + height
            + "&BBOX=" + geoRect.getMinX() + "," + geoRect.getMinY() + "," + geoRect.getMaxX() + "," + geoRect.getMaxY()
            + "&EXCEPTIONS=INIMAGE"
            + "&FORMAT=image/png"
            + "&LAYERS=" + UrlEscapers.urlPathSegmentEscaper().escape(layers);
   }

   public static BufferedImage createErrorImage(int width, int height, String... lines) {
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      int fontSize = 14;
      g.setColor(ColorUtils.AZURE);
      g.fillRect(0, 0, width, height);
      g.setColor(ColorUtils.POWDERBLUE);
      g.setFont(UiUtils.labelFont().deriveFont(Font.BOLD, fontSize));
      int y = height - fontSize * lines.length;
      for (int i = 0; i < lines.length; i++) {
         GuiText.draw(g, lines[i], 10, y + i * fontSize);
      }
      g.dispose();
      return image;
   }
}
