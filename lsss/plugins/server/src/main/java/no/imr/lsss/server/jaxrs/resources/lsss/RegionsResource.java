package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.IllegalEditException;
import no.imr.korona.region.Layer;
import no.imr.korona.region.Mask;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.export.ExportUtils;
import no.imr.lsss.server.pojo.ApiEchogramPoint;
import no.imr.lsss.server.pojo.ApiPingMask;
import no.imr.lsss.server.pojo.RegionInfo;
import no.imr.lsss.server.pojo.RegionSelectionRequest;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class RegionsResource {
   private final LSSS lsss;

   RegionsResource(LSSS lsss) {
      this.lsss = lsss;
   }

   @GET
   @Path("deletion")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ApiPingMask> getDeletion() {
      Mask mask = lsss.getRegionManager().getMaskingManager().getMask(lsss.getInterpretationSettings().getChannel());
      return lsss.getInterpretationSettings().getDataFileSet().getPingIndices().stream()
            .mapMulti((pingIndex, consumer) -> {
               List<FloatRange> depthRanges = mask.get(pingIndex).getFloatRanges();
               if (!depthRanges.isEmpty()) {
                  consumer.accept(new ApiPingMask(pingIndex, depthRanges));
               }
            });
   }

   @POST
   @Path("deletion")
   @Consumes(MediaType.APPLICATION_JSON)
   public void postDeletion(@QueryParam("delete") @DefaultValue("true") boolean delete,
                            @QueryParam("allFrequencies") @DefaultValue("false") boolean allFrequencies,
                            List<ApiPingMask> apiPingMasks) {
      NavigableMap<PingIndex, FloatRangeSet> mask = LsssServerUtils.toMask(lsss.getInterpretationSettings().getDataFileSet(), apiPingMasks);
      if (delete) {
         if (allFrequencies) {
            lsss.getRegionManager().getMaskingManager().maskAllChannels(mask);
         } else {
            lsss.getRegionManager().getMaskingManager().mask(mask, lsss.getInterpretationSettings().getChannel());
         }
      } else {
         if (allFrequencies) {
            lsss.getRegionManager().getMaskingManager().unmaskAllChannels(mask);
         } else {
            lsss.getRegionManager().getMaskingManager().unmask(mask, lsss.getInterpretationSettings().getChannel());
         }
      }
   }

   @GET
   @Path("exclusion")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<List<ApiEchogramPoint>> getExclusion() {
      return lsss.getRegionManager().getExclusionManager().getExclusions().stream()
            .map(pingRange -> List.of(new ApiEchogramPoint(pingRange.begin()), new ApiEchogramPoint(pingRange.end())));
   }

   @POST
   @Path("exclusion")
   @Consumes(MediaType.APPLICATION_JSON)
   public void postExclusion(@QueryParam("exclude") @DefaultValue("true") boolean exclude, List<ApiEchogramPoint> apiEchogramPoints) {
      PingRange pingRange = LsssServerUtils.toPingRange(lsss.getInterpretationSettings().getDataFileSet(), apiEchogramPoints);
      if (exclude) {
         lsss.getRegionManager().getExclusionManager().excludeRange(pingRange);
      } else {
         lsss.getRegionManager().getExclusionManager().includeRange(pingRange);
      }
   }

   @GET
   @Path("label")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<String> getLabels(@BeanParam RegionSelectionRequest request) {
      return lsss.getRegionManager().regionStream()
            .filter(getRegionPredicate(request))
            .flatMap(region -> region.getLabels().stream())
            .distinct()
            .sorted();
   }

   @Path("label/{label}")
   public RegionsLabelResource getLabelResource(@PathParam("label") String label) {
      return new RegionsLabelResource(lsss, label);
   }

   @POST
   @Path("merge-layers")
   public Response mergeLayers() {
      List<String> errors = lsss.getRegionManager().getLayerManager().mergeSelectedLayers();
      if (errors.isEmpty()) {
         return Response.ok()
               .build();
      } else {
         return Response.status(Response.Status.BAD_REQUEST)
               .entity(errors)
               .type(MediaType.APPLICATION_JSON)
               .build();
      }
   }

   @GET
   @Path("region")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<RegionInfo> region(@BeanParam RegionSelectionRequest request,
                                    @QueryParam("all") boolean all,
                                    @QueryParam("type") @Nullable Boolean type,
                                    @QueryParam("labels") @Nullable Boolean labels,
                                    @QueryParam("scrutiny") @Nullable Boolean scrutiny,
                                    @QueryParam("boundingBox") @Nullable Boolean boundingBox) {
      return lsss.getRegionManager().regionStream()
            .filter(getRegionPredicate(request))
            .map(region -> {
               RegionInfo regionInfo = new RegionInfo(region.getObjectNumber());
               if (isIncluded(all, type)) {
                  regionInfo.type = RegionInfo.toType(region);
               }
               if (isIncluded(all, labels)) {
                  regionInfo.labels = region.getLabels();
               }
               if (isIncluded(all, scrutiny)) {
                  regionInfo.scrutiny = ExportUtils.makeScrutiny(lsss, region.getInterpretation());
               }
               if (isIncluded(all, boundingBox)) {
                  regionInfo.boundingBox = LsssServerUtils.toBoundingBox(region, lsss);
               }
               return regionInfo;
            });
   }

   private static boolean isIncluded(boolean all, @Nullable Boolean option) {
      return all && (option == null || option) || (option != null && option);
   }

   @Path("region/{id}")
   public RegionResource region(@Context Request request, @PathParam("id") int id) {
      Region region = lsss.getRegionManager().regionStream()
            .filter(r -> r.getObjectNumber() == id)
            .findFirst()
            .orElseThrow(() -> new NotFoundException(Integer.toString(id)));
      if (!request.getMethod().equals(HttpMethod.GET) && region.isReadOnly()) {
         throw new IllegalEditException();
      }
      return new RegionResource(lsss, region);
   }

   @GET
   @Path("selection")
   @Produces(MediaType.APPLICATION_JSON)
   public IntStream getSelection() {
      return lsss.getRegionManager().getSelectedRegions().stream()
            .mapToInt(Region::getObjectNumber);
   }

   @POST
   @Path("selection")
   @Consumes(MediaType.APPLICATION_JSON)
   public void postSelection(RegionSelectionRequest request) {
      Predicate<Region> predicate = getRegionPredicate(request);

      switch (request.operation) {
         case ADD -> lsss.getRegionManager().selectRegions(predicate);
         case REMOVE -> lsss.getRegionManager().deselectRegions(predicate);
         case RETAIN -> lsss.getRegionManager().deselectRegions(predicate.negate());
         case SET -> lsss.getRegionManager().replaceSelectedRegions(predicate);
      }
   }

   private Predicate<Region> getRegionPredicate(RegionSelectionRequest request) {
      if (request.idsAsQueryParam != null) {
         try {
            request.ids = JsonUtils.parseSet(request.idsAsQueryParam, Integer.class);
         } catch (Exception e) {
            throw new BadRequestException("Error parsing ids: " + e.getMessage(), e);
         }
      }
      if (request.labelsAsQueryParam != null) {
         try {
            request.labels = JsonUtils.parseSet(request.labelsAsQueryParam, String.class);
         } catch (Exception e) {
            throw new BadRequestException("Error parsing labels: " + e.getMessage(), e);
         }
      }

      List<Predicate<Region>> predicates = new ArrayList<>();
      if (request.all != null) {
         predicates.add(_ -> request.all);
      }
      if (request.layers != null) {
         predicates.add(region -> request.layers == region instanceof Layer);
      }
      if (request.schools != null) {
         predicates.add(region -> request.schools == region instanceof School);
      }
      if (request.selected != null) {
         predicates.add(region -> request.selected == region.isSelected());
      }
      if (request.visible != null) {
         predicates.add(region -> request.visible == region.getPingRange().intersects(lsss.getInterpretationSettings().getPingRange()));
      }
      if (request.ids != null) {
         predicates.add(region -> request.ids.contains(region.getObjectNumber()));
      }
      if (request.labels != null) {
         predicates.add(region -> region.getLabels().containsAll(request.labels));
      }

      return predicates.stream()
            .reduce(Predicate::and)
            .orElse(_ -> true);
   }
}
