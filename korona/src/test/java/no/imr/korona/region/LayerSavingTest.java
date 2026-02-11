package no.imr.korona.region;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DataManagerTestUtils;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class LayerSavingTest {
   @Test
   void testLayerRestoration() throws WorkFileException {
      DataManager dataManager = DataManagerTestUtils.testDataManager();
      RegionManager regionManager = RegionManagerTestUtils.createTestRegionManager(dataManager);

      SegmentHandle segmentHandle1 = new ConstantSyntheticData().withFirstAndLastPingNumber(1, 100).toSegmentHandle();
      SegmentHandle segmentHandle2 = new ConstantSyntheticData().withFirstAndLastPingNumber(101, 110).toSegmentHandle();

      //load both files
      DataManagerTestUtils.open(dataManager, segmentHandle1, segmentHandle2);
      regionManager.setupDefaultBoundaries(_ -> 15, _ -> 500);

      //create a divider exactly at the file boundary
      regionManager.addVerticalDivider(dataManager.getDataFileSet().getPingIndex(101));
      //create a curve boundary in the first file
      regionManager.getLayerManager().addCurveBoundary(new EchogramPoint(dataManager.getDataFileSet().getPingIndex(50), 30), IdentityDepthTransform.INSTANCE);

      //save interpretation
      Element xml = regionManager.toXml(PingRange.of(
            dataManager.getDataFileSet().getPingIndex(101),
            dataManager.getDataFileSet().getTotalRange().end()));

      //load only second file
      DataManagerTestUtils.open(dataManager, segmentHandle2);
      regionManager.setupDefaultBoundaries(_ -> 0, _ -> 0);
      regionManager.fromXml(xml);

      RegionValidation.checkLayers(regionManager.getLayerManager());

      List<Region> regions = regionManager.regionStream().toList();
      assertEquals(1, regions.size());

      Region region = regions.getFirst();
      PingRange expectedRange = PingRange.of(
            dataManager.getDataFileSet().getPingIndex(101),
            dataManager.getDataFileSet().getTotalRange().end());
      assertEquals(expectedRange, region.getPingRange());

      assertEquals(FloatRangeSet.of(FloatRange.of(15, 500)), region.getDepthRanges(region.getPingRange().begin()));
   }
}
