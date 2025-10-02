package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.imr.korona.region.Interpretation;
import no.imr.korona.region.Region;
import no.imr.lsss.database.tables.QualityEnum;
import no.imr.lsss.framework.export.ExportUtils;
import no.imr.lsss.framework.export.pojo.ExportScrutiny;
import no.imr.lsss.modules.interpretation.InterpretationModule;
import no.imr.lsss.server.pojo.StoreRequest;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class InterpretationModuleResource extends ModuleResource<InterpretationModule> {
   InterpretationModuleResource(InterpretationModule module) {
      super(module);
   }

   @POST
   @Path("database")
   @Consumes(MediaType.APPLICATION_JSON)
   public void store(@QueryParam("onlyPreviouslyStored") @DefaultValue("false") boolean onlyPreviouslyStored, @Nullable StoreRequest storeRequest) {
      if (onlyPreviouslyStored) {
         module.store(InterpretationModule.StoreAction.ONLY_PREVIOUSLY_STORED);
         return;
      }
      if (storeRequest == null) {
         throw new BadRequestException("Missing request body");
      }
      if (storeRequest.quality == null) {
         throw new BadRequestException("Missing property: quality");
      }
      if (storeRequest.frequencies == null) {
         throw new BadRequestException("Missing property: frequencies");
      }
      QualityEnum qualityEnum = QualityEnum.valueToQualityEnum(storeRequest.quality);
      if (qualityEnum == null) {
         throw new BadRequestException("Unknown quality: " + storeRequest.quality);
      }
      module.setQuality(qualityEnum);
      module.frequencies.setValue(storeRequest.frequencies);
      module.store(InterpretationModule.StoreAction.ALL);
   }

   @DELETE
   @Path("database")
   @Consumes(MediaType.APPLICATION_JSON)
   public void delete(@QueryParam("keepStoringSettings") @DefaultValue("false") boolean keepStoringSettings) {
      InterpretationModule.DeleteAction deleteAction = keepStoringSettings
            ? InterpretationModule.DeleteAction.KEEP_STORING_SETTINGS
            : InterpretationModule.DeleteAction.ALL;
      module.delete(deleteAction);
   }

   @POST
   @Path("scrutiny")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setScrutiny(ExportScrutiny scrutiny) {
      List<Interpretation> interpretations = module.getLSSS().getRegionManager().getSelectedRegions().stream()
            .filter(Region::isWritable)
            .map(Region::getInterpretation)
            .toList();
      ExportUtils.applyScrutiny(module.getLSSS(), interpretations, scrutiny);
   }
}
