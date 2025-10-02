package no.imr.korona.data.datagrams;

import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.tools.parameter.Name;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class KoronaDatagramPluginTest {
   @Test
   void types() {
      KoronaDatagramPlugin plugin = new KoronaDatagramPlugin(new Name("x"));

      List<DatagramType> datagramTypes = plugin.getDatagramTypes();
      assertEquals(datagramTypes.size(), new HashSet<>(datagramTypes).size());

      List<DatagramSubType> datagramSubTypes = plugin.getSubDatagramTypes();
      assertEquals(datagramSubTypes.size(), new HashSet<>(datagramSubTypes).size());
   }
}
