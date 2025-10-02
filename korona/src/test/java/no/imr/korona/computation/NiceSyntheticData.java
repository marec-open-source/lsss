package no.imr.korona.computation;

import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.time.NTDate;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;

/**
 * As file: NiceSyntheticData:1-5.lsss-ss.
 */
public final class NiceSyntheticData extends SyntheticData {
   private final long ntDate;

   public NiceSyntheticData() {
      long millis = LocalDate.of(2006, Month.MAY, 5).atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000;
      ntDate = NTDate.timeInMillisToNTDate(millis);
   }

   @Override
   protected long getNTDate(long pingNumber) {
      return ntDate + pingNumber * 1000 * 10000;
   }

   @Override
   protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
      float[] sv = new float[1000];
      for (int i = 0; i < sv.length; i++) {
         sv[i] = 15000 - 20 * i;
      }
      powerData.setSv(sv);
   }
}
