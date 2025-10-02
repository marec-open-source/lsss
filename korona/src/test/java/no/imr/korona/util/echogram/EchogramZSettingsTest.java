package no.imr.korona.util.echogram;

import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class EchogramZSettingsTest {
   @Test
   void test() {
      EchogramZSettings zSettings = new DefaultEchogramZSettings();
      zSettings.setHeight(100);

      PingIndex pingIndex = new DefaultPingIndex(0, 0, 0, null);

      assertEquals(Float.POSITIVE_INFINITY, zSettings.depthToY(Float.POSITIVE_INFINITY, pingIndex));
      assertEquals(Float.NEGATIVE_INFINITY, zSettings.depthToY(Float.NEGATIVE_INFINITY, pingIndex));

      assertEquals(Integer.MAX_VALUE, zSettings.depthToYIndex(Float.POSITIVE_INFINITY, pingIndex));
      assertEquals(Integer.MIN_VALUE, zSettings.depthToYIndex(Float.NEGATIVE_INFINITY, pingIndex));

      assertEquals(Float.POSITIVE_INFINITY, zSettings.zToY(Float.POSITIVE_INFINITY));
      assertEquals(Float.NEGATIVE_INFINITY, zSettings.zToY(Float.NEGATIVE_INFINITY));

      assertEquals(Integer.MAX_VALUE, zSettings.zToYIndex(Float.POSITIVE_INFINITY));
      assertEquals(Integer.MIN_VALUE, zSettings.zToYIndex(Float.NEGATIVE_INFINITY));

      assertEquals(0, zSettings.zToY(zSettings.getMinZoomedZ()));
      assertEquals(zSettings.getHeight() / 4f, zSettings.zToY(zSettings.getZoomedZRange().fractionToValue(0.25f)));
      assertEquals(zSettings.getHeight(), zSettings.zToY(zSettings.getMaxZoomedZ()));
   }
}
