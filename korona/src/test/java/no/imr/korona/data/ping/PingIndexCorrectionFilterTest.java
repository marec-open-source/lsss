package no.imr.korona.data.ping;

import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.concurrent.AsyncHandle;
import no.marec.lsss.api.util.GeoPoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class PingIndexCorrectionFilterTest {
   @Test
   void byGeoPos() throws IOException {
      check(new PingIndexCorrectionOptions(true, false));
   }

   @Test
   void byVesselDistance() throws IOException {
      check(new PingIndexCorrectionOptions(false, true));
   }

   private static void check(PingIndexCorrectionOptions pingIndexCorrectionOptions) throws IOException {
      ConstantSyntheticData syntheticData = new ConstantSyntheticData() {
         @Override
         protected void addOtherDatagrams(PingIndex pingIndex, PingData pingData) {
            switch ((int) pingIndex.getPingNumber()) {
               case 1 -> {
                  pingData.add(new NmeaPingItem(pingIndex.getNTDate(), "$GPVTG,090,T,094,M,1.01,N,20.7,K,D"));
                  pingData.add(new NmeaPingItem(pingIndex.getNTDate(), "$GPGGA,042822,6100.2300,N,00207.0613,E,2,06,01.9,30.7,M,46.1,M,07.0,0685"));
               }
               case 2 -> {
                  pingData.add(new NmeaPingItem(pingIndex.getNTDate(), "$GPVTG,090,T,094,M,2.01,N,20.7,K,D"));
                  pingData.add(new NmeaPingItem(pingIndex.getNTDate(), "$GPGGA,042822,6100.2311,N,00207.0617,E,2,06,01.9,30.7,M,46.1,M,07.0,0685"));
               }
               default -> {
                  fail(pingIndex.toString());
               }
            }
         }
      };
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(1, 1000);
      PingIndexCorrectionFilter pingSource = new PingIndexCorrectionFilter(syntheticDataFile.toPingReader());
      pingSource.setPingIndexCorrectionOptions(pingIndexCorrectionOptions);

      AsyncHandle asyncHandle = new AsyncHandle();
      Ping ping1 = pingSource.nextPing(asyncHandle);
      assertNotNull(ping1);
      assertEquals(0, ping1.getVesselDistance());
      GeoPoint geoPos1 = ping1.getPingIndex().getGeographicalPosition();
      assertNotNull(geoPos1);
      assertEquals(002f + 07.0613 / 60, geoPos1.getX());
      assertEquals(61 + 00.2300 / 60, geoPos1.getY());

      Ping ping2 = pingSource.nextPing(asyncHandle);
      assertNotNull(ping2);
      double deltaSeconds = PingMapping.TIME.distance(ping1, ping2);
      double nmiPerSecond = 2.01 / 3600;
      assertEquals(deltaSeconds * nmiPerSecond, ping2.getVesselDistance(), 1e-6);
      GeoPoint geoPos2 = ping2.getPingIndex().getGeographicalPosition();
      assertNotNull(geoPos2);
      assertEquals(002f + 07.0617 / 60, geoPos2.getX());
      assertEquals(61 + 00.2311 / 60, geoPos2.getY());
   }
}
