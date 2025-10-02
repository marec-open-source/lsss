package no.imr.tools.xml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class JaxbUtilsTest {
   @Test
   void validFloatAttribute() throws IOException {
      String xml = """
            <data x="0.8"></data>
            """;
      assertEquals(0.8, JaxbUtils.read(Data.class, xml).x);
   }

   @Test
   void invalidFloatAttribute() {
      String xml = """
            <data x="0,8"></data>
            """;
      assertThrows(IOException.class, () -> JaxbUtils.read(Data.class, xml));
   }

   @XmlRootElement(name = "data")
   static final class Data {
      @XmlAttribute
      public double x;
   }
}
