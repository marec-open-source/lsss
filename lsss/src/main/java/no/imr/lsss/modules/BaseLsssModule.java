package no.imr.lsss.modules;

import com.google.common.base.Suppliers;
import no.imr.korona.region.RegionManager;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.framework.config.ConfigurationManager;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.concurrent.ConcurrentObject;
import no.imr.tools.help.HelpID;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for LSSS modules.
 */
public abstract sealed class BaseLsssModule extends ConcurrentObject implements ParameterContainer
      permits BaseDataModule, BaseModuleOverlay, BaseViewModule {
   /**
    * For modules that need to store more than parameters.
    */
   protected static final String XML_MODULE = "module";

   private final FeaturePlugin plugin;
   private final Name name;
   private final String description;

   BaseLsssModule(ModuleInfo<?> moduleInfo) {
      super(moduleInfo.plugin().getLSSS().getInterpretationSettings().createObservingSerialExecutor());

      plugin = moduleInfo.plugin();
      name = moduleInfo.name();
      description = moduleInfo.description();
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of();
   }

   public FeaturePlugin getPlugin() {
      return plugin;
   }

   public void close() {
   }

   public Name getName() {
      return name;
   }

   /**
    * Returns the name of this module used for presentation to the user.
    *
    * @return the name presented to the user
    */
   public String getDisplayName() {
      return name.displayName();
   }

   /**
    * Returns the persistent name of this module.
    * The persistent name is used for identification in serializations.
    *
    * @return the persistent name used for serialization
    */
   public String getPersistentName() {
      return name.persistentName();
   }

   public String getDescription() {
      return description;
   }

   public HelpID getHelpID() {
      return plugin.getHelpSet().createHelpID(getPersistentName());
   }

   @Override
   public String toString() {
      return getDisplayName();
   }

   public JMenuItem createConfigureMenuItem() {
      JMenuItem configureItem = MiscIcons.SETTINGS.on(new JMenuItem("Configure " + getDisplayName() + "..."));
      configureItem.addActionListener(_ -> getConfigurationManager().showDialog(this));
      return configureItem;
   }

   public boolean isConfigurable() {
      return false;
   }

   /**
    * Returns a swing component for configuring this module.
    *
    * @return a swing component or {@code null}
    */
   public @Nullable JComponent createConfigurationEditor() {
      List<? extends BaseParameter<?>> parameters = getConfigurationEditorParameters();
      if (parameters.isEmpty()) {
         return null;
      }
      ParameterEditor parameterEditor = new ParameterEditor(parameters);
      parameterEditor.getGUIConfig().setHorizontalFill(true);

      List<Component> components = new ArrayList<>();
      String infoText = getInfoText();
      if (infoText != null) {
         components.add(ConfigurationUnit.createInfoComponent(infoText));
      }
      components.add(parameterEditor.getEditorComponent());
      JScrollPane scrollPane = GuiUtils.createScrollPane(components);
      parameterEditor.addPopupMenuMouseListener(scrollPane);
      return scrollPane;
   }

   protected List<? extends BaseParameter<?>> getConfigurationEditorParameters() {
      return getParameters();
   }

   public @Nullable String getInfoText() {
      return null;
   }

   public Configurable getConfigurable() {
      return new ParameterCollection(this);
   }

   public Element toXml() {
      return getConfigurable().toXml();
   }

   public void fromXml(Element element) {
      getConfigurable().fromXml(element);
   }

   public void stressTestValidation() {
   }

   //----

   public LSSS getLSSS() {
      return plugin.getLSSS();
   }

   protected ConfigurationManager getConfigurationManager() {
      return getLSSS().getConfigurationManager();
   }

   protected ModuleManager getModuleManager() {
      return getLSSS().getModuleManager();
   }

   protected RegionManager getRegionManager() {
      return getLSSS().getRegionManager();
   }

   protected InterpretationSettings getInterpretationSettings() {
      return getLSSS().getInterpretationSettings();
   }

   protected <M extends BaseLsssModule> Supplier<M> moduleSupplier(Class<M> clazz) {
      return Suppliers.memoize(() -> getModuleManager().getModule(clazz));
   }
}
