package no.imr.korona.data.ping.items.channel;

import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AngleDataTest {
   @Test
   void byteConversion() {
      RawFileTransducer transducer = new RawFileTransducer();
      RawFileTransducer.Xml0Info xml0Info = new RawFileTransducer.Xml0Info();
      xml0Info.setByteAngleScalingAthwartship(2);
      xml0Info.setByteAngleScalingAlongship((float) (2 / Math.sqrt(3)));
      transducer.setXml0Info(xml0Info);

      byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8, -128, 127};
      float[] electricalAngles = AngleData.bytesToElectricalAngles(bytes, transducer);
      assertEquals(1 * (180f / 128f) * 2, electricalAngles[0]);
      assertEquals(2 * (180f / 128f) * ((float) (2 / Math.sqrt(3))), electricalAngles[1]);
      assertArrayEquals(bytes, AngleData.electricalAnglesToBytes(electricalAngles, transducer));
   }
}
