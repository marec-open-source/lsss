package no.imr.korona.data.ping.items.configuration;

import no.imr.korona.data.datagrams.Con0Datagram;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RawFileConfigurationTest {
   @Test
   void type() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1);
      RawFileConfiguration rawFileConfiguration = syntheticDataFile.getRawFileConfiguration();
      assertEquals(Con0Datagram.TYPE, rawFileConfiguration.toDatagrams().getFirst().getDatagramType());
   }

   @Test
   void kHzToChannel() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1);

      RawFileConfiguration rawFileConfiguration = syntheticDataFile.getRawFileConfiguration();
      assertEquals(1, rawFileConfiguration.lastChannelWithKHz(18));
      assertEquals(2, rawFileConfiguration.lastChannelWithKHz(38));
      assertEquals(3, rawFileConfiguration.lastChannelWithKHz(70));
      assertEquals(4, rawFileConfiguration.lastChannelWithKHz(120));
      assertEquals(5, rawFileConfiguration.lastChannelWithKHz(200));
      assertEquals(6, rawFileConfiguration.lastChannelWithKHz(364));

      assertEquals(-1, rawFileConfiguration.lastChannelWithKHz(0));
      assertEquals(-1, rawFileConfiguration.lastChannelWithKHz(17));
      assertEquals(-1, rawFileConfiguration.lastChannelWithKHz(19));
      assertEquals(-1, rawFileConfiguration.lastChannelWithKHz(119));

      RawFileTransducer originalTransducer = rawFileConfiguration.getTransducers().getFirst();
      RawFileTransducer newTransducer = rawFileConfiguration.newChannel(originalTransducer);
      assertNotSame(originalTransducer, newTransducer);
      assertSame(rawFileConfiguration.getTransducers().get(6), newTransducer);
      assertNotEquals(originalTransducer.getChannelId(), newTransducer.getChannelId());
      assertEquals(7, rawFileConfiguration.lastChannelWithKHz(18));
   }

   @Test
   void equalsTest() {
      RawFileConfiguration rawFileConfiguration = new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1).getRawFileConfiguration();
      RawFileConfiguration copy = rawFileConfiguration.makeCopy();
      assertNotSame(rawFileConfiguration, copy);
      assertNotEquals(rawFileConfiguration, copy);
      assertEquals(rawFileConfiguration.toDatagrams().getFirst().toByteBufferIncludingHeader(), rawFileConfiguration.toDatagrams().getFirst().toByteBufferIncludingHeader());
   }
}
