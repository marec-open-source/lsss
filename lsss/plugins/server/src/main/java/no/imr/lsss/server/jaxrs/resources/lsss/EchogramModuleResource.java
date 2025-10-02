package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.region.EchogramSelection;
import no.imr.korona.region.School;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.server.pojo.ApiEchogramPoint;
import no.imr.lsss.server.pojo.ApiPingMask;
import no.imr.lsss.server.pojo.RegionInfo;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.math.Function1D;
import no.imr.tools.misc.SelectionAction;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.NavigableMap;

public final class EchogramModuleResource extends ModuleResource<EchogramModule> {
   private final LSSS lsss;
   private final DataFileSet dataFileSet;

   EchogramModuleResource(EchogramModule module) {
      super(module);

      lsss = module.getLSSS();
      dataFileSet = lsss.getInterpretationSettings().getDataFileSet();
   }

   @GET
   @Path("current-echogram-point")
   @Produces(MediaType.APPLICATION_JSON)
   public ApiEchogramPoint getCurrentEchogramPoint() {
      PingIndex pingIndex = lsss.getInterpretationSettings().mouseover().getPingIndex();
      if (pingIndex == null) {
         return new ApiEchogramPoint();
      }
      Float depth = lsss.getInterpretationSettings().mouseover().getDepth();
      Float z = depth != null ? module.getZSettings().depthToZ(depth, pingIndex) : null;
      return new ApiEchogramPoint(pingIndex, z);
   }

   @POST
   @Path("current-echogram-point")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setCurrentEchogramPoint(ApiEchogramPoint apiEchogramPoint) {
      PingIndex pingIndex = toPingIndex(apiEchogramPoint);
      if (pingIndex.equals(dataFileSet.getTotalRange().end())) {
         pingIndex = dataFileSet.previousOrSame(pingIndex);
      }
      Float depth = apiEchogramPoint.z != null
            ? module.getZSettings().zToDepth(apiEchogramPoint.z, pingIndex)
            : null;
      lsss.getInterpretationSettings().mouseover().setPos(pingIndex, depth);
   }

   @POST
   @Path("horizontal-layer-boundary")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addLayerHorizontalBoundary(List<ApiEchogramPoint> apiEchogramPoints) {
      if (apiEchogramPoints.isEmpty()) {
         throw new BadRequestException("No points");
      }
      EchogramPingSettings pingSettings = lsss.getInterpretationSettings().getPingSettings();
      double[] xValues = new double[apiEchogramPoints.size()];
      double[] zValues = new double[apiEchogramPoints.size()];
      for (int i = 0; i < apiEchogramPoints.size(); i++) {
         ApiEchogramPoint apiEchogramPoint = apiEchogramPoints.get(i);
         PingIndex pingIndex = toPingIndexOrNull(apiEchogramPoint);
         if (pingIndex == null || apiEchogramPoint.z == null) {
            throw new BadRequestException("Invalid point at index " + i + ": " + LsssServerUtils.toJsonString(apiEchogramPoint));
         }
         xValues[i] = pingSettings.pingIndexToX(pingIndex);
         zValues[i] = apiEchogramPoint.z;
      }
      Function1D xToZ = Function1D.interpolate(xValues, zValues);
      ToFloatFunction<PingIndex> pingIndexToDepth = pingIndex -> {
         float x = pingSettings.pingIndexToX(pingIndex);
         return module.getZSettings().zToDepth(xToZ.eval(x), pingIndex);
      };
      PingIndex pingIndex = toPingIndex(apiEchogramPoints.get(apiEchogramPoints.size() / 2));
      lsss.getRegionManager().addHorizontalLayerBoundary(pingIndex, pingIndexToDepth);
   }

   @POST
   @Path("school-boundary")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public RegionInfo addSchoolBoundary(List<ApiEchogramPoint> apiEchogramPoints) {
      List<EchogramPoint> echogramPoints = apiEchogramPoints.stream()
            .map(this::toEchogramPoint)
            .toList();
      School school = lsss.getRegionManager().addSchool(echogramPoints, module.getZSettings().getDepthTransform());
      if (school == null) {
         throw new BadRequestException("Could not add school");
      }
      return new RegionInfo(school, lsss);
   }

   @POST
   @Path("school-mask")
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   public RegionInfo addSchoolMask(List<ApiPingMask> apiPingMasks) {
      NavigableMap<PingIndex, FloatRangeSet> mask = LsssServerUtils.toMask(lsss.getInterpretationSettings().getDataFileSet(), apiPingMasks);
      School school = lsss.getRegionManager().addSchool(mask);
      if (school == null) {
         throw new BadRequestException("Could not add school");
      }
      return new RegionInfo(school, lsss);
   }

   @POST
   @Path("select-point")
   @Consumes(MediaType.APPLICATION_JSON)
   public void selectPoint(@QueryParam("action") @DefaultValue("REPLACE") String actionName, ApiEchogramPoint apiEchogramPoint) {
      PingIndex pingIndex = toPingIndexOrNull(apiEchogramPoint);
      PingRange pingRange = toSelectionPingRange(pingIndex, pingIndex);
      FloatRange zRange = toSelectionZRange(apiEchogramPoint, apiEchogramPoint);
      SelectionAction action = LsssServerUtils.nameToEnum(SelectionAction.class, actionName);
      EchogramRectangle echogramRectangle = new EchogramRectangle(pingRange, zRange, module.getZSettings().getDepthTransform());
      lsss.getRegionManager().doEchogramSelection(new EchogramSelection(action, echogramRectangle));
   }

