package no.imr.korona.region;

import no.imr.korona.data.datamanager.DataFileSet;
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
import java.util.function.Function;

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
   void imageSpaceDistance() {
      DataManager dataManager = DataManagerTestUtils.testDataManager();
      DataManagerTestUtils.open(dataManager, new TestSyntheticData().withFirstAndLastPingNumber(0, 50).toSegmentHandle());
      DataFileSet dataFileSet = dataManager.getDataFileSet();
      TestPingSettings pingSettings = new TestPingSettings(dataManager);

      PingIndex pingIndex1 = dataFileSet.getPingIndex(20);
      PingIndex pingIndex2 = dataFileSet.getPingIndex(30);
      PingRange pingRange = PingRange.of(pingIndex1, pingIndex2);

      Function<Integer, Float> pingNumberToX = pingNumber -> pingSettings.pingIndexToX(dataFileSet.getPingIndex(pingNumber));

      assertEquals(10, SchoolBoundaryObject.imageSpaceDistance(pingSettings, pingRange, pingNumberToX.apply(10)));
      assertEquals(1, SchoolBoundaryObject.imageSpaceDistance(pingSettings, pingRange, pingNumberToX.apply(19)));
      assertEquals(0, SchoolBoundaryObject.imageSpaceDistance(pingSettings, pingRange, pingNumberToX.apply(20)));
      assertEquals(0, SchoolBoundaryObject.imageSpaceDistance(pingSettings, pingRange, pingNumberToX.apply(25)));
      assertEquals(0, SchoolBoundaryObject.imageSpaceDistance(pingSettings, pingRange, pingNumberToX.apply(30)));
      assertEquals(1, SchoolBoundaryObject.imageSpaceDistance(pingSettings, pingRange, pingNumberToX.apply(31)));
      assertEquals(9, SchoolBoundaryObject.imageSpaceDistance(pingSettings, pingRange, pingNumberToX.apply(39)));
   }

   @Test
   void testPingRangeComparator() {
      DataManager dataManager = DataManagerTestUtils.testDataManager();
      DataManagerTestUtils.open(dataManager, new TestSyntheticData().withFirstAndLastPingNumber(0, 200).toSegmentHandle());
      TestPingSettings pingSettings = new TestPingSettings(dataManager);
      PingRange totalRange = dataManager.getDataFileSet().getTotalRange();

      PingIndex pingIndex1 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber());
      PingIndex pingIndex2 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 50);
      PingIndex pingIndex3 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 100);
      PingIndex pingIndex4 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 150);

      PingRange pr1 = PingRange.of(pingIndex1, pingIndex2);
      PingRange pr2 = PingRange.of(pingIndex2, pingIndex3);
      PingRange pr3 = PingRange.of(pingIndex3, pingIndex4);

      // Inside first ping range:
      PingIndex testPingIndex1 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 20);
      assertEquals(List.of(pr1, pr2, pr3),
            SchoolBoundaryObject.sortedPingRanges(List.of(pr2, pr3, pr1), pingSettings.pingIndexToX(testPingIndex1), pingSettings));

      // Inside middle ping range, but closer to the last ping range than the first:
      PingIndex testPingIndex2 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 90);
      assertEquals(List.of(pr2, pr3, pr1),
            SchoolBoundaryObject.sortedPingRanges(List.of(pr1, pr2, pr3), pingSettings.pingIndexToX(testPingIndex2), pingSettings));

      // Inside last ping range:
      PingIndex testPingIndex3 = dataManager.getDataFileSet().getPingIndex(totalRange.begin().getPingNumber() + 120);
      assertEquals(List.of(pr3, pr2, pr1),
            SchoolBoundaryObject.sortedPingRanges(List.of(pr1, pr2, pr3), pingSettings.pingIndexToX(testPingIndex3), pingSettings));
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
