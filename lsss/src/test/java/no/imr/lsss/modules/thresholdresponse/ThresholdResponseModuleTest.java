package no.imr.lsss.modules.thresholdresponse;

import no.imr.korona.data.formats.synthetic.TestSyntheticData;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Region;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.integration.IntegrationArea;
import no.imr.lsss.modules.integration.RegionIntegrationModule;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ThresholdResponseModuleTest {
   private LSSS lsss;

   @BeforeEach
   void beforeEach() {
      lsss = LsssTestUtils.start(List.of(RegionIntegrationModule.class), List.of(ThresholdResponseModule.class));
      LsssTestUtils.open(lsss, new TestSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle());
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   @Test
   void test() {
      List<Region> regions = lsss.getRegionManager().regionStream().toList();
      assertEquals(1, regions.size());
      Region region = regions.getFirst();
      lsss.getRegionManager().replaceSelectedRegions(region);

      test(region, -82, -30);
      test(region, -60, -30);
      test(region, -50, -40);
   }

   private void test(Region region, float minLogSv, float maxLogSv) {
      FloatRange logSvRange = FloatRange.of(minLogSv, maxLogSv);
      PingRange pingRange = region.getPingRange();
      lsss.getRegionManager().getThresholdManager().set(pingRange, true, logSvRange.min(), logSvRange.max());

      RegionIntegrationModule regionIntegrationModule = lsss.getModuleManager().getModule(RegionIntegrationModule.class);
      ThresholdResponseModule thresholdResponseModule = lsss.getModuleManager().getModule(ThresholdResponseModule.class);

      lsss.getInterpretationSettings().waitUntilFinished();

      float sa = regionIntegrationModule.getSa(region, IntegrationArea.TOTAL);
      assertTrue(sa > 0);
      assertEquals(sa, thresholdResponseModule.getSa(region, logSvRange), sa * 1e-6);
   }
}
