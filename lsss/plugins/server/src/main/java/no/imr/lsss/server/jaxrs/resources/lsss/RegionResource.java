package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.region.Region;
import no.imr.korona.region.School;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.export.ExportUtils;
import no.imr.lsss.framework.export.pojo.ExportScrutiny;
import no.imr.lsss.server.pojo.ApiPingMask;
import no.imr.lsss.server.pojo.RegionInfo;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.stream.Stream;

public final class RegionResource {
   private final LSSS lsss;
   private final Region region;

   RegionResource(LSSS lsss, Region region) {
      this.lsss = lsss;
      this.region = region;
   }

   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public RegionInfo getInfo() {
      return new RegionInfo(region, lsss);
   }

   @DELETE
   @Produces(MediaType.APPLICATION_JSON)
   public void delete() {
      if (!(region instanceof School school)) {
         throw new BadRequestException("Only applicable to schools");
      }
      lsss.getRegionManager().deleteSchool(school);
   }

   @GET
   @Path("mask")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ApiPingMask> getMask() {
      return lsss.getInterpretationSettings().getDataFileSet().getPingIndexStream(region.getPingRange())
            .map(pingIndex -> {
               List<FloatRange> depthRanges = lsss.getRegionManager().getNonMaskedRegionDepthRanges(region, pingIndex).getFloatRanges();
               return depthRanges.isEmpty() ? null : new ApiPingMask(pingIndex, depthRanges);
            })
            .filter(Objects::nonNull);
   }

   @POST
   @Path("mask")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setMask(List<ApiPingMask> apiPingMasks) {
      if (!(region instanceof School school)) {
         throw new BadRequestException("Only applicable to schools");
      }
      NavigableMap<PingIndex, FloatRangeSet> mask = LsssServerUtils.toMask(lsss.getInterpretationSettings().getDataFileSet(), apiPingMasks);
      lsss.getRegionManager().setSchoolMask(school, mask);
   }

   @GET
   @Path("scrutiny")
   @Produces(MediaType.APPLICATION_JSON)
   public ExportScrutiny getScrutiny() {
      return ExportUtils.makeScrutiny(lsss, region.getInterpretation());
   }

   @POST
   @Path("scrutiny")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setScrutiny(ExportScrutiny scrutiny) {
      ExportUtils.applyScrutiny(lsss, List.of(region.getInterpretation()), scrutiny);
   }
}
