package no.imr.korona.data.datamanager;

import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.formats.synthetic.TestSyntheticData;
import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DataFileSetTest {
   @Test
   void testGetIdxDatagram() {
      DataFileSet dataFileSet = DataManagerTestUtils.load(new TestSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle());

      PingIndex firstIdx = dataFileSet.getTotalRange().begin();
      PingIndex lastIdx = dataFileSet.getTotalRange().end();

      assertEquals(firstIdx, dataFileSet.getPingIndex(firstIdx.getPingNumber()));
      assertEquals(lastIdx, dataFileSet.getPingIndex(lastIdx.getPingNumber()));

      for (long pingNumber = firstIdx.getPingNumber(); pingNumber <= lastIdx.getPingNumber(); pingNumber++) {
         assertEquals(pingNumber, dataFileSet.getPingIndex(pingNumber).getPingNumber());
      }
   }

   @Test
   void getPingIndices() {
      DataFileSet dataFileSet = DataManagerTestUtils.load(new TestSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle());

      PingRange totalRange = dataFileSet.getTotalRange();
      long pingNumber = totalRange.begin().getPingNumber();
      for (PingIndex pingIndex : dataFileSet.getPingIndices(totalRange)) {
         assertEquals(pingNumber, pingIndex.getPingNumber());
         pingNumber++;
      }

      assertEquals(totalRange.end().getPingNumber(), pingNumber);

      assertEquals(List.of(), dataFileSet.getPingIndices(PingRange.EMPTY_RANGE));

      assertEquals(List.of(), dataFileSet.getPingIndices(PingRange.of(totalRange.begin(), totalRange.begin())));
   }

   @Test
   void testIdx() {
      DataFileSet dataFileSet = DataManagerTestUtils.load(new TestSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle());

      for (DataFile dataFile : dataFileSet.getDataFiles()) {
         long firstPingNumber = dataFile.getPingRange().begin().getPingNumber();
         List<? extends PingIndex> pingIdxDatagrams = dataFile.getPingIndices();
         for (int i = 0; i < pingIdxDatagrams.size(); i++) {
            assertEquals(firstPingNumber + i, pingIdxDatagrams.get(i).getPingNumber());
         }
      }
   }

   @Test
   void getContainingDataFileIndex() {
      assertEquals(0, DataFileSet.empty().getContainingDataFileIndex(new DefaultPingIndex(0, 0, 0, null)));

      DataFileSet dataFileSet = DataManagerTestUtils.load(
            new TestSyntheticData().withFirstAndLastPingNumber(1, 10).toSegmentHandle(),
            new TestSyntheticData().withFirstAndLastPingNumber(11, 20).toSegmentHandle(),
            new TestSyntheticData().withFirstAndLastPingNumber(21, 30).toSegmentHandle()
      );
      assertEquals(3, dataFileSet.getDataFiles().size());
      assertEquals(-1, dataFileSet.getContainingDataFileIndex(new DefaultPingIndex(0, 0, 0, null)));
      assertEquals(0, dataFileSet.getContainingDataFileIndex(dataFileSet.getPingIndex(1)));
      assertEquals(0, dataFileSet.getContainingDataFileIndex(dataFileSet.getPingIndex(10)));
      assertEquals(1, dataFileSet.getContainingDataFileIndex(dataFileSet.getPingIndex(11)));
      assertEquals(1, dataFileSet.getContainingDataFileIndex(dataFileSet.getPingIndex(20)));
      assertEquals(2, dataFileSet.getContainingDataFileIndex(dataFileSet.getPingIndex(21)));
      assertEquals(2, dataFileSet.getContainingDataFileIndex(dataFileSet.getPingIndex(30)));
      assertEquals(3, dataFileSet.getContainingDataFileIndex(dataFileSet.getTotalRange().end()));
   }

   //-------------------

   private static final class DataFileSetTestSyntheticData extends ConstantSyntheticData {
      @Override
      protected double getVesselDistance(long pingNumber) {
         if (pingNumber <= 100) {
            return 0.01 * pingNumber;
         } else {
            return 0.01 * 100 + 0.1 * (pingNumber - 100);
         }
      }
   }

   @Test
   void testDataFileRangeMaps() {
      DataFileSet dataFileSet = DataManagerTestUtils.load(
            new DataFileSetTestSyntheticData().withFirstAndLastPingNumber(1, 100).toSegmentHandle(),
            new DataFileSetTestSyntheticData().withFirstAndLastPingNumber(101, 200).toSegmentHandle()
      );
      assertEquals(200, dataFileSet.getTotalRange().getPingCount());
      assertNotNull(dataFileSet.getContainingPingIndex(1, PingMapping.DISTANCE));
      assertNotNull(dataFileSet.getContainingPingIndex(1.01, PingMapping.DISTANCE));
      assertNotNull(dataFileSet.getContainingPingIndex(1.02, PingMapping.DISTANCE));
   }

   private static final class WrapAroundTestSyntheticData extends ConstantSyntheticData {
      private static final long WRAP_PING_NUMBER = 1_000_000;
      private static final double WRAP = 10_000;

      private final boolean subtractWrap;

      private WrapAroundTestSyntheticData(boolean subtractWrap) {
         this.subtractWrap = subtractWrap;
      }

      @Override
      protected double getVesselDistance(long pingNumber) {
         double vesselDistance = 0.01 * pingNumber;
         if (subtractWrap) {
            vesselDistance -= WRAP;
         }
         return vesselDistance;
      }

      @Override
      protected @Nullable WrapAround getWrapAround(SyntheticDataFile syntheticDataFile) {
         if (syntheticDataFile.getFirstPingNumber() < WRAP_PING_NUMBER && WRAP_PING_NUMBER <= syntheticDataFile.getLastPingNumber()) {
            return new WrapAround(syntheticDataFile.createPingIndex(WRAP_PING_NUMBER), WRAP);
         } else {
            return null;
         }
      }
   }

   @Test
   void testWrapAroundInFile() {
      DataFileSet dataFileSet = DataManagerTestUtils.load(
            new WrapAroundTestSyntheticData(false).withFirstAndLastPingNumber(999_990, 1_000_010).toSegmentHandle(),
            new WrapAroundTestSyntheticData(true).withFirstAndLastPingNumber(1_000_011, 1_000_020).toSegmentHandle()
      );
      assertEquals(9_999.99, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(999_999)));
      assertEquals(10_000, dataFileSet.getPingIndex(1_000_000).getVesselDistance());
      assertEquals(0, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(1_000_000)));
      assertEquals(10_000.11, dataFileSet.getPingIndex(1_000_011).getVesselDistance());
      assertEquals(0.11, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(1_000_011)), 1e-12);
   }

   @Test
   void testWrapAroundBetweenFiles() {
      DataFileSet dataFileSet = DataManagerTestUtils.load(
            new WrapAroundTestSyntheticData(false).withFirstAndLastPingNumber(999_990, 999_999).toSegmentHandle(),
            new WrapAroundTestSyntheticData(true).withFirstAndLastPingNumber(1_000_000, 1_000_010).toSegmentHandle()
      );
      assertEquals(2, dataFileSet.getDataFiles().size());
      for (DataFile dataFile : dataFileSet.getDataFiles()) {
         assertNull(dataFile.getWrapAround());
      }
      assertEquals(9_999.99, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(999_999)));
      assertEquals(10_000, dataFileSet.getPingIndex(1_000_000).getVesselDistance());
      assertEquals(0, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(1_000_000)));
   }
}
