package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.StreamingOutput;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.server.pojo.OverlayInfo;
import no.imr.lsss.server.pojo.ParameterInfo;
import no.imr.lsss.server.pojo.values.BooleanValue;
import no.imr.lsss.server.pojo.values.ObjectValue;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.Utils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.stream.Stream;

public class ModuleResource<T extends BaseLsssModule> {
   protected final T module;

   ModuleResource(T module) {
      this.module = module;
   }

   @GET
   @Path("config/xml")
   @Produces(MediaType.APPLICATION_XML)
   public byte[] getConfig() {
      Element xml = module.toXml();
      return XmlUtils.toDefaultBytes(xml);
   }

   @POST
   @Path("config/xml")
   @Consumes(MediaType.APPLICATION_XML)
   public void setConfig(byte[] bytes) throws IOException {
      module.fromXml(XmlUtils.readDocument(bytes).getRootElement());
   }

   @GET
   @Path("config/parameter")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ParameterInfo> getParameters() {
      return LsssServerUtils.getParameterInfos(module.getConfigurable());
   }

   @GET
   @Path("config/parameter/{path: .*}")
   @Produces(MediaType.APPLICATION_JSON)
   public ObjectValue getParameter(@PathParam("path") String path) {
      return LsssServerUtils.getParameterValue(module.getConfigurable(), path);
   }

   @POST
   @Path("config/parameter/{path: .*}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setParameter(@PathParam("path") String path, String value) {
      LsssServerUtils.setParameterValue(module.getConfigurable(), path, value);
   }

   @GET
   @Path("data")
   @Produces(MediaType.APPLICATION_JSON)
   public PojoData getData() {
      if (!(module instanceof PojoDataContainer pojoDataContainer)) {
         throw new BadRequestException(module.getPersistentName() + " does not expose any data");
      }
      throwIfNotEnabled();
      return pojoDataContainer.getPojoData();
   }

   @GET
   @Path("docked")
   @Produces(MediaType.APPLICATION_JSON)
   public BooleanValue getDocked() {
      BaseViewModule viewModule = asViewModule();
      BaseViewModule.FloatableInfo floatableInfo = viewModule.getFloatableInfo();
      if (floatableInfo == null) {
         throw new BadRequestException(viewModule.getPersistentName() + " is not dockable");
      }
      return new BooleanValue(!floatableInfo.isFloating());
   }

   @POST
   @Path("docked")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setDocked(BooleanValue booleanValue) {
      BaseViewModule viewModule = asViewModule();
      BaseViewModule.FloatableInfo floatableInfo = viewModule.getFloatableInfo();
      if (floatableInfo == null) {
         throw new BadRequestException(viewModule.getPersistentName() + " is not dockable");
      }
      SwingUtilities.invokeLater(() -> floatableInfo.setFloating(!booleanValue.value));
   }

   @GET
   @Path("enabled")
   @Produces(MediaType.APPLICATION_JSON)
   public BooleanValue getEnabled() {
      return new BooleanValue(module.isEnabled());
   }

   @POST
   @Path("enabled")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setEnabled(BooleanValue booleanValue) {
      BaseViewModule viewModule = asViewModule();
      if (viewModule.getFloatableInfo() == null) {
         throw new BadRequestException(viewModule.getPersistentName() + " cannot be enabled/disabled");
      }
      viewModule.setEnabled(booleanValue.value);
   }

   @GET
   @Path("image")
   @Produces("image/png")
   public StreamingOutput getImage() {
      BaseViewModule viewModule = asViewModule();
      throwIfNotEnabled();
      BufferedImage image = GuiUtils.getNowOrWait(() -> GuiUtils.toImage(viewModule.getViewHolder().getView().getApiComponent()));
      return out -> ImageIO.write(image, "png", out);
   }

   @GET
   @Path("overlay")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<OverlayInfo> getOverlays(@QueryParam("enabled") @Nullable Boolean enabled,
                                          @QueryParam("hasData") @Nullable Boolean hasData,
                                          @QueryParam("data") @DefaultValue("false") boolean data) {
      return asOverlaidModule().userVisibleForegroundOverlays()
            .filter(o -> enabled == null || enabled == o.isEnabled())
            .filter(o -> hasData == null || hasData == o instanceof PojoDataContainer)
            .sorted(Utils.comparingIgnoringCase(BaseLsssModule::getPersistentName))
            .map(o -> new OverlayInfo(o, data));
   }

   @Path("overlay/{id}")
   public OverlayResource getOverlay(@PathParam("id") String id) {
      BaseModuleOverlay overlay = asOverlaidModule().userVisibleForegroundOverlays()
            .filter(o -> o.getPersistentName().equals(id))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(id));
      return new OverlayResource(overlay);
   }

   private void throwIfNotEnabled() {
      if (!module.isEnabled()) {
         throw new BadRequestException(module.getPersistentName() + " is not enabled");
      }
   }

   private BaseViewModule asViewModule() {
      if (!(module instanceof BaseViewModule viewModule)) {
         throw new BadRequestException(module.getPersistentName() + " is not a view module");
      }
      return viewModule;
   }

   private BaseOverlaidModule<?> asOverlaidModule() {
      if (!(module instanceof BaseOverlaidModule<?> overlaidModule)) {
         throw new BadRequestException(module.getPersistentName() + " does not have overlays");
      }
      return overlaidModule;
   }
}
