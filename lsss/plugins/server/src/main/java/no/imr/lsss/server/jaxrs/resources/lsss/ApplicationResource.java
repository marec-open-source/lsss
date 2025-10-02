package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.application.DatabaseConf;
import no.imr.lsss.server.pojo.ConfigurationUnitInfo;
import no.imr.lsss.server.pojo.LsssInfo;
import no.imr.lsss.server.pojo.values.BooleanValue;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.Utils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.io.IOException;
import java.util.stream.Stream;

public final class ApplicationResource {
   private final LSSS lsss;

   ApplicationResource(LSSS lsss) {
      this.lsss = lsss;
   }

   @GET
   @Path("config/xml")
   @Produces(MediaType.APPLICATION_XML)
   public byte[] getConfig() {
      Element xml = lsss.getConfigurationManager().getApplicationConfiguration().toXml();
      return XmlUtils.toDefaultBytes(xml);
   }

   @POST
   @Path("config/xml")
   @Consumes(MediaType.APPLICATION_XML)
   public void setConfig(byte[] bytes) throws IOException {
      Element xml = XmlUtils.readDocument(bytes).getRootElement();
      LsssServerUtils.doInGuiThread(() -> {
         lsss.getConfigurationManager().getApplicationConfiguration().fromXml(xml);
      });
   }

   @GET
   @Path("config/unit")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ConfigurationUnitInfo> getConfigurationUnits() {
      return getApplicableConfigurationUnits()
            .sorted(Utils.comparingIgnoringCase(ConfigurationUnit::getPersistentName))
            .map(ConfigurationUnitInfo::new);
   }

   @Path("config/unit/{id}")
   public ConfigurationUnitResource getConfigurationUnitResource(@PathParam("id") String id) {
      ConfigurationUnit configurationUnit = getApplicableConfigurationUnits()
            .filter(unit -> unit.getPersistentName().equals(id))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(id));
      if (configurationUnit instanceof DatabaseConf databaseConf) {
         return new DatabaseConfResource(databaseConf);
      }
      return new ConfigurationUnitResource(configurationUnit);
   }

   private Stream<ConfigurationUnit> getApplicableConfigurationUnits() {
      return lsss.getConfigurationManager().getApplicationConfiguration().getAllUnitsRecursively();
   }

   @POST
   @Path("exit")
   public void exit() {
      lsss.close();
   }

   @GET
   @Path("info")
   @Produces(MediaType.APPLICATION_JSON)
   public LsssInfo getInfo() {
      return new LsssInfo();
   }

   @GET
   @Path("ready")
   @Produces(MediaType.APPLICATION_JSON)
   public BooleanValue isReady() {
      return new BooleanValue(true);
   }

   @POST
   @Path("save")
   public void save() {
      lsss.getConfigurationManager().getApplicationConfiguration().saveDefault();
   }
}
