package no.imr.lsss.framework.config.application;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.BaseSystemFeatureService;
import no.imr.lsss.framework.ServiceCollection;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.FeatureService;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class PluginConfTest {
   private static final ServiceCollection SERVICES = new ServiceCollection(List.of(
         new BaseSystemFeatureService(),
         new TestFeatureService("OnByDefault", true),
         new TestFeatureService("OffByDefault", false)
   ));

   @Test
   void noPluginConfElement() {
      assertEquals(Set.of("OffByDefault"), PluginConf.getDeactivatedPlugins(SERVICES, null));
   }

   @Test
   void pluginConfElementWithoutParameters() {
      assertEquals(Set.of("OffByDefault"), PluginConf.getDeactivatedPlugins(SERVICES, createPluginConfElement()));
   }

   @Test
   void explicitValuesOverrideDefaults() {
      Element element = createPluginConfElement();
      Element parameters = element.element(ConfigurationUnit.XML_CONFIGURATION).addElement(ParameterCollection.XML_PARAMETERS);
      parameters.addElement("OnByDefault").addText("false");
      parameters.addElement("OffByDefault").addText("true");
      assertEquals(Set.of("OnByDefault"), PluginConf.getDeactivatedPlugins(SERVICES, element));
   }

   @Test
   void missingParameterKeepsDefault() {
      Element element = createPluginConfElement();
      Element parameters = element.element(ConfigurationUnit.XML_CONFIGURATION).addElement(ParameterCollection.XML_PARAMETERS);
      parameters.addElement("OnByDefault").addText("true");
      assertEquals(Set.of("OffByDefault"), PluginConf.getDeactivatedPlugins(SERVICES, element));
   }

   private static Element createPluginConfElement() {
      Element element = DocumentHelper.createElement(ConfigurationUnit.XML_UNIT)
            .addAttribute(ConfigurationUnit.XML_NAME, PluginConf.NAME.persistentName());
      element.addElement(ConfigurationUnit.XML_CONFIGURATION);
      return element;
   }

   private static final class TestFeatureService extends FeatureService {
      private final boolean enabledByDefault;

      private TestFeatureService(String name, boolean enabledByDefault) {
         super(new Name(name));
         this.enabledByDefault = enabledByDefault;
      }

      @Override
      public boolean isEnabledByDefault() {
         return enabledByDefault;
      }

      @Override
      public FeaturePlugin createPlugin(LSSS lsss) {
         throw new UnsupportedOperationException();
      }
   }
}
