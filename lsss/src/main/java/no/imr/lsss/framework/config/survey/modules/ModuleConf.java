package no.imr.lsss.framework.config.survey.modules;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.tools.Utils;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.VerticalScrollablePanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * For configuring the modules.
 */
public final class ModuleConf extends ConfigurationUnit {
   public ModuleConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("ModuleConf", "Modules"),
            "Modules and overlays with configurable settings");
   }

   @Override
   public void setup() {
      super.setup();

      getLSSS().getModuleManager().getModules().stream()
            .filter(module -> !(module instanceof BaseModuleOverlay))
            .filter(BaseLsssModule::isConfigurable)
            .sorted(Utils.comparingIgnoringCase(BaseLsssModule::getDisplayName))
            .map(ModuleConfigurationUnit::new)
            .forEach(this::addSubConfigurationUnit);
   }

   public Optional<ModuleConfigurationUnit> moduleToConfigurationUnit(BaseLsssModule module) {
      return getAllUnitsRecursively(ModuleConfigurationUnit.class)
            .filter(unit -> unit.getModule() == module)
            .findFirst();
   }

   @Override
   public JComponent getComponent() {
      List<ConfigurationUnit> addedUnits = new ArrayList<>();

      HtmlStringBuilder text = new HtmlStringBuilder().html("<h2>Modules</h2>");
      addToList(text, getSubUnits(), addedUnits);

      JTextPane textPane = createInfoComponent(text.toString());
      GuiUtils.addHrefListener(textPane, href -> {
         int i = Integer.parseInt(href);
         getConfigurationManager().showDialog(addedUnits.get(i));
      });

      JPanel panel = VerticalScrollablePanel.wrap(textPane);
      panel.setBorder(GuiUtils.DEFAULT_MARGIN);

      return new JScrollPane(panel);
   }

   private static void addToList(HtmlStringBuilder text, List<ConfigurationUnit> unitsToAdd, List<ConfigurationUnit> addedUnits) {
      if (unitsToAdd.isEmpty()) {
         return;
      }
      text.html("<ul>");
      for (ConfigurationUnit configurationUnit : unitsToAdd) {
         text.html("<li><a href=" + addedUnits.size() + ">").text(configurationUnit.getDisplayName()).html("</a>")
               .html("<span style='color: gray'> - ").text(configurationUnit.getDescription()).html("</span></li>");
         addedUnits.add(configurationUnit);
         addToList(text, configurationUnit.getSubUnits(), addedUnits);
      }
      text.html("</ul>");
   }
}
