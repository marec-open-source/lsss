package no.imr.korona.data.util.mask;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DataManagerTestUtils;
import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

final class MaskUtilsTest {
   @Test
   void contains() {
      DefaultPingIndex p1 = new DefaultPingIndex(1, 1, 0, null);
      DefaultPingIndex p2 = new DefaultPingIndex(2, 2, 0, null);
      DefaultPingIndex p3 = new DefaultPingIndex(3, 3, 0, null);
      DefaultPingIndex p4 = new DefaultPingIndex(4, 4, 0, null);
      DefaultPingIndex p5 = new DefaultPingIndex(5, 5, 0, null);

      NavigableMap<PingIndex, FloatRangeSet> mask = new TreeMap<>();
      mask.put(p2, FloatRangeSet.of(FloatRange.of(1, 3)));
      mask.put(p4, FloatRangeSet.of(List.of(FloatRange.of(1, 3), FloatRange.of(8, 9))));

      assertTrue(MaskUtils.contains(mask, p2, 1));
      assertTrue(MaskUtils.contains(mask, p4, 2));
      assertTrue(MaskUtils.contains(mask, p4, 8));

      assertFalse(MaskUtils.contains(mask, p1, 1));
      assertFalse(MaskUtils.contains(mask, p2, 3));
      assertFalse(MaskUtils.contains(mask, p3, 2));
      assertFalse(MaskUtils.contains(mask, p4, 4));
      assertFalse(MaskUtils.contains(mask, p5, 1));
   }

   @Test
   void isContainedIn() {
      DefaultPingIndex p1 = new DefaultPingIndex(1, 1, 0, null);
      DefaultPingIndex p2 = new DefaultPingIndex(2, 2, 0, null);

      NavigableMap<PingIndex, FloatRangeSet> mask = new TreeMap<>();
      mask.put(p1, FloatRangeSet.of(FloatRange.of(1, 3)));
      mask.put(p2, FloatRangeSet.of(List.of(FloatRange.of(1, 3), FloatRange.of(8, 9))));

      assertTrue(MaskUtils.isContainedIn(mask, pingIndex -> FloatRange.of(1, 9)));
      assertFalse(MaskUtils.isContainedIn(mask, pingIndex -> FloatRange.of(2, 9)));
      assertFalse(MaskUtils.isContainedIn(mask, pingIndex -> FloatRange.of(1, 8)));
   }

   @Test
   void intersectionByFloatRangeFunction() {
      DefaultPingIndex p1 = new DefaultPingIndex(1, 1, 0, null);
      DefaultPingIndex p2 = new DefaultPingIndex(2, 2, 0, null);

      NavigableMap<PingIndex, FloatRangeSet> mask = new TreeMap<>();
      mask.put(p1, FloatRangeSet.of(FloatRange.of(1, 3)));
      mask.put(p2, FloatRangeSet.of(List.of(FloatRange.of(1, 3), FloatRange.of(6, 9))));

      assertEquals(mask, MaskUtils.intersection(mask, pingIndex -> FloatRange.of(1, 9)));
      assertEquals(Map.of(), MaskUtils.intersection(mask, pingIndex -> FloatRange.of(3, 6)));
      assertEquals(
            Map.of(
                  p2, FloatRangeSet.of(List.of(FloatRange.of(6, 9)))
            ),
            MaskUtils.intersection(mask, pingIndex -> FloatRange.of(4, 9))
      );
      assertEquals(
            Map.of(
                  p1, FloatRangeSet.of(FloatRange.of(1, 3)),
                  p2, FloatRangeSet.of(List.of(FloatRange.of(1, 3), FloatRange.of(6, 7)))
            ),
            MaskUtils.intersection(mask, pingIndex -> FloatRange.of(1, 7))
      );
   }

