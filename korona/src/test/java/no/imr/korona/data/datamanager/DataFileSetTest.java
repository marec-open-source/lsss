package no.imr.korona.data.datamanager;

import no.imr.korona.data.formats.synthetic.TestSyntheticData;
import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class DataFileSetTest {
   private static DataFileSet load(SegmentHandle... segmentHandles) {
      return new DataFileSet(new DefaultDataConfiguration(), new FileOpenRequest(List.of(segmentHandles)));
   }

   @Test
   void testGetIdxDatagram() {
      DataFileSet dataFileSet = load(new TestSyntheticData().toSegmentHandle(1, 1000));

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
      DataFileSet dataFileSet = load(new TestSyntheticData().toSegmentHandle(1, 1000));

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
      DataFileSet dataFileSet = load(new TestSyntheticData().toSegmentHandle(1, 1000));

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

      DataFileSet dataFileSet = load(
            new TestSyntheticData().toSegmentHandle(1, 10),
            new TestSyntheticData().toSegmentHandle(11, 20),
            new TestSyntheticData().toSegmentHandle(21, 30));
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
      DataFileSet dataFileSet = load(
            new DataFileSetTestSyntheticData().toSegmentHandle(1, 100),
            new DataFileSetTestSyntheticData().toSegmentHandle(101, 200));

      assertEquals(200, dataFileSet.getTotalRange().getPingCount());
      assertNotNull(dataFileSet.getContainingPingIndex(1, PingMapping.DISTANCE));
      assertNotNull(dataFileSet.getContainingPingIndex(1.01, PingMapping.DISTANCE));
      assertNotNull(dataFileSet.getContainingPingIndex(1.02, PingMapping.DISTANCE));
   }

   private static final class WrapAroundTestSyntheticData extends ConstantSyntheticData {
      private static final long WRAP_PING_NUMBER = 1000000;
      private static final double WRAP = 10000;

      private WrapAroundTestSyntheticData() {
      }

      @Override
      protected double getVesselDistance(long pingNumber) {
         double vesselDistance = 0.01 * pingNumber;
         if (getFirstPingNumber() >= WRAP_PING_NUMBER) {
            vesselDistance -= WRAP;
         }
         return vesselDistance;
      }

      @Override
      protected @Nullable WrapAround getWrapAround() {
         if (getFirstPingNumber() < WRAP_PING_NUMBER && WRAP_PING_NUMBER <= getLastPingNumber()) {
            return new WrapAround(createPingIndex(WRAP_PING_NUMBER), WRAP);
         } else {
            return null;
         }
      }
   }

   @Test
   void testWrapAroundInFile() {
      DataFileSet dataFileSet = load(
            new WrapAroundTestSyntheticData().toSegmentHandle(999990, 1000010),
            new WrapAroundTestSyntheticData().toSegmentHandle(1000011, 1000020));

      assertEquals(9999.99, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(999999)));
      assertEquals(10000, dataFileSet.getPingIndex(1000000).getVesselDistance());
      assertEquals(0, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(1000000)));
      assertEquals(10000.11, dataFileSet.getPingIndex(1000011).getVesselDistance());
      assertEquals(0.11, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(1000011)), 1e-12);
   }

   @Test
   void testWrapAroundBetweenFiles() {
      DataFileSet dataFileSet = load(
            new WrapAroundTestSyntheticData().toSegmentHandle(999990, 999999),
            new WrapAroundTestSyntheticData().toSegmentHandle(1000000, 1000010));

      assertEquals(2, dataFileSet.getDataFiles().size());
      for (DataFile dataFile : dataFileSet.getDataFiles()) {
         assertNull(dataFile.getWrapAround());
      }
      assertEquals(9999.99, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(999999)));
      assertEquals(10000, dataFileSet.getPingIndex(1000000).getVesselDistance());
      assertEquals(0, dataFileSet.getVesselDistanceUncorrectedForWrapAround(dataFileSet.getPingIndex(1000000)));
   }
}
