package no.imr.lsss.modules.korona.tracking;

import com.google.common.collect.ImmutableSortedMap;
import no.imr.korona.data.datagrams.TBR0Datagram;
import no.imr.korona.data.datagrams.TNF0Datagram;
import no.imr.korona.data.datagrams.TTC0Datagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.lsss.LSSS;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.range.FloatRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TrackEditingTest {
   private LSSS lsss;
   private TrackInfoModule trackInfoModule;
   private TrackEditing trackEditing;

   @BeforeEach
   void beforeEach() {
      lsss = LsssTestUtils.start(List.of(TrackInfoModule.class), List.of());
      trackInfoModule = lsss.getModuleManager().getModule(TrackInfoModule.class);
      LsssTestUtils.open(lsss, new TestSyntheticData().withFirstAndLastPingNumber(1, 100).toSegmentHandle());
      trackEditing = trackInfoModule.getTrackEditing();
      trackEditing.getWorkFileExtra().beginFromXml();
      trackEditing.getWorkFileExtra().endFromXml();
      lsss.getInterpretationSettings().waitUntilFinished();
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   @Test
   void splitTrackAtMissingPing() {
      assertEquals(1, trackInfoModule.getTrackInfos().size());
      assertEquals(1, trackInfoModule.getValidIds().size());

      DataFileSet dataFileSet = lsss.getInterpretationSettings().getDataFileSet();
      TrackId trackId = new TrackId(dataFileSet.getDataFiles().getFirst(), 0);
      TrackId newTrackId = trackEditing.newTrack(ImmutableSortedMap.of(
            pingIndex(10), new TrackBorder(trackId, 1, FloatRange.of(7, 9), 8, true),
            pingIndex(11), new TrackBorder(trackId, 1, FloatRange.of(7, 9), 8, true),
            pingIndex(15), new TrackBorder(trackId, 1, FloatRange.of(7, 9), 8, true),
            pingIndex(16), new TrackBorder(trackId, 1, FloatRange.of(7, 9), 8, true)
      ));
      assertEquals(2, trackInfoModule.getTrackInfos().size());
      assertEquals(2, trackInfoModule.getValidIds().size());

      List<TrackId> splitTrackIds = trackEditing.split(newTrackId, pingIndex(13));
      assertEquals(3, trackInfoModule.getTrackInfos().size());
      assertEquals(3, trackInfoModule.getValidIds().size());
      assertEquals(2, splitTrackIds.size());
      assertEquals(PingRange.of(pingIndex(10), pingIndex(12)), trackInfoModule.getTrackInfos().get(splitTrackIds.get(0)).pingRange());
      assertEquals(PingRange.of(pingIndex(15), pingIndex(17)), trackInfoModule.getTrackInfos().get(splitTrackIds.get(1)).pingRange());
   }

   private PingIndex pingIndex(int pingNumber) {
      return lsss.getInterpretationSettings().getDataFileSet().getPingIndex(pingNumber);
   }

   private static final class TestSyntheticData extends ConstantSyntheticData {
      @Override
      public void addOtherDatagrams(PingIndex pingIndex, PingData pingData) {
         long pingNumber = pingIndex.getPingNumber();
         if (pingNumber >= 15 && pingNumber <= 19) {
            pingData.add(new TBR0Datagram(pingIndex.getInstant(), 0, 1, FloatRange.of(5, 10), 7));
         }
         if (pingNumber == 20) {
            pingData.add(new TNF0Datagram(pingIndex.getInstant(), 0, 1, true, 5, 1));
         }
         if (pingNumber == 100) {
            pingData.add(new TTC0Datagram(pingIndex.getInstant(), new int[]{0},
                  List.of(getInstant(20))));
         }
      }
   }
}
