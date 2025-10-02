package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;
import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.server.pojo.ParameterInfo;
import no.imr.lsss.server.pojo.values.BooleanValue;
import no.imr.lsss.server.pojo.values.ObjectValue;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.stream.Stream;

public final class OverlayResource {
   private final BaseModuleOverlay overlay;

   OverlayResource(BaseModuleOverlay overlay) {
      this.overlay = overlay;
   }

   @GET
   @Path("config/xml")
   @Produces(MediaType.APPLICATION_XML)
   public byte[] getConfig() {
      Element xml = overlay.toXml();
      return XmlUtils.toDefaultBytes(xml);
   }

   @POST
   @Path("config/xml")
   @Consumes(MediaType.APPLICATION_XML)
   public void setConfig(byte[] bytes) throws IOException {
      overlay.fromXml(XmlUtils.readDocument(bytes).getRootElement());
   }

   @GET
   @Path("config/parameter")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ParameterInfo> getParameters() {
      return LsssServerUtils.getParameterInfos(overlay.getConfigurable());
   }

   @GET
   @Path("config/parameter/{path: .*}")
   @Produces(MediaType.APPLICATION_JSON)
   public ObjectValue getParameter(@PathParam("path") String path) {
      return LsssServerUtils.getParameterValue(overlay.getConfigurable(), path);
   }

   @POST
   @Path("config/parameter/{path: .*}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setParameter(@PathParam("path") String path, String value) {
      LsssServerUtils.setParameterValue(overlay.getConfigurable(), path, value);
   }

   @GET
   @Path("data")
   @Produces(MediaType.APPLICATION_JSON)
   public PojoData getData() {
      if (!(overlay instanceof PojoDataContainer pojoDataContainer)) {
         throw new BadRequestException(overlay.getPersistentName() + " does not expose any data");
      }
      if (!overlay.isEnabled()) {
         throw new BadRequestException(overlay.getPersistentName() + " is not enabled");
      }
      return pojoDataContainer.getPojoData();
   }

   @GET
   @Path("enabled")
   @Produces(MediaType.APPLICATION_JSON)
   public BooleanValue getEnabled() {
      return new BooleanValue(overlay.isEnabledByUser());
   }

   @POST
   @Path("enabled")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setEnabled(BooleanValue booleanValue) {
      overlay.setEnabledByUser(booleanValue.value);
   }

   @GET
   @Path("image")
   @Produces("image/png")
   public StreamingOutput getImage() {
      if (!overlay.isEnabled()) {
         throw new BadRequestException(overlay.getPersistentName() + " is not enabled");
      }
      BufferedImage image = new BufferedImage(overlay.getWidth(), overlay.getHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      OverlayDisplayData displayData = overlay.getDisplayData();
      if (displayData != null) {
         displayData.draw(g);
         displayData.drawText(g);
      }
      g.dispose();
      return out -> ImageIO.write(image, "png", out);
   }
}
