package no.imr.korona.region;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DataManagerTestUtils;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.Curve;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class LayerEditTest {
   private DataManager dataManager;
   private LayerManager layerManager;

   private PingIndex idx1;
   private PingIndex idx2;
   private PingIndex idx3;

   @BeforeEach
   void beforeEach() {
      dataManager = DataManagerTestUtils.testDataManager();
      DataManagerTestUtils.open(dataManager, new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle());

      layerManager = RegionManagerTestUtils.createTestRegionManager(dataManager).getLayerManager();
      layerManager.setupInitialLayerBoundaries(_ -> 0, _ -> 500);

      PingRange totalRange = dataManager.getDataFileSet().getTotalRange();
      idx1 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 50);
      idx2 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 67);
      idx3 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 200);
   }

   @Test
   void testEditCurve() {
      EchogramPoint insertPoint1 = new EchogramPoint(idx1, 100);
      EchogramPoint insertPoint2 = new EchogramPoint(idx2, 200);

      EchogramPoint editPointStart = new EchogramPoint(idx1, 210);
      EchogramPoint editPointEnd = new EchogramPoint(idx2, 210);

      LayerAndBoundaryPair<CurveBoundary> p1 = layerManager.addCurveBoundary(insertPoint1, IdentityDepthTransform.INSTANCE);
      assertNotNull(p1);
      CurveBoundary b1 = p1.boundary();
      LayerAndBoundaryPair<CurveBoundary> p2 = layerManager.addCurveBoundary(insertPoint2, IdentityDepthTransform.INSTANCE);
      assertNotNull(p2);
      CurveBoundary b2 = p2.boundary();

      layerManager.editBoundary(editPointStart, editPointEnd, IdentityDepthTransform.INSTANCE, b1);
      for (long pingNumber = editPointStart.pingIndex().getPingNumber(); pingNumber < editPointEnd.pingIndex().getPingNumber(); pingNumber++) {
         PingIndex index = dataManager.getDataFileSet().getPingIndex(pingNumber);
         assertTrue(b1.getCurve().getDepth(index) <= b2.getCurve().getDepth(index));
      }
   }

   @Test
   void testEditConnector() {
      EchogramPoint insertPoint1 = new EchogramPoint(idx1, 100);
      EchogramPoint insertPoint2 = new EchogramPoint(idx1, 150);

      layerManager.addCurveBoundary(insertPoint1, IdentityDepthTransform.INSTANCE);
      layerManager.addVerticalBoundary(insertPoint2);

      CurveBoundary endBoundary = layerManager.findClosestCurveBoundary(insertPoint1, FloatRange.of(0, 500));
      assertNotNull(endBoundary);

      LayerConnector connector = layerManager.findClosestLayerConnector(insertPoint1);
      assertNotNull(connector);

      EchogramPoint moveToPoint = new EchogramPoint(idx1, 90);

      layerManager.editConnector(moveToPoint, IdentityDepthTransform.INSTANCE, connector);

      assertTrue(endBoundary.getCurve().getStartDepth() < 100);
   }

   @Test
   void testSearchForSubCurves() {
      EchogramPoint insertPoint1 = new EchogramPoint(idx1, 100);
      EchogramPoint insertPoint2 = new EchogramPoint(idx2, 50);

      LayerAndBoundaryPair<CurveBoundary> pair = layerManager.addCurveBoundary(insertPoint1, IdentityDepthTransform.INSTANCE);
      assertNotNull(pair);
      CurveBoundary curve = pair.boundary();
      layerManager.addVerticalBoundary(insertPoint2);

      PingRange range = PingRange.of(idx1, idx3);
      List<Curve> curves = curve.searchForCurvesInPingRange(range);

      assertEquals(2, curves.size());
   }
}
