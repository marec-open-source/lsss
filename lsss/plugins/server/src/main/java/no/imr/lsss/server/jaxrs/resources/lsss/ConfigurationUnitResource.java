package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.server.pojo.ParameterInfo;
import no.imr.lsss.server.pojo.values.ObjectValue;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.io.IOException;
import java.util.stream.Stream;

public class ConfigurationUnitResource {
   private final ConfigurationUnit configurationUnit;

   ConfigurationUnitResource(ConfigurationUnit configurationUnit) {
      this.configurationUnit = configurationUnit;
   }

   @GET
   @Path("xml")
   @Produces(MediaType.APPLICATION_XML)
   public byte[] getConfig() {
      Element xml = configurationUnit.toConfigurationXml();
      return XmlUtils.toDefaultBytes(xml);
   }

   @POST
   @Path("xml")
   @Consumes(MediaType.APPLICATION_XML)
   public void setConfig(byte[] bytes) throws IOException {
      Element xml = XmlUtils.readDocument(bytes).getRootElement();
      LsssServerUtils.doInGuiThread(() -> {
         configurationUnit.fromConfigurationXml(xml);
         configurationUnit.getConfigurationManager().ok();
      });
   }

   @GET
   @Path("parameter")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ParameterInfo> getParameters() {
      return LsssServerUtils.getParameterInfos(configurationUnit.getParameterCollection());
   }

   @GET
   @Path("parameter/{path: .*}")
   @Produces(MediaType.APPLICATION_JSON)
   public ObjectValue getParameter(@PathParam("path") String path) {
      return LsssServerUtils.getParameterValue(configurationUnit.getParameterCollection(), path);
   }

   @POST
   @Path("parameter/{path: .*}")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setParameter(@PathParam("path") String path, String value) {
      LsssServerUtils.doInGuiThread(() -> {
         LsssServerUtils.setParameterValue(configurationUnit.getParameterCollection(), path, value);
         configurationUnit.getConfigurationManager().ok();
      });
   }
}
