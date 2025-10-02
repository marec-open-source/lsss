package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.imr.lsss.database.util.LanguageUtils;
import no.imr.lsss.framework.config.survey.acousticcategories.AcousticCategoryConf;
import no.imr.lsss.server.pojo.AcousticCategoryInfo;

import java.util.Comparator;
import java.util.stream.Stream;

public final class AcousticCategoryConfResource extends ConfigurationUnitResource {
   private final AcousticCategoryConf acousticCategoryConf;

   AcousticCategoryConfResource(AcousticCategoryConf acousticCategoryConf) {
      super(acousticCategoryConf);

      this.acousticCategoryConf = acousticCategoryConf;
   }

   @GET
   @Path("category")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<AcousticCategoryInfo> getCategory() {
      LanguageUtils languageUtils = acousticCategoryConf.getConfigurationManager().getLanguageUtils();
      return acousticCategoryConf.getAllAcousticCategories().stream()
            .sorted(Comparator.comparingInt(acousticCategory -> acousticCategory.getCompId().getAcousticCategory()))
            .map(acousticCategory -> new AcousticCategoryInfo(acousticCategory, languageUtils));
   }
}
