package no.imr.korona.computation;

import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;

/**
 * As file: NiceSyntheticData:1-5.lsss-ss.
 */
public final class NiceSyntheticData extends SyntheticData {
   private final Instant startInstant;

   public NiceSyntheticData() {
      startInstant = LocalDate.of(2006, Month.MAY, 5).atStartOfDay().toInstant(ZoneOffset.UTC);
   }

   @Override
   public Instant getInstant(long pingNumber) {
      return startInstant.plusSeconds(pingNumber);
   }

   @Override
   public void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
      float[] sv = new float[1000];
      for (int i = 0; i < sv.length; i++) {
         sv[i] = 15000 - 20 * i;
      }
      powerData.setSv(sv);
   }
}
