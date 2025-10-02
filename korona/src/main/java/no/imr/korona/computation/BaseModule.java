package no.imr.korona.computation;

import no.imr.korona.data.ping.PingSource;
import no.imr.tools.help.HelpID;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.input.GUIConfig;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Base class for all computation modules.
 */
public abstract sealed class BaseModule implements ParameterContainer
      permits GeneralPingModule, SimplePingModule, ConcurrentPingModule {

   private static final Set<ModuleInfo> DEPRECATED_MODULE_INFOS = new CopyOnWriteArraySet<>();

   public final BooleanParameter active = new BooleanParameter(new Name("Active"),
         true,
         "If the module is not active, then all datagrams pass unaltered through it");

   public final StringParameter comment = new StringParameter(new Name("Comment"));

   private @Nullable ModuleContainer moduleContainer;
   private @Nullable ModuleInfo moduleInfo;

   BaseModule() {
      comment.setProperty(BaseParameter.KEY_LEFT_ALIGNED, true);
      comment.setProperty(BaseParameter.KEY_HORIZONTAL_FILL, true);
      comment.setProperty(BaseParameter.KEY_COMBINE_INPUT_AND_DESCRIPTION, true);
      comment.addListenerAndNotify(text -> {
         comment.setPersistable(!text.isBlank());
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment
            //---
      );
   }

   public ModuleInfo getModuleInfo() {
      ModuleInfo moduleInfo = this.moduleInfo;
      if (moduleInfo != null) {
         return moduleInfo;
      }
      for (ModuleInfo info : getModuleContainer().getKorona().getModuleManager().getModuleInfos()) {
         if (info.moduleClass().equals(getClass())) {
            setModuleInfo(info);
            return info;
         }
      }
      throw new IllegalStateException("No module info for " + getClass().getName());
   }

   void setModuleInfo(ModuleInfo moduleInfo) {
      this.moduleInfo = moduleInfo;

      if (moduleInfo.isDeprecated() && DEPRECATED_MODULE_INFOS.add(moduleInfo)) {
         Log.global.warning("Module " + getDisplayName() + " is deprecated and will be removed in a future version.");
      }
   }

   public @Nullable BaseModuleComputation doConfigure(ComputationContext computationContext, PingSource pingSource) throws IOException {
      if (!active.getBooleanValue()) {
         return null;
      }
      checkConfigFileSettings();
      try {
         return createComputation(computationContext, pingSource);
      } catch (IgnoreModuleComputationException e) {
         return null;
      }
   }

   private void checkConfigFileSettings() throws ConfigFileSettingsException {
      for (Name name : getRequiredConfigFileServiceNames()) {
         FileParameter fileParameter = getModuleContainer().getConfigFileSettings().getOptionalFileParameter(name);
         if (fileParameter == null) {
            throw new ConfigFileSettingsException(this, name, "Config file \"" + name.displayName() + "\" is not available in context");
         }
         Path file = fileParameter.getFile();
         if (file == null) {
            throw new ConfigFileSettingsException(this, name, "Config file \"" + name.displayName() + "\" is not specified");
         }
         if (!Files.exists(file)) {
            throw new ConfigFileSettingsException(this, name, file, "Does not exist");
         }
      }
   }

   public abstract @Nullable BaseModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException, IgnoreModuleComputationException;

   protected void configureWithoutData() {
   }

   public String getPersistentName() {
      return getModuleInfo().getPersistentName();
   }

   public String getDisplayName() {
      return getModuleInfo().getDisplayName();
   }

   public HelpID getHelpID() {
      return getModuleInfo().getHelpID();
   }

   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of();
   }

   public List<Name> getOptionalConfigFileServiceNames() {
      return List.of();
   }

   public @Nullable Path getOptionalConfigFile(Name name) {
      assert getOptionalConfigFileServiceNames().contains(name) : name;
      return getModuleContainer().getConfigFileSettings().getFile(name);
   }

   public Path getRequiredConfigFile(Name name) {
      assert getRequiredConfigFileServiceNames().contains(name) : name;
      Path file = getModuleContainer().getConfigFileSettings().getFile(name);
      if (file == null) {
         throw new IllegalArgumentException(name.persistentName());
      }
      return file;
   }

   public ModuleContainer getModuleContainer() {
      ModuleContainer moduleContainer = this.moduleContainer;
      if (moduleContainer == null) {
         throw new IllegalStateException("No module container for " + getClass().getName());
      }
      return moduleContainer;
   }

   public void setModuleContainer(ModuleContainer moduleContainer) {
      this.moduleContainer = moduleContainer;
   }

   public Element toXml() {
      Element element = DocumentHelper.createElement(ModuleList.XML_MODULE)
            .addAttribute(ModuleList.XML_NAME, getPersistentName());
      element.add(new ParameterCollection(this).toXml());
      return element;
   }

   public void fromXml(Element element) {
      comment.setValue("");
      Element parametersElement = element.element(ParameterCollection.XML_PARAMETERS);
      if (parametersElement != null) {
         new ParameterCollection(this).fromXml(parametersElement);
      }
   }

   public void customizeGUIConfig(GUIConfig guiConfig) {
   }

   public void runSmokeTest() throws Exception {
   }
}
