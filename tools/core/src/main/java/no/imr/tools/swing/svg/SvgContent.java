package no.imr.tools.swing.svg;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;

public final class SvgContent {
   private final GraphicsNode node;
   public final double width;
   public final double height;

   public SvgContent(URL url) {
      Document document;
      try {
         SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
         document = factory.createDocument(url.toString());
      } catch (IOException e) {
         throw new UncheckedIOException("Error loading " + url, e);
      }
      GVTBuilder gvtBuilder = new GVTBuilder();
      BridgeContext bridgeContext = new BridgeContext(new UserAgentAdapter());
      node = gvtBuilder.build(bridgeContext, document);
      Dimension2D size = bridgeContext.getDocumentSize();
      width = size.getWidth();
      height = size.getHeight();
   }

   public BufferedImage toImage(double imageWidth, double imageHeight) {
      BufferedImage image = new BufferedImage(
            SvgImage.toImageSize(imageWidth),
            SvgImage.toImageSize(imageHeight),
            BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.scale(imageWidth / width, imageHeight / height);
      node.paint(g);
      g.dispose();
      return image;
   }
}