   @Test
   void incompleteBoundaryToMask() {
      DataManager dataManager = DataManagerTestUtils.testDataManager();
      DataManagerTestUtils.open(dataManager, new ConstantSyntheticData(1000, 0).withFirstAndLastPingNumber(1, 10).toSegmentHandle());

      DataFileSet dataFileSet = dataManager.getDataFileSet();
      PingIndex p1 = dataFileSet.getTotalRange().begin();
      PingIndex p2 = dataFileSet.getPingIndex(p1.getPingNumber() + 1);
      PingIndex p3 = dataFileSet.getPingIndex(p1.getPingNumber() + 2);
      PingIndex p4 = dataFileSet.getPingIndex(p1.getPingNumber() + 3);
      PingIndex p5 = dataFileSet.getPingIndex(p1.getPingNumber() + 4);

      // Empty
      incompleteBoundaryToMask(dataFileSet, List.of(), Map.of());

      // 12345
      // XXXX-
      // XXXX-
      // XXXX-
      incompleteBoundaryToMask(dataFileSet,
            List.of(
                  new EchogramPoint(p1, 400),
                  new EchogramPoint(p5, 400),
                  new EchogramPoint(p5, 100),
                  new EchogramPoint(p1, 100)
            ), Map.of(
                  p1, FloatRangeSet.of(FloatRange.of(100, 400)),
                  p2, FloatRangeSet.of(FloatRange.of(100, 400)),
                  p3, FloatRangeSet.of(FloatRange.of(100, 400)),
                  p4, FloatRangeSet.of(FloatRange.of(100, 400))
            )
      );

      // 12345
      // XXXX-
      // --XX-
      // XXXX-
      incompleteBoundaryToMask(dataFileSet,
            List.of(
                  new EchogramPoint(p1, 400),
                  new EchogramPoint(p5, 400),
                  new EchogramPoint(p5, 100),
                  new EchogramPoint(p1, 100),
                  new EchogramPoint(p1, 200),
                  new EchogramPoint(p3, 200),
                  new EchogramPoint(p3, 300),
                  new EchogramPoint(p1, 300)
            ), Map.of(
                  p1, FloatRangeSet.of(List.of(FloatRange.of(100, 200), FloatRange.of(300, 400))),
                  p2, FloatRangeSet.of(List.of(FloatRange.of(100, 200), FloatRange.of(300, 400))),
                  p3, FloatRangeSet.of(FloatRange.of(100, 400)),
                  p4, FloatRangeSet.of(FloatRange.of(100, 400))
            )
      );

      // 12345
      // XXXX-
      // XX---
      // XXXX-
      incompleteBoundaryToMask(dataFileSet,
            List.of(
                  new EchogramPoint(p1, 400),
                  new EchogramPoint(p5, 400),
                  new EchogramPoint(p5, 300),
                  new EchogramPoint(p3, 300),
                  new EchogramPoint(p3, 200),
                  new EchogramPoint(p5, 200),
                  new EchogramPoint(p5, 100),
                  new EchogramPoint(p1, 100)
            ), Map.of(
                  p1, FloatRangeSet.of(FloatRange.of(100, 400)),
                  p2, FloatRangeSet.of(FloatRange.of(100, 400)),
                  p3, FloatRangeSet.of(List.of(FloatRange.of(100, 200), FloatRange.of(300, 400))),
                  p4, FloatRangeSet.of(List.of(FloatRange.of(100, 200), FloatRange.of(300, 400)))
            )
      );

      // 12345
      // --X--
      // -XXX-
      incompleteBoundaryToMask(dataFileSet,
            List.of(
                  new EchogramPoint(p1, 200),
                  new EchogramPoint(p5, 200),
                  new EchogramPoint(p3, 100)
            ), Map.of(
                  p2, FloatRangeSet.of(FloatRange.of(150, 200)),
                  p3, FloatRangeSet.of(FloatRange.of(100, 200)),
                  p4, FloatRangeSet.of(FloatRange.of(150, 200))
            )
      );
   }

   private static void incompleteBoundaryToMask(DataFileSet dataFileSet, List<EchogramPoint> points, Map<PingIndex, FloatRangeSet> mask) {
      // Test both orientations and all starting points.

      points = new ArrayList<>(points); // Make mutable

      for (int i = 0; i < points.size(); i++) {
         assertEquals(mask, MaskUtils.incompleteBoundaryToMask(points, dataFileSet, IdentityDepthTransform.INSTANCE));
         points.add(points.removeFirst());
      }

      Collections.reverse(points);

      for (int i = 0; i < points.size(); i++) {
         assertEquals(mask, MaskUtils.incompleteBoundaryToMask(points, dataFileSet, IdentityDepthTransform.INSTANCE));
         points.add(points.removeFirst());
      }
   }
}
