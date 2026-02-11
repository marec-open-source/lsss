package no.imr.korona.region;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DataManagerTestUtils;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.data.util.mask.MaskOutlineTracer;
import no.imr.korona.data.util.mask.MaskUtils;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.CyclicList;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

final class SchoolTest {
   private DataManager dataManager;
   private RegionManager regionManager;

   private PingIndex idx0;
   private PingIndex idx1;

   @BeforeEach
   void beforeEach() {
      dataManager = DataManagerTestUtils.testDataManager();
      DataManagerTestUtils.open(dataManager, new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle());

      regionManager = RegionManagerTestUtils.createTestRegionManager(dataManager);
      regionManager.getLayerManager().setupInitialLayerBoundaries(_ -> 0, _ -> 500);

      PingRange totalRange = dataManager.getDataFileSet().getTotalRange();
      idx0 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber());
      idx1 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 50);
   }

   @Test
   void testSchoolSubtraction() {
      float d1 = 100;
      float d2 = 200;
      float d3 = 300;
      float d4 = 400;

      int channel = 1;

      Layer layer = regionManager.getLayerManager().getRegion(new EchogramPoint(idx0, d1));
      assertNotNull(layer);
      FloatRange layerDepthRange = layer.getDepthRange(idx0);

      // Add one school.
      School school1 = regionManager.addSchool(new EchogramPoint(idx0, d1), new EchogramPoint(idx1, d2), IdentityDepthTransform.INSTANCE);
      assertNotNull(school1);
      assertEquals(List.of(
                  FloatRange.of(layerDepthRange.min(), d1),
                  FloatRange.of(d2, layerDepthRange.max())
            ),
            regionManager.getDepthRangesForChannel(layer, dataManager.getDataFileSet().getPing(idx0), channel).getFloatRanges()
      );
      // Add another school.
      School school2 = regionManager.addSchool(new EchogramPoint(idx0, d3), new EchogramPoint(idx1, d4), IdentityDepthTransform.INSTANCE);
      assertNotNull(school2);
      assertEquals(List.of(
                  FloatRange.of(layerDepthRange.min(), d1),
                  FloatRange.of(d2, d3),
                  FloatRange.of(d4, layerDepthRange.max())
            ),
            regionManager.getDepthRangesForChannel(layer, dataManager.getDataFileSet().getPing(idx0), channel).getFloatRanges()
      );
      // Delete a school.
      regionManager.deleteSchool(school1);
      assertEquals(List.of(
                  FloatRange.of(layerDepthRange.min(), d3),
                  FloatRange.of(d4, layerDepthRange.max())
            ),
            regionManager.getDepthRangesForChannel(layer, dataManager.getDataFileSet().getPing(idx0), channel).getFloatRanges()
      );
   }

   @Test
   void testSchoolArea() {
      FloatRange depthRange = FloatRange.of(100, 200);
      PingRange pingRange = PingRange.of(idx0, dataManager.getDataFileSet().getPingIndex(idx1.getPingNumber() + 1));
      List<EchogramPoint> points = List.of(
            new EchogramPoint(pingRange.begin(), depthRange.max()),
            new EchogramPoint(dataManager.getDataFileSet().getPingIndex(idx0.getPingNumber() + 4), depthRange.max()),
            new EchogramPoint(dataManager.getDataFileSet().getPingIndex(idx0.getPingNumber() + 5), depthRange.max() + 2.5f),
            new EchogramPoint(dataManager.getDataFileSet().getPingIndex(idx0.getPingNumber() + 39), depthRange.max() + 19.5f),
            new EchogramPoint(dataManager.getDataFileSet().getPingIndex(idx0.getPingNumber() + 40), depthRange.max()),
            new EchogramPoint(pingRange.end(), depthRange.max()),
            new EchogramPoint(pingRange.end(), depthRange.min()),
            new EchogramPoint(pingRange.begin(), depthRange.min())
      );
      NavigableMap<PingIndex, FloatRangeSet> oldMask = MaskUtils.incompleteBoundaryToMask(points, regionManager.getPingContainer(), IdentityDepthTransform.INSTANCE);

      float oldArea = 0;
      for (FloatRangeSet oldRanges : oldMask.values()) {
         for (FloatRange oldRange : oldRanges) {
            oldArea += oldRange.getSize();
         }
      }
      assertEquals(5485, oldArea);

      School school = School.create(regionManager, oldMask);
      NavigableMap<PingIndex, FloatRangeSet> newMask = new TreeMap<>();
      float newArea = 0;
      for (PingIndex pingIndex : dataManager.getDataFileSet().getPingIndices(school.getPingRange())) {
         FloatRangeSet newRanges = school.getDepthRanges(pingIndex);
         for (FloatRange newRange : newRanges) {
            newArea += newRange.getSize();
         }
         newMask.put(pingIndex, newRanges);
      }
      assertEquals(oldArea, newArea);

      for (Map.Entry<PingIndex, FloatRangeSet> entry : oldMask.entrySet()) {
         FloatRangeSet oldRangeSet = entry.getValue();
         FloatRangeSet newRangeSet = newMask.get(entry.getKey());
         assertEquals(oldRangeSet, newRangeSet);
      }
   }

   @Test
   void maskToBoundaryToMask() {
      JUnitUtils.runWithRandom(random -> {
         DataFileSet dataFileSet = dataManager.getDataFileSet();
         List<EchogramPoint> points = IntStream.range(0, random.nextInt(5, 20))
               .mapToObj(_ -> {
                  PingIndex pingIndex = dataFileSet.getPingIndex(idx0.getPingNumber() + random.nextInt(50));
                  float depth = random.nextFloat(100, 200);
                  return new EchogramPoint(pingIndex, depth);
               })
               .toList();
         NavigableMap<PingIndex, FloatRangeSet> mask = MaskUtils.incompleteBoundaryToMask(points, dataFileSet, IdentityDepthTransform.INSTANCE);
         if (mask.isEmpty()) {
            return;
         }
         mask = MaskUtils.toDisjointMasks(mask, dataFileSet).getFirst();
         mask = MaskUtils.fillHoles(mask, dataFileSet);
         // Now `mask` is a single connected mask without holes.

         Set<CyclicList<EchogramPoint>> boundaries = MaskOutlineTracer.createBoundary(mask, dataFileSet);
         assertEquals(1, boundaries.size());
         CyclicList<EchogramPoint> boundary = boundaries.iterator().next();
         NavigableMap<PingIndex, FloatRangeSet> maskForBoundary = MaskOutlineTracer.createMaskForSingleBoundary(boundary);

         assertEquals(mask, maskForBoundary);
      });
   }
}
