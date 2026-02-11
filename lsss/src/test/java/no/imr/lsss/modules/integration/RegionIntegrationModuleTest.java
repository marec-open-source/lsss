package no.imr.lsss.modules.integration;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.DataLoadingMode;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class RegionIntegrationModuleTest {
   private static final float SV_VALUE = PowerData.logSvToSv(-56);

   private static final class TestSyntheticData extends SyntheticData {
      @Override
      protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
         float[] sv = new float[2000];
         Arrays.fill(sv, SV_VALUE);
         powerData.setSv(sv);
      }

      @Override
      protected float getBottomDepth(PingIndex pingIndex, int channel) {
         return 500;
      }
   }

   private LSSS lsss;
   private DataManager dataManager;
   private InterpretationSettings interpretationSettings;
   private RegionManager regionManager;
   private RegionIntegrationModule regionIntegrationModule;

   @BeforeEach
   void beforeEach() {
      lsss = LsssTestUtils.start(List.of(RegionIntegrationModule.class), List.of());
      dataManager = lsss.getDataManager();
      interpretationSettings = lsss.getInterpretationSettings();
      regionManager = lsss.getRegionManager();
      regionIntegrationModule = lsss.getModuleManager().getModule(RegionIntegrationModule.class);

      LsssTestUtils.open(lsss, new TestSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle());
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   @Test
   void testSa() {
      DataFileSet dataFileSet = dataManager.getDataFileSet();
      doCheck(PingRange.of(dataFileSet.getPingIndex(30), dataFileSet.getPingIndex(269)), FloatRange.of(100, 200));
      doCheck(PingRange.of(dataFileSet.getPingIndex(145), dataFileSet.getPingIndex(681)), FloatRange.of(100, 300));
   }

   private void doCheck(PingRange pingRange, FloatRange depthRange) {
      PingIndex centerPingIndex = dataManager.getDataFileSet().getPingIndex((pingRange.begin().getPingNumber() + pingRange.end().getPingNumber()) / 2);
      float centerDepth = depthRange.getCenter();

      interpretationSettings.setDataLoadingMode(DataLoadingMode.BROWSE);
      interpretationSettings.setPingRange(dataManager.getDataFileSet().getTotalRange());

      regionManager.replaceSelectedRegions(regionManager.getLayerManager().getLayers());
      regionManager.getLayerManager().mergeSelectedLayers();
      assertEquals(1, regionManager.getLayerManager().getLayers().size());

      regionManager.getLayerManager().addVerticalBoundary(new EchogramPoint(pingRange.begin(), centerDepth));
      regionManager.getLayerManager().addVerticalBoundary(new EchogramPoint(pingRange.end(), centerDepth));

      regionManager.getLayerManager().addCurveBoundary(new EchogramPoint(centerPingIndex, depthRange.min()), IdentityDepthTransform.INSTANCE);
      regionManager.getLayerManager().addCurveBoundary(new EchogramPoint(centerPingIndex, depthRange.max()), IdentityDepthTransform.INSTANCE);
      assertEquals(5, regionManager.getLayerManager().getLayers().size());

      Region region = regionManager.getRegion(new EchogramPoint(centerPingIndex, centerDepth));
      assertNotNull(region);
      regionManager.replaceSelectedRegions(region);
      assertEquals(1, regionManager.getSelectedRegions().size());

      FloatRangeSet depthRanges = regionManager.getDepthRangesForChannel(region, dataManager.getDataFileSet().getPing(centerPingIndex), interpretationSettings.getChannel());
      assertEquals(List.of(depthRange), depthRanges.getFloatRanges());

      interpretationSettings.setPingRange(pingRange);
      interpretationSettings.setDataLoadingMode(DataLoadingMode.DETAIL);

      interpretationSettings.waitUntilFinished();

      PowerData powerData = interpretationSettings.getDataFileSet().getPing(pingRange.begin()).getPowerData(interpretationSettings.getChannel());
      assertNotNull(powerData);

      float expectedSa = SV_VALUE * depthRange.getSize();
      float actualSa = regionIntegrationModule.getSa(IntegrationArea.TOTAL);
      float errorTol = SV_VALUE * 1e-4f;
      assertEquals(expectedSa, actualSa, errorTol);

      List<IntegrationCurvePoint> curve = regionIntegrationModule.getSaCurve(region);
      assertEquals(pingRange.getPingCount() + 1, curve.size());
      for (int i = 0; i < curve.size(); i++) {
         IntegrationCurvePoint point = curve.get(i);
         assertEquals(i, point.pingIndex().getPingNumber() - pingRange.begin().getPingNumber());
         PingCache pingCache = point.pingCache();
         if (i == curve.size() - 1) {
            assertNull(pingCache);
         } else {
            assertNotNull(pingCache);
            assertEquals(expectedSa, pingCache.getVerticallyIntegratedSvTotal(), errorTol);
         }
         float accumulatedDistance = point.accumulatedDistance();
         assertEquals((float) PingMapping.DISTANCE.distance(pingRange.begin(), point.pingIndex()), accumulatedDistance);
         assertEquals(expectedSa * accumulatedDistance, point.getHorizontallyIntegratedSv(IntegrationArea.TOTAL), errorTol * accumulatedDistance);
      }
   }
}
