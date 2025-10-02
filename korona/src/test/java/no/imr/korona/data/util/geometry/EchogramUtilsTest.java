package no.imr.korona.data.util.geometry;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DataManagerTestUtils;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class EchogramUtilsTest {
   @Test
   void computeLine() {
      DataManager dataManager = DataManagerTestUtils.testDataManager();
      DataManagerTestUtils.open(dataManager, new ConstantSyntheticData(1000, 0).toSegmentHandle(1, 10));

      DataFileSet dataFileSet = dataManager.getDataFileSet();
      PingIndex p1 = dataFileSet.getTotalRange().begin();
      PingIndex p2 = dataFileSet.getPingIndex(p1.getPingNumber() + 1);
      PingIndex p3 = dataFileSet.getPingIndex(p1.getPingNumber() + 2);
      PingIndex p4 = dataFileSet.getPingIndex(p1.getPingNumber() + 3);
      PingIndex p5 = dataFileSet.getPingIndex(p1.getPingNumber() + 4);

      // Horizontal left to right:
      assertEquals(List.of(
                  new EchogramPoint(p1, 100),
                  new EchogramPoint(p2, 125),
                  new EchogramPoint(p3, 150),
                  new EchogramPoint(p4, 175),
                  new EchogramPoint(p5, 200)
            ),
            EchogramUtils.computeLine(IdentityDepthTransform.INSTANCE, new EchogramPoint(p1, 100), new EchogramPoint(p5, 200), dataFileSet));

      // Horizontal right to left:
      assertEquals(List.of(
                  new EchogramPoint(p5, 200),
                  new EchogramPoint(p4, 175),
                  new EchogramPoint(p3, 150),
                  new EchogramPoint(p2, 125),
                  new EchogramPoint(p1, 100)
            ),
            EchogramUtils.computeLine(IdentityDepthTransform.INSTANCE, new EchogramPoint(p5, 200), new EchogramPoint(p1, 100), dataFileSet));

      // Vertical downwards:
      assertEquals(List.of(
                  new EchogramPoint(p1, 100),
                  new EchogramPoint(p1, 200)
            ),
            EchogramUtils.computeLine(IdentityDepthTransform.INSTANCE, new EchogramPoint(p1, 100), new EchogramPoint(p1, 200), dataFileSet));

      // Vertical upwards:
      assertEquals(List.of(
                  new EchogramPoint(p1, 200),
                  new EchogramPoint(p1, 100)
            ),
            EchogramUtils.computeLine(IdentityDepthTransform.INSTANCE, new EchogramPoint(p1, 200), new EchogramPoint(p1, 100), dataFileSet));
   }
}
