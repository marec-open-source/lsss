package no.imr.korona.computation.plugin;

import no.imr.tools.compile.CompileException;
import no.imr.tools.xml.XmlUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

final class PluginModuleTest {
   @Test
   void examples() throws CompileException {
      assertTrue(Example.EXAMPLES.size() > 1);
      for (Example example : Example.EXAMPLES) {
         PluginModule.compile(example.getImplementation());
      }
   }

   @Test
   void xmlDescriptionToComment() throws IOException {
      PluginModule pluginModule = new PluginModule();
      assertEquals("", pluginModule.comment.getValue());
      String xml = """
            <module>
               <parameters>
                  <parameter name='Description'>Some text</parameter>
               </parameters>
            </module>
            """;
      pluginModule.fromXml(XmlUtils.readDocument(xml).getRootElement());
      assertEquals("Some text", pluginModule.comment.getValue());
   }
}