   @POST
   @Path("select-rectangle")
   @Consumes(MediaType.APPLICATION_JSON)
   public void selectRectangle(@QueryParam("action") @DefaultValue("REPLACE") String actionName, List<ApiEchogramPoint> apiEchogramPoints) {
      LsssServerUtils.validateEchogramPoints(apiEchogramPoints, 2);

      ApiEchogramPoint a = apiEchogramPoints.get(0);
      ApiEchogramPoint b = apiEchogramPoints.get(1);

      PingRange pingRange = toSelectionPingRange(toPingIndexOrNull(a), toPingIndexOrNull(b));
      FloatRange zRange = toSelectionZRange(a, b);

      SelectionAction action = LsssServerUtils.nameToEnum(SelectionAction.class, actionName);
      EchogramRectangle echogramRectangle = new EchogramRectangle(pingRange, zRange, module.getZSettings().getDepthTransform());
      lsss.getRegionManager().doEchogramSelection(new EchogramSelection(action, echogramRectangle));
   }

   @POST
   @Path("vertical-divider")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addVerticalDivider(ApiEchogramPoint apiEchogramPoint) {
      PingIndex pingIndex = toPingIndex(apiEchogramPoint);
      lsss.getRegionManager().addVerticalDivider(pingIndex);
   }

   @POST
   @Path("vertical-layer-boundary")
   @Consumes(MediaType.APPLICATION_JSON)
   public void addLayerVerticalBoundary(ApiEchogramPoint apiEchogramPoint) {
      EchogramPoint echogramPoint = toEchogramPoint(apiEchogramPoint);
      lsss.getRegionManager().getLayerManager().addVerticalBoundary(echogramPoint);
   }

   @GET
   @Path("zoom")
   @Produces(MediaType.APPLICATION_JSON)
   public List<ApiEchogramPoint> zoom() {
      PingRange pingRange = lsss.getInterpretationSettings().getPingRange();
      FloatRange zRange = module.getZSettings().getZoomedZRange();
      ApiEchogramPoint a = new ApiEchogramPoint(pingRange.begin(), zRange.min());
      ApiEchogramPoint b = new ApiEchogramPoint(pingRange.end(), zRange.max());
      return List.of(a, b);
   }

   @POST
   @Path("zoom")
   @Consumes(MediaType.APPLICATION_JSON)
   public void zoom(List<ApiEchogramPoint> apiEchogramPoints) {
      LsssServerUtils.validateEchogramPoints(apiEchogramPoints, 2);

      ApiEchogramPoint a = apiEchogramPoints.get(0);
      ApiEchogramPoint b = apiEchogramPoints.get(1);

      PingIndex pingIndexA = toPingIndexOrNull(a);
      PingIndex pingIndexB = toPingIndexOrNull(b);
      if (pingIndexA != null && pingIndexB != null) {
         lsss.getInterpretationSettings().setPingRange(PingRange.ofUnsorted(pingIndexA, pingIndexB));
      }
      if (a.z != null && b.z != null) {
         module.getZSettings().setZ(FloatRange.ofUnsorted(a.z, b.z));
      }
   }

   @GET
   @Path("zoom/max")
   @Produces(MediaType.APPLICATION_JSON)
   public List<ApiEchogramPoint> zoomMax() {
      PingRange pingRange = lsss.getInterpretationSettings().getDataFileSet().getTotalRange();
      FloatRange zRange = module.getZSettings().getMaxZRange();
      ApiEchogramPoint a = new ApiEchogramPoint(pingRange.begin(), zRange.min());
      ApiEchogramPoint b = new ApiEchogramPoint(pingRange.end(), zRange.max());
      return List.of(a, b);
   }

   private @Nullable PingIndex toPingIndexOrNull(ApiEchogramPoint apiEchogramPoint) {
      return LsssServerUtils.toPingIndexOrNull(dataFileSet, apiEchogramPoint.pingIndex);
   }

   private PingIndex toPingIndex(ApiEchogramPoint apiEchogramPoint) {
      PingIndex pingIndex = toPingIndexOrNull(apiEchogramPoint);
      if (pingIndex == null) {
         throw new BadRequestException("No ping index for " + LsssServerUtils.toJsonString(apiEchogramPoint));
      }
      return pingIndex;
   }

   private static float toZ(ApiEchogramPoint apiEchogramPoint) {
      Float z = apiEchogramPoint.z;
      if (z == null) {
         throw new BadRequestException("No z for " + LsssServerUtils.toJsonString(apiEchogramPoint));
      }
      return z;
   }

   private EchogramPoint toEchogramPoint(ApiEchogramPoint apiEchogramPoint) {
      PingIndex pingIndex = toPingIndex(apiEchogramPoint);
      float z = toZ(apiEchogramPoint);
      float depth = module.getZSettings().zToDepth(z, pingIndex);
      return new EchogramPoint(pingIndex, depth);
   }

   private PingRange toSelectionPingRange(@Nullable PingIndex a, @Nullable PingIndex b) {
      return a == null || b == null
            ? dataFileSet.getTotalRange()
            : PingRange.from(List.of(a, b), dataFileSet);
   }

   private static FloatRange toSelectionZRange(ApiEchogramPoint a, ApiEchogramPoint b) {
      return a.z == null || b.z == null
            ? FloatRange.ALL
            : FloatRange.ofUnsorted(a.z, b.z).expandToNonDegenerated();
   }
}
