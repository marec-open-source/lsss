package no.imr.korona.region;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DataManagerTestUtils;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.echogram.EchogramPingSettings;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SchoolBoundaryObjectTest {
   private static final class TestPingSettings extends EchogramPingSettings {
      private final DataManager dataManager;
      private final int width;
      private final PingMapping pingMapping = PingMapping.NUMBER;
      private final double distanceToXFactor;

      private TestPingSettings(DataManager dataManager) {
         this.dataManager = dataManager;
         setPingRange(dataManager.getDataFileSet().getTotalRange());
         double d = pingMapping.distance(getPingRange());
         width = dataManager.getDataFileSet().getTotalRange().getPingCount();
         distanceToXFactor = width / d;
      }

      @Override
      public PingContainer getPingContainer() {
         return dataManager.getDataFileSet();
      }

      @Override
      public int getWidth() {
         return width;
      }

      @Override
      public float pingIndexToX(PingIndex pingIndex) {
         double distance = pingMapping.distance(getPingRange().begin(), pingIndex);
         return (float) (distance * distanceToXFactor);
      }

      @Override
      public PingIndex xToClosestPingIndex(double x) {
         double distance = x / distanceToXFactor;
         return getPingContainer().getClosestPingIndex(getPingRange().begin(), distance, pingMapping);
      }

      @Override
      public @Nullable PingIndex xToContainingPingIndex(double x) {
         double distance = x / distanceToXFactor;
         return getPingContainer().getContainingPingIndex(getPingRange().begin(), distance, pingMapping);
      }

      @Override
      public void zoom(PingRange pingRange) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void zoom(double x, double zoomFactor) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void zoomOut() {
         throw new UnsupportedOperationException();
      }
   }

   @Test
   void testPingRangeComparator() {
      DataManager dataManager = DataManagerTestUtils.testDataManager();
      DataManagerTestUtils.open(dataManager, new TestSyntheticData().toSegmentHandle(1, 1000));
      PingRange totalRange = dataManager.getDataFileSet().getTotalRange();

      PingIndex pingIndex1 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber());
      PingIndex pingIndex2 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 50);
      PingIndex pingIndex3 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 100);
      PingIndex pingIndex4 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 150);

      PingRange pingRange1 = PingRange.of(pingIndex1, pingIndex3);
      PingRange pingRange2 = PingRange.of(pingIndex2, pingIndex4);

      //A ping index which is closer to pingRange2 than pingRange1
      PingIndex testPingIndex1 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 74);

      List<PingRange> pingRangeList = Arrays.asList(pingRange1, pingRange2);

      TestPingSettings pingSettings = new TestPingSettings(dataManager);
      SchoolBoundaryObject.sortPingRanges(pingRangeList, testPingIndex1, pingSettings);
      assertEquals(pingRangeList, List.of(pingRange2, pingRange1));

      //Re-sorting should give the same result
      SchoolBoundaryObject.sortPingRanges(pingRangeList, testPingIndex1, pingSettings);
      assertEquals(pingRangeList, List.of(pingRange2, pingRange1));

      //A ping index which is closer to pingRange1 than pingRange2
      PingIndex testPingIndex2 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 49);

      SchoolBoundaryObject.sortPingRanges(pingRangeList, testPingIndex2, pingSettings);
      assertEquals(pingRangeList, List.of(pingRange1, pingRange2));
   }

   private static final class TestSyntheticData extends SyntheticData {
      @Override
      protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
         float[] sv = new float[2000];
         Arrays.fill(sv, PowerData.logSvToSv(-56));
         powerData.setSv(sv);
      }

      @Override
      protected float getBottomDepth(PingIndex pingIndex, int channel) {
         return 500;
      }
   }
}
