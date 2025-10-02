package no.imr.korona.data.ping;

import no.imr.korona.data.formats.synthetic.SyntheticPingReader;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.tools.concurrent.AsyncHandle;
import no.marec.lsss.api.util.GeoPoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class PingIndexCorrectionFilterTest {
   @Test
   void test() throws IOException {
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
                  pingData.add(new NmeaPingItem(pingIndex.getNTDate(), "$GPGGA,042822,6100.2300,S,00207.0613,W,2,06,01.9,30.7,M,46.1,M,07.0,0685"));
               }
               default -> {
                  fail(pingIndex.toString());
               }
            }
         }
      };
      PingIndexCorrectionFilter pingSource = new PingIndexCorrectionFilter(new SyntheticPingReader(syntheticData));

      AsyncHandle asyncHandle = new AsyncHandle();
      Ping ping1 = pingSource.nextPing(asyncHandle);
      assertNotNull(ping1);
      assertEquals(0, ping1.getVesselDistance());
      GeoPoint geoPos1 = ping1.getPingIndex().getGeographicalPosition();
      assertNotNull(geoPos1);
      assertEquals(002f + 07.0613 / 60, geoPos1.getX(), 1e-6);
      assertEquals(61 + 00.2300 / 60, geoPos1.getY(), 1e-6);

      Ping ping2 = pingSource.nextPing(asyncHandle);
      assertNotNull(ping2);
      double deltaSeconds = PingMapping.TIME.distance(ping1, ping2);
      double nmiPerSecond = 2.01 / 3600;
      assertEquals(deltaSeconds * nmiPerSecond, ping2.getVesselDistance(), 1e-6);
      GeoPoint geoPos2 = ping2.getPingIndex().getGeographicalPosition();
      assertNotNull(geoPos2);
      assertEquals(-(002f + 07.0613 / 60), geoPos2.getX(), 1e-6);
      assertEquals(-(61 + 00.2300 / 60), geoPos2.getY(), 1e-6);
   }
}
