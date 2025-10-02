package no.imr.lsss.framework.config.survey.modules;

import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.tools.Utils;
import no.imr.tools.help.HelpID;
import org.dom4j.Element;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

/**
 * The configuration of one module.
 */
public final class ModuleConfigurationUnit extends ConfigurationUnit {
   private final BaseLsssModule module;

   ModuleConfigurationUnit(BaseLsssModule module) {
      super(module.getPlugin(), module.getName(), module.getDescription());

      this.module = module;

      if (module instanceof BaseOverlaidModule<?> overlaidModule) {
         overlaidModule.userVisibleForegroundOverlays()
               .filter(BaseLsssModule::isConfigurable)
               .sorted(Utils.comparingIgnoringCase(BaseLsssModule::getDisplayName))
               .map(ModuleConfigurationUnit::new)
               .forEach(this::addSubConfigurationUnit);
      }
   }

   @Override
   public HelpID getHelpID() {
      return module.getHelpID();
   }

   @Override
   public JComponent getComponent() {
      JComponent editor = module.createConfigurationEditor();
      if (editor == null) {
         return new JScrollPane(new JLabel("No displayable configuration for " + module.getDisplayName() + ".", JLabel.CENTER));
      }
      return editor;
   }

   @Override
   public void addToConfigurationXml(Element configurationElement) {
      configurationElement.add(module.toXml());
   }

   @Override
   public void fromConfigurationXml(Element configurationElement) {
      Element element = configurationElement.elements().getFirst();
      module.fromXml(element);
   }

   public BaseLsssModule getModule() {
      return module;
   }
}
