package no.imr.korona.data.datagrams.subdatagrams.echoline;

import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.LsssDatagram;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.Utils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class EchoLineDataTest {
   @Test
   void test() throws DatagramFormatException {
      SyntheticData syntheticData = new SyntheticData() {
         @Override
         protected float getEffectivePulseDuration(PingIndex pingIndex, int channel) {
            return super.getPulseDuration(pingIndex, channel) * 0.8f;
         }

         @Override
         protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
            short[] power = new short[30];
            Arrays.fill(power, PowerData.EK60_SHORT_POWER_NULL);
            power[10] = -1000;
            power[11] = -1100;
            power[20] = -2000;
            power[21] = -2100;
            powerData.setSvFromShortPower(power);
         }
      };
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(1, 1000);

      PingIndex pingIndex = syntheticDataFile.createPingIndex(1);
      PowerData powerData = syntheticDataFile.createPowerData(pingIndex, 1);
      assertNotNull(powerData);
      float[] sv = powerData.getSv();
      EchoLineData echoLineData = new EchoLineData(powerData, List.of(
            new EchoLineData.EchoLine(10, new float[]{sv[10], sv[11]}, null),
            new EchoLineData.EchoLine(20, new float[]{sv[20], sv[21]}, null)
      ));

      List<BaseDatagram> datagrams = echoLineData.toDatagrams();
      LsssDatagram lsssDatagram = Utils.getFirstOrThrow(datagrams, LsssDatagram.class);
      ByteBuffer byteBuffer = lsssDatagram.toByteBufferExcludingHeader();

      LsssDatagram lsssDatagram2 = new LsssDatagram(echoLineData.getNTDate(), byteBuffer, new DatagramTypeManager());
      EchoLineSubDatagram echoLineSubDatagram = (EchoLineSubDatagram) lsssDatagram2.getSubDatagram();

      EchoLineData echoLineData2 = new EchoLineData(echoLineSubDatagram, syntheticDataFile.getPingConfiguration(), powerData.getEffectivePulseDuration());
      assertEquals(echoLineData.getEchoLines().size(), echoLineData2.getEchoLines().size());
      for (int i = 0; i < echoLineData.getEchoLines().size(); i++) {
         EchoLineData.EchoLine echoLine = echoLineData.getEchoLines().get(i);
         EchoLineData.EchoLine echoLine2 = echoLineData2.getEchoLines().get(i);
         assertEquals(echoLine.startSample(), echoLine2.startSample());
         assertArrayEquals(echoLine.sv(), echoLine2.sv());
      }
      assertArrayEquals(powerData.getSv(), echoLineData2.getPowerData().getSv(), 1e-15f);

      PingConversion pingConversion = new PingConversion(syntheticDataFile.toFile(), syntheticDataFile.getPingConfiguration(), datagrams, () -> null);
      PowerData powerData2 = (PowerData) pingConversion.getPingItems().getFirst();
      assertArrayEquals(powerData.getSv(), powerData2.getSv(), 1e-15f);
   }
}
