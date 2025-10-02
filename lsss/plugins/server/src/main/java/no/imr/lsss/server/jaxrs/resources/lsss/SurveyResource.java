package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
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
import no.imr.lsss.framework.config.survey.acousticcategories.AcousticCategoryConf;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.framework.config.survey.modules.ModuleConf;
import no.imr.lsss.framework.config.survey.modules.ModuleConfigurationUnit;
import no.imr.lsss.server.pojo.ConfigurationUnitInfo;
import no.imr.lsss.server.pojo.values.StringValue;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.Utils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;

import java.io.IOException;
import java.util.stream.Stream;

public final class SurveyResource {
   private final LSSS lsss;

   SurveyResource(LSSS lsss) {
      this.lsss = lsss;
   }

   @POST
   @Path("close")
   public void close() {
      LsssServerUtils.doInGuiThread(() -> {
         lsss.getSurveyManager().closeByUser();
      });
   }

   @GET
   @Path("config/xml")
   @Produces(MediaType.APPLICATION_XML)
   public byte[] getConfig() {
      Element xml = lsss.getSurveyManager().toSurveyXml();
      return XmlUtils.toDefaultBytes(xml);
   }

   @POST
   @Path("config/xml")
   @Consumes(MediaType.APPLICATION_XML)
   public void setConfig(byte[] bytes) throws IOException {
      Element xml = XmlUtils.readDocument(bytes).getRootElement();
      LsssServerUtils.doInGuiThread(() -> {
         lsss.getSurveyManager().fromSurveyXml(xml);
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
      return switch (configurationUnit) {
         case AcousticCategoryConf acousticCategoryConf -> {
            yield new AcousticCategoryConfResource(acousticCategoryConf);
         }
         case DataConf dataConf -> {
            yield new DataConfResource(dataConf);
         }
         default -> {
            yield new ConfigurationUnitResource(configurationUnit);
         }
      };
   }

   private Stream<ConfigurationUnit> getApplicableConfigurationUnits() {
      return lsss.getConfigurationManager().getSurveyConfiguration().getAllUnitsRecursively()
            .filter(unit -> !(unit instanceof ModuleConf) && !(unit instanceof ModuleConfigurationUnit));
   }

   @POST
   @Path("open")
   @Consumes(MediaType.APPLICATION_JSON)
   public void open(StringValue surveyFile) {
      LsssServerUtils.doInGuiThread(() -> {
         lsss.getSurveyManager().open(java.nio.file.Path.of(surveyFile.value));
      });
   }

   @POST
   @Path("save")
   public void save() {
      if (!lsss.getSurveyManager().isOpen()) {
         throw new BadRequestException("No survey opened");
      }
      LsssServerUtils.doInGuiThread(() -> {
         lsss.getSurveyManager().save();
      });
   }
}
