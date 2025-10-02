package no.imr.lsss.framework.config.application;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.BaseSystemFeatureService;
import no.imr.lsss.framework.ServiceCollection;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.tools.Utils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;
import org.dom4j.Element;

import javax.swing.JComponent;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class PluginConf extends ConfigurationUnit {
   public static final Name NAME = new Name("PluginConf", "Plugins");

   private final List<BooleanParameter> pluginsEnabled;

   PluginConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, NAME, "Selection of which plugins to use");

      pluginsEnabled = toPluginEnabled(getLSSS().getLsssConfig().serviceCollection);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return pluginsEnabled;
   }

   @Override
   public boolean hasConfigurationComponent() {
      return !pluginsEnabled.isEmpty();
   }

   @Override
   public JComponent getComponent() {
      return GuiUtils.createScrollPane(
            createInfoComponent("""
                  <h2>Plugins</h2>
                  <p>Changes take effect on restart.</p>
                  """),
            createParameterEditor().getEditorComponent());
   }

   public static Set<String> getDeactivatedPlugins(ServiceCollection serviceCollection, Element element) {
      List<BooleanParameter> pluginsEnabled = toPluginEnabled(serviceCollection);
      Element parametersElement = element.element(XML_CONFIGURATION).element(ParameterCollection.XML_PARAMETERS);
      if (parametersElement != null) {
         new ParameterCollection(pluginsEnabled).fromXml(parametersElement);
      }
      return pluginsEnabled.stream()
            .filter(Predicate.not(BooleanParameter::getBooleanValue))
            .map(BooleanParameter::getPersistentName)
            .collect(Collectors.toSet());
   }

   private static List<BooleanParameter> toPluginEnabled(ServiceCollection serviceCollection) {
      return serviceCollection.getAllFeatureServices().stream()
            .filter(service -> !(service instanceof BaseSystemFeatureService))
            .map(service -> {
               BooleanParameter parameter = new BooleanParameter(service.getName(), true);
               SvgIcon icon = service.getIcon();
               parameter.setProperty(BaseParameter.KEY_ICON, Optional.of(icon != null ? icon : MiscIcons.EMPTY));
               return parameter;
            })
            .sorted(Utils.comparingIgnoringCase(BooleanParameter::getDisplayName))
            .toList();
   }
}
