package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.export.StreamingExporter;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.echogram.ColorBarModule;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.interpretation.InterpretationModule;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.server.ResourceService;
import no.imr.lsss.server.jaxrs.JaxRsApplication;
import no.imr.lsss.server.pojo.ExportInfo;
import no.imr.lsss.server.pojo.ModuleInfo;
import no.imr.lsss.server.pojo.PluginInfo;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LsssResource {
   private final JaxRsApplication jaxRsApplication;
   private final LSSS lsss;
   private final Map<String, ResourceService> idToResourceService;

   public LsssResource(JaxRsApplication jaxRsApplication) {
      this.jaxRsApplication = jaxRsApplication;
      lsss = jaxRsApplication.getLSSS();
      idToResourceService = ServiceLoader.load(ResourceService.class).stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toUnmodifiableMap(
                  service -> service.getName().persistentName(),
                  Function.identity()));
   }

   @GET
   public Response getRoot(@Context UriInfo uriInfo) {
      return getRootFile(uriInfo);
   }

   @GET
   @Path("{file: \\w*\\.html}")
   public Response getRootFile(@Context UriInfo uriInfo) {
      return DocResource.redirectToLsssDoc(uriInfo, Response.Status.TEMPORARY_REDIRECT);
   }

   @Path("application")
   public ApplicationResource getApplicationResource() {
      return new ApplicationResource(lsss);
   }

   @Path("data")
   public DataResource getDataResource() {
      return new DataResource(lsss);
   }

   @Path("database")
   public DatabaseResource getDatabaseResource() {
      return new DatabaseResource(jaxRsApplication);
   }

   @Path("doc")
   public DocResource getDocResource() {
      return new DocResource();
   }

   @Path("events")
   public Class<EventsResource> getEventsResource() {
      return EventsResource.class;
   }

   @GET
   @Path("export")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ExportInfo> getExports() {
      return Utils.getAllOfType(lsss.getExportManager().getExporters(), StreamingExporter.class)
            .sorted(Utils.comparingIgnoringCase(export -> export.getName().persistentName()))
            .map(ExportInfo::new);
   }

   @Path("export/{id}")
   public ExportResource getExportResource(@PathParam("id") String id) {
      StreamingExporter exporter = Utils.getAllOfType(lsss.getExportManager().getExporters(), StreamingExporter.class)
            .filter(e -> e.getName().persistentName().equals(id))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(id));
      return new ExportResource(jaxRsApplication.getJsonMapper().writer(), exporter);
   }

   @GET
   @Path("module")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ModuleInfo> getModules(@QueryParam("enabled") @Nullable Boolean enabled,
                                        @QueryParam("hasData") @Nullable Boolean hasData,
                                        @QueryParam("hasView") @Nullable Boolean hasView,
                                        @QueryParam("hasOverlays") @Nullable Boolean hasOverlays,
                                        @QueryParam("data") @DefaultValue("false") boolean data) {
      return lsss.getModuleManager().getModules().stream()
            .filter(module -> module instanceof BaseViewModule || module instanceof BaseDataModule)
            .filter(module -> enabled == null || enabled == module.isEnabled())
            .filter(module -> hasData == null || hasData == module instanceof PojoDataContainer)
            .filter(module -> hasView == null || hasView == module instanceof BaseViewModule)
            .filter(module -> hasOverlays == null || hasOverlays == module instanceof BaseOverlaidModule)
            .sorted(Utils.comparingIgnoringCase(BaseLsssModule::getPersistentName))
            .map(module -> new ModuleInfo(module, data));
   }

   @Path("module/{id}")
   public ModuleResource<?> getModuleResource(@PathParam("id") String id) {
      BaseLsssModule module = lsss.getModuleManager().getModules().stream()
            .filter(m -> m.getPersistentName().equals(id))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(id));
      return switch (module) {
         case BaseModuleOverlay _ -> {
            throw new BadRequestException("Cannot access overlays this way. Instead try module/{overlaidModule}/overlay/{overlay}");
         }
         case ColorBarModule colorBarModule -> {
            yield new ColorBarModuleResource(colorBarModule);
         }
         case EchogramModule echogramModule -> {
            yield new EchogramModuleResource(echogramModule);
         }
         case InterpretationModule interpretationModule -> {
            yield new InterpretationModuleResource(interpretationModule);
         }
         default -> {
            yield new ModuleResource<>(module);
         }
      };
   }

   @Path("package")
   public PackageResource getAction() {
      return new PackageResource(lsss);
   }

   @GET
   @Path("plugin")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<PluginInfo> getPlugin() {
      return lsss.getPluginManager().getFeaturePlugins().stream()
            .sorted(Utils.comparingIgnoringCase(FeaturePlugin::getPersistentName))
            .map(PluginInfo::new);
   }

   @Path("plugin/{id}")
   public PluginResource getPluginResource(@PathParam("id") String id) {
      FeaturePlugin plugin = lsss.getPluginManager().getFeaturePlugins().stream()
            .filter(p -> p.getPersistentName().equals(id))
            .findFirst()
            .orElseThrow(() -> new NotFoundException(id));
      ResourceService service = idToResourceService.get(id);
      if (service != null) {
         PluginResource pluginResource = service.getPluginResource(plugin);
         if (pluginResource != null) {
            return pluginResource;
         }
      }
      return new PluginResource();
   }

   @Path("regions")
   public RegionsResource getRegionsResource() {
      return new RegionsResource(lsss);
   }

   @Path("survey")
   public SurveyResource getSurveyResource() {
      return new SurveyResource(lsss);
   }

   @Path("system")
   public SystemResource getSystemResource() {
      return new SystemResource();
   }
}
