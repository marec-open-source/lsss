package no.imr.korona.region;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DataManagerTestUtils;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class LayerTest {
   private DataManager dataManager;
   private LayerManager layerManager;

   private PingIndex idx0;
   private PingIndex idx1;
   private PingIndex idx2;
   private PingIndex idx3;
   private PingIndex idx4;

   @BeforeEach
   void beforeEach() {
      dataManager = DataManagerTestUtils.testDataManager();
      DataManagerTestUtils.open(dataManager, new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle());

      layerManager = RegionManagerTestUtils.createTestRegionManager(dataManager).getLayerManager();
      layerManager.setupInitialLayerBoundaries(_ -> 0, _ -> 500);

      PingRange totalRange = dataManager.getDataFileSet().getTotalRange();
      idx0 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber());
      idx1 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 50);
      idx2 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 67);
      idx3 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 200);
      idx4 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 290);
   }

   @Test
   void initialLayer() {
      PingIndex firstIdx = dataManager.getDataFileSet().getTotalRange().begin();
      PingIndex lastIdx = dataManager.getDataFileSet().getTotalRange().end();

      Layer layer = layerManager.getLayer(new EchogramPoint(firstIdx, 100));

      assertNotNull(layer);
      assertNull(layerManager.getLayer(new EchogramPoint(firstIdx, -100)));

      assertNotNull(layerManager.getLayer(new EchogramPoint(idx0, 100)));
      assertNull(layerManager.getLayer(new EchogramPoint(idx4, -100)));

      assertNull(layerManager.getLayer(new EchogramPoint(lastIdx, 100)));
      assertNotNull(layerManager.getLayer(new EchogramPoint(dataManager.getDataFileSet().getPingIndex(lastIdx.getPingNumber() - 1), 100)));
   }

   @Test
   void mergeLayers() {
      EchogramPoint point1 = new EchogramPoint(idx1, 100);
      EchogramPoint point2 = new EchogramPoint(idx2, 200);
      EchogramPoint point3 = new EchogramPoint(idx3, 300);

      assertSame(layerManager.getLayer(point1), layerManager.getLayer(point3));

      //layerManager.addCurveBoundary(point2);
      layerManager.addVerticalBoundary(point2);

      assertNotSame(layerManager.getLayer(point1), layerManager.getLayer(point3));

      layerManager.mergeLayers(List.of(
            Objects.requireNonNull(layerManager.getLayer(point1)),
            Objects.requireNonNull(layerManager.getLayer(point3))
      ));

      assertSame(layerManager.getLayer(point1), layerManager.getLayer(point3));
   }

   @Test
   void addCurveBoundary() {
      EchogramPoint point1 = new EchogramPoint(idx1, 100);
      EchogramPoint point2 = new EchogramPoint(idx2, 200);
      EchogramPoint point3 = new EchogramPoint(idx2, 300);
      EchogramPoint point4 = new EchogramPoint(idx3, 400);

      layerManager.addCurveBoundary(point2, IdentityDepthTransform.INSTANCE);

      Layer upperLayer = layerManager.getLayer(point1);
      assertNotNull(upperLayer);
      layerManager.selectRegion(upperLayer);

      assertEquals(Set.of(upperLayer), layerManager.getSelectedRegions());

      layerManager.addVerticalBoundary(point3);

      assertEquals(Set.of(upperLayer), layerManager.getSelectedRegions());

      layerManager.addCurveBoundary(point4, IdentityDepthTransform.INSTANCE);
      Layer layer = layerManager.getLayer(point4);
      assertNotNull(layer);
      CurveBoundary upper = layer.findUpperBoundary(point4.pingIndex());
      CurveBoundary lower = layer.findLowerBoundary(point4.pingIndex());
      assertNotEquals(upper, lower);
   }

   @Test
   void addVerticalBoundary() {
      EchogramPoint point1 = new EchogramPoint(idx1, 100);
      EchogramPoint point2 = new EchogramPoint(idx1, 200);
      EchogramPoint point3 = new EchogramPoint(idx1, 300);

      EchogramPoint point4 = new EchogramPoint(idx1, 150);
      EchogramPoint point5 = new EchogramPoint(idx2, 250);
      EchogramPoint point6 = new EchogramPoint(idx3, 150);

      layerManager.addCurveBoundary(point1, IdentityDepthTransform.INSTANCE);
      layerManager.addCurveBoundary(point2, IdentityDepthTransform.INSTANCE);
      layerManager.addCurveBoundary(point3, IdentityDepthTransform.INSTANCE);

      layerManager.addVerticalBoundary(point4);
      layerManager.addVerticalBoundary(point5);
      layerManager.addVerticalBoundary(point6);
   }

   @Test
   void editBoundary() {
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
   void editConnector() {
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
}
